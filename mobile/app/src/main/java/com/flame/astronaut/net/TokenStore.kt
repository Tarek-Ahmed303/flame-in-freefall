package com.flame.astronaut.net

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flame.astronaut.domain.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.sessionStore by preferencesDataStore(name = "session")

/**
 * Persists the login session in DataStore. The bearer token is AES-GCM encrypted with a key that lives in the
 * Android Keystore, so the stored value is useless outside this app.
 */
class TokenStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token_enc")
    private val nameKey = stringPreferencesKey("name")
    private val emailKey = stringPreferencesKey("email")
    private val moduleKey = stringPreferencesKey("module")

    /** Cached profile, null when signed out. */
    val user: Flow<UserProfile?> = context.sessionStore.data.map { p -> p.toProfile() }

    /** Synchronous read for the OkHttp interceptor (runs on a background thread). */
    fun currentToken(): String? = runBlocking { context.sessionStore.data.first()[tokenKey]?.let(::decrypt) }

    suspend fun save(token: String, user: UserProfile) {
        context.sessionStore.edit {
            it[tokenKey] = encrypt(token)
            it[nameKey] = user.name
            it[emailKey] = user.email
            it[moduleKey] = user.module
        }
    }

    suspend fun saveUser(user: UserProfile) {
        context.sessionStore.edit {
            it[nameKey] = user.name
            it[emailKey] = user.email
            it[moduleKey] = user.module
        }
    }

    suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }

    private fun Preferences.toProfile(): UserProfile? {
        if (this[tokenKey] == null) return null
        return UserProfile(this[nameKey].orEmpty(), this[emailKey].orEmpty(), this[moduleKey].orEmpty())
    }

    // ---- Keystore AES-GCM
    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val out = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    /** Returns null if the value is unreadable (e.g. keystore key was reset) → the user is simply signed out. */
    private fun decrypt(encoded: String): String? = runCatching {
        val raw = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
            .apply { init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, raw, 0, IV_SIZE)) }
        String(cipher.doFinal(raw, IV_SIZE, raw.size - IV_SIZE), Charsets.UTF_8)
    }.getOrNull()

    private companion object {
        const val ALIAS = "flame_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}
