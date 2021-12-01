package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.util.JSON

data class GetSessionConfigRequestDTO (
    val type: String,
    val id: Int,
    val sessionId: String
) {
    @ExperimentalUnsignedTypes
    fun asJsonString(): String = JSON.toJsonString(this)

    companion object {
        @ExperimentalUnsignedTypes
        fun fromJsonString(jsonString: String): GetSessionConfigRequestDTO? = JSON.fromJsonString(jsonString)
    }
}
