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

        fun showNewPhoneVerificationDialog(newPhone: String, onCodeEntered: (String) -> Unit)

        fun showPasswordDialog(email: String, hashedPassword: String, onPasswordConfirmed: (String) -> Unit)
        fun showPhoneVerificationDialog(phoneNumber: String, onCodeEntered: (String) -> Unit)

}

