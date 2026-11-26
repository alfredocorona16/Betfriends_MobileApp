package com.betfriends.app.data.bet

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.betfriends.app.data.auth.FirebaseAuthRepository
import com.betfriends.app.domain.model.Bet
import com.betfriends.app.domain.model.BetInvitation
import com.betfriends.app.domain.model.BetParticipant
import com.betfriends.app.domain.model.BetType
import com.betfriends.app.domain.model.UserProfile
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ModeratorEmulatorTest {
    @Test
    fun threeAccountsRecordApproveAndCreditOneWinner() {
        val testId = UUID.randomUUID().toString()
        val options = FirebaseOptions.Builder().setProjectId("demo-betfriends")
            .setApplicationId("1:123456789:android:abcdef123456").setApiKey("test-api-key").build()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val apps = mutableListOf<FirebaseApp>()
        fun player(suffix: String): Player {
            val app = FirebaseApp.initializeApp(context, options, "$testId-$suffix")
            apps.add(app)
            val auth = FirebaseAuth.getInstance(app).apply { useEmulator("10.0.2.2", 9099) }
            val db = FirebaseFirestore.getInstance(app).apply { useEmulator("10.0.2.2", 8080) }
            val profile = await<UserProfile> { done ->
                FirebaseAuthRepository(auth, db).register("Alex", "$testId-$suffix@example.test", "Prueba-123!", done)
            }
            return Player(profile, db, FirebaseBetRepository(db))
        }
        try {
            val creator = player("creador")
            val racer = player("corredor")
            val moderator = player("moderador")
            val now = LocalDateTime.now()
            val participants = listOf(creator, racer, moderator).map { BetParticipant(it.uid, it.profile.nombre, it.profile.correo) }
            val bet = Bet(testId, "Llegada de prueba", "", BetType.TIME, now.minusMinutes(5), now.plusMinutes(5),
                participants = participants, moderatorId = moderator.uid)
            await<Unit> { done -> creator.repo.createBet(bet, creator.profile, { done(Result.success(Unit)) },
                { done(Result.failure(IllegalStateException(it))) }) }
            for (invitee in listOf(racer, moderator)) {
                val invitation = BetInvitation("${bet.id}_${invitee.uid}", bet.id, bet.title, creator.uid,
                    creator.profile.nombre, invitee.uid, invitee.profile.nombre)
                await<Unit> { done -> invitee.repo.acceptInvitation(invitation, invitee.uid, { done(Result.success(Unit)) },
                    { done(Result.failure(IllegalStateException(it))) }) }
            }
            await<Unit> { racer.repo.submitCheckIn(bet.id, racer.uid, null, it) }
            assertTrue(runCatching { await<Unit> { creator.repo.approveWinner(bet.id, creator.uid, racer.uid, it) } }.isFailure)
            await<Unit> { moderator.repo.approveWinner(bet.id, moderator.uid, racer.uid, it) }
            val ref = moderator.db.collection("apuestas").document(bet.id)
            val saved = requireNotNull(BetDocumentAdapter().toBet(Tasks.await(ref.get(Source.SERVER))))
            assertEquals(racer.uid, saved.winnerId)
            assertEquals(moderator.uid, saved.approvedBy)
            repeat(2) { await<Unit> { moderator.repo.settlePoints(saved, moderator.uid, it) } }
            for ((player, expected) in listOf(creator to 10L, racer to 60L, moderator to 0L)) {
                val doc = Tasks.await(player.db.collection("usuarios").document(player.uid).get(Source.SERVER))
                assertEquals(expected, doc.getLong("puntos"))
            }
        } finally {
            apps.forEach { app -> FirebaseAuth.getInstance(app).signOut(); app.delete() }
        }
    }

    private data class Player(val profile: UserProfile, val db: FirebaseFirestore, val repo: FirebaseBetRepository) {
        val uid: String get() = profile.uid
    }

    private fun <T> await(start: ((Result<T>) -> Unit) -> Unit): T {
        val result = CompletableFuture<T>()
        start { it.fold(result::complete, result::completeExceptionally) }
        return result.get(45, TimeUnit.SECONDS)
    }
}
