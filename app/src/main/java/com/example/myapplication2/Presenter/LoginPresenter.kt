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

    fun handleLogin(username: String, password: String) {
        if (isAccountLocked()) {
            val remainingTime = (lockStartTime + lockDuration - System.currentTimeMillis()) / 1000
            view.showAccountLocked(remainingTime)
            return
        }

        if (username.isNotEmpty() && password.isNotEmpty()) {
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
