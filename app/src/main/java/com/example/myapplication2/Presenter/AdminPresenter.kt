package com.example.myapplication2.Presenter

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.myapplication2.interfacepackage.AdminView
import com.example.myapplication2.interfacepackage.PasswordType
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.ExportRepo
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.google.gson.Gson

class AdminPresenter(
    private val view: AdminView,
    private val userRepo: UserRepo,
    private val sintomoRepo: SintomoRepo,
    private val exportRepo: ExportRepo,
    private val context: Context
) {



   /* fun loadUserFromPreferences(): Utente? {
        return view.loadUserFromPreferences()
    }
*/


    // Funzione per rimuovere i dati utente dalle Shared Preferences
   /* fun clearUserPreferences() {
        val sharedPreferences = context.getSharedPreferences(USER_PREFERENCES, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }*/

    fun addSintomo(sintomo: String) {
        if (sintomo.isNotEmpty()) {
            sintomoRepo.aggiungiSintomo(sintomo) { success ->
                if (success) view.showAddSintomoSuccess()
                else view.showAddSintomoError()
            }
        }
    }
    fun saveUserData(
        userId: String,
        email: String,
        phone: String,
        username: String,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        // Aggiorna l'email
        if (email.isNotEmpty()) {
            userRepo.updateUserEmail(userId, email) { success ->
                //if (success) view.showSuccess("Email aggiornata") else view.showError("Errore nell'aggiornamento dell'email")
            }
        }

        // Aggiorna il numero di telefono
        if (phone.isNotEmpty()) {
            userRepo.updatePhoneNumber(userId, phone) { success ->
                //if (success) view.showSuccess("Numero di telefono aggiornato") else view.showError("Errore nell'aggiornamento del numero di telefono")
            }
        }

        // Aggiorna lo username
        if (username.isNotEmpty()) {
            userRepo.updateUsername(userId, username) { success ->
                //if (success) view.showSuccess("Username aggiornato") else view.showError("Errore nell'aggiornamento dello username")
            }
        }


        // Cambio password
        if (newPassword.isNotEmpty() && newPassword == confirmPassword) {

            userRepo.changePassword(userId, oldPassword, newPassword) { success ->
                Log.d("PasswordChange", "Cambio password riuscito")
                //if (success) view.showSuccess("Password aggiornata") else view.showError("Errore nel cambio password")
            }
        } else if (newPassword != confirmPassword) {

            view.showError("Le nuove password non coincidono")
        }
    }

    fun loadSintomi() {
        val nomiSintomi = mutableListOf<String>()
        val idSintomi = mutableListOf<String>()

        // Supponiamo che `caricaSintomi` popoli entrambe le liste.
        sintomoRepo.caricaSintomi(nomiSintomi, idSintomi) {
            view.showSintomiList(nomiSintomi, idSintomi)
        }
    }
    fun removeSintomo(sintomoId: String) {
        sintomoRepo.rimuoviSintomo(sintomoId) { success ->
            if (success) {
                view.showRemoveSintomoSuccess()
                loadSintomi() // Ricarica la lista dei sintomi dopo la rimozione
            } else {
                view.showRemoveSintomoError()
            }
        }
    }

    fun updateUserEmail(userId: String, email: String) {
        userRepo.updateUserEmail(userId, email) { success ->
            if (success) view.showUpdateSuccess("Email aggiornata con successo")
            else view.showUpdateError("Errore nell'aggiornamento dell'email")
        }
    }
    fun loadUserData(intentUser: Utente?) {
        val user = intentUser ?: view.loadUserFromPreferences()
        if (user != null) {
            view.saveUserToPreferences(user)
            //view.showUserWelcomeMessage(user.username ?: "")
            //scheduleNotificationsIfNeeded(user.id)
        } else {
            view.showError("Utente non trovato, accesso negato.")
            // Eventuale gestione dell'errore per la mancata autenticazione
        }
    }


    fun logout() {
        view.showLogoutConfirmation()
    }

    fun confirmLogout() {
        view.returnToMain()
    }

    fun exportToExcel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Controllo se il permesso è stato già concesso
            if (view.hasWritePermission()) {
                // Chiama il metodo di esportazione
                exportRepo.fetchDataAndGenerateExcel(context)
            } else {
                // Richiede il permesso se non è stato concesso
                view.requestWritePermission()
            }
        } else {
            // Esporta direttamente per versioni di Android inferiori a M
            exportRepo.fetchDataAndGenerateExcel(context)
        }
    }

    fun onRequestPermissionsResult(granted: Boolean, context: Context) {
        if (granted) {
            exportRepo.fetchDataAndGenerateExcel(context)
        } else {
            view.showPermissionDeniedError()
        }
    }


}



