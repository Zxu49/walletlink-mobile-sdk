# Build Instructions

[WalletLink](https://github.com/CoinbaseWallet/walletlink) is an open protocol that lets users connect their mobile wallets to your DApp.

With the WalletLink SDK, your mobile wallet will be able to interact with DApps on the desktop and be able to sign web3 transactions and messages.

[Our Team Fork](https://github.com/Zxu49/walletlink-mobile-sdk) has expanded the Android part SDK on this basis, so that the SDK can not only allow to support mobile wallet, but also could support DApp side to send web3 transactions and message to wallets.

## How to use our SDK

### Installation

**Prerequisite:**

* An Android project under a Git repository,  you project should be a git repo before use [git submoudle](https://git-scm.com/book/en/v2/Git-Tools-Submodules) command.
* This SDK uses [git submodule](https://git-scm.com/book/en/v2/Git-Tools-Submodules) to combine different components for relatively independent development, they may recursively relate on some [core library](https://github.com/Zxu49/CBCore). Make sure your SDK is kept up-to-date before using.
* Lastest Android studio arctic fox may not be able to automatically import submodules, you can choose to manually import submodules or use other versions of android studio. See [issues](https://stackoverflow.com/questions/68862846/i-cant-import-module-from-source-the-finish-button-is-off)

#### 1. Add WalletLink as a submodule

```
# In your android repo path, add our forked WalletLink as a submodule
$ git submodule add git@github.com:Zxu49/walletlink-mobile-sdk.git
```

#### 2. Update the submodule

```
# You may find the directorie of libiray of walletlink empty, init the submodule and update
$ git submodule update --init --recursive
```

#### 3. Import the module in your Android Project

In android studio, go to **File menu**, choose -> **New** ->  **Import module**, select **walletlink/android** folder as module. If button is off after selected, see [issues](https://stackoverflow.com/questions/68862846/i-cant-import-module-from-source-the-finish-button-is-off).

#### [Optional] 4. Upate you the submodule in your repo

You may need to update the submoudle before using.

```
# Use it to update after you first time init the submodule.
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

```
val walletLink = WalletLink(notificationUrl, context)
```



1. Genearted sercet and session id into QR code, and established websocket to bridge server

```
walletLink.sendHostSessionRequest(sessionID, secret)
```

```
walletLink.sendStartTransaction(data,sessionID,secret)
```

```
walletLink.sendSignPersonalData(data, sessionID, secret)

```
```
walletLink.sendCancel(data,sessionID,secret)

```
