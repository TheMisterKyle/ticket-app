package com.example.ticketapp.wear

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

/** Static launcher: no polling, ticket roll, or separate session on the watch face. */
class TicketShortcutService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? =
        shortcut(request.complicationType, preview = false)

    override fun getPreviewData(type: ComplicationType): ComplicationData? = shortcut(type, preview = true)

    private fun shortcut(type: ComplicationType, preview: Boolean): ComplicationData? {
        val description = PlainComplicationText.Builder("Open Ticket Toss").build()
        val image = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_ticket_shortcut)).build()
        val action = if (preview) null else PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("Toss").build(), description,
            ).setMonochromaticImage(image).setTapAction(action).build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText.Builder("Ticket Toss").build(), description,
            ).setMonochromaticImage(image).setTapAction(action).build()
            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
                image, description,
            ).setTapAction(action).build()
            else -> null
        }
    }
}
