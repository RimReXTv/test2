package com.example.blockchain.core

import android.content.Context
import android.util.Log
import com.example.blockchain.database.AppDatabase
import com.example.blockchain.database.BlockEntity
import com.example.blockchain.database.SettingEntity
import com.example.blockchain.database.TransactionEntity
import com.example.blockchain.database.TakiumDao
import com.example.blockchain.crypto.CryptoEngine
import com.example.blockchain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Core Blockchain manager for Takium (TKM).
 * Handles: Ledger management, UTXO calculation, Block processing,
 * Mempool caching, Genesis block creation, and Database persistence.
 */
class BlockchainEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val database: AppDatabase = AppDatabase.getDatabase(context)
    val dao: TakiumDao = database.takiumDao()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // In-memory representation of the active blockchain
    private val _blocks = MutableStateFlow<List<Block>>(emptyList())
    val blocks: StateFlow<List<Block>> = _blocks

    // In-memory UTXO Pool to quickly verify transaction state and calculate balances
    private val utxoPool = mutableMapOf<String, UTXO>()

    // Unconfirmed transactions (Mempool)
    private val _mempool = MutableStateFlow<List<Transaction>>(emptyList())
    val mempool: StateFlow<List<Transaction>> = _mempool

    // General status info
    private val _currentDifficulty = MutableStateFlow(2) // Prefix matching: e.g. "00" (fast & secure for interactive app)
    val currentDifficulty: StateFlow<Int> = _currentDifficulty

    private val _syncState = MutableStateFlow("Idle")
    val syncState: StateFlow<String> = _syncState

    init {
        scope.launch(Dispatchers.IO) {
            initializeLedger()
        }
    }

    /**
     * Initializes the ledger from the database, creating the Genesis Block if empty.
     */
    private suspend fun initializeLedger() {
        val count = dao.getBlockCount()
        if (count == 0) {
            Log.d("Takium", "Database empty. Generating Genesis Block.")
            val genesis = createGenesisBlock()
            saveBlockToDb(genesis)
        }
        loadLedgerFromDb()
    }

    /**
     * Constructs the hardcoded Genesis block with Takeshi Shin's dedicated message.
     */
    private fun createGenesisBlock(): Block {
        val genesisMsg = "Bitcoin philosophy was peer-to-peer electronic cash, but people started using it as digital gold. We created Takium to solve this issue and complete the missing decentralized cash utility."
        val genesisTx = Transaction(
            txId = "00000000000000000000000000000000",
            inputs = listOf(
                TxInput(
                    prevTxId = "00000000000000000000000000000000",
                    prevOutputIndex = -1,
                    signature = genesisMsg,
                    senderPubKey = "TakeshiShin"
                )
            ),
            outputs = listOf(
                TxOutput(
                    recipientAddress = "TKM_TakeshiGenesisWalletAddressFirstNode",
                    amount = 50.0
                )
            ),
            timestamp = 1780000000000L, // Custom fixed timestamp
            fee = 0.0
        )

        val header = BlockHeader(
            index = 0,
            timestamp = 1780000000000L,
            previousHash = "0000000000000000000000000000000000000000000000000000000000000000",
            merkleRoot = CryptoEngine.sha256(genesisTx.txId),
            nonce = 42,
            difficulty = 2
        )

        // Find correct genesis hash for difficulty 2
        var nonce = 0L
        var finalHash = ""
        while (true) {
            val candidateHeader = header.copy(nonce = nonce)
            val candidateBlock = Block(candidateHeader, "", listOf(genesisTx))
            val hash = candidateBlock.calculateHash()
            if (Consensus.verifyDifficulty(hash, 2)) {
                finalHash = hash
                break
            }
            nonce++
        }

        val finalizedHeader = header.copy(nonce = nonce)
        return Block(finalizedHeader, finalHash, listOf(genesisTx))
    }

    /**
     * Loads ledger history and rebuilds UTXO pool.
     */
    suspend fun loadLedgerFromDb() {
        val entities = dao.getAllBlocks()
        val blockList = entities.sortedBy { it.blockIndex }.map { entity ->
            val adapter = moshi.adapter(BlockHeader::class.java)
            val header = BlockHeader(
                index = entity.blockIndex,
                timestamp = entity.timestamp,
                previousHash = entity.previousHash,
                merkleRoot = entity.merkleRoot,
                nonce = entity.nonce,
                difficulty = entity.difficulty
            )
            val txAdapter = moshi.adapter<List<Transaction>>(
                Types.newParameterizedType(List::class.java, Transaction::class.java)
            )
            val txs = txAdapter.fromJson(entity.transactionsJson) ?: emptyList()
            Block(header, entity.blockHash, txs)
        }

        withContext(Dispatchers.Main) {
            _blocks.value = blockList
            rebuildUtxoPool(blockList)
            updateDifficulty(blockList)
        }

        // Also load unconfirmed txs (mempool)
        val mempoolEntities = dao.getMempoolTransactions()
        val mempoolTxs = mempoolEntities.map { entity ->
            val txAdapter = moshi.adapter<List<TxInput>>(
                Types.newParameterizedType(List::class.java, TxInput::class.java)
            )
            val outAdapter = moshi.adapter<List<TxOutput>>(
                Types.newParameterizedType(List::class.java, TxOutput::class.java)
            )
            Transaction(
                txId = entity.txId,
                inputs = txAdapter.fromJson(entity.inputsJson) ?: emptyList(),
                outputs = outAdapter.fromJson(entity.outputsJson) ?: emptyList(),
                timestamp = entity.timestamp,
                fee = entity.fee
            )
        }
        withContext(Dispatchers.Main) {
            _mempool.value = mempoolTxs
        }
    }

    private fun updateDifficulty(blockList: List<Block>) {
        if (blockList.size < Consensus.DIFFICULTY_ADJUSTMENT_INTERVAL) {
            _currentDifficulty.value = 2 // Baseline
            return
        }
        // Calculate new difficulty every window
        val lastBlock = blockList.last()
        val windowIndex = (blockList.size - Consensus.DIFFICULTY_ADJUSTMENT_INTERVAL).toInt()
        val windowBlock = blockList[windowIndex]
        val newDiff = Consensus.calculateNextDifficulty(
            lastBlock.header.timestamp,
            windowBlock.header.timestamp,
            lastBlock.header.difficulty
        )
        _currentDifficulty.value = newDiff
    }

    /**
     * Evaluates all blocks to construct the live unspent transaction output state.
     */
    private fun rebuildUtxoPool(blockList: List<Block>) {
        utxoPool.clear()
        for (block in blockList) {
            for (tx in block.transactions) {
                // Remove spent outputs
                for (input in tx.inputs) {
                    utxoPool.remove("${input.prevTxId}:${input.prevOutputIndex}")
                }
                // Add new outputs
                for ((index, output) in tx.outputs.withIndex()) {
                    val utxo = UTXO(
                        txId = tx.txId,
                        outputIndex = index,
                        address = output.recipientAddress,
                        amount = output.amount
                    )
                    utxoPool[utxo.key] = utxo
                }
            }
        }
    }

    /**
     * Calculates the balance of an address by scanning UTXO outputs.
     */
    fun getAddressBalance(address: String): Double {
        return utxoPool.values.filter { it.address == address }.sumOf { it.amount }
    }

    /**
     * Gets all spendable UTXOs for a specific address.
     */
    fun getAddressUtxos(address: String): List<UTXO> {
        return utxoPool.values.filter { it.address == address }
    }

    /**
     * Submits a transaction to the local memory and database mempool.
     */
    suspend fun submitTransaction(tx: Transaction): Boolean {
        // Validate transaction fee and basic inputs/outputs
        if (tx.inputs.isEmpty() && !tx.isCoinbase()) return false
        if (tx.outputs.isEmpty()) return false

        // Double spend validation inside Mempool
        val currentMempool = _mempool.value
        for (input in tx.inputs) {
            val key = "${input.prevTxId}:${input.prevOutputIndex}"
            if (!utxoPool.containsKey(key)) {
                return false // Output is already spent or does not exist!
            }
            // Ensure no transaction in the mempool is spending the same output
            val isDoubleSpentInMempool = currentMempool.any { mTx ->
                mTx.inputs.any { mInput -> mInput.prevTxId == input.prevTxId && mInput.prevOutputIndex == input.prevOutputIndex }
            }
            if (isDoubleSpentInMempool) {
                return false
            }
        }

        // Save transaction to DB as unconfirmed (mempool status)
        val moshiInputs = moshi.adapter<List<TxInput>>(
            Types.newParameterizedType(List::class.java, TxInput::class.java)
        )
        val moshiOutputs = moshi.adapter<List<TxOutput>>(
            Types.newParameterizedType(List::class.java, TxOutput::class.java)
        )

        val entity = TransactionEntity(
            txId = tx.txId,
            blockHash = null, // Mempool status
            timestamp = tx.timestamp,
            inputsJson = moshiInputs.toJson(tx.inputs),
            outputsJson = moshiOutputs.toJson(tx.outputs),
            fee = tx.fee,
            isCoinbase = tx.isCoinbase(),
            amount = tx.outputs.sumOf { it.amount }
        )

        dao.insertTransaction(entity)

        withContext(Dispatchers.Main) {
            _mempool.value = currentMempool + tx
        }
        return true
    }

    /**
     * Consolidates Mempool transactions and appends a newly mined block.
     */
    suspend fun addMinedBlock(block: Block): Boolean {
        val lastBlock = _blocks.value.lastOrNull()
        if (!Consensus.validateBlock(block, lastBlock, _currentDifficulty.value)) {
            Log.e("Takium", "Invalid mined block received.")
            return false
        }

        saveBlockToDb(block)

        // Clear confirmed transactions from database and mempool flow
        for (tx in block.transactions) {
            dao.confirmTransaction(tx.txId, block.hash)
        }

        loadLedgerFromDb()
        return true
    }

    private suspend fun saveBlockToDb(block: Block) {
        val txAdapter = moshi.adapter<List<Transaction>>(
            Types.newParameterizedType(List::class.java, Transaction::class.java)
        )
        val entity = BlockEntity(
            blockHash = block.hash,
            blockIndex = block.header.index,
            timestamp = block.header.timestamp,
            previousHash = block.header.previousHash,
            merkleRoot = block.header.merkleRoot,
            nonce = block.header.nonce,
            difficulty = block.header.difficulty,
            transactionsJson = txAdapter.toJson(block.transactions)
        )
        dao.insertBlock(entity)

        // Add confirmed transaction history records
        val moshiInputs = moshi.adapter<List<TxInput>>(
            Types.newParameterizedType(List::class.java, TxInput::class.java)
        )
        val moshiOutputs = moshi.adapter<List<TxOutput>>(
            Types.newParameterizedType(List::class.java, TxOutput::class.java)
        )

        for (tx in block.transactions) {
            val txEntity = TransactionEntity(
                txId = tx.txId,
                blockHash = block.hash,
                timestamp = tx.timestamp,
                inputsJson = moshiInputs.toJson(tx.inputs),
                outputsJson = moshiOutputs.toJson(tx.outputs),
                fee = tx.fee,
                isCoinbase = tx.isCoinbase(),
                amount = tx.outputs.sumOf { it.amount }
            )
            dao.insertTransaction(txEntity)
        }
    }

    /**
     * Resets the entire chain to Genesis for clean simulation.
     */
    suspend fun resetBlockchain() {
        dao.clearBlocks()
        dao.clearTransactions()
        initializeLedger()
    }
}
