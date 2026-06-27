package com.example.blockchain.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blockchain.core.BlockchainEngine
import com.example.blockchain.crypto.BIP39
import com.example.blockchain.crypto.CryptoEngine
import com.example.blockchain.database.SettingEntity
import com.example.blockchain.mining.MiningEngine
import com.example.blockchain.model.*
import com.example.blockchain.network.P2PNetwork
import com.example.blockchain.rpc.RPCEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyPair

/**
 * Android ViewModel coordinating all Takium decentralized components.
 * Exposes observable StateFlows to the Compose UI.
 */
class TakiumViewModel(application: Application) : AndroidViewModel(application) {

    val blockchainEngine = BlockchainEngine(application, viewModelScope)
    val p2pNetwork = P2PNetwork(application, blockchainEngine, viewModelScope)
    val miningEngine = MiningEngine(blockchainEngine, viewModelScope)
    val rpcEngine = RPCEngine(blockchainEngine, p2pNetwork)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // UI Navigation/Authentication State
    private val _isWalletLoaded = MutableStateFlow(false)
    val isWalletLoaded: StateFlow<Boolean> = _isWalletLoaded

    // Wallet State
    private val _mnemonic = MutableStateFlow("")
    val mnemonic: StateFlow<String> = _mnemonic

    private val _walletAddress = MutableStateFlow("")
    val walletAddress: StateFlow<String> = _walletAddress

    private var localKeyPair: KeyPair? = null

    // Balance (TKM)
    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance

    // Settings
    private val _nodeMode = MutableStateFlow("Lite Node") // Lite Node vs Full Node
    val nodeMode: StateFlow<String> = _nodeMode

    // Merchant Pay State
    private val _generatedQrCodeText = MutableStateFlow("")
    val generatedQrCodeText: StateFlow<String> = _generatedQrCodeText

    // RPC interactive terminal state
    private val _rpcCommand = MutableStateFlow("getbalance")
    val rpcCommand: StateFlow<String> = _rpcCommand

    private val _rpcParams = MutableStateFlow("")
    val rpcParams: StateFlow<String> = _rpcParams

    private val _rpcResponseText = MutableStateFlow("")
    val rpcResponseText: StateFlow<String> = _rpcResponseText

    init {
        // Automatically check if a seed phrase already exists in settings ("Cookie" persistence)
        viewModelScope.launch(Dispatchers.IO) {
            val savedSeedSetting = blockchainEngine.dao.getSetting("wallet_seed_phrase")
            val savedModeSetting = blockchainEngine.dao.getSetting("node_mode")

            if (savedSeedSetting != null && savedSeedSetting.value.isNotEmpty()) {
                loadWalletFromMnemonic(savedSeedSetting.value)
            }
            if (savedModeSetting != null) {
                withContext(Dispatchers.Main) {
                    _nodeMode.value = savedModeSetting.value
                }
            }

            // Start decentralized peer mesh network automatically
            p2pNetwork.startNetwork()

            // Observe blockchain state to automatically recalculate the wallet's balance
            blockchainEngine.blocks.collect {
                updateBalance()
            }
        }
    }

    /**
     * Generates a new high-entropy 12-word BIP-39 mnemonic phrase without saving or initializing yet.
     */
    fun generateNewMnemonic(): String {
        return BIP39.generateMnemonic()
    }

    /**
     * Completes setup by saving the confirmed seed phrase to database and loading the wallet.
     */
    fun completeWalletSetup(phrase: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blockchainEngine.dao.insertSetting(SettingEntity("wallet_seed_phrase", phrase))
            loadWalletFromMnemonic(phrase)
        }
    }

    /**
     * Instantiates a new high-entropy 12-word wallet, saving it to settings for persistence.
     */
    fun createNewWallet() {
        viewModelScope.launch(Dispatchers.IO) {
            val phrase = BIP39.generateMnemonic()
            blockchainEngine.dao.insertSetting(SettingEntity("wallet_seed_phrase", phrase))
            loadWalletFromMnemonic(phrase)
        }
    }

    /**
     * Imports an existing 12-word seed phrase. Returns true if successful.
     */
    fun importWallet(seedPhrase: String): Boolean {
        if (!BIP39.isValid(seedPhrase)) {
            return false
        }
        viewModelScope.launch(Dispatchers.IO) {
            blockchainEngine.dao.insertSetting(SettingEntity("wallet_seed_phrase", seedPhrase))
            loadWalletFromMnemonic(seedPhrase)
        }
        return true
    }

    /**
     * Internal loader which derives keys, address, and initializes local node identities.
     */
    private suspend fun loadWalletFromMnemonic(phrase: String) {
        val seedBytes = BIP39.deriveSeed(phrase)
        // Derive master KeyPair at BIP44 path (index 0)
        val keyPair = CryptoEngine.deriveKeyPair(seedBytes, 0)
        val address = CryptoEngine.generateAddress(keyPair.public)

        localKeyPair = keyPair
        p2pNetwork.initializeLocalPeerId(keyPair.public)

        withContext(Dispatchers.Main) {
            _mnemonic.value = phrase
            _walletAddress.value = address
            _isWalletLoaded.value = true
            updateBalance()
        }
    }

    /**
     * Computes the balance based on current block history.
     */
    fun updateBalance() {
        val address = _walletAddress.value
        if (address.isNotEmpty()) {
            _balance.value = blockchainEngine.getAddressBalance(address)
        }
    }

    /**
     * Deletes the persistent "Cookie" wallet seed and logs out.
     */
    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            miningEngine.stopMining()
            blockchainEngine.dao.insertSetting(SettingEntity("wallet_seed_phrase", ""))
            withContext(Dispatchers.Main) {
                _mnemonic.value = ""
                _walletAddress.value = ""
                _balance.value = 0.0
                _isWalletLoaded.value = false
                localKeyPair = null
            }
        }
    }

    /**
     * Returns the base64 encoded ECDSA Public Key.
     */
    fun getPublicKeyBase64(): String {
        val keyPair = localKeyPair ?: return ""
        return android.util.Base64.encodeToString(keyPair.public.encoded, android.util.Base64.NO_WRAP)
    }

    /**
     * Returns the base64 encoded ECDSA Private Key.
     */
    fun getPrivateKeyBase64(): String {
        val keyPair = localKeyPair ?: return ""
        return android.util.Base64.encodeToString(keyPair.private.encoded, android.util.Base64.NO_WRAP)
    }

    /**
     * Toggles Node storage capacity (Full node vs Lite node)
     */
    fun setNodeMode(mode: String) {
        _nodeMode.value = mode
        viewModelScope.launch(Dispatchers.IO) {
            blockchainEngine.dao.insertSetting(SettingEntity("node_mode", mode))
        }
    }

    /**
     * Initiates mobile phone CPU mining.
     */
    fun startMining() {
        val minerAddress = _walletAddress.value
        if (minerAddress.isNotEmpty()) {
            miningEngine.startMining(minerAddress)
        }
    }

    /**
     * Halts mobile mining.
     */
    fun stopMining() {
        miningEngine.stopMining()
    }

    /**
     * Creates a signed transaction sending TKM to a recipient.
     */
    fun sendTransaction(recipient: String, amount: Double, fee: Double = 0.001, onResult: (Boolean, String) -> Unit) {
        val senderAddr = _walletAddress.value
        val keyPair = localKeyPair

        if (senderAddr.isEmpty() || keyPair == null) {
            onResult(false, "Wallet is not authenticated.")
            return
        }

        if (amount <= 0) {
            onResult(false, "Amount must be greater than zero.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val spendableUtxos = blockchainEngine.getAddressUtxos(senderAddr)
            val totalAvailable = spendableUtxos.sumOf { it.amount }

            if (totalAvailable < amount + fee) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Insufficient balance. Available: $totalAvailable TKM, Needed: ${amount + fee} TKM")
                }
                return@launch
            }

            // Select inputs (greedy algorithm to collect enough UTXO values)
            var accumulated = 0.0
            val inputs = mutableListOf<TxInput>()
            for (utxo in spendableUtxos) {
                inputs.add(TxInput(prevTxId = utxo.txId, prevOutputIndex = utxo.outputIndex))
                accumulated += utxo.amount
                if (accumulated >= amount + fee) break
            }

            // Create outputs
            val outputs = mutableListOf<TxOutput>()
            outputs.add(TxOutput(recipientAddress = recipient, amount = amount))

            // Handle change output
            val change = accumulated - (amount + fee)
            if (change > 0.0) {
                outputs.add(TxOutput(recipientAddress = senderAddr, amount = change))
            }

            // Build base Transaction
            val baseTx = Transaction(
                txId = "",
                inputs = emptyList(), // Filled post-signing
                outputs = outputs,
                timestamp = System.currentTimeMillis(),
                fee = fee
            )

            // Calculate base ID for signing
            val txId = baseTx.calculateId()

            // Cryptographically Sign the Tx ID
            val pubKeyBase64 = android.util.Base64.encodeToString(keyPair.public.encoded, android.util.Base64.NO_WRAP)
            val signature = CryptoEngine.sign(keyPair.private, txId)

            // Populate fully signed inputs
            val signedInputs = inputs.map { input ->
                input.copy(signature = signature, senderPubKey = pubKeyBase64)
            }

            val finalizedTx = baseTx.copy(txId = txId, inputs = signedInputs)

            // Broadcast/Submit transaction to Mempool
            val success = blockchainEngine.submitTransaction(finalizedTx)
            withContext(Dispatchers.Main) {
                if (success) {
                    onResult(true, "Transaction published successfully! TxID: ${txId.take(12)}...")
                    updateBalance()
                } else {
                    onResult(false, "Failed to publish transaction. Rejected by Consensus Rules.")
                }
            }
        }
    }

    /**
     * Merchant Utility: Generates a payload structure representing a QR Code request.
     */
    fun generateMerchantPaymentQr(amount: Double) {
        val address = _walletAddress.value
        if (address.isEmpty()) return

        val requestMap = mapOf(
            "protocol" to "takium",
            "type" to "pay",
            "recipient" to address,
            "amount" to amount,
            "merchantName" to "Takium Merchant Mode"
        )
        val adapter = moshi.adapter<Map<*, *>>(Map::class.java)
        _generatedQrCodeText.value = adapter.toJson(requestMap) ?: ""
    }

    /**
     * Customer Utility: Executes a QR payment from a scanned QR text payload.
     */
    fun executeQrPayment(scannedPayload: String, onResult: (Boolean, String) -> Unit) {
        try {
            val adapter = moshi.adapter<Map<*, *>>(Map::class.java)
            val data = adapter.fromJson(scannedPayload) ?: throw IllegalArgumentException("Malformed QR")
            
            if (data["protocol"] != "takium" || data["type"] != "pay") {
                onResult(false, "Invalid QR protocol. Not a Takium Merchant QR Code.")
                return
            }

            val recipient = data["recipient"] as String
            val amount = (data["amount"] as? Double) ?: (data["amount"] as? String)?.toDoubleOrNull() ?: 0.0

            sendTransaction(recipient, amount, fee = 0.001, onResult)
        } catch (e: Exception) {
            onResult(false, "Error parsing QR code: ${e.message}")
        }
    }

    /**
     * Resets chain
     */
    fun resetChain() {
        viewModelScope.launch(Dispatchers.IO) {
            blockchainEngine.resetBlockchain()
            updateBalance()
        }
    }

    /**
     * Interactive RPC executor inside app settings/developer tab.
     */
    fun executeRpc() {
        viewModelScope.launch(Dispatchers.IO) {
            val method = _rpcCommand.value
            val paramsStr = _rpcParams.value
            val params = if (paramsStr.trim().isEmpty()) emptyList() else paramsStr.split(",").map { it.trim() }

            val response = rpcEngine.handleRpcRequest(method, params)
            withContext(Dispatchers.Main) {
                _rpcResponseText.value = response
            }
        }
    }

    fun setRpcCommand(cmd: String) {
        _rpcCommand.value = cmd
    }

    fun setRpcParams(params: String) {
        _rpcParams.value = params
    }
}
