package com.example.blockchain.mining

import android.util.Log
import com.example.blockchain.core.BlockchainEngine
import com.example.blockchain.core.Consensus
import com.example.blockchain.crypto.CryptoEngine
import com.example.blockchain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.SecureRandom

/**
 * Android-friendly Background Mining Engine for Takium.
 * Features:
 * 1. Active thread mining of candidate blocks.
 * 2. CPU Safe Mode (battery protection, throttles mining thread).
 * 3. Real-time Hashrate monitoring.
 * 4. Merkle Root transaction aggregation.
 */
class MiningEngine(
    private val blockchainEngine: BlockchainEngine,
    private val scope: CoroutineScope
) {
    private val _isMining = MutableStateFlow(false)
    val isMining: StateFlow<Boolean> = _isMining

    private val _hashRate = MutableStateFlow(0L) // Hashrate in hashes/sec (e.g. 15,200 H/s)
    val hashRate: StateFlow<Long> = _hashRate

    private val _batterySafeMode = MutableStateFlow(true)
    val batterySafeMode: StateFlow<Boolean> = _batterySafeMode

    private val _minedBlocksCount = MutableStateFlow(0)
    val minedBlocksCount: StateFlow<Int> = _minedBlocksCount

    private var miningJob: Job? = null

    /**
     * Starts background mining.
     */
    fun startMining(minerAddress: String) {
        if (_isMining.value) return
        _isMining.value = true

        miningJob = scope.launch(Dispatchers.Default) {
            val random = SecureRandom()
            var hashesChecked = 0L
            var lastRateCheckTime = System.currentTimeMillis()

            while (isActive && _isMining.value) {
                // Fetch latest blockchain height and previous block hash
                val blocks = blockchainEngine.blocks.value
                val lastBlock = blocks.lastOrNull()
                val nextHeight = (lastBlock?.header?.index ?: -1L) + 1L
                val previousHash = lastBlock?.hash ?: "0000000000000000000000000000000000000000000000000000000000000000"
                val difficulty = blockchainEngine.currentDifficulty.value

                // 1. Prepare coinbase transaction (block reward)
                val coinbaseReward = Consensus.getBlockReward(nextHeight)
                val coinbaseTx = Transaction(
                    txId = "", // Filled after calculation
                    inputs = listOf(
                        TxInput(
                            prevTxId = "00000000000000000000000000000000",
                            prevOutputIndex = -1,
                            signature = "Mined by Takium Mobile Node",
                            senderPubKey = "TakiumMobileMiner"
                        )
                    ),
                    outputs = listOf(
                        TxOutput(recipientAddress = minerAddress, amount = coinbaseReward)
                    ),
                    timestamp = System.currentTimeMillis(),
                    fee = 0.0
                )
                val coinbaseWithId = coinbaseTx.copy(txId = coinbaseTx.calculateId())

                // 2. Fetch unconfirmed transactions from Mempool
                val mempoolTxs = blockchainEngine.mempool.value
                val blockTransactions = listOf(coinbaseWithId) + mempoolTxs

                // 3. Calculate Merkle Root
                val txIds = blockTransactions.map { it.txId }
                val merkleRoot = calculateMerkleRoot(txIds)

                // 4. Mine Block Nonce
                val timestamp = System.currentTimeMillis()
                val baseHeader = BlockHeader(
                    index = nextHeight,
                    timestamp = timestamp,
                    previousHash = previousHash,
                    merkleRoot = merkleRoot,
                    nonce = 0L,
                    difficulty = difficulty
                )

                var nonce = random.nextLong()
                var foundBlock = false

                // Window iteration to allow yielding thread and tracking hashrate accurately
                for (step in 0 until 1000) {
                    if (!isActive || !_isMining.value) break

                    val candidateBlock = Block(
                        header = baseHeader.copy(nonce = nonce),
                        hash = "",
                        transactions = blockTransactions
                    )
                    val computedHash = candidateBlock.calculateHash()
                    hashesChecked++

                    if (Consensus.verifyDifficulty(computedHash, difficulty)) {
                        // Found valid Proof-of-Work!
                        val minedBlock = candidateBlock.copy(hash = computedHash)
                        val success = blockchainEngine.addMinedBlock(minedBlock)
                        if (success) {
                            _minedBlocksCount.value += 1
                            foundBlock = true
                            Log.d("Takium", "Successfully mined block #$nextHeight with hash $computedHash")
                        }
                        break
                    }
                    nonce++
                }

                // Calculate real-time hashrate
                val now = System.currentTimeMillis()
                val elapsed = now - lastRateCheckTime
                if (elapsed >= 1000) {
                    val rate = (hashesChecked * 1000) / elapsed
                    _hashRate.value = rate
                    hashesChecked = 0
                    lastRateCheckTime = now
                }

                if (foundBlock) {
                    // Let the node rest briefly
                    delay(2000)
                }

                // Battery Safe throttle to avoid CPU overheating
                if (_batterySafeMode.value) {
                    delay(80) // Rest sleep to keep thermal levels low
                } else {
                    yield() // Simply yield to other coroutines
                }
            }
            _hashRate.value = 0L
        }
    }

    /**
     * Stops the mining process.
     */
    fun stopMining() {
        _isMining.value = false
        _hashRate.value = 0L
        miningJob?.cancel()
        miningJob = null
    }

    fun setBatterySafeMode(enabled: Boolean) {
        _batterySafeMode.value = enabled
    }

    /**
     * Builds standard Merkle tree path from list of transaction hashes.
     */
    private fun calculateMerkleRoot(txIds: List<String>): String {
        if (txIds.isEmpty()) return CryptoEngine.sha256("")
        var currentList = txIds.toMutableList()
        while (currentList.size > 1) {
            val nextList = mutableListOf<String>()
            for (i in 0 until currentList.size step 2) {
                val left = currentList[i]
                val right = if (i + 1 < currentList.size) currentList[i + 1] else left
                nextList.add(CryptoEngine.sha256(left + right))
            }
            currentList = nextList
        }
        return currentList[0]
    }
}
