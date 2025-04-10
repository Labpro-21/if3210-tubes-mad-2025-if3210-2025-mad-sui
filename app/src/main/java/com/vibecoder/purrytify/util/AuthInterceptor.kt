package com.vibecoder.purrytify.util

import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor @Inject constructor(private val tokenManager: TokenManager) : Interceptor {

    @Throws(IOException::class)
    // TODO: For the next iteration must to consider this
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val path = originalRequest.url.encodedPath
        if (path.contains("login") || path.contains("refresh-token")) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking { tokenManager.token.first() }

        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val newRequest =
                originalRequest
                        .newBuilder()
                        .header("Authorization", "Bearer $token")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .method(originalRequest.method, originalRequest.body)
                        .build()

        return chain.proceed(newRequest)
    }
}
