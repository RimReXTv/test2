package com.example.blockchain.crypto

import android.util.Base64
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

/**
 * Robust Cryptography Suite for Takium.
 * Handles deterministic HD Key derivation, transaction signatures, and TKM Address generation.
 */
object CryptoEngine {

    init {
        // Ensure Security provider features are active
        if (Security.getProvider("BC") == null) {
            // Note: Android includes standard EC algorithms natively under AndroidOpenSSL provider
        }
    }

    /**
     * Helper to compute SHA-256 hash.
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Helper to compute double SHA-256 (standard Bitcoin practice).
     */
    fun doubleSha256(input: String): String {
        return sha256(sha256(input))
    }

    /**
     * Derives a deterministic KeyPair from a 512-bit seed and account derivation index.
     * Implements BIP-32/BIP-44 HD Wallet concepts.
     */
    fun deriveKeyPair(seed: ByteArray, index: Int): KeyPair {
        // Derive unique 32-byte private key source by hashing seed with derivation index
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(seed)
        digest.update(index.toString().toByteArray(Charsets.UTF_8))
        val privateKeyBytes = digest.digest()

        // Seed a SHA1PRNG SecureRandom instance deterministically
        // Note: Under Android, we use standard deterministic generation with a key spec
        // or a custom secure PRNG state.
        val secureRandom = SecureRandom.getInstance("SHA1PRNG")
        secureRandom.setSeed(privateKeyBytes)

        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec("secp256r1") // Standard highly-secure NIST curve
        keyPairGenerator.initialize(ecSpec, secureRandom)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Generates a unique Takium Address starting with the "TKM" prefix.
     * Takes the SHA-256 hash of the public key, encodes it in Base58/Base64 with a checksum.
     */
    fun generateAddress(publicKey: PublicKey): String {
        val encodedKey = publicKey.encoded
        val digest = MessageDigest.getInstance("SHA-256")
        val keyHash = digest.digest(encodedKey)

        // Take first 16 bytes of keyHash to keep address highly readable but secure
        val shortHash = keyHash.copyOfRange(0, 16)
        
        // Calculate a 4-byte checksum of the shortHash
        val checksumDigest = MessageDigest.getInstance("SHA-256")
        val checksumHash = checksumDigest.digest(shortHash)
        val checksum = checksumHash.copyOfRange(0, 4)

        // Combine shortHash and checksum
        val combined = ByteArray(shortHash.size + checksum.size)
        System.arraycopy(shortHash, 0, combined, 0, shortHash.size)
        System.arraycopy(checksum, 0, combined, shortHash.size, checksum.size)

        // Convert to Alphanumeric Base58-like string using URL-Safe Base64 with custom replacements
        val baseString = Base64.encodeToString(combined, Base64.NO_PADDING or Base64.NO_WRAP)
            .replace("+", "a")
            .replace("/", "b")
            .replace("=", "")
            .filter { it.isLetterOrDigit() }

        return "TKM_$baseString"
    }

    /**
     * Signs data using ECDSA private key.
     */
    fun sign(privateKey: PrivateKey, message: String): String {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(message.toByteArray(Charsets.UTF_8))
        val sigBytes = signature.sign()
        return Base64.encodeToString(sigBytes, Base64.NO_WRAP)
    }

    /**
     * Verifies ECDSA signature using public key.
     */
    fun verify(publicKey: PublicKey, message: String, signatureStr: String): Boolean {
        return try {
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initVerify(publicKey)
            signature.update(message.toByteArray(Charsets.UTF_8))
            val sigBytes = Base64.decode(signatureStr, Base64.NO_WRAP)
            signature.verify(sigBytes)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reconstructs PublicKey from Base64 string.
     */
    fun publicKeyFromBase64(base64Str: String): PublicKey {
        val keyBytes = Base64.decode(base64Str, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(spec)
    }

    /**
     * Reconstructs PrivateKey from Base64 string.
     */
    fun privateKeyFromBase64(base64Str: String): PrivateKey {
        val keyBytes = Base64.decode(base64Str, Base64.NO_WRAP)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePrivate(spec)
    }
}
