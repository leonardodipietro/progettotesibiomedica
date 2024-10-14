package com.example.myapplication2.Presenter


import android.util.Log
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
            userRepo.verifyUserCredentials(username, password) { isSuccess, errorMessage, admin, user ->
                if (isSuccess) {
                    resetLock()
                    if (admin != null) {
                        view.showLoginSuccess(admin, user)
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
            userRepo.getEmailByUsername(username) { email, error ->
                if (email != null) {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                view.showResetPasswordEmailSent()
                                Log.d("ResetPassword", "Email di reset inviata a: $email")
                            } else {
                                view.showResetPasswordError("Errore nell'invio dell'email di reset. Controlla l'email associata.")
                                Log.d("ResetPassword", "Errore nell'invio dell'email di reset: ${task.exception?.message}")
                            }
                        }
                } else {
                    view.showResetPasswordError(error ?: "Errore sconosciuto")
                }
            }
        } else {
            view.promptForUsername()
        }
    }

}
