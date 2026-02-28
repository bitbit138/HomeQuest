package dev.tombit.homequest.interfaces

interface AuthCallback {
    fun onAuthSuccess()
    fun onAuthFailure(errorMessage: String)
}
