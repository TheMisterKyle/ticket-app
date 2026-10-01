package com.example.ticketapp.relay

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.TimeUnit

object WatchTransport {
    const val REQUEST = "/ticket-toss/request"
    const val REPLY = "/ticket-toss/reply"
    suspend fun deliver(context: Context, event: TicketEvent): String {
        repeat(2) { attempt ->
            val result = request(context, event)
            if (result == "DELIVERED" || result == "PHONE_OFFLINE") return result
            if (attempt == 0) delay(500)
        }
        return "FAILED"
    }
    suspend fun request(context: Context, event: TicketEvent? = null): String = withContext(Dispatchers.IO) {
        val client = Wearable.getMessageClient(context)
        val id = event?.id ?: UUID.randomUUID().toString()
        val reply = CompletableDeferred<String>()
        var target: String? = null
        val listener = MessageClient.OnMessageReceivedListener { message ->
            val text = message.data.toString(Charsets.UTF_8)
            if (message.path == REPLY && message.sourceNodeId == target && text.startsWith("$id|"))
                reply.complete(text.substringAfter('|'))
        }
        try {
            val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes, 2, TimeUnit.SECONDS)
            target = nodes.firstOrNull { it.isNearby }?.id ?: nodes.firstOrNull()?.id
            if (target == null) return@withContext "PHONE_OFFLINE"
            Tasks.await(client.addListener(listener), 2, TimeUnit.SECONDS)
            Tasks.await(client.sendMessage(target!!, REQUEST,
                (event?.encode() ?: "STATUS|$id").toByteArray(Charsets.UTF_8)), 2, TimeUnit.SECONDS)
            withTimeoutOrNull(8500) { reply.await() } ?: "FAILED"
        } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { "FAILED" }
        finally { client.removeListener(listener) }
    }
}
