package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.util.JSON

data class HostWeb3ResponseDTO(
    val type: String = "WEB3_RESPONSE",
    val id: String,
    val response: HostWeb3Response
) {
    companion object {
        fun fromJson(json: ByteArray): HostWeb3ResponseDTO? = JSON.fromJsonString(String(json, Charsets.UTF_8))
    }
}

data class HostWeb3Response(val method: String, val result: String?, val errorMessage: String?) {
    companion object {
        fun fromJson(json: ByteArray): HostWeb3Response? = JSON.fromJsonString(String(json, Charsets.UTF_8))
    }
}
