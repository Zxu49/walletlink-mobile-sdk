package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.wallet.core.util.JSON
import com.coinbase.walletlink.models.RequestMethod

data class JsonRPCRequestCancelDataDTO(
    val type: String = "WEB3_REQUEST",
    val id: String,
    val request: Web3RequestCancelData,
    val origin: String
) : JsonSerializable {
    override fun asJsonString(): String = JSON.toJsonString(this)
    companion object
}

data class Web3RequestCancelData(val method: RequestMethod)

