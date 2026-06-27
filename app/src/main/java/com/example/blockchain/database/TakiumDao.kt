package com.example.blockchain.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TakiumDao {

    // Blocks
    @Query("SELECT * FROM blocks ORDER BY blockIndex DESC")
    fun getAllBlocksFlow(): Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocks ORDER BY blockIndex DESC")
    suspend fun getAllBlocks(): List<BlockEntity>

    @Query("SELECT * FROM blocks WHERE blockHash = :hash LIMIT 1")
    suspend fun getBlockByHash(hash: String): BlockEntity?

    @Query("SELECT * FROM blocks WHERE blockIndex = :index LIMIT 1")
    suspend fun getBlockByIndex(index: Long): BlockEntity?

    @Query("SELECT COUNT(*) FROM blocks")
    fun getBlockCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocks")
    suspend fun getBlockCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: BlockEntity)

    @Query("DELETE FROM blocks")
    suspend fun clearBlocks()

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE blockHash IS NULL ORDER BY timestamp DESC")
    fun getMempoolTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE blockHash IS NULL ORDER BY timestamp DESC")
    suspend fun getMempoolTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE txId = :txId")
    suspend fun deleteTransaction(txId: String)

    @Query("UPDATE transactions SET blockHash = :blockHash WHERE txId = :txId")
    suspend fun confirmTransaction(txId: String, blockHash: String)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    // Peers
    @Query("SELECT * FROM peers ORDER BY lastSeen DESC")
    fun getAllPeersFlow(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers")
    suspend fun getAllPeers(): List<PeerEntity>

    @Query("SELECT * FROM peers WHERE isOnline = 1")
    suspend fun getOnlinePeers(): List<PeerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: PeerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeers(peers: List<PeerEntity>)

    @Query("DELETE FROM peers WHERE peerId = :peerId")
    suspend fun deletePeer(peerId: String)

    @Query("UPDATE peers SET isOnline = :isOnline, lastSeen = :lastSeen WHERE peerId = :peerId")
    suspend fun updatePeerStatus(peerId: String, isOnline: Boolean, lastSeen: Long)

    // Settings
    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)
}
