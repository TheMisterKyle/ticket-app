package com.example.ticketapp.relay

import org.junit.Assert.*
import org.junit.Test

class TicketEventTest {
    @Test fun exactOutcomesSurviveRoundTrip() {
        for (colour in listOf("GREEN", "RED")) for (awarded in listOf(true, false))
            for (origin in listOf("WATCH", "PHONE")) for (mode in Destination.entries) {
                val event = TicketEvent.create(colour, awarded, if (colour == "GREEN") .6f else .5f, origin, mode)
                assertEquals(event, TicketEvent.decode(event.encode()))
                assertEquals(mode, Destination.restore(mode.storedName(origin)))
            }
    }
    @Test fun invalidProtocolIsRejected() {
        val text = TicketEvent.create("GREEN", true, .6f, "WATCH", Destination.PC_ONLY).encode()
        for (invalid in listOf(text.replace("V1|", "V2|"), text.replace("|60|", "|61|"),
            text.replace("|GREEN|", "|BLUE|"), text.replace("|WATCH|", "|DESKTOP|"), text + "|extra")) {
            assertTrue(runCatching { TicketEvent.decode(invalid) }.isFailure)
        }
    }
    @Test fun privacyAndAudioRouting() {
        assertFalse(Destination.PC_ONLY.local); assertFalse(Destination.PC_ONLY.sound)
        assertTrue(Destination.LOCAL_ONLY.local); assertTrue(Destination.LOCAL_ONLY.sound)
        assertFalse(Destination.LOCAL_ONLY.pc)
        assertTrue(Destination.LOCAL_AND_PC.local); assertTrue(Destination.LOCAL_AND_PC.pc)
        assertFalse(Destination.LOCAL_AND_PC.sound)
    }
}
