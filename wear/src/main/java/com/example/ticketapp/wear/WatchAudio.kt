package com.example.ticketapp.wear

import android.content.Context
import com.example.ticketapp.relay.TicketAudio

class WatchAudio(context: Context) {
    private val audio = TicketAudio(context)
    var ambient: Boolean
        get() = audio.ambient
        set(value) { audio.ambient = value }
    fun play(resource: Int) = audio.play(resource)
    fun stop() = audio.stop()
    suspend fun drumroll(audible: Boolean) = audio.finish(R.raw.drumroll, audible)
    suspend fun outcome(result: TicketOutcome, audible: Boolean) = audio.finish(when {
        result.colour == TicketColour.GREEN && result.awarded -> R.raw.green_win
        result.colour == TicketColour.RED && result.awarded -> R.raw.red_win
        result.colour == TicketColour.GREEN -> R.raw.green_miss
        else -> R.raw.red_miss
    }, audible)
}
