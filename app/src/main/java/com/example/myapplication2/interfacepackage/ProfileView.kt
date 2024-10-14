package com.example.myapplication2.interfacepackage

import com.example.myapplication2.model.Utente

interface ProfileView {
        fun populateUserData(user: Utente)
        fun showSuccess(message: String)
        fun showError(message: String)
        fun confirmDeletion()
        fun navigateToHome()
        fun requestPhoneVerification(phone: String)
        fun clearUserPreferences() // Aggiunto
        fun stopNotification() //
}

