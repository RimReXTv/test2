package com.example.blockchain.model

import com.example.blockchain.crypto.CryptoEngine
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TxInput(
    val prevTxId: String,
    val prevOutputIndex: Int,
    val signature: String = "",       // Signature verifying permission to spend
    val senderPubKey: String = ""    // Public key of the sender
)

@JsonClass(generateAdapter = true)
data class TxOutput(
    val recipientAddress: String,
    val amount: Double
)

@JsonClass(generateAdapter = true)
data class Transaction(
    val txId: String,
    val inputs: List<TxInput>,
    val outputs: List<TxOutput>,
    val timestamp: Long = System.currentTimeMillis(),
    val fee: Double = 0.001
) {
    /**
     * Compute transaction ID using double SHA-256 of its contents.
     */
    fun calculateId(): String {
        val dataStr = inputs.joinToString { "${it.prevTxId}:${it.prevOutputIndex}" } +
                outputs.joinToString { "${it.recipientAddress}:${it.amount}" } +
                timestamp.toString() + fee.toString()
        return CryptoEngine.doubleSha256(dataStr)
    }

    /**
     * Returns true if it's a coinbase transaction (mining reward).
     */
    fun isCoinbase(): Boolean {
        return inputs.size == 1 && inputs[0].prevTxId == "00000000000000000000000000000000"
    }
}

@JsonClass(generateAdapter = true)
data class BlockHeader(
    val index: Long,
    val timestamp: Long,
    val previousHash: String,
    val merkleRoot: String,
    val nonce: Long,
    val difficulty: Int
)

@JsonClass(generateAdapter = true)
data class Block(
    val header: BlockHeader,
    val hash: String,
    val transactions: List<Transaction>
) {
    /**
     * Re-calculates block header hash to prevent modifications.
     */
    fun calculateHash(): String {
        val dataStr = header.index.toString() +
                header.timestamp.toString() +
                header.previousHash +
                header.merkleRoot +
                header.nonce.toString() +
                header.difficulty.toString()
        return CryptoEngine.doubleSha256(dataStr)
    }
}

data class UTXO(
    val txId: String,
    val outputIndex: Int,
    val address: String,
    val amount: Double
) {
    val key: String get() = "$txId:$outputIndex"
}
