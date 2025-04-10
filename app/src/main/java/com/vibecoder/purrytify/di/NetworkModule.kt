package com.vibecoder.purrytify.di

import android.content.Context
import com.vibecoder.purrytify.data.remote.PurrytifyApi
import com.vibecoder.purrytify.util.AuthInterceptor
import com.vibecoder.purrytify.util.NetworkMonitor
import com.vibecoder.purrytify.util.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = "http://34.101.226.132:3000/"
    private const val CONNECT_TIMEOUT = 15L // seconds
    private const val READ_TIMEOUT = 30L // seconds

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
            loggingInterceptor: HttpLoggingInterceptor,
            authInterceptor: AuthInterceptor,
            @ApplicationContext context: Context
    ): OkHttpClient {
        // OWASP M8
        val trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        val trustManager = trustManagers[0] as X509TrustManager

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagers, SecureRandom())

        // Only allow modern and secure connection specs
        val connectionSpec =
                ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                        .build()

        return OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .connectionSpecs(listOf(connectionSpec, ConnectionSpec.CLEARTEXT))
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .hostnameVerifier { hostname, session -> true }
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(true)
                .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    @Provides
    @Singleton
    fun providePurrytifyApi(retrofit: Retrofit): PurrytifyApi {
        return retrofit.create(PurrytifyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }
}
