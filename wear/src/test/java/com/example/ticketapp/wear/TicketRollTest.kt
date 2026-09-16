package com.example.ticketapp.wear

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketRollTest {
    @Test
    fun greenDefaultsToSixtyPercent() {
        val index = TicketRoll.defaultIndex(TicketColour.GREEN)
        assertEquals(0.60f, TicketRoll.probability(TicketColour.GREEN, index), 0.001f)
    }

    @Test
    fun redDefaultsToFiftyPercent() {
        val index = TicketRoll.defaultIndex(TicketColour.RED)
        assertEquals(0.50f, TicketRoll.probability(TicketColour.RED, index), 0.001f)
    }

    @Test
    fun boundaryRollsAreDeterministic() {
        assertTrue(TicketRoll.roll(TicketColour.GREEN, 4, 0.999f).awarded)
        assertFalse(TicketRoll.roll(TicketColour.RED, 0, 0.10f).awarded)
    }

    @Test
    fun indicesAreClamped() {
        assertEquals(0.15f, TicketRoll.probability(TicketColour.GREEN, -10), 0.001f)
        assertEquals(1.00f, TicketRoll.probability(TicketColour.RED, 99), 0.001f)
    }
}
