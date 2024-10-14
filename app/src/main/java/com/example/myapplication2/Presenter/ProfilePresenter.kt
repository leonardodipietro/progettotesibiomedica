package com.example.myapplication2.Presenter

import com.example.myapplication2.interfacepackage.ProfilePresenterInterface
import com.example.myapplication2.interfacepackage.ProfileView
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.UserRepo

class ProfilePresenter(private val view: ProfileView, private val userRepo: UserRepo) {
    fun loadUserData(userId: String) {
        userRepo.getUserData(userId) { user ->
            if (user != null) {
                view.populateUserData(user)
            } else {
                view.showError("Dati utente non trovati")
            }
        }
    }

    fun saveUserData(
        userId: String,
        email: String,
        phone: String,
        username: String,
        name: String,
        address: String,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        // Aggiorna l'email
        if (email.isNotEmpty()) {
            userRepo.updateUserEmail(userId, email) { success ->
                if (success) view.showSuccess("Email aggiornata") else view.showError("Errore nell'aggiornamento dell'email")
            }
        }

        // Aggiorna il numero di telefono
        if (phone.isNotEmpty()) {
            userRepo.updatePhoneNumber(userId, phone) { success ->
                if (success) view.showSuccess("Numero di telefono aggiornato") else view.showError("Errore nell'aggiornamento del numero di telefono")
            }
        }

        // Aggiorna lo username
        if (username.isNotEmpty()) {
            userRepo.updateUsername(userId, username) { success ->
                if (success) view.showSuccess("Username aggiornato") else view.showError("Errore nell'aggiornamento dello username")
            }
        }

        // Aggiorna il nome
        if (name.isNotEmpty()) {
            userRepo.updateName(userId, name) { success ->
                if (success) view.showSuccess("Nome aggiornato") else view.showError("Errore nell'aggiornamento del nome")
            }
        }

        // Aggiorna l'indirizzo
        if (address.isNotEmpty()) {
            userRepo.updateAddress(userId, address) { success ->
                if (success) view.showSuccess("Indirizzo aggiornato") else view.showError("Errore nell'aggiornamento dell'indirizzo")
            }
        }

        // Cambio password
        if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
            userRepo.changePassword(userId, oldPassword, newPassword) { success ->
                if (success) view.showSuccess("Password aggiornata") else view.showError("Errore nel cambio password")
            }
        } else if (newPassword != confirmPassword) {
            view.showError("Le nuove password non coincidono")
        }
    }

    fun logout() {
        view.navigateToHome()
    }

    fun deleteAccount(user: Utente) {
        view.confirmDeletion()
    }

    fun confirmDeletion(user: Utente) {
        user?.id?.let {
            userRepo.deleteAccount(it) { success ->
                if (success) {
                    view.showSuccess("Account eliminato")
                    view.navigateToHome()
                } else {
                    view.showError("Errore eliminazione account")
                }
            }
        }
    }

    fun verifyPhone(code: String) {
        // Logica di verifica
        view.showSuccess("Numero verificato")
    }
}


