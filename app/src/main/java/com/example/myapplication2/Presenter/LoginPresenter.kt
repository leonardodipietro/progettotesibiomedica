package com.example.myapplication2.Presenter


import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication2.R
import com.example.myapplication2.interfacepackage.LoginInterface
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class LoginPresenter(
    private val view: LoginInterface,
    private val userRepo: UserRepo,
    private val context: Context
) {

    private var loginAttempts = 0
    private var lockStartTime: Long = 0
    private var lockDuration: Long = 0
    private var isPasswordVisible = false


    fun handleLogin(input: String, password: String) {
        if (isAccountLocked()) {
            val remainingTime = (lockStartTime + lockDuration - System.currentTimeMillis()) / 1000
            view.showAccountLocked(remainingTime)
            return }
        when {
            input.contains("@") -> {
                verifyLoginByEmail(input, password)
            }
            input.matches(Regex("^\\+[0-9]+$")) -> {
                verifyLoginByPhoneNumber(input, password)
            }
            else -> {
                verifyLoginByUsername(input, password)
            } } }
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
            val specificErrorMessage = when {
                errorMessage == "Utente non trovato" -> {
                    when {
                        user?.email?.contains("@") == true -> context.getString(R.string.dialog_error_email_not_found)
                        user?.phoneNumber?.matches(Regex("^\\+[0-9]+$")) == true -> context.getString(R.string.dialog_error_phone_not_found) // Usa la stringa localizzata
                        else -> context.getString(R.string.dialog_error_username_not_found)
                    }
                }
                errorMessage == "Password errata" -> context.getString(R.string.dialog_error_wrong_password)
                else -> errorMessage ?: context.getString(R.string.dialog_error_authentication_generic)
            }
            view.showLoginFailure(specificErrorMessage)
        } }
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
    fun handleShowPassword() {
        isPasswordVisible = !isPasswordVisible
        view.togglePasswordVisibility(isPasswordVisible)
    }

    private fun isAccountLocked(): Boolean {
        return System.currentTimeMillis() < lockStartTime + lockDuration
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
                            } } } else {
                    handleResetPasswordWithPhone(username)
                } } } else {
            view.promptForUsername()
        } } } }
    fun handleResetPasswordWithPhone(username: String) {
        if (username.isNotEmpty()) {
            // Recupera i dati utente dal database
            userRepo.getUserByUsername(username) { user ->
                if (user != null && user.phoneNumber != null) {
                    val phoneNumber = user.phoneNumber
                    sendPhoneAuthenticationCode(phoneNumber) { verificationId, error ->
                        if (verificationId != null) {
                            view.showVerificationDialog(username, verificationId)
                        } else {
                            view.showResetPasswordError(error ?: "Errore durante l'invio del codice di verifica.")
                        } }
                } else {
                    view.showResetPasswordError("Nessun numero di telefono associato a questo username.")
                } } } else {
            view.promptForUsername() } }

    private fun sendPhoneAuthenticationCode(
        phoneNumber: String,
        callback: (verificationId: String?, error: String?) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context as Activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Codice verificato automaticamente (non necessario qui)
                    callback(null, null)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // Errore durante la verifica
                    callback(null, e.message)
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    // Codice inviato con successo
                    callback(verificationId, null)
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    /*
    * fun handleResetPasswordWithPhone(username: String) {
    if (username.isNotEmpty()) {
        // Recupera i dati utente dal database
        userRepo.getUserByUsername(username) { user ->
            if (user != null && user.phoneNumber != null) {
                val phoneNumber = user.phoneNumber

                // Invia il codice di verifica al numero recuperato
                sendPhoneAuthenticationCode(phoneNumber) { verificationId, error ->
                    if (verificationId != null) {
                        // Mostra un dialogo per inserire il codice OTP
                        view.showVerificationDialog(username, verificationId)
                    } else {
                        view.showResetPasswordError(error ?: "Errore durante l'invio del codice di verifica.")
                    }
                }
            } else {
                view.showResetPasswordError("Nessun numero di telefono associato a questo username.")
            }
        }
    } else {
        view.promptForUsername()
    }
}

private fun sendPhoneAuthenticationCode(
    phoneNumber: String,
    callback: (verificationId: String?, error: String?) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val options = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(phoneNumber) // Numero di telefono dal database
        .setTimeout(60L, TimeUnit.SECONDS) // Timeout per il codice OTP
        .setActivity(context as Activity) // Activity corrente
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Codice verificato automaticamente (non necessario qui)
                callback(null, null)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // Errore durante la verifica
                callback(null, e.message)
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                // Codice inviato con successo
                callback(verificationId, null)
            }
        })
        .build()

    PhoneAuthProvider.verifyPhoneNumber(options)
}

    * */



}
