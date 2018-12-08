package com.example.ullas.fingerprint

interface AuthenticationListener {

    fun onAuthenticationSuccess(decryptPassword: String)

    fun onAuthenticationFailure(error: String? = null)
}