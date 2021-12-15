// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.walletlink.dtos.*
import com.coinbase.walletlink.models.RequestMethod
import org.junit.Assert
import org.junit.Test

class JsonRPCSerializableTests {

    @Test
    fun testJsonRPCRequestDAppPermissionDataDTO() {
        val id = "1"
        val appName = "CS690 Team 15 DApp"
        val appLogoUrl = "https://app.compound.finance/images/compound-192.png"
        val origin = "https://www.usfca.edu"

        val dto = JsonRPCRequestDAppPermissionDataDTO(
            id = id, // the id for identify order of process to call method, since the response is async
            request = JsonRPCRequestDAppPermissionDataDTO.Request(
                method = "requestEthereumAccounts",  // The RPC method name
                params = JsonRPCRequestDAppPermissionDataDTO.Params(
                    appName,  // The app name showing on the wallet
                    appLogoUrl // The logo url for showing on the wallet
                )
            ),
            origin = origin // the url of dapp, should use the same 'origin' field in one socket connection.
        )

        val json = convertToJson(dto)
        val expected = """
            {"type":"WEB3_REQUEST","id":"1","request":{"method":"requestEthereumAccounts","params":{"appName":"CS690 Team 15 DApp","appLogoUrl":"https://app.compound.finance/images/compound-192.png"}},"origin":"https://www.usfca.edu"}
        """.trimIndent()

        Assert.assertEquals(expected, json)
    }

    @Test
    fun testJsonRPCRequestPersonalDataDTO() {
        val id = "13a09f7199d39999"
        val address = "0x568d46f6a798cd75a9beb60a8f57879043a69c3b"
        val addPrefix = false
        val typedDataJson = "{\"test\" : \"String\"}"
        val origin = "https://www.usfca.edu"
        val inputString = "hello"
        val dto = JsonRPCRequestPersonalDataDTO(
            id = id,  // the id for identify order of process to call method, since the response is async
            request = Web3RequestPersonalData(
                method = RequestMethod.SignEthereumMessage, // The RPC method name
                params = SignEthereumMessageParamsRPC(
                    inputString, // The custom string
                    address, // The wallet address
                    addPrefix, // The add prefix for input string
                    typedDataJson // The type of params using on smart contract
                )
            ),
            origin = origin // the url of dapp, should use the same 'origin' field in one socket connection.
        )


        val json = convertToJson(dto)
        val expected = """{"type":"WEB3_REQUEST","id":"13a09f7199d39999","request":{"method":"signEthereumMessage","params":{"message":"hello","address":"0x568d46f6a798cd75a9beb60a8f57879043a69c3b","addPrefix":false,"typedDataJson":"{\"test\" : \"String\"}"}},"origin":"https://www.usfca.edu"}""".trimIndent()
        Assert.assertEquals(expected, json)
    }

    @Test
    fun testJsonRPCRequestTypedDataDTO() {
        val id = "13a09f7199d39999"
        val fromAddress = "0x568d46f6a798cd75a9beb60a8f57879043a69999"
        val toAddress = "0xadAe4A6d32e91aF731d17AD5e63FD8629c4DF784"
        val weiValue = "100000000000000000"
        val jsonData = "transaction"
        val nonce = 1
        val gasPriceInWei = "0"
        val gasLimit = "0"
        val chainId = 3
        val shouldSubmit = true
        val origin = "https://www.usfca.edu"
        val dto = JsonRPCRequestTypedDataDTO(
            id = id,  // the id for identify order of process to call method, since the response is async
            request = Web3RequestTypedData(
                method = RequestMethod.SignEthereumTransaction, // The RPC method name
                params = SignEthereumTransactionParamsRPC(
                    fromAddress, // Wallet address
                    toAddress, // Smart contract address
                    weiValue, // The smallest unit of cryptocurrency
                    jsonData, // The value of params using on smart contract
                    nonce, // Random number to avoid Replay Attacks of transaction
                    gasPriceInWei, // The price paid for a transaction.
                    gasLimit, // The maximum price in one transaction
                    chainId, // Blockchain Id - 3 is test network
                    shouldSubmit // Whether should submit or not for a transaction
                )
            ),
            origin = origin // the url of dapp, should use the same 'origin' field in one socket connection.
        )


        val json = convertToJson(dto)
        val expected = """
            {"type":"WEB3_REQUEST","id":"13a09f7199d39999","request":{"method":"signEthereumTransaction","params":{"fromAddress":"0x568d46f6a798cd75a9beb60a8f57879043a69999","toAddress":"0xadAe4A6d32e91aF731d17AD5e63FD8629c4DF784","weiValue":"100000000000000000","data":"transaction","nonce":1,"gasPriceInWei":"0","gasLimit":"0","chainId":3,"shouldSubmit":true}},"origin":"https://www.usfca.edu"}
            """.trimIndent()
        Assert.assertEquals(expected, json)
    }

    @Test
    fun testJsonRPCRequestTransactionDataDTO() {
        val id = "13a09f7199d39999"
        val signedTransaction = "d34baaa2edc3c91a63ded45d0c7fe34edabfe24e0487338c52420a042e544350d4c3501aba9b49da3f3288b9b02d63d97c2a3171bd8feff5f66d2426ab6d76488a37a07045d6fb94b3dbb44a21f3ac5f52de5179c0f8471e052c2c86d54408156de8aa6f938ae55eadbebbcc46238fb97e2edb7b20c12a7ac1fa34da05e2450064ad394b88fce08e3145feccae02d4be3d26c64d7318789dd102ff5904bde0658c2cb11e24ad40775c43cd065eeb9bc2615b686b6bcfd1fed3fcd994ffaf999295579d4f9294cef1a4b620025f62b27b7f632378e2fe632f16c01c793432b89fb8a83d417560afb1554ff09d30cbf33303596c37b4ce04886a40ac81624007b222447ebcc2ecd41c3bd4446798029a44f1bc6c1b8b25238d6471df1ce022999aac75da9f505c711a933c9a169fc3372edbe4c00364424aed7282a1008399fabe873e79f6048217fdd18d3a0daa8462e2a6fc6bff31b95dfbfc4be2a1d301a89c758b1c7192ac0adf29b625c6b7b503b1bff4f94d2fcee19fab31243189a43a5bbb8f1c0da9fd84b838ec658f94a43ec4dbba1987d4b410ea0f"
        val chainId = 3
        val origin = "https://www.usfca.edu"
        val dto = JsonRPCRequestTransactionDataDTO(id = id, request = Web3RequestTransactionData(method = RequestMethod.SubmitEthereumTransaction, params = SubmitEthereumTransactionParamsRPC(
            signedTransaction,
            chainId
        )
        ), origin = origin)

        val json = convertToJson(dto)
        val expected = """
            {"type":"WEB3_REQUEST","id":"13a09f7199d39999","request":{"method":"submitEthereumTransaction","params":{"signedTransaction":"d34baaa2edc3c91a63ded45d0c7fe34edabfe24e0487338c52420a042e544350d4c3501aba9b49da3f3288b9b02d63d97c2a3171bd8feff5f66d2426ab6d76488a37a07045d6fb94b3dbb44a21f3ac5f52de5179c0f8471e052c2c86d54408156de8aa6f938ae55eadbebbcc46238fb97e2edb7b20c12a7ac1fa34da05e2450064ad394b88fce08e3145feccae02d4be3d26c64d7318789dd102ff5904bde0658c2cb11e24ad40775c43cd065eeb9bc2615b686b6bcfd1fed3fcd994ffaf999295579d4f9294cef1a4b620025f62b27b7f632378e2fe632f16c01c793432b89fb8a83d417560afb1554ff09d30cbf33303596c37b4ce04886a40ac81624007b222447ebcc2ecd41c3bd4446798029a44f1bc6c1b8b25238d6471df1ce022999aac75da9f505c711a933c9a169fc3372edbe4c00364424aed7282a1008399fabe873e79f6048217fdd18d3a0daa8462e2a6fc6bff31b95dfbfc4be2a1d301a89c758b1c7192ac0adf29b625c6b7b503b1bff4f94d2fcee19fab31243189a43a5bbb8f1c0da9fd84b838ec658f94a43ec4dbba1987d4b410ea0f","chainId":3}},"origin":"https://www.usfca.edu"}
        """.trimIndent()
        Assert.assertEquals(expected, json)
    }

    @Test
    fun testJsonRPCRequestCancelDataDTO() {
        val id = "13a09f7199d39999"
        val origin = "https://www.usfca.edu"
        val dto = JsonRPCRequestCancelDataDTO(
            id = id,
            request = Web3RequestCancelData(
                method = RequestMethod.RequestCanceled
            ),
            origin = origin
        )

        val json = convertToJson(dto)
        val expected = """
            {"type":"WEB3_REQUEST","id":"13a09f7199d39999","request":{"method":"requestCanceled"},"origin":"https://www.usfca.edu"}
        """.trimIndent()
        Assert.assertEquals(expected, json)
    }


    private fun convertToJson(serializable: JsonSerializable): String {
        return serializable.asJsonString()
    }
}
