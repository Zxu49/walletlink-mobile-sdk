package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.wallet.core.util.JSON
import com.coinbase.walletlink.models.RequestMethod

data class JsonRPCRequestTypedDataDTO(
    val type: String = "WEB3_REQUEST",
    val id: String,
    val request: Web3RequestTypedData,
    val origin: String
) : JsonSerializable {
    override fun asJsonString(): String = JSON.toJsonString(this)
    companion object
}

data class Web3RequestTypedData(val method: RequestMethod, val params: SignEthereumTransactionParamsRPC)

data class SignEthereumTransactionParamsRPC(
    val fromAddress: String,
    val toAddress: String?,
    val weiValue: String,
    val data: String,
    val nonce: Int?,
    val gasPriceInWei: String?,
    val gasLimit: String?,
    val chainId: Int,
    val shouldSubmit: Boolean
)
