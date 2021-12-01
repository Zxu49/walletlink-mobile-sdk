package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.util.JSON
import com.coinbase.wallet.crypto.extensions.encryptUsingAES256GCM
import com.squareup.moshi.Json

data class JsonRPCRequestDTO (
    val type: String = "WEB3_REQUEST",
    val id: String,
    val request: Request,
    val origin: String
) {
    @ExperimentalUnsignedTypes
    fun asJsonString(): String = JSON.toJsonString(this)

    companion object {
        @ExperimentalUnsignedTypes
        fun fromJsonString(jsonString: String): JsonRPCRequestDTO? = JSON.fromJsonString(jsonString)
    }
}data class Request (
    val method: String,
    val params: Params
)

data class Params (
    val appName: String,

    @Json(name = "appLogoUrl")
    val appLogoURL: String
)
