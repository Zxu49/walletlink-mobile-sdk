// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink

import android.content.Context
import com.coinbase.wallet.core.extensions.asUnit
import com.coinbase.wallet.core.extensions.reduceIntoMap
import com.coinbase.wallet.core.extensions.unwrap
import com.coinbase.wallet.core.extensions.zipOrEmpty
import com.coinbase.wallet.core.util.BoundedSet
import com.coinbase.wallet.core.util.JSON
import com.coinbase.wallet.core.util.Optional
import com.coinbase.wallet.core.util.toOptional
import com.coinbase.wallet.crypto.extensions.decryptUsingAES256GCM
import com.coinbase.wallet.crypto.extensions.encryptUsingAES256GCM
import com.coinbase.walletlink.apis.WalletLinkConnection
import com.coinbase.walletlink.dtos.*
import com.coinbase.walletlink.dtos.PublishEventTestDTO
import com.coinbase.walletlink.exceptions.WalletLinkException
import com.coinbase.walletlink.models.*
import com.coinbase.walletlink.repositories.LinkRepository
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * WalletLink SDK interface
 *
 * @property notificationUrl Webhook URL used to push notifications to mobile client
 * @param context Android context
 */
class WalletLink(private val notificationUrl: URL, context: Context) : WalletLinkInterface {
    private val requestsSubject = PublishSubject.create<HostRequest>()
    private val requestsScheduler = Schedulers.single()
    private val processedRequestIds = BoundedSet<HostRequestId>(3000)
    private val linkRepository = LinkRepository(context)
    private val disposeBag = CompositeDisposable()
    private var connections = ConcurrentHashMap<URL, WalletLinkConnection>()
    // new
    private val serialScheduler = Schedulers.single()
    override val requestsObservable: Observable<HostRequest> = requestsSubject.hide()
    // add observable
    private val responseSubject = PublishSubject.create<String>()
    val responseObservable: Observable<String> = responseSubject.hide()
    // add address observable
    private val addressSubject = PublishSubject.create<String>()
    val addressObservable: Observable<String> = addressSubject.hide()

    override fun sessions(): List<Session> = linkRepository.sessions

    override fun observeSessions(): Observable<List<Session>> = linkRepository.observeSessions()

    override fun connect(userId: String, metadata: ConcurrentHashMap<ClientMetadataKey, String>) {
        val connections = ConcurrentHashMap<URL, WalletLinkConnection>()
        val sessionsByUrl = linkRepository.sessions.reduceIntoMap(HashMap<URL, List<Session>>()) { acc, session ->
            val sessions = acc[session.url]?.toMutableList()?.apply { add(session) }
            acc[session.url] = sessions?.toList() ?: mutableListOf(session)
        }

        for ((rpcUrl, sessions) in sessionsByUrl) {
            val conn = WalletLinkConnection(
                url = rpcUrl,
                userId = userId,
                notificationUrl = notificationUrl,
                linkRepository = linkRepository,
                metadata = metadata
            )
            observeConnection(conn)
            sessions.forEach { connections[it.url] = conn }
        }
        this.connections = connections
    }

    override fun disconnect() {
        disposeBag.clear()
        connections.values.forEach { it.disconnect() }
        connections.clear()
    }

    override fun link(
        sessionId: String,
        secret: String,
        version: String?,
        url: URL,
        userId: String,
        metadata: Map<ClientMetadataKey, String>
    ): Single<Unit> {
        connections[url]?.let { connection ->
            return connection.link(sessionId = sessionId, secret = secret, version = version)
        }

        val connection = WalletLinkConnection(
            url = url,
            userId = userId,
            notificationUrl = notificationUrl,
            linkRepository = linkRepository,
            metadata = metadata
        )

        connections[url] = connection

        return connection.link(sessionId = sessionId, secret = secret, version = version)
            .map { observeConnection(connection) }
            .doOnError { connections.remove(url) }
    }

    override fun unlink(session: Session): Single<Unit> {
        val connection = connections[session.url]
            ?: return Single.error(WalletLinkException.NoConnectionFound(session.url))
        return connection.destroySession(sessionId = session.id)
            .map { linkRepository.delete(url = session.url, sessionId = session.id) }
    }

    override fun setMetadata(key: ClientMetadataKey, value: String): Single<Unit> = connections.values
        .map { it.setMetadata(key = key, value = value).asUnit().onErrorReturn { Single.just(Unit) } }
        .zipOrEmpty()
        .asUnit()

    override fun approve(requestId: HostRequestId, signedData: ByteArray): Single<Unit> {
        val connection = connections[requestId.url] ?: return Single.error(
            WalletLinkException.NoConnectionFound(requestId.url)
        )

        return connection.approve(requestId, signedData)
    }

    override fun reject(requestId: HostRequestId): Single<Unit> {
        val connection = connections[requestId.url] ?: return Single.error(
            WalletLinkException.NoConnectionFound(requestId.url)
        )

        return connection.reject(requestId)
    }

    override fun markAsSeen(requestIds: List<HostRequestId>): Single<Unit> = requestIds
        .map { linkRepository.markAsSeen(it, it.url).onErrorReturn { Unit } }
        .zipOrEmpty()
        .asUnit()

    override fun getRequest(eventId: String, sessionId: String, url: URL): Single<HostRequest> {
        val session = linkRepository.getSession(sessionId, url)
            ?: return Single.error(WalletLinkException.SessionNotFound)

        return linkRepository.getPendingRequests(session)
            .map { requests ->
                requests.firstOrNull { eventId == it.hostRequestId.eventId } ?: throw WalletLinkException.EventNotFound
            }
    }

    // MARK: - Helpers

    private fun observeConnection(conn: WalletLinkConnection) {
        conn.requestsObservable
            .observeOn(requestsScheduler)
            .map { Optional(it) }
            .onErrorReturn { Optional(null) }
            .unwrap()
            .subscribe { request ->
                val hostRequestId = request.hostRequestId

                if (processedRequestIds.has(hostRequestId)) {
                    return@subscribe
                }

                processedRequestIds.add(hostRequestId)
                requestsSubject.onNext(request)
            }
            .addTo(disposeBag)

        conn.disconnectSessionObservable
            .observeOn(requestsScheduler)
            .map { request -> request.toOptional() }
            .onErrorReturn { null }
            .unwrap()
            .subscribeBy(onNext = { sessionId -> linkRepository.delete(url = conn.url, sessionId = sessionId) })
            .addTo(disposeBag)
    }

    /**
     * Create host session on DAPP (the 1st request)
     * @param data encrypted data with id, app name, url, origin
     * @param sessionID random generated session ID
     * @param secret random generated secret
     */
    override fun sendHostSessionRequest(data: String, sessionID : String, secret : String){
        WebsocketClient.startRequest()
        WebsocketClient.sendHostSessionMessage(sessionID,secret)
        WebsocketClient.responseObservable
            .observeOn(serialScheduler)
            .subscribe { processHostSessionResponse(data, it, sessionID, secret) }
            .addTo(disposeBag)
    }

    /**
     * Use WebSocket to send isLink request (the 2nd request)
     * @param sessionID session ID
     */
    private fun sendIsLinkedRequest(sessionID : String){
        WebsocketClient.sendIsLinkedMessage(sessionID)
    }

    /**
     * Use WebSocket to send session config request (the 3rd request)
     * @param sessionID session ID
     */
    private fun sendSessionConfig(sessionID : String){
        WebsocketClient.sendGetSessionConfigMessage(sessionID)
    }

    /**
     * Send the DAPP permission event
     * @param data encrypted data with id, app name, url, origin
     * @param sessionID session ID
     */
    private fun sendDAppPermissionDataEvent(data: String, sessionID : String){
        val event = PublishEventTestDTO(id = 4,  sessionId = sessionID, event = EventType.Web3Request, data = data)
        WebsocketClient.sendDataString(event.asJsonString())
    }

    /**
     * Send the sign personal data event
     * @param data encrypted data containing inputString, address, addPrefix, typedDataJson
     * @param sessionID session ID
     */
    fun sendSignPersonalData(data : String, sessionID: String) {
        sendSignPersonalDataEvent(data, sessionID)
    }

    /**
     * Private function for sending sign personal data event
     */
    private fun sendSignPersonalDataEvent(data: String, sessionID: String) {
        val event = PublishEventTestDTO(id = 5,  sessionId = sessionID, event = EventType.Web3Request, data = data)
        WebsocketClient.sendDataString(event.asJsonString())
    }

    /**
     * Send the sign typed data event
     * @param data encrypted data containing fromAddress, toAddress, weiValue, jsonData, nonce, gasPriceInWei, gasLimit, chainId, shouldSubmit
     * @param sessionID session ID
     */
    fun sendSignTypedData(data : String, sessionID: String){
        sendSignTypedDataEvent(data, sessionID)
    }

    /**
     * Private function for sending sign typed data event
     */
    private fun sendSignTypedDataEvent(data: String, sessionID: String) {
        val event = PublishEventTestDTO(id = 6,  sessionId = sessionID, event = EventType.Web3Request, data = data)
        WebsocketClient.sendDataString(event.asJsonString())
    }

    /**
     * Send the signed transaction to the wallet
     * @param data encrypted data containing signedTransaction, chainId
     * @param sessionID session ID
     */
    fun sendStartTransaction(data : String, sessionID: String) {
        sendStartTransactionEvent(data, sessionID)
    }

    /**
     * Private function for sending signed transaction
     */
    private fun sendStartTransactionEvent(data: String, sessionID: String) {
        val event = PublishEventTestDTO(id = 7,  sessionId = sessionID, event = EventType.Web3Request, data = data)
        WebsocketClient.sendDataString(event.asJsonString())
    }

    /**
     * Cancel a transaction
     * @param data encrypted data containing id and origin
     * @param sessionID session ID
     */
    fun sendCancel(data : String, sessionID: String) {
        sendCancelEvent(data, sessionID)
    }

    /**
     * Private function for canceling a transaction
     */
    private fun sendCancelEvent(data: String, sessionID: String) {
        val event = PublishEventTestDTO(id = 8,  sessionId = sessionID, event = EventType.Web3Request, data = data)
        WebsocketClient.sendDataString(event.asJsonString())
    }

    /**
     * Function for handling host session responses
     * @param data used for DApp permission
     * @param incoming incoming host session response
     * @param secret the random generated secret
     */
    private fun processHostSessionResponse(data: String, incoming : String ,sessionID: String, secret: String) {
        try{
            if (incoming == "Socket Closed") {
                responseSubject.onNext("Socket close in processHostSessionResponse")
            }
            val obj = JSONObject(incoming)

            when (obj.getString("type")) {
                "OK" -> sendIsLinkedRequest(sessionID)
                "IsLinkedOK" -> sendSessionConfig(sessionID)
                "GetSessionConfigOK" -> sendDAppPermissionDataEvent(data, sessionID)
                "PublishEventOK" -> {
                    WebsocketClient.requestsObservable
                        .observeOn(serialScheduler)
                        .subscribe { processIncomingData(it, secret) }
                        .addTo(disposeBag)
                }

            }
        } catch (e : WalletLinkException) {
            println(e)
        }
    }

    /**
     * Function for handling event responses
     * @param incoming response from the wallet
     * @param secret the random generated secret
     */
    private fun processIncomingData (incoming : String, secret: String) {
        try{
            val obj = JSONObject(incoming)
            val type = obj.getString("type")
            if (type == "Linked") {
                responseSubject.onNext("Wallet has connected!!")
            } else if (type == "Event") {
                val data = obj.getString("data")
                decryptAddress(data,secret)
                val x = decryptData(data, secret)
                val r = x?.response
                val e = r?.errorMessage
                if (e != "User rejected signature request") {
                    responseSubject.onNext("The wallet has approved your request!!!")
                } else {
                    responseSubject.onNext("The wallet has rejected your request, please try again")
                }
            }
        } catch (e : Exception) {
            println(e)
        }
    }

    /**
     * Function for fetching wallet address from the response of DApp permission
     * @param data the response of DApp permission request
     * @param secret the random generated secret
     */
    private fun decryptAddress(data : String, secret: String) {
        try {
            val x = data.decryptUsingAES256GCM(secret).toByteString()
            val xToUTF8 = JSONObject(x.string(Charsets.UTF_8))
            val response = xToUTF8.getJSONObject("response")
            val result = response.getJSONArray("result")
            val address = result.get(0).toString()
            addressSubject.onNext(address)
        } catch (e : Exception) {
            println(e)
        }
    }

    /**
     * Function for decrypting data using AES 256
     * @param data data to be decrypted
     * @param secret the random generated secret for decrypting data
     */
    private fun decryptData(data : String, secret: String): HostWeb3ResponseDTO? {
        val jsonString = data.decryptUsingAES256GCM(secret).toString(Charsets.UTF_8)
        return JSON.fromJsonString(jsonString)
    }
}
