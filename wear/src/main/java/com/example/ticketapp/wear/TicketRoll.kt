package com.example.ticketapp.wear

import kotlin.random.Random

enum class TicketColour {
    GREEN,
    RED,
}

data class TicketOutcome(
    val colour: TicketColour,
    val awarded: Boolean,
    val probability: Float,
)

object TicketRoll {
    val greenSteps = listOf(0.15f, 0.35f, 0.60f, 0.80f, 1.00f)
    val redSteps = listOf(0.10f, 0.25f, 0.50f, 0.75f, 1.00f)

    fun defaultIndex(colour: TicketColour): Int = 2

    fun probability(colour: TicketColour, index: Int): Float {
        val steps = if (colour == TicketColour.GREEN) greenSteps else redSteps
        return steps[index.coerceIn(steps.indices)]
    }

    fun roll(
        colour: TicketColour,
        index: Int,
        randomValue: Float = Random.nextFloat(),
    ): TicketOutcome {
        val probability = probability(colour, index)
        return TicketOutcome(
            colour = colour,
            awarded = randomValue < probability,
            probability = probability,
        )
    }
}
