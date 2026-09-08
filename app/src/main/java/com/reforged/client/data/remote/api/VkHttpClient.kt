package com.reforged.client.data.remote.api

import com.reforged.client.data.local.TokenStorage
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VkHttpClient @Inject constructor(
    private val tokenStorage: TokenStorage
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        defaultRequest {
            url("https://vk.ru/")
        }
    }

    private val mutex = Mutex()
    private var lastRequestTime = 0L
    private val minRequestInterval = 350L

    /**
     * Executes a safe request to VK API with rate limiting (max 3 requests per second).
     * Automatically adds base URL, version (v=5.199), and access_token.
     */
    suspend fun safeRequest(
        method: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        // Rate Limiting Logic
        mutex.withLock {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastRequest = currentTime - lastRequestTime
            if (timeSinceLastRequest < minRequestInterval) {
                delay(minRequestInterval - timeSinceLastRequest)
            }
            lastRequestTime = System.currentTimeMillis()
        }

        return client.get("method/$method") {
            parameter("v", "5.199")
            parameter("access_token", tokenStorage.accessToken)
            block()
        }
    }
}
