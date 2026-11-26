package com.betfriends.app.data.auth

import com.betfriends.app.domain.model.ProfileCompletionRequired
import com.betfriends.app.domain.model.UserProfile
import com.betfriends.app.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override val hasActiveSession: Boolean
        get() = auth.currentUser != null

    override fun login(
        correo: String,
        password: String,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(
            correo,
            password
        ).addOnSuccessListener { authResult ->

            val firebaseUser = authResult.user

            if (firebaseUser == null) {
                onResult(
                    Result.failure(
                        IllegalStateException(
                            "No fue posible obtener la información del usuario."
                        )
                    )
                )
                return@addOnSuccessListener
            }

            loadUserProfile(
                firebaseUser = firebaseUser,
                onResult = onResult
            )
        }.addOnFailureListener { error ->
            onResult(
                Result.failure(
                    convertAuthError(error)
                )
            )
        }
    }

    override fun register(
        nombre: String,
        correo: String,
        password: String,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(
            correo,
            password
        ).addOnSuccessListener { authResult ->

            val firebaseUser = authResult.user

            if (firebaseUser == null) {
                onResult(
                    Result.failure(
                        IllegalStateException(
                            "La cuenta fue creada, pero no se pudo obtener el usuario."
                        )
                    )
                )
                return@addOnSuccessListener
            }

            completeProfile(firebaseUser.uid, nombre, onResult)
        }.addOnFailureListener { error ->
            onResult(
                Result.failure(
                    convertAuthError(error)
                )
            )
        }
    }

    override fun completeProfile(
        uid: String,
        nombre: String,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || user.uid != uid || email.isNullOrBlank()) {
            onResult(Result.failure(IllegalStateException("Inicia sesión de nuevo para continuar.")))
            return
        }

        val name = nombre.trim()
        val ref = firestore.collection(USERS_COLLECTION).document(uid)
        firestore.runTransaction { transaction ->
            val existing = transaction.get(ref)
            if (existing.exists()) {
                existing.toProfile(user)
            } else {
                val profile = UserProfile(uid = uid, nombre = name, correo = email, puntos = INITIAL_POINTS)
                transaction.set(ref, mapOf(
                    FIELD_NAME to profile.nombre,
                    FIELD_EMAIL to profile.correo,
                    FIELD_POINTS to profile.puntos,
                    FIELD_CREATED_AT to FieldValue.serverTimestamp()
                ))
                profile
            }
        }.addOnSuccessListener { profile ->
            onResult(Result.success(profile))
        }.addOnFailureListener { error ->
            // Una transacción puede fallar incluso si Auth ya confirmó el registro.
            onResult(Result.failure(ProfileCompletionRequired(uid, name, error)))
        }
    }

    override fun getCurrentUserProfile(
        onResult: (Result<UserProfile>) -> Unit
    ) {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            onResult(
                Result.failure(
                    IllegalStateException(
                        "No existe una sesión activa."
                    )
                )
            )
            return
        }

        loadUserProfile(
            firebaseUser = firebaseUser,
            onResult = onResult
        )
    }

    override fun updateProfileName(
        nombre: String,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            onResult(
                Result.failure(
                    IllegalStateException(
                        "No existe una sesión activa."
                    )
                )
            )
            return
        }

        firestore
            .collection(USERS_COLLECTION)
            .document(firebaseUser.uid)
            .update(FIELD_NAME, nombre)
            .addOnSuccessListener {
                loadUserProfile(
                    firebaseUser = firebaseUser,
                    onResult = onResult
                )
            }
            .addOnFailureListener {
                onResult(
                    Result.failure(
                        IllegalStateException(
                            "No fue posible guardar los cambios. " +
                                    "Revisa tu conexión e inténtalo de nuevo."
                        )
                    )
                )
            }
    }

    override fun logout() {
        auth.signOut()
    }

    private fun loadUserProfile(
        firebaseUser: FirebaseUser,
        onResult: (Result<UserProfile>) -> Unit
    ) {
        firestore
            .collection(USERS_COLLECTION)
            .document(firebaseUser.uid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {
                    onResult(Result.success(document.toProfile(firebaseUser)))
                } else if (document.metadata.isFromCache) {
                    onResult(Result.failure(IllegalStateException("Conéctate para consultar tu perfil.")))
                } else {
                    onResult(Result.failure(ProfileCompletionRequired(firebaseUser.uid)))
                }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(IllegalStateException(
                    "No fue posible consultar el perfil. Revisa tu conexión e inténtalo de nuevo.", error
                )))
            }
    }

    private fun DocumentSnapshot.toProfile(user: FirebaseUser) = UserProfile(
        uid = user.uid,
        nombre = getString(FIELD_NAME) ?: "Usuario",
        correo = getString(FIELD_EMAIL) ?: user.email.orEmpty(),
        puntos = (get(FIELD_POINTS) as? Number)?.toInt() ?: INITIAL_POINTS
    )

    private fun convertAuthError(
        error: Exception
    ): Exception {
        val message = when (error) {
            is FirebaseAuthInvalidCredentialsException -> {
                "El correo o la contraseña son incorrectos."
            }

            is FirebaseAuthUserCollisionException -> {
                "Este correo ya está registrado. Inicia sesión para continuar o completar tu perfil."
            }

            is FirebaseAuthWeakPasswordException -> {
                "La contraseña debe tener al menos 6 caracteres."
            }

            is FirebaseNetworkException -> {
                "No se pudo conectar con Firebase. Revisa tu conexión."
            }

            else -> {
                "No fue posible completar la autenticación."
            }
        }

        return IllegalStateException(
            message,
            error
        )
    }

    private companion object {
        const val USERS_COLLECTION = "usuarios"

        const val FIELD_NAME = "nombre"
        const val FIELD_EMAIL = "correo"
        const val FIELD_POINTS = "puntos"
        const val FIELD_CREATED_AT = "fechaCreacion"

        const val INITIAL_POINTS = 0
    }
}