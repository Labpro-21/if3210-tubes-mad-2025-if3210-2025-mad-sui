package com.vibecoder.purrytify.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.Charset
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val dataStore = context.dataStore
    private val TAG = "TokenManager"
    private val KEY_ALIAS = "purrytify_encryption_key"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private val GCM_IV_LENGTH = 12

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("encrypted_jwt_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("encrypted_refresh_token")
        private val IV_TOKEN_KEY = stringPreferencesKey("iv_jwt_token")
        private val IV_REFRESH_TOKEN_KEY = stringPreferencesKey("iv_refresh_token")
    }

    val token: Flow<String?> =
            dataStore.data.map { preferences ->
                val encryptedToken = preferences[TOKEN_KEY]
                val iv = preferences[IV_TOKEN_KEY]

                if (encryptedToken != null && iv != null) {
                    try {
                        decrypt(encryptedToken, iv)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error decrypting token", e)
                        null
                    }
                } else {
                    null
                }
            }

    val refreshToken: Flow<String?> =
            dataStore.data.map { preferences ->
                val encryptedToken = preferences[REFRESH_TOKEN_KEY]
                val iv = preferences[IV_REFRESH_TOKEN_KEY]

                if (encryptedToken != null && iv != null) {
                    try {
                        decrypt(encryptedToken, iv)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error decrypting refresh token", e)
                        null
                    }
                } else {
                    null
                }
            }

    suspend fun saveToken(token: String) {
        try {
            val (encryptedToken, iv) = encrypt(token)

            dataStore.edit { preferences ->
                preferences[TOKEN_KEY] = encryptedToken
                preferences[IV_TOKEN_KEY] = iv
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving token", e)
        }
    }

    suspend fun saveRefreshToken(refreshToken: String) {
        try {
            val (encryptedToken, iv) = encrypt(refreshToken)

            dataStore.edit { preferences ->
                preferences[REFRESH_TOKEN_KEY] = encryptedToken
                preferences[IV_REFRESH_TOKEN_KEY] = iv
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving refresh token", e)
        }
    }

    suspend fun deleteToken() {
        dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(IV_TOKEN_KEY)
        }
    }

    suspend fun deleteRefreshToken() {
        dataStore.edit { preferences ->
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(IV_REFRESH_TOKEN_KEY)
        }
    }

    // OWASP M9: Secure encryption functions
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, ANDROID_KEYSTORE)

            val keyGenParameterSpec =
                    KeyGenParameterSpec.Builder(
                                    KEY_ALIAS,
                                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                            )
                            .setBlockModes(ENCRYPTION_BLOCK_MODE)
                            .setEncryptionPaddings(ENCRYPTION_PADDING)
                            .setRandomizedEncryptionRequired(true)
                            .build()

            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }

        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun encrypt(plainText: String): Pair<String, String> {
        val cipher =
                Cipher.getInstance(
                        "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
                )
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val iv = cipher.iv
        val ivString = Base64.getEncoder().encodeToString(iv)

        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charset.forName("UTF-8")))
        val encryptedString = Base64.getEncoder().encodeToString(encryptedBytes)

        return Pair(encryptedString, ivString)
    }

    private fun decrypt(encryptedText: String, ivString: String): String {
        val iv = Base64.getDecoder().decode(ivString)
        val encryptedBytes = Base64.getDecoder().decode(encryptedText)

        val cipher =
                Cipher.getInstance(
                        "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
                )
        val spec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return String(decryptedBytes, Charset.forName("UTF-8"))
    }
}
