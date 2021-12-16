import android.annotation.SuppressLint
import android.util.Log
import com.coinbase.wallet.core.util.JSON
import com.coinbase.wallet.crypto.extensions.sha256
import com.coinbase.walletlink.dtos.GetSessionConfigRequestDTO
import com.coinbase.walletlink.dtos.HostSessionRequestDTO
import com.coinbase.walletlink.models.HostRequest
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit
/**
 * A helper WebSocket class for handing DApp requests and responses
 */
object WebsocketClient {
    private const val NORMAL_CLOSURE_STATUS = 1000
    private var sClient: OkHttpClient? = null
    var sWebSocket: WebSocket? = null
    var listener : EchoWebSocketListener? = null
    var message : String = ""
    private val requestsSubject = PublishSubject.create<String>()
    val requestsObservable: Observable<String> = requestsSubject.hide()
    private val responseSubject = PublishSubject.create<String>()
    val responseObservable: Observable<String> = responseSubject.hide()
    private const val backendURL : String = "wss://www.walletlink.org/rpc"

    @Synchronized
    fun startRequest() {
        if (sClient == null) {
            sClient = OkHttpClient.Builder()
                .pingInterval(5, TimeUnit.SECONDS)
                .build()
        }
        if (sWebSocket == null) {
            val request = Request.Builder().url(backendURL).build()
            listener = EchoWebSocketListener()
            sWebSocket = sClient!!.newWebSocket(request, listener!!)
        }
    }

    @ExperimentalUnsignedTypes
    private fun sendHostSessionMessage(webSocket: WebSocket, sessionID: String, secret: String) {
        val sessionKey = "$sessionID, $secret WalletLink".sha256()
        println("sessionKey: $sessionKey")
        webSocket.send(JSON.toJsonString(HostSessionRequestDTO("HostSession",1,sessionID,sessionKey)))
    }

    private fun sendIsLinkedMessage(webSocket: WebSocket, sessionID: String) {
        webSocket.send("{\"type\":\"IsLinked\",\"id\":2,\"sessionId\":\"$sessionID\"}")
    }

    @ExperimentalUnsignedTypes
    private fun sendGetSessionConfigMessage(webSocket: WebSocket, sessionID: String) {
        webSocket.send(JSON.toJsonString(GetSessionConfigRequestDTO("GetSessionConfig",3,sessionID)))
    }

    private fun sendPublishEventMessage(webSocket: WebSocket, sessionID: String, data: String) {
        webSocket.send("{\"type\":\"PublishEvent\",\"id\":4,\"sessionId\":\"$sessionID\",\"event\":\"Web3Request\", \"data\":\"$data\",\"callWebhook\":true}")
    }

    private fun sendHeartBeatMessage(webSocket: WebSocket) {
        webSocket.send("h")
    }

    fun sendHostSessionMessage(sessionID:String, secret: String) {
        if (sWebSocket != null) {
            sendHostSessionMessage(sWebSocket!!,sessionID, secret)
        }
    }

    fun sendIsLinkedMessage(sessionID:String) {
        if (sWebSocket != null) {
            sendIsLinkedMessage(sWebSocket!!, sessionID)
        }
    }

    fun sendGetSessionConfigMessage(sessionID:String) {
        if (sWebSocket != null) {
            sendGetSessionConfigMessage(sWebSocket!!, sessionID)
        }
    }

    fun sendPublishEventMessage(sessionID : String, data : String) {
        if (sWebSocket != null) {
            sendPublishEventMessage(sWebSocket!!, sessionID, data)
        }
    }

    fun sendHeartBeatMessage() {
        if (sWebSocket != null) {
            sendHeartBeatMessage(sWebSocket!!)
        }
    }

    fun sendDataString(dataString : String) {
        if (sWebSocket != null) {
            sWebSocket?.send(dataString)
        }
    }


    @Synchronized
    fun closeWebSocket() {
        if (sWebSocket != null) {
            sWebSocket!!.close(NORMAL_CLOSURE_STATUS, "Goodbye!")
            sWebSocket = null
        }
    }

    private fun resetWebSocket() {
        synchronized(WebsocketClient::class.java) { sWebSocket = null }
    }

    open class EchoWebSocketListener : WebSocketListener() {
        @SuppressLint("LogNotTimber")
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.i(TAG, "Receiving: $text")
            if (text != "" && text != "h") {
                val obj = JSONObject(text)
                val type = obj.getString("type")
                if (type == "Event" || type == "Linked") {
                    requestsSubject.onNext(text)
                }
                if (type == "OK" || type == "IsLinkedOK" || type == "GetSessionConfigOK" || type == "PublishEventOK" ) {
                    responseSubject.onNext(text)
                }
            }
            message  = text
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.i(TAG, "Receiving: " + bytes.hex())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null)
            Log.i(TAG, "Closing: $code $reason")
            resetWebSocket()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "Closed: $code $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            t.printStackTrace()
            resetWebSocket()
            responseSubject.onNext("Socket Closed!")
        }

        companion object {
            private const val TAG = "EchoWebSocketListener"
        }
    }
}
