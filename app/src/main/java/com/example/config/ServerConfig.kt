package com.example.config

/**
 * Apna Koyeb FQDN yahan daalo (bina last slash ke).
 * Example: "https://animexz-yourname.koyeb.app"
 */
object ServerConfig {
    const val BASE_URL = "https://YOUR-APP-NAME.koyeb.app"

    /** Server ke apne anime IDs is number se start hote hain (AniList IDs se alag). */
    const val CUSTOM_ID_START = 1_000_000

    val isConfigured: Boolean
        get() = BASE_URL.startsWith("http") && !BASE_URL.contains("YOUR-APP-NAME")
}
