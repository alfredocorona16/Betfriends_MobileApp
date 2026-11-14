package com.betfriends.app.data.auth

import com.betfriends.app.domain.model.UserProfile
import com.betfriends.app.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
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

            val userProfile = UserProfile(
                uid = firebaseUser.uid,
                nombre = nombre,
                correo = correo,
                saldo = INITIAL_BALANCE
            )

            val userData = hashMapOf<String, Any>(
                FIELD_NAME to userProfile.nombre,
                FIELD_EMAIL to userProfile.correo,
                FIELD_BALANCE to userProfile.saldo,
                FIELD_CREATED_AT to FieldValue.serverTimestamp()
            )

            firestore
                .collection(USERS_COLLECTION)
                .document(firebaseUser.uid)
                .set(userData)
                .addOnSuccessListener {
                    onResult(
                        Result.success(userProfile)
                    )
                }
                .addOnFailureListener {
                    auth.signOut()

                    onResult(
                        Result.failure(
                            IllegalStateException(
                                "La cuenta fue creada, pero no se pudo guardar el perfil."
                            )
                        )
                    )
                }
        }.addOnFailureListener { error ->
            onResult(
                Result.failure(
                    convertAuthError(error)
                )
            )
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

                if (!document.exists()) {
                    auth.signOut()

                    onResult(
                        Result.failure(
                            IllegalStateException(
                                "La cuenta existe, pero no tiene un perfil en Firestore."
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                val balance = (
                        document.get(FIELD_BALANCE) as? Number
                        )?.toDouble() ?: 0.0

                val userProfile = UserProfile(
                    uid = firebaseUser.uid,
                    nombre = document.getString(FIELD_NAME)
                        ?: "Usuario",
                    correo = document.getString(FIELD_EMAIL)
                        ?: firebaseUser.email.orEmpty(),
                    saldo = balance
                )

                onResult(
                    Result.success(userProfile)
                )
            }
            .addOnFailureListener {
                onResult(
                    Result.failure(
                        IllegalStateException(
                            "No fue posible consultar el perfil del usuario."
                        )
                    )
                )
            }
    }

    private fun convertAuthError(
        error: Exception
    ): Exception {
        val message = when (error) {
            is FirebaseAuthInvalidCredentialsException -> {
                "El correo o la contraseña son incorrectos."
            }

            is FirebaseAuthUserCollisionException -> {
                "Ya existe una cuenta registrada con este correo."
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
        const val FIELD_BALANCE = "saldo"
        const val FIELD_CREATED_AT = "fechaCreacion"

        const val INITIAL_BALANCE = 1000.0
    }
}