package com.example.ticketapp

import android.content.Context
import com.example.ticketapp.relay.CompanionTransport
import com.example.ticketapp.relay.TicketEvent
import com.example.ticketapp.relay.WatchTransport
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

/** Called on the Data Layer worker thread even when the phone activity is closed. */
class WatchRelayService : WearableListenerService() {
    override fun onMessageReceived(message: MessageEvent) {
        if (message.path != WatchTransport.REQUEST || message.data.size > 256) return
        val prefs = getSharedPreferences("companion", Context.MODE_PRIVATE)
        val text = message.data.toString(Charsets.UTF_8)
        var id = ""
        val status = runCatching {
            if (text.startsWith("STATUS|")) {
                id = text.substringAfter('|')
                require(java.util.UUID.fromString(id).toString() == id)
                if (runBlocking { CompanionTransport.exchange(prefs, "PING") }) "PC_READY" else "PC_OFFLINE"
            } else {
                val event = TicketEvent.decode(text); id = event.id
                require(event.origin == "WATCH" && event.destination in listOf("PC_ONLY", "WATCH_AND_PC"))
                if (runBlocking { CompanionTransport.deliver(prefs, event) }) "DELIVERED" else "FAILED"
            }
        }.getOrDefault("FAILED")
        if (id.isNotBlank()) Wearable.getMessageClient(this).sendMessage(
            message.sourceNodeId, WatchTransport.REPLY, "$id|$status".toByteArray(Charsets.UTF_8))
    }
}
