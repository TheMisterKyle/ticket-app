package com.example.ticketapp.relay

import java.util.UUID

enum class Destination(val local: Boolean, val pc: Boolean, val sound: Boolean) {
    LOCAL_ONLY(true, false, true), PC_ONLY(false, true, false), LOCAL_AND_PC(true, true, false);
    fun storedName(origin: String) = when (this) {
        LOCAL_ONLY -> "${origin}_ONLY"
        PC_ONLY -> "PC_ONLY"
        LOCAL_AND_PC -> "${origin}_AND_PC"
    }
    companion object {
        fun restore(value: String?, fallback: Destination = LOCAL_ONLY) = when {
            value == "PC_ONLY" -> PC_ONLY
            value?.endsWith("_AND_PC") == true -> LOCAL_AND_PC
            value?.endsWith("_ONLY") == true -> LOCAL_ONLY
            else -> fallback
        }
    }
}

/** V1 deliberately has a strict, bounded ASCII representation shared by both controllers. */
data class TicketEvent(
    val id: String, val colour: String, val awarded: Boolean,
    val probabilityPercent: Int, val origin: String, val destination: String, val timestamp: Long,
) {
    val outcome: String get() = colour.lowercase().replaceFirstChar { it.uppercase() } + if (awarded) "Win" else "Miss"
    fun encode() = "V1|$id|$colour|${if (awarded) 1 else 0}|$probabilityPercent|$origin|$destination|$timestamp"
    companion object {
        fun create(colour: String, awarded: Boolean, probability: Float, origin: String, mode: Destination) =
            TicketEvent(UUID.randomUUID().toString(), colour, awarded, (probability * 100).toInt(), origin, mode.storedName(origin), System.currentTimeMillis())
        fun decode(text: String): TicketEvent {
            require(text.length <= 256)
            val p = text.split('|')
            require(p.size == 8 && p[0] == "V1")
            require(UUID.fromString(p[1]).toString() == p[1])
            require(p[2] in listOf("GREEN", "RED") && p[3] in listOf("0", "1"))
            val probability = p[4].toInt()
            require(probability in if (p[2] == "GREEN") listOf(15,35,60,80,100) else listOf(10,25,50,75,100))
            require(p[5] in listOf("PHONE", "WATCH"))
            require(p[6] in listOf("PC_ONLY", "${p[5]}_ONLY", "${p[5]}_AND_PC"))
            val timestamp = p[7].toLong(); require(timestamp > 0)
            return TicketEvent(p[1], p[2], p[3] == "1", probability, p[5], p[6], timestamp)
        }
    }
}
