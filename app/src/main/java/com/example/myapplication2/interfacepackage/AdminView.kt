package com.example.myapplication2.interfacepackage

import com.example.myapplication2.model.Utente


interface AdminView {
    // Gestione utente
    fun showUserData(email: String?, phone: String?, username: String?)
    fun showUserNotFoundError()

    // Gestione sintomi
    fun showAddSintomoSuccess()
    fun showAddSintomoError()
    fun showRemoveSintomoSuccess()
    fun showRemoveSintomoError()
    fun showSintomiList(sintomi: List<String>,sintomid: List<String>)
    fun showError(message: String)
    // Gestione modifica
    fun showUpdateSuccess(message: String)
    fun showUpdateError(message: String)


    // Visualizzazione password
    fun togglePasswordVisibility(passwordType: PasswordType, isVisible: Boolean)

    fun loadUserFromPreferences(): Utente?

    fun showLogoutConfirmation()
    fun returnToMain()
    fun hasWritePermission(): Boolean
    fun showExportSuccessMessage()
    fun showExportErrorMessage()
    // Esportazione dati
    fun requestWritePermission()
    fun showPermissionDeniedError()
    fun saveUserToPreferences(user: Utente)
    fun clearUserPreferences()
}

enum class PasswordType {
    OLD_PASSWORD, NEW_PASSWORD, CONFIRM_PASSWORD
}
