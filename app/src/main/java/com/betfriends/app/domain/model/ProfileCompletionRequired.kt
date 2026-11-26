package com.betfriends.app.domain.model

/** La cuenta de Auth existe y esta sesión permite completar el perfil. */
class ProfileCompletionRequired(
    val uid: String,
    val suggestedName: String = "",
    cause: Throwable? = null
) : Exception("Completa tu perfil para continuar.", cause)
