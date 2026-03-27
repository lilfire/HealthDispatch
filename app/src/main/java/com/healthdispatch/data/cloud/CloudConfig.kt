package com.healthdispatch.data.cloud

data class CloudConfig(
    val url: String = "",
    val apiKey: String = ""
) {
    val isConfigured: Boolean get() = url.isNotBlank() && apiKey.isNotBlank()
}
