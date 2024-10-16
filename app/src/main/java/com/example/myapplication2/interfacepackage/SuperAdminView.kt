package com.example.myapplication2.interfacepackage

import com.example.myapplication2.model.Utente

interface SuperAdminView {
    fun showError(message: String)
    fun showSuccess(message: String)
    fun showLogoutConfirmation()
    fun returnToMain()

    fun saveUserToPreferences(user: Utente)
    fun clearUserPreferences()

    fun loadUserFromPreferences(): Utente?

    fun clearInputFields()
}