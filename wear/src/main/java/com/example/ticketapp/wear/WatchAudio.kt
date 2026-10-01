package com.example.ticketapp.wear

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

class WatchAudio(private val context: Context) {
    private var player: MediaPlayer? = null
    var ambient: Boolean = false
    fun play(resource: Int) {
        stop()
        if (ambient || !(context as LifecycleOwner).lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        player = MediaPlayer.create(context, resource, AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(), 0)
        player?.setOnCompletionListener { stop() }
        player?.start()
    }
    fun stop() { player?.release(); player = null }
    fun outcome(result: TicketOutcome) = play(when {
        result.colour == TicketColour.GREEN && result.awarded -> R.raw.green_win
        result.colour == TicketColour.RED && result.awarded -> R.raw.red_win
        result.colour == TicketColour.GREEN -> R.raw.green_miss
        else -> R.raw.red_miss
    })
}
