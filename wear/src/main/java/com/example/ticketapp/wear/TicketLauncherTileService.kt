package com.example.ticketapp.wear

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture

/** A static swipe-to launcher. All ticket controls stay in the existing activity. */
class TicketLauncherTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val launch = ActionBuilders.LaunchAction.Builder().setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(packageName)
                .setClassName(MainActivity::class.java.name)
                .build(),
        ).build()
        val button = LayoutElementBuilders.Box.Builder()
            .setWidth(dp(144f)).setHeight(dp(64f))
            .setModifiers(ModifiersBuilders.Modifiers.Builder()
                .setClickable(ModifiersBuilders.Clickable.Builder().setId("open-ticket-toss").setOnClick(launch).build())
                .setSemantics(ModifiersBuilders.Semantics.Builder().setContentDescription("Open Ticket Toss").build())
                .setBackground(ModifiersBuilders.Background.Builder()
                    .setColor(ColorBuilders.argb(0xFF42D77D.toInt()))
                    .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(22f)).build()).build())
                .build())
            .addContent(text("OPEN", 22f, 0xFF090B0D.toInt()))
            .build()
        val content = LayoutElementBuilders.Column.Builder()
            .addContent(text("TICKET TOSS", 18f, 0xFFF6F0DF.toInt()))
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(16f)).build())
            .addContent(button)
            .build()
        val root = LayoutElementBuilders.Box.Builder().setWidth(expand()).setHeight(expand())
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setBackground(
                ModifiersBuilders.Background.Builder().setColor(ColorBuilders.argb(0xFF090B0D.toInt())).build(),
            ).build()).addContent(content).build()
        return completed(TileBuilders.Tile.Builder()
            .setResourcesVersion("launcher-1")
            .setTileTimeline(TimelineBuilders.Timeline.Builder().addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder().setLayout(
                    LayoutElementBuilders.Layout.Builder().setRoot(root).build(),
                ).build(),
            ).build()).build())
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        completed(ResourceBuilders.Resources.Builder().setVersion("launcher-1").build())

    private fun <T> completed(value: T): ListenableFuture<T> =
        CallbackToFutureAdapter.getFuture { completer -> completer.set(value); "ticket-launcher" }

    private fun text(value: String, size: Float, colour: Int) = LayoutElementBuilders.Text.Builder()
        .setText(value).setFontStyle(LayoutElementBuilders.FontStyle.Builder()
            .setSize(sp(size)).setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD).setColor(ColorBuilders.argb(colour)).build()).build()
}
