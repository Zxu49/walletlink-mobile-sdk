package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.wallet.core.util.JSON
import com.coinbase.walletlink.models.RequestMethod

data class JsonRPCRequestTransactionDataDTO(
    val type: String = "WEB3_REQUEST",
    val id: String,
    val request: Web3RequestTransactionData,
    val origin: String
) : JsonSerializable {
    override fun asJsonString(): String = JSON.toJsonString(this)
    companion object
}

data class Web3RequestTransactionData(val method: RequestMethod, val params: SubmitEthereumTransactionParamsRPC)

data class SubmitEthereumTransactionParamsRPC(val signedTransaction: String, val chainId: Int)
