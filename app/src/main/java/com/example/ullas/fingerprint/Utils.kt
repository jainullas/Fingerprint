package com.example.ullas.fingerprint

import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64

import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PrivateKey
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.security.spec.InvalidKeySpecException
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

import android.content.Context.FINGERPRINT_SERVICE
import androidx.annotation.Nullable


internal object Utils {

    private const val KEY_ALIAS = "FINGERPRINT_KEY_PAIR_ALIAS"
    private const val KEY_STORE = "AndroidKeyStore"
    private const val CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

    private var keyStore: KeyStore? = null
    private var keyPairGenerator: KeyPairGenerator? = null
    private var cipher: Cipher? = null

    val cryptoObject: FingerprintManager.CryptoObject?
        get() = if (initKeyStore() && initCipher() && initKey() && initCipherMode(Cipher.DECRYPT_MODE)) {
            FingerprintManager.CryptoObject(cipher)
        } else null


    fun checkSensorState(context: Context): Boolean {
        val fingerprintManager = context.getSystemService(FINGERPRINT_SERVICE) as FingerprintManager
        if (fingerprintManager.isHardwareDetected) {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            return keyguardManager.isKeyguardSecure && fingerprintManager.hasEnrolledFingerprints()
        } else
            return false
    }

    @Nullable
    fun encryptString(string: String): String? {
        try {
            if (initKeyStore() && initCipher() && initKey() && initCipherMode(Cipher.ENCRYPT_MODE)) {
                val bytes = cipher?.doFinal(string.toByteArray())
                return Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        }

        return null
    }

    @Nullable
    fun decryptString(string: String, cipher: Cipher): String? {
        try {
            val bytes = Base64.decode(string, Base64.NO_WRAP)
            return String(cipher.doFinal(bytes))
        } catch (exception: IllegalBlockSizeException) {
            exception.printStackTrace()
        } catch (exception: BadPaddingException) {
            exception.printStackTrace()
        }

        return null
    }

    private fun initKeyStore(): Boolean {
        try {
            keyStore = KeyStore.getInstance(KEY_STORE)
            keyStore?.load(null)
            return true
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        }

        return false
    }

    private fun initCipher(): Boolean {
        try {
            cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            return true
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        }

        return false
    }

    private fun initCipherMode(mode: Int): Boolean {
        try {
            keyStore?.load(null)
            when (mode) {
                Cipher.ENCRYPT_MODE -> {
                    val key = keyStore?.getCertificate(KEY_ALIAS)?.publicKey
                    val unrestricted = KeyFactory.getInstance(key?.algorithm).generatePublic(X509EncodedKeySpec(key?.encoded))
                    val spec = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
                    cipher?.init(mode, unrestricted, spec)
                }

                Cipher.DECRYPT_MODE -> {
                    val privateKey = keyStore?.getKey(KEY_ALIAS, null) as PrivateKey
                    cipher?.init(mode, privateKey)
                }
                else -> return false
            }
            return true
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: UnrecoverableKeyException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: InvalidAlgorithmParameterException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        }
        return false
    }

    private fun initKey(): Boolean {
        try {
            return keyStore?.containsAlias(KEY_ALIAS) == true || generateNewKey()
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        }
        return false
    }

    private fun generateNewKey(): Boolean {
        if (initKeyGenerator()) {
            try {
                keyPairGenerator?.initialize(KeyGenParameterSpec.Builder(KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .setUserAuthenticationRequired(true)
                        .build())
                keyPairGenerator?.generateKeyPair()
                return true
            } catch (e: InvalidAlgorithmParameterException) {
                e.printStackTrace()
            }

        }
        return false
    }

    private fun initKeyGenerator(): Boolean {
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEY_STORE)
            return true
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: NoSuchProviderException) {
            e.printStackTrace()
        }
        return false
    }
}