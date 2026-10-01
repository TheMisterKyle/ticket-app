package com.example.ticketapp.relay

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Local results follow actual completion; silent results use the same asset's duration. */
class TicketAudio(private val context: Context) {
    private var player: MediaPlayer? = null
    private var completion: CompletableDeferred<Unit>? = null
    private val durations = mutableMapOf<Int, Long>()
    var ambient: Boolean = false
    private fun canPlay() = !ambient && (context as LifecycleOwner).lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    private fun create(resource: Int) = MediaPlayer.create(context, resource, AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(), 0)

    private fun duration(resource: Int): Long = durations.getOrPut(resource) {
        runCatching { create(resource)?.let { media ->
            try { media.duration.toLong().coerceAtLeast(1) } finally { media.release() }
        } ?: 2500L }.getOrDefault(2500L)
    }

    fun play(resource: Int) {
        stop()
        if (!canPlay()) return
        player = runCatching { create(resource) }.getOrNull()
        player?.setOnCompletionListener { stop() }
        player?.setOnErrorListener { _, _, _ -> stop(); true }
        player?.start()
    }

    suspend fun finish(resource: Int, audible: Boolean) {
        stop()
        val length = duration(resource)
        if (!audible || !canPlay()) { delay(length); return }
        val media = runCatching { create(resource) }.getOrNull()
        if (media == null) { delay(length); return }
        val done = CompletableDeferred<Unit>()
        player = media
        completion = done
        media.setOnCompletionListener { done.complete(Unit) }
        media.setOnErrorListener { _, _, _ -> done.complete(Unit); true }
        try {
            media.start()
            withTimeoutOrNull(length + 2000) { done.await() }
        } finally {
            if (player === media) stop()
        }
    }

    fun stop() {
        player?.release(); player = null
        completion?.complete(Unit); completion = null
    }
}
