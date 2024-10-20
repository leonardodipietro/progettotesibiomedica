package com.example.myapplication2.interfacepackage

import com.example.myapplication2.model.Utente

    interface ProfileAdminView {

        // Visualizzazione dati utente
        fun showUserData(email: String?, phone: String?, username: String?)
        fun populateUserData(utente: Utente)
        fun showUserNotFoundError()

        // Gestione aggiornamenti dati utente
        fun showUpdateSuccess(message: String)
        fun showUpdateError(message: String)

        // Gestione password
        fun togglePasswordVisibility(passwordType: PasswordType, isVisible: Boolean)

        // Messaggi di errore
        fun showError(message: String)

        // Gestione logout
       /* fun showLogoutConfirmation()
        fun returnToMain()*/

        // Preferenze utente
        //fun loadUserFromPreferences(): Utente?
        fun saveUserToPreferences(user: Utente)
        fun clearUserPreferences()
    }
    enum class PasswordType {
    OLD_PASSWORD, NEW_PASSWORD, CONFIRM_PASSWORD
    }