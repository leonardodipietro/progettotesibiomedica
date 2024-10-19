package com.example.myapplication2.Presenter


import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication2.interfacepackage.LoginInterface
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.auth.FirebaseAuth

class LoginPresenter(
    private val view: LoginInterface,
    private val userRepo: UserRepo
) {

    private var loginAttempts = 0
    private var lockStartTime: Long = 0
    private var lockDuration: Long = 0
    private var isPasswordVisible = false

    fun handleLogin(input: String, password: String) {
        if (isAccountLocked()) {
            val remainingTime = (lockStartTime + lockDuration - System.currentTimeMillis()) / 1000
            view.showAccountLocked(remainingTime)
            return
        }
        when {
            input.contains("@") -> {
                Log.d("handleLogin", "Rilevata email: $input")
                verifyLoginByEmail(input, password)
            }
            input.matches(Regex("^\\+[0-9]+$")) -> {
                Log.d("handleLogin", "Rilevato numero di telefono: $input")
                verifyLoginByPhoneNumber(input, password)
            }
            else -> {
                Log.d("handleLogin", "Rilevato username: $input")
                verifyLoginByUsername(input, password)
            }
        }



       /* if (username.isNotEmpty() && password.isNotEmpty()) {
            //ricambiare con admin se necessario
            userRepo.verifyUserCredentials(username, password) { isSuccess, errorMessage, ruolo, user ->
                if (isSuccess) {
                    resetLock()
                    if (ruolo != null) {
                        Log.d("LoginPresenter", "Accesso riuscito, utente admin: $ruolo,,,, $user")
                        view.showLoginSuccess(ruolo, user)
                    }
                } else {
                    handleFailedLogin()
                    if (errorMessage != null) {
                        view.showLoginFailure(errorMessage)
                    }
                }
            }
        } else {
            view.promptForUsername()
        }*/
    }

    private fun verifyLoginByEmail(email: String, password: String) {
        userRepo.verifyUserByEmail(email, password) { isSuccess, errorMessage, ruolo, user ->
            handleLoginResult(isSuccess, errorMessage, ruolo, user)
        }
    }

    private fun verifyLoginByPhoneNumber(phoneNumber: String, password: String) {
        userRepo.verifyUserByPhone(phoneNumber, password) { isSuccess, errorMessage, ruolo, user ->
            handleLoginResult(isSuccess, errorMessage, ruolo, user)
        }
    }

    private fun verifyLoginByUsername(username: String, password: String) {
        userRepo.verifyUserCredentials(username, password) { isSuccess, errorMessage, ruolo, user ->
            handleLoginResult(isSuccess, errorMessage, ruolo, user)
        }
    }

    private fun handleLoginResult(isSuccess: Boolean, errorMessage: String?, ruolo: String?, user: Utente?) {
        if (isSuccess) {
            view.showLoginSuccess(ruolo ?: "user", user)
        } else {
            handleFailedLogin()
            //da vedere  view.showLoginFailure(errorMessage ?: "Errore di autenticazione")
            // Messaggio d'errore specifico in base all'input
            val specificErrorMessage = when {
                errorMessage == "Utente non trovato" -> {
                    when {
                        // Messaggio specifico per email non trovata
                        user?.email?.contains("@") == true -> "Email non trovata. Verifica di aver inserito correttamente l'email."
                        // Messaggio specifico per numero di telefono non trovato
                        user?.phoneNumber?.matches(Regex("^\\+[0-9]+$")) == true -> "Numero di telefono non trovato. Verifica di aver inserito correttamente il numero."
                        // Messaggio specifico per username non trovato
                        else -> "Username non trovato. Verifica di aver inserito correttamente lo username."
                    }
                }
                // Messaggio di password errata
                errorMessage == "Password errata" -> "Password errata. Riprova o reimposta la password."
                // Altri errori generici
                else -> errorMessage ?: "Errore di autenticazione"
            }
            view.showLoginFailure(specificErrorMessage)
        }
    }







    fun handleShowPassword() {
        isPasswordVisible = !isPasswordVisible
        view.togglePasswordVisibility(isPasswordVisible)
    }

    private fun isAccountLocked(): Boolean {
        return System.currentTimeMillis() < lockStartTime + lockDuration
    }

    private fun handleFailedLogin() {
        loginAttempts++
        lockDuration = when (loginAttempts) {
            5 -> 10 * 60 * 1000
            10 -> 60 * 60 * 1000
            15 -> 24 * 60 * 60 * 1000
            else -> lockDuration
        }
        if (loginAttempts in listOf(5, 10, 15)) {
            lockStartTime = System.currentTimeMillis()
        }
    }

    private fun resetLock() {
        loginAttempts = 0
        lockStartTime = 0
        lockDuration = 0
    }

     fun handleResetPassword(username: String) {



        if (username.isNotEmpty()) {
            userRepo.checkUsernameExists(username) { exists ->
                if (exists) {
            userRepo.getEmailByUsername(username) { email, error ->
                if (email != null) {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                view.showResetPasswordEmailSent()
                                Log.d("ResetPassword", "Email inviata a: $email")
                            } else {
                                view.showResetPasswordError("Errore nell'invio dell'email di reset. Controlla l'email associata.")
                            }
                        }
                } else {
                    view.showResetPasswordNotification(username)
                }
            }
        } else {
            view.promptForUsername()
        }
    }
        }
     }



}
