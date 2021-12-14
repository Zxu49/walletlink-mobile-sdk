# Build Instructions

[WalletLink](https://github.com/CoinbaseWallet/walletlink) is an open protocol that lets users connect their mobile wallets to your DApp.

With the WalletLink SDK, your mobile wallet will be able to interact with DApps on the desktop and be able to sign web3 transactions and messages.

[Our Team Fork](https://github.com/Zxu49/walletlink-mobile-sdk) has expanded the Android part SDK on this basis, so that the SDK can not only support mobile wallets, but also could support DApp to send web3 transactions and messages to wallets.

## How to use our SDK

### Installation

**Prerequisite:**

* An Android project under a Git repository, your project should be a git repo before using the [git submodule](https://git-scm.com/book/en/v2/Git-Tools-Submodules) command.
* This SDK uses the [git submodule](https://git-scm.com/book/en/v2/Git-Tools-Submodules) to combine different components for relatively independent development; they may recursively relate to the same [core library](https://github.com/Zxu49/CBCore). Make sure your SDK is kept up-to-date before using.
* Latest Android studio arctic fox may not be able to automatically import submodules, you can choose to manually import submodules or use other versions of android studio. See [issues](https://stackoverflow.com/questions/68862846/i-cant-import-module-from-source-the-finish-button-is-off)

#### 1. Add WalletLink as a submodule

```
# In your android repo path, add our forked WalletLink as a submodule
$ git submodule add git@github.com:Zxu49/walletlink-mobile-sdk.git
```

#### 2. Update the submodule

```
# You may find the directory of library of wallet link empty, init the submodule and update
$ git submodule update --init --recursive
```

#### 3. Import the module in your Android Project

In android studio, go to **File menu**, choose -> **New** ->  **Import module**, select **walletlink/android** folder as module. If the finish button is off after selected, see [issues](https://stackoverflow.com/questions/68862846/i-cant-import-module-from-source-the-finish-button-is-off).

#### [Optional] 4. Upate you the submodule in your repo

You may need to update the submodule before using.

```
# Use it to update after your first time initializing the submodule.
$ git submodule update --remote --recursive
```

### Clean up

```
# Remove the submodule entry from .git/config
git submodule deinit -f path/to/submodule

# Remove the submodule directory from the superproject's .git/modules directory
rm -rf .git/modules/path/to/submodule

# Remove the entry in .gitmodules and remove the submodule directory located at path/to/submodule
git rm -f path/to/submodule
```

## Usage

### Wallet Side

1. Monitoring the network and Init the walletlink instance

```
    val intentFilter = IntentFilter().apply { addAction(ConnectivityManager.CONNECTIVITY_ACTION) }
    this.registerReceiver(Internet, intentFilter)
    Internet.startMonitoring()
```

```
    val walletLink = WalletLink(notificationUrl, context)
```

2. Link to bridge server, the session id and secret come from Dapp will generate a dapp instance and be subscribed on walletlink instance.

```kotlin
    // To pair the device with a browser after scanning WalletLink QR code
    walletLink.link(
        sessionId = sessionId,
        secret = secret,
        url = serverUrl,
        userId = userId,
        metadata = mapOf(ClientMetadataKey.EthereumAddress to wallet.primaryAddress)
    )
        .subscribeBy(onSuccess = {
    // New WalletLink connection was established
        }, onError = { error ->
    // Error while connecting to WalletLink server (walletlinkd)
        })
        .addTo(disposeBag)
```

3. Listen on incoming requests from the subscribed Dapp

```kotlin
    /*
    Listen on incoming requests, it should be requestsObservable not requests,
    the origin sdk readme is incorrect
    */
    walletLink.requestsObservable
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeBy(onNext = { request ->
    // New unseen request
        })
        .addTo(disposeBag)
```

4. The function is not provided in original branch

```kotlin
    // Approve DApp permission request (EIP-1102)
    walletLink.approveDappPermission(request.hostRequestId)
        .subscribeBy(onSuccess = {
    // Dapp received EIP-1102 approval
        }, onError = { error ->
    // Dapp failed to receive EIP-1102 approval
        })
        .addTo(disposeBag)
```

5. Approve or reject the request come from Dapp

```kotlin
    // Approve a given transaction/message signing request
    walletLink.approve(request.hostRequestId, signedData)
        .subscribeBy(onSuccess = {
    // Dapp received request approval
        }, onError = { error ->
    // Dapp failed to receive request approval
        })
        .addTo(disposeBag)


    // Reject transaction/message/EIP-1102 request
    walletLink.reject(request.hostRequestId)
        .subscribeBy(onSuccess = {
    // Dapp received request rejection
        }, onError = { error ->
    // Dapp failed to receive request rejection
        })
        .addTo(disposeBag)
```

### Dapp Side

Init the instance like wallet side
```
    val walletLink = WalletLink(notificationUrl, context)
```

1. Generated secret and session id into QR code, and established websocket to bridge server

```
    // This will connect to wallet link server using websocket
    // data is JSON RPC for establish host session, more info see our example format
    // session Id is 32 width randomly generated alphabet string 
    // secret is 64 width randomly generated alphabet string 

    walletLink.sendHostSessionRequest(data, sessionID, secret)
```

2. Using observable mode listing the response from connected socket

```
    walletLink.responseObservable
        .observeOn(serialScheduler)
        .subscribe { 
        // get socket response from socket
        }.addTo(disposeBag)
```
3. Using observable mode listing the address from connected socket

```
    walletLink.addressObservable
        .observeOn(serialScheduler)
        .subscribe {
        // get socket address from socket
        }
        .addTo(disposeBag)
```

4. Send personal string to bridge sever, then pass it to connected wallet  
```
    // data JsonRPCRequestPersonalDataDTO class encrypted by secret using AES
    val data = JSON.toJsonString(jsonRPC).encryptUsingAES256GCM(secret)
    walletLink.sendSignPersonalData(data, sessionID)
```

5. Send typed data (JSON-RPC) to bridge sever, then pass it to connected wallet  
```
    // data JsonRPCRequestTypedDataDTO class encrypted by secret using AES
    val data = JSON.toJsonString(jsonRPC).encryptUsingAES256GCM(secret)
    walletLink.sendSignTypedData(data, sessionID)
```

6. Submit the Transaction request to bridge sever, then pass it to connected wallet  

```
    // data JsonRPCRequestTransactionDataDTO class encrypted by secret using AES
    // the data will be wrapped inside the PublishEventDTO
    val data = JSON.toJsonString(jsonRPC).encryptUsingAES256GCM(secret)
    walletLink.sendStartTransaction(data,sessionID,secret)  
```

7. Cancel the previous the Transaction request to bridge sever, then pass it to connected wallet  

```
    // data JsonRPCRequestCancelDataDTO class encrpted by secret using AES
    // the data will be wrapped inside the PublishEventDTO
    val data = JSON.toJsonString(jsonRPC).encryptUsingAES256GCM(secret)
    walletLink.sendCancel(data,sessionID,secret)

```

### The DTO class (Data Transfer Object)

The DAPP and wallet will use several DTOs to transfer data. 
To extend the functionality of DApp, we create five JSON-RPC related DTO classes for easier transfer data. Here are some examples showing how to use these objects.

1. JsonRPCRequestDAppPermissionDataDTO

Request the connection permission of the wallet. 
If the wallet rejects, the socket should be disconnected.
If approved, the following DTO should use the same 'origin' field.

```
    val jsonRPC = JsonRPCRequestDAppPermissionDataDTO(
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
```

2. JsonRPCRequestPersonalDataDTO

For sending personal string to confirm the encryption method.

```
    val jsonRPC = JsonRPCRequestPersonalDataDTO(
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
```

3. JsonRPCRequestTypedDataDTO

For sending typed data to confirm the format of RPC is matching, also use for sending a real transaction request to wallet

```
    val jsonRPC = JsonRPCRequestTypedDataDTO(
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
```

4. JsonRPCRequestTransactionDataDTO

For submit a transaction given the signedTransaction to wallet

```
    val jsonRPC = JsonRPCRequestTransactionDataDTO(
        id = id,  // the id for identify order of process to call method, since the response is async
        request = Web3RequestTransactionData( // The RPC method name 
            method = RequestMethod.SubmitEthereumTransaction,
            params = SubmitEthereumTransactionParamsRPC(
                signedTransaction,  // String - Signature transaction data in hexadecimal format
                chainId  // Blockchain Id - 3 is test network
            )
        ), 
        origin = origin // the url of dapp, should use the same 'origin' field in one socket connection.
    )
```