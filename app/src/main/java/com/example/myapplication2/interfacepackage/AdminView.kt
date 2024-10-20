package com.example.myapplication2.interfacepackage

import com.example.myapplication2.model.Sintomo
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
    fun showSintomiList(nomiSintomi: List<String>, idSintomi: List<String>)
    fun showSintomiListUser(sintomi: List<Pair<Sintomo, String>>)

    // Messaggi di errore e successo
    fun showError(message: String)
    fun showUpdateSuccess(message: String)
    fun showUpdateError(message: String)

    // Gestione esportazione dati
    fun showExportSuccessMessage()
    fun showExportErrorMessage()

    // Gestione dei permessi
    fun requestWritePermission()
    fun showPermissionDeniedError()
    fun hasWritePermission(): Boolean

    // Gestione logout
    fun showLogoutConfirmation()
    fun returnToMain()

    // Preferenze utente
    fun saveUserToPreferences(user: Utente)
    fun loadUserFromPreferences(): Utente?
    fun clearUserPreferences()
}
