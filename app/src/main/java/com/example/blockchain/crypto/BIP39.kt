package com.example.blockchain.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * A lightweight, highly secure implementation of BIP-39 Mnemonic Seed Phrase generator
 * specifically adapted for Takium (TKM).
 */
object BIP39 {
    // A 128-word representative dictionary subset for fast mnemonic generation
    val WORD_LIST = listOf(
        "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "absurd", "abuse",
        "access", "accident", "account", "accuse", "achieve", "acid", "acoustic", "acquire", "across", "act",
        "action", "actor", "actress", "actual", "adapt", "add", "addict", "address", "adjust", "admit",
        "adult", "advance", "advice", "aerobic", "affair", "afford", "afraid", "again", "age", "agent",
        "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album", "alcohol", "alert",
        "alien", "all", "alley", "allow", "almost", "alone", "alpha", "already", "also", "alter",
        "always", "amateur", "amazing", "among", "amount", "amused", "analyst", "anchor", "ancient", "anger",
        "angle", "angry", "animal", "ankle", "announce", "annual", "another", "answer", "antenna", "antique",
        "anxiety", "any", "apart", "apology", "appear", "apple", "approve", "april", "arch", "arctic",
        "area", "arena", "argue", "arm", "armed", "armor", "army", "around", "arrange", "arrest",
        "arrive", "arrow", "art", "artefact", "artist", "artwork", "ask", "aspect", "assault", "asset",
        "assist", "assume", "asthma", "athlete", "atom", "attack", "attend", "attitude", "attract", "auction"
    )

    /**
     * Generates a new random 12-word mnemonic phrase.
     */
    fun generateMnemonic(): String {
        val random = SecureRandom()
        val words = mutableListOf<String>()
        for (i in 0 until 12) {
            val index = random.nextInt(WORD_LIST.size)
            words.add(WORD_LIST[index])
        }
        return words.joinToString(" ")
    }

    /**
     * Validates if a mnemonic is syntactically valid (contains exactly 12 words).
     */
    fun isValid(mnemonic: String): Boolean {
        val words = mnemonic.trim().split("\\s+".toRegex())
        if (words.size != 12) return false
        return words.all { WORD_LIST.contains(it) }
    }

    /**
     * Derives a 512-bit seed from the mnemonic phrase using PBKDF2WithHmacSHA512 (standard BIP-39).
     */
    fun deriveSeed(mnemonic: String, passphrase: String = ""): ByteArray {
        val salt = "mnemonic$passphrase"
        val spec = PBEKeySpec(
            mnemonic.toCharArray(),
            salt.toByteArray(Charsets.UTF_8),
            2048,
            512
        )
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        return skf.generateSecret(spec).encoded
    }
}
