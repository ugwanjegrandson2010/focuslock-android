package com.focuslock.app.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PinManager {
    private val alias = "focus_lock_pin_key"

    private fun getKey() {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!ks.containsAlias(alias)) {
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).setDigests(KeyProperties.DIGEST_SHA256)
                .setKeySize(256)
                .build()
            val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
            kg.init(spec)
            kg.generateKey()
        }
    }

    fun newSalt(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(pin: String, salt: String): String {
        getKey()
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val secretKey = ks.getKey(alias, null)
        val mac = Mac.getInstance("HmacSHA256").apply { init(secretKey) }
        return Base64.encodeToString(mac.doFinal((salt + pin).toByteArray()), Base64.NO_WRAP)
    }

    fun verify(pin: String, salt: String, expected: String): Boolean {
        return try {
            val actual = hash(pin, salt)
            constantTimeEquals(actual, expected)
        } catch (e: Exception) {
            false
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }
}
