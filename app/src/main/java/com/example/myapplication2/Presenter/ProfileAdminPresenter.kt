package com.example.myapplication2.Presenter

import com.example.myapplication2.ProfileAdminActivity
import com.example.myapplication2.repository.UserRepo

class ProfileAdminPresenter(
    private val view: ProfileAdminActivity,
    private val userRepo: UserRepo
) {
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
        if (email.isNotEmpty())

            /*{userRepo.updateUserEmail(userId, email) { success ->
                if (success) view.showUpdateSuccess("Email aggiornata con successo")
                else view.showUpdateError("Errore nell'aggiornamento dell'email")
            }}*/



        // Aggiorna il numero di telefono
        if (phone.isNotEmpty()) {
            userRepo.updatePhoneNumber(userId, phone) { success ->
                if (success) view.showUpdateSuccess("Numero di telefono aggiornato con successo")
                else view.showUpdateError("Errore nell'aggiornamento del numero di telefono")
            }
        }

        // Aggiorna lo username
        if (username.isNotEmpty()) {
            userRepo.updateUsername(userId, username) { success ->
                if (success) view.showUpdateSuccess("Username aggiornato con successo")
                else view.showUpdateError("Errore nell'aggiornamento dello username")
            }
        }

        // Cambio password
        if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
            userRepo.changePassword(userId, oldPassword, newPassword) { success ->
                if (success) view.showUpdateSuccess("Password aggiornata con successo")
                else view.showUpdateError("Errore nel cambio password")
            }
        } else if (newPassword != confirmPassword) {
            view.showError("Le nuove password non coincidono")
        }
    }

    fun loadUserData(userId: String) {
        userRepo.getUserData(userId) { user ->
            if (user != null) {
                view.populateUserData(user)
            } else {
                view.showError("Dati utente non trovati")
            }
        }
    }
}
