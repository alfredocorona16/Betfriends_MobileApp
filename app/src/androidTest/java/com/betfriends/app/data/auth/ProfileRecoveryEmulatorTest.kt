package com.betfriends.app.data.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.betfriends.app.domain.model.ProfileCompletionRequired
import com.betfriends.app.domain.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ProfileRecoveryEmulatorTest {
    @Test
    fun failedWriteCanBeRecoveredAfterLoginWithoutOverwritingExistingProfile() {
        val id = UUID.randomUUID().toString()
        val email = "recovery-$id@example.test"
        val password = "Demo-pass-123!"
        val options = FirebaseOptions.Builder()
            .setProjectId("demo-betfriends")
            .setApplicationId("1:123456789:android:abcdef123456")
            .setApiKey("test-api-key")
            .build()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = FirebaseApp.initializeApp(context, options, "recovery-$id")
        val auth = FirebaseAuth.getInstance(app).apply { useEmulator("10.0.2.2", 9099) }
        val db = FirebaseFirestore.getInstance(app).apply { useEmulator("10.0.2.2", 8080) }
        val repo = FirebaseAuthRepository(auth, db)
        try {
            // Auth acepta las credenciales. Las reglas de Firestore rechazan este nombre.
            val failed = awaitProfile { repo.register("a".repeat(41), email, password, it) }
            assertTrue(failed.exceptionOrNull() is ProfileCompletionRequired)
            val uid = requireNotNull(auth.currentUser).uid
            assertEquals(uid, (failed.exceptionOrNull() as ProfileCompletionRequired).uid)

            repo.logout()
            assertNotNull(awaitProfile { repo.register("Ana", email, password, it) }.exceptionOrNull())
            val login = awaitProfile { repo.login(email, password, it) }
            assertEquals(uid, (login.exceptionOrNull() as ProfileCompletionRequired).uid)
            val restored = awaitProfile { repo.getCurrentUserProfile(it) }
            assertEquals(uid, (restored.exceptionOrNull() as ProfileCompletionRequired).uid)

            val saved = awaitProfile { repo.completeProfile(uid, "Ana", it) }.getOrThrow()
            assertEquals(UserProfile(uid, "Ana", email, 0), saved)
            val retried = awaitProfile { repo.completeProfile(uid, "Otro nombre", it) }.getOrThrow()
            assertEquals(saved, retried)
            assertEquals(saved, awaitProfile { repo.getCurrentUserProfile(it) }.getOrThrow())
        } finally {
            auth.signOut()
            app.delete()
        }
    }

    private fun awaitProfile(call: ((Result<UserProfile>) -> Unit) -> Unit): Result<UserProfile> {
        val latch = CountDownLatch(1)
        var result: Result<UserProfile>? = null
        call {
            result = it
            latch.countDown()
        }
        check(latch.await(45, TimeUnit.SECONDS)) { "Los emuladores de Firebase no respondieron." }
        return checkNotNull(result)
    }
}
