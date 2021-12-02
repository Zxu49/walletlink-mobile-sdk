// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.wallet.core.util.JSON
import com.coinbase.walletlink.models.RequestMethod
import com.squareup.moshi.Types
import java.net.URL

data class JsonRPCRequestPersonalDataDTO(
    val type: String = "WEB3_REQUEST",
    val id: String,
    val request: Web3RequestPersonalData,
    val origin: String
) : JsonSerializable {
    override fun asJsonString(): String = JSON.toJsonString(this)
    companion object
}

data class Web3RequestPersonalData(val method: RequestMethod, val params: SignEthereumMessageParamsRPC)

data class SignEthereumMessageParamsRPC(
    val message: String,
    val address: String,
    val addPrefix: Boolean,
    val typedDataJson: String?
)
