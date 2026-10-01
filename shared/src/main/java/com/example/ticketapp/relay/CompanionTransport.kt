package com.example.ticketapp.relay

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object CompanionTransport {
    suspend fun exchange(prefs: SharedPreferences, payload: String): Boolean = withContext(Dispatchers.IO) {
        val host = prefs.getString("host", "") ?: ""
        val code = prefs.getString("code", "") ?: ""
        if (host.isBlank() || !code.matches(Regex("[0-9]{4}"))) return@withContext false
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, 45832), 700)
                socket.soTimeout = 1200
                socket.getOutputStream().bufferedWriter(Charsets.UTF_8).apply {
                    write("TICKETTOSS|$code|$payload\n"); flush()
                }
                socket.getInputStream().bufferedReader(Charsets.UTF_8).readLine() ==
                    if (payload == "PING") "OK" else "OK|${payload.split('|')[1]}"
            }
        }.getOrDefault(false)
    }
    suspend fun deliver(prefs: SharedPreferences, event: TicketEvent): Boolean {
        repeat(3) { attempt ->
            if (exchange(prefs, event.encode())) return true
            if (attempt < 2) delay(500)
        }
        return false
    }
}
