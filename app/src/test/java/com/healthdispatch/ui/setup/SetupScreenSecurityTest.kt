package com.healthdispatch.ui.setup

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SetupScreenSecurityTest {

    private val setupScreenSource: String by lazy {
        val possiblePaths = listOf(
            "app/src/main/java/com/healthdispatch/ui/setup/SetupScreen.kt",
            "../app/src/main/java/com/healthdispatch/ui/setup/SetupScreen.kt"
        )
        val file = possiblePaths.map { File(it) }.firstOrNull { it.exists() }
            ?: File(System.getProperty("user.dir"))
                .resolve("app/src/main/java/com/healthdispatch/ui/setup/SetupScreen.kt")
        file.readText()
    }

    @Test
    fun apiKeyField_usesPasswordVisualTransformation() {
        assertTrue(
            "API key field should use PasswordVisualTransformation for masking",
            setupScreenSource.contains("PasswordVisualTransformation")
        )
    }

    @Test
    fun setupScreen_importsPasswordVisualTransformation() {
        assertTrue(
            "SetupScreen should import PasswordVisualTransformation",
            setupScreenSource.contains("import androidx.compose.ui.text.input.PasswordVisualTransformation")
        )
    }

    @Test
    fun setupScreen_noLogStatements() {
        val logPatterns = listOf(
            "Log.d(", "Log.i(", "Log.v(", "Log.w(", "Log.e(",
            "println(", "System.out.print"
        )
        val foundLogs = logPatterns.filter { setupScreenSource.contains(it) }
        assertTrue(
            "SetupScreen should not contain log statements that could leak secrets, found: $foundLogs",
            foundLogs.isEmpty()
        )
    }

    @Test
    fun syncWorker_noSecretLogging() {
        val possiblePaths = listOf(
            "app/src/main/java/com/healthdispatch/sync/SyncWorker.kt",
            "../app/src/main/java/com/healthdispatch/sync/SyncWorker.kt"
        )
        val file = possiblePaths.map { File(it) }.firstOrNull { it.exists() }
            ?: File(System.getProperty("user.dir"))
                .resolve("app/src/main/java/com/healthdispatch/sync/SyncWorker.kt")
        val source = file.readText()

        val sensitiveLogPatterns = listOf("apiKey", "apikey", "api_key", "password", "secret", "token")
        val logLines = source.lines().filter { line ->
            line.contains("Log.") && sensitiveLogPatterns.any { pattern ->
                line.contains(pattern, ignoreCase = true)
            }
        }
        assertTrue(
            "SyncWorker should not log sensitive data, found: $logLines",
            logLines.isEmpty()
        )
    }
}
