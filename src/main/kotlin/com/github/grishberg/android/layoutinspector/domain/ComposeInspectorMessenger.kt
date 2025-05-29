package com.github.grishberg.android.layoutinspector.domain

import com.android.ddmlib.Client
import com.android.layoutinspector.common.AppLogger
import com.google.protobuf.CodedInputStream
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Command
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Response

/**
 * Messenger for communicating with the Compose Layout Inspector running on the target device.
 * Handles sending commands and receiving responses through JDWP protocol.
 */
class ComposeInspectorMessenger(
    private val logger: AppLogger,
    private val client: Client
) : AppInspectorMessenger {
    override suspend fun sendCommand(command: Command.Builder.() -> Unit): Response {
        val commandBuilder = Command.newBuilder()
        command(commandBuilder)
        val builtCommand = commandBuilder.build()

        logger.d("ComposeInspectorMessenger: sending command ${builtCommand.commandCase}")

        try {
            // Convert command to bytes
            val commandBytes = builtCommand.toByteArray()

            // Send command through JDWP and get response bytes
            val responseBytes = client.sendCommand(COMPOSE_INSPECTOR_CHUNK_TYPE, commandBytes)

            // Parse response with increased recursion limit for deep compose trees
            val inputStream = CodedInputStream.newInstance(responseBytes).apply {
                setRecursionLimit(Integer.MAX_VALUE)
            }
            return Response.parseFrom(inputStream)
        } catch (e: Exception) {
            logger.e("ComposeInspectorMessenger", "Failed to send command: ${e.message}")
            throw e
        }
    }

    companion object {
        // Unique identifier for Compose Inspector commands in JDWP protocol
        private const val COMPOSE_INSPECTOR_CHUNK_TYPE = 0x2B
    }
} 