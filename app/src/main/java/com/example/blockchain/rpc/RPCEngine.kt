package com.example.blockchain.rpc

import com.example.blockchain.core.BlockchainEngine
import com.example.blockchain.network.P2PNetwork
import com.example.blockchain.model.Block
import com.example.blockchain.model.Transaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Embedded RPC API Server Engine for Takium.
 * Exposes core protocol JSON-RPC methods:
 * - getbalance
 * - sendtransaction
 * - getblock
 * - getheight
 * - getpeerinfo
 */
class RPCEngine(
    private val blockchainEngine: BlockchainEngine,
    private val p2pNetwork: P2PNetwork
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    /**
     * Executes an RPC Command in simulated JSON-RPC format.
     * Returns the formatted JSON Response.
     */
    suspend fun handleRpcRequest(method: String, params: List<String>): String = withContext(Dispatchers.Default) {
        val responseMap = mutableMapOf<String, Any>()
        responseMap["jsonrpc"] = "2.0"
        responseMap["id"] = System.currentTimeMillis()

        try {
            when (method.lowercase()) {
                "getbalance" -> {
                    val address = params.getOrNull(0) ?: throw IllegalArgumentException("Address parameter is required")
                    val balance = blockchainEngine.getAddressBalance(address)
                    responseMap["result"] = mapOf("address" to address, "balance" to balance, "coin" to "TKM")
                }

                "getheight" -> {
                    val height = blockchainEngine.blocks.value.size.toLong()
                    responseMap["result"] = mapOf("height" to height)
                }

                "getblock" -> {
                    val blockParam = params.getOrNull(0) ?: throw IllegalArgumentException("Block index or hash is required")
                    val block: Block? = if (blockParam.all { it.isDigit() }) {
                        val index = blockParam.toLong()
                        blockchainEngine.blocks.value.find { it.header.index == index }
                    } else {
                        blockchainEngine.blocks.value.find { it.hash == blockParam }
                    }

                    if (block != null) {
                        responseMap["result"] = block
                    } else {
                        responseMap["error"] = mapOf("code" to -32602, "message" to "Block not found")
                    }
                }

                "sendtransaction" -> {
                    // Expects a simple json transaction string or serialized parameters
                    val txJson = params.getOrNull(0) ?: throw IllegalArgumentException("Transaction payload parameter required")
                    val adapter = moshi.adapter(Transaction::class.java)
                    val tx = adapter.fromJson(txJson)

                    if (tx != null) {
                        val success = blockchainEngine.submitTransaction(tx)
                        responseMap["result"] = mapOf("txId" to tx.txId, "accepted" to success)
                    } else {
                        responseMap["error"] = mapOf("code" to -32602, "message" to "Invalid transaction formatting")
                    }
                }

                "getpeerinfo" -> {
                    val peers = p2pNetwork.activePeers.value
                    responseMap["result"] = peers.map { peer ->
                        mapOf(
                            "peerId" to peer.peerId,
                            "address" to "${peer.ipAddress}:${peer.port}",
                            "isFullNode" to peer.isFullNode,
                            "isOnline" to peer.isOnline,
                            "lastSeen" to peer.lastSeen
                        )
                    }
                }

                else -> {
                    responseMap["error"] = mapOf("code" to -32601, "message" to "Method not found")
                }
            }
        } catch (e: Exception) {
            responseMap["error"] = mapOf("code" to -32603, "message" to (e.message ?: "Internal RPC error"))
        }

        val jsonAdapter = moshi.adapter<Map<*, *>>(Map::class.java)
        jsonAdapter.toJson(responseMap) ?: "{}"
    }
}
