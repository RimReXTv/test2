package com.example.blockchain.database

import androidx.room.*
import com.example.blockchain.model.BlockHeader
import com.example.blockchain.model.Transaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "blocks")
data class BlockEntity(
    @PrimaryKey val blockHash: String,
    val blockIndex: Long,
    val timestamp: Long,
    val previousHash: String,
    val merkleRoot: String,
    val nonce: Long,
    val difficulty: Int,
    val transactionsJson: String // Serialized transactions List
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val txId: String,
    val blockHash: String?, // Null if in mempool
    val timestamp: Long,
    val inputsJson: String,
    val outputsJson: String,
    val fee: Double,
    val isCoinbase: Boolean,
    val amount: Double // Simple aggregate amount for display
)

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey val peerId: String,
    val ipAddress: String,
    val port: Int,
    val isFullNode: Boolean,
    val isOnline: Boolean,
    val lastSeen: Long
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

class Converters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromTransactionList(list: List<Transaction>): String {
        val type = Types.newParameterizedType(List::class.java, Transaction::class.java)
        val adapter = moshi.adapter<List<Transaction>>(type)
        return adapter.toJson(list) ?: "[]"
    }

    @TypeConverter
    fun toTransactionList(json: String): List<Transaction> {
        val type = Types.newParameterizedType(List::class.java, Transaction::class.java)
        val adapter = moshi.adapter<List<Transaction>>(type)
        return adapter.fromJson(json) ?: emptyList()
    }
}
