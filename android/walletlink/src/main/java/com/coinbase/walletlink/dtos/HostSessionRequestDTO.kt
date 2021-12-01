package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.util.JSON

data class HostSessionRequestDTO (
    val type: String,
    val id: Int,
    val sessionId: String,
    val sessionKey: String
) {
    @ExperimentalUnsignedTypes
    fun asJsonString(): String = JSON.toJsonString(this)

    companion object {
        @ExperimentalUnsignedTypes
        fun fromJsonString(jsonString: String): HostSessionRequestDTO? = JSON.fromJsonString(jsonString)
    }
}
