package com.example.myapplication2.interfacepackage

import com.example.myapplication2.model.Utente

    interface LoginInterface {
       // fun showResetPasswordNotification(username:String)
        // Metodi che la View implementa per gestire i feedback
        //fun showLoginSuccess(admin: Boolean, user: Utente?)
        fun showLoginSuccess(ruolo: String, user: Utente?)
        fun showLoginFailure(errorMessage: String)
        fun showAccountLocked(remainingTime: Long)
        fun showResetPasswordEmailSent()
        fun showResetPasswordError(errorMessage: String)

        fun promptForUsername()
        fun togglePasswordVisibility(isVisible: Boolean)

        // Metodi che la View chiama per interagire con il Presenter
        fun onLoginClicked(username: String, password: String)
        fun onResetPasswordClicked(username: String)
        fun onShowPasswordClicked()

        fun showVerificationDialog(username: String, verificationId: String)
    }

