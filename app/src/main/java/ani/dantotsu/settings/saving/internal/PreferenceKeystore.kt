package ani.dantotsu.settings.saving.internal

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec

//used to encrypt and decrypt json strings on import and export
class PreferenceKeystore {
    companion object {
        fun generateKey(alias: String) {
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setRandomizedEncryptionRequired(false)
                    .build()
            )
            keyGenerator.generateKey()
        }

        fun encryptWithPassword(
            password: CharArray,
            plaintext: String,
            salt: ByteArray
        ): ByteArray {
            val secretKey = deriveKeyFromPassword(password, salt)
            val cipher =
                Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(ByteArray(16)))
            return cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        }

        fun decryptWithPassword(
            password: CharArray,
            ciphertext: ByteArray,
            salt: ByteArray
        ): String {
            val secretKey = deriveKeyFromPassword(password, salt)
            val cipher =
                Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}")
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                IvParameterSpec(ByteArray(16))
            ) // Use the correct IV
            return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        }

        fun generateSalt(): ByteArray {
            val random = SecureRandom()
            val salt = ByteArray(16)
            random.nextBytes(salt)
            return salt
        }

        private fun deriveKeyFromPassword(password: CharArray, salt: ByteArray): SecretKey {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password, salt, 10000, 256)
            return factory.generateSecret(spec)
        }

    }
}