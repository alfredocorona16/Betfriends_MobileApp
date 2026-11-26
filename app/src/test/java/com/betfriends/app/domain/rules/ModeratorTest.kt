package com.betfriends.app.domain.rules

import com.betfriends.app.domain.model.*
import com.betfriends.app.location.DeviceLocation
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class ModeratorTest {
    private val start = LocalDateTime.of(2026, 1, 1, 12, 0)
    private val members = listOf("a", "b", "m").map { BetParticipant(it, "Alex") }
    private val bet = Bet("r", "Carrera", "", BetType.TIME, start, start.plusHours(1),
        creatorId = "a", participants = members, moderatorId = "m")
    private val arrival = BetRules.checkIn(bet, "b", start, null)
    private val pending = bet.copy(checkIns = listOf(arrival))

    @Test fun approvalUsesTheAuthenticatedModeratorAndAnActualCompetitorArrival() {
        BetRules.requireApproval(pending, "m", "b")
        for ((actor, candidate) in listOf("a" to "b", "m" to "a", "m" to "m", "x" to "b")) {
            assertThrows(IllegalArgumentException::class.java) { BetRules.requireApproval(pending, actor, candidate) }
        }
        assertTrue(PointsRules.calculate(pending).isEmpty())
    }

    @Test fun moderatorAndUnacceptedUsersCannotCompete() {
        for (actor in listOf("m", "x")) {
            assertThrows(IllegalArgumentException::class.java) { BetRules.checkIn(bet, actor, start, null) }
        }
        val invited = members.map { if (it.id == "b") it.copy(invitationStatus = ParticipantInvitationStatus.PENDING) else it }
        assertThrows(IllegalArgumentException::class.java) { BetRules.checkIn(bet.copy(participants = invited), "b", start, null) }
        assertThrows(IllegalArgumentException::class.java) { BetRules.checkIn(pending, "b", start, null) }
        assertThrows(IllegalArgumentException::class.java) { BetRules.checkIn(bet, "a", start.minusMinutes(1), null) }
        assertThrows(IllegalArgumentException::class.java) { BetRules.checkIn(bet, "a", start.plusHours(2), null) }
    }

    @Test fun locationsAreCheckedBeforeSubmission() {
        val gps = bet.copy(type = BetType.LOCATION, targetLatitude = 19.3, targetLongitude = -98.2, radiusMeters = 100)
        assertEquals("a", BetRules.checkIn(gps, "a", start, DeviceLocation(19.3, -98.2, 5f)).participantId)
        for (location in listOf(null, DeviceLocation(20.0, -98.2, 5f), DeviceLocation(19.3, -98.2, 200f))) {
            assertThrows(IllegalArgumentException::class.java) { BetRules.checkIn(gps, "a", start, location) }
        }
    }

    @Test fun awardsDistinguishDuplicateNamesAndExcludeModerator() {
        val finished = pending.copy(status = BetStatus.FINISHED, approvedBy = "m", winnerId = "b", winnerName = "Alex")
        assertEquals(mapOf("a" to 10, "b" to 60), PointsRules.calculate(finished).associate { it.userId to it.points })
        assertThrows(IllegalArgumentException::class.java) { BetRules.requireApproval(finished, "m", "a") }
    }

    @Test fun aLegacyFinishedResultKeepsItsWinnerAndDoesNotNeedApproval() {
        val old = bet.copy(status = BetStatus.FINISHED, moderatorId = null, winnerName = "Alex")
        assertFalse(old.isOpen)
        assertNull(old.approvedBy)
        assertEquals("a", PointsRules.calculate(old).single { it.wasWinner }.userId)
    }
}
