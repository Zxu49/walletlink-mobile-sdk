package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.wallet.core.util.JSON
import com.squareup.moshi.Json

data class JsonRPCRequestDAppPermissionDataDTO (
    val type: String = "WEB3_REQUEST",
    val id: String,
    val request: Request,
    val origin: String
) : JsonSerializable {
    @ExperimentalUnsignedTypes
    override fun asJsonString(): String = JSON.toJsonString(this)

    companion object {
        @ExperimentalUnsignedTypes
        fun fromJsonString(jsonString: String): JsonRPCRequestDAppPermissionDataDTO? = JSON.fromJsonString(jsonString)
    }

    data class Request (
        val method: String,
        val params: Params
    )

    class Params (
        val appName: String,
        @Json(name = "appLogoUrl")
        val appLogoURL: String
    )
}
