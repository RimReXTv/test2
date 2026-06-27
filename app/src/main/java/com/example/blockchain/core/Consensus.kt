package com.example.blockchain.core

import com.example.blockchain.model.Block
import com.example.blockchain.model.Transaction
import java.security.MessageDigest

/**
 * Consensus Engine for the Takium (TKM) blockchain.
 * Implements Proof-of-Work, block verification, dynamic difficulty adjustment,
 * and block reward halving.
 */
object Consensus {
    const val INITIAL_BLOCK_REWARD = 50.0 // 50 TKM per block
    const val HALVING_INTERVAL = 100L // Halves every 100 blocks for clear presentation
    const val BLOCK_TIME_TARGET_SECONDS = 15 // Target 15 seconds per block for mobile/interactive pace
    const val DIFFICULTY_ADJUSTMENT_INTERVAL = 5L // Adjust difficulty every 5 blocks

    /**
     * Calculates the mining reward for a given block height, accounting for halving.
     */
    fun getBlockReward(blockHeight: Long): Double {
        val halvings = blockHeight / HALVING_INTERVAL
        if (halvings >= 64) return 0.0 // Avoid numeric underflow
        return INITIAL_BLOCK_REWARD / (1L shl halvings.toInt()).toDouble()
    }

    /**
     * Verifies if a given hash satisfies the current difficulty level.
     * The hash must have 'difficulty' number of leading '0' characters (hexadecimal representation).
     */
    fun verifyDifficulty(hash: String, difficulty: Int): Boolean {
        val targetPrefix = "0".repeat(difficulty)
        return hash.startsWith(targetPrefix)
    }

    /**
     * Determines the next difficulty level based on the time taken to mine the last window of blocks.
     * Keeps the block generation rate stable and safe.
     */
    fun calculateNextDifficulty(
        lastBlockTimestamp: Long,
        firstBlockInWindowTimestamp: Long,
        currentDifficulty: Int
    ): Int {
        val actualTimeTaken = (lastBlockTimestamp - firstBlockInWindowTimestamp) / 1000 // Convert to seconds
        val targetTimeTaken = BLOCK_TIME_TARGET_SECONDS * DIFFICULTY_ADJUSTMENT_INTERVAL

        // Prevent sudden massive difficulty swings (max +/- 1 change per adjustment window)
        val calculated = when {
            actualTimeTaken < targetTimeTaken / 2 -> currentDifficulty + 1 // Too fast, increase difficulty
            actualTimeTaken > targetTimeTaken * 2 -> Math.max(1, currentDifficulty - 1) // Too slow, reduce difficulty
            else -> currentDifficulty // Within acceptable range
        }
        // Limit range between 1 and 3 to keep mobile mining extremely fast and responsive
        return calculated.coerceIn(1, 3)
    }

    /**
     * Verifies the integrity of a Block structure.
     * Checks:
     * 1. Hash prefix meets difficulty target.
     * 2. Hash corresponds exactly to the headers.
     * 3. Coinbase transaction reward is correct.
     * 4. Double spending checks on transactions.
     */
    fun validateBlock(block: Block, previousBlock: Block?, expectedDifficulty: Int): Boolean {
        // 1. Verify difficulty
        if (!verifyDifficulty(block.hash, expectedDifficulty)) {
            return false
        }

        // 2. Verify hash calculation
        if (block.hash != block.calculateHash()) {
            return false
        }

        // 3. Verify previous hash pointer
        if (previousBlock != null && block.header.previousHash != previousBlock.hash) {
            return false
        }

        // 4. Verify Coinbase tx
        val coinbaseTx = block.transactions.firstOrNull { it.isCoinbase() } ?: return false
        val expectedReward = getBlockReward(block.header.index)
        val actualReward = coinbaseTx.outputs.sumOf { it.amount }
        if (actualReward > expectedReward + coinbaseTx.fee) {
            return false // Miner claimed more reward than allowed
        }

        return true
    }
}
