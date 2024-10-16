package com.example.myapplication2.Presenter

import android.app.Activity
import com.example.myapplication2.interfacepackage.ProfileView
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import org.mindrot.jbcrypt.BCrypt
import java.util.concurrent.TimeUnit
import android.util.Log

class ProfilePresenter(private val view: ProfileView, private val userRepo: UserRepo) {
    private var verificationId: String? = null
    private var phoneNumber: String? = null
    private var auth = FirebaseAuth.getInstance()
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

        if(oldPassword.isNotEmpty()) {
            if (email.isNotEmpty()) {
                // Step 1: Recupera l'email corrente
                userRepo.getUserEmail(userId) { currentEmail ->
                    if (currentEmail != null && currentEmail != email) {
                        // Step 2: Autentica temporaneamente l'utente con l'email corrente e oldPassword
                        val credential = EmailAuthProvider.getCredential(currentEmail, oldPassword)
                        FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnCompleteListener { authTask ->
                                if (authTask.isSuccessful) {
                                    // Step 3: Invia richiesta di verifica per l'aggiornamento dell'email su Firebase Authentication
                                    val firebaseUser = auth.currentUser
                                    Log.d("AUTH USER","AUTH USER $firebaseUser")
                                    firebaseUser?.verifyBeforeUpdateEmail(email)
                                        ?.addOnCompleteListener { updateTask ->
                                            if (updateTask.isSuccessful) {
                                                // Step 4: Aggiorna l'email nel database
                                                userRepo.updateUserEmail(userId, email) { success ->
                                                    if (success) {
                                                        view.showSuccess("Email di verifica inviata. Controlla la nuova email per confermare il cambiamento.")
                                                    } else {
                                                        view.showError("Errore nell'aggiornamento dell'email nel database")
                                                    }
                                                }
                                            } else {
                                                view.showError("Errore nell'invio della richiesta di verifica su Firebase Authentication")
                                                val error = updateTask.exception?.message ?: "Errore sconosciuto"
                                                Log.e("verifyBeforeUpdateEmail", "Errore: $error")
                                            }
                                        }
                                } else {
                                    userRepo.updateUserEmail(userId, email) { success ->
                                        if (success) {
                                            view.showSuccess("Email aggiornata correttamente")
                                            // Cambio password
                                            if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
                                                userRepo.changePassword(userId, oldPassword, newPassword) { success ->
                                                    if (success) view.showSuccess("Password aggiornata") else view.showError("Errore nel cambio password")
                                                }
                                            } else if (newPassword != confirmPassword) {
                                                view.showError("Le nuove password non coincidono")
                                            }
                                        } else {
                                            view.showError("Errore nell'aggiornamento dell'email nel database")
                                            // Cambio password
                                            if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
                                                userRepo.changePassword(userId, oldPassword, newPassword) { success ->
                                                    if (success) view.showSuccess("Password aggiornata") else view.showError("Errore nel cambio password")
                                                }
                                            } else if (newPassword != confirmPassword) {
                                                view.showError("Le nuove password non coincidono")
                                            }
                                        }
                                    }
                                }
                            }
                    } else if (currentEmail == email) {
                        view.showError("La nuova email coincide con quella attuale")
                        // Cambio password
                        if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
                            userRepo.changePassword(userId, oldPassword, newPassword) { success ->
                                if (success) view.showSuccess("Password aggiornata") else view.showError("Errore nel cambio password")
                            }
                        } else if (newPassword != confirmPassword) {
                            view.showError("Le nuove password non coincidono")
                        }
                    } else {
                        view.showError("Impossibile recuperare l'email corrente")
                        // Cambio password
                        if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
                            userRepo.changePassword(userId, oldPassword, newPassword) { success ->
                                if (success) view.showSuccess("Password aggiornata") else view.showError("Errore nel cambio password")
                            }
                        } else if (newPassword != confirmPassword) {
                            view.showError("Le nuove password non coincidono")
                        }
                    }
                }
            } else {
                view.showError("L'email non può essere vuota")
                // Cambio password
                if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
                    userRepo.changePassword(userId, oldPassword, newPassword) { success ->
                        if (success) view.showSuccess("Password aggiornata") else view.showError("Errore nel cambio password")
                    }
                } else if (newPassword != confirmPassword) {
                    view.showError("Le nuove password non coincidono")
                }
            }
        }


        if (phone.isNotEmpty()) {
            userRepo.checkPhoneNumberExists(phone) { exists ->
                if (exists) {
                    view.showError("Numero di telefono già in uso, seleziona un altro")
                } else {
                    userRepo.updatePhoneNumber(userId, phone) { success ->
                        if (success) view.showSuccess("Numero di telefono aggiornato")
                        else view.showError("Errore nell'aggiornamento del numero di telefono")
                    }
                }
            }
        }


          // Aggiorna lo username solo se non è già in uso
        if (username.isNotEmpty()) {
            userRepo.checkUsernameExists(username) { exists ->
                if (exists) {
                    view.showError("Username già in uso, selezionare un altro")
                } else {
                    userRepo.updateUsername(userId, username) { success ->
                        if (success) view.showSuccess("Username aggiornato") else view.showError("Errore nell'aggiornamento dello username")
                    }
                }
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
        /*if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
            userRepo.changePassword(userId, oldPassword, newPassword) { success ->
                if (success) view.showSuccess("Password aggiornata") else view.showError("Errore nel cambio password")
            }
        } else if (newPassword != confirmPassword) {
            view.showError("Le nuove password non coincidono")
        }*/
    }

    fun logout() {
        view.navigateToHome()
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
    fun deleteAccount(user: Utente) {
        user.id?.let { userId ->
            userRepo.getUserData(userId) { userData ->
                if (userData != null) {
                    if (userData.email != null && userData.password != null) {
                        view.showPasswordDialog(userData.email, userData.password) { password ->
                            if (BCrypt.checkpw(password, userData.password)) {
                                auth.signInWithEmailAndPassword(userData.email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            deleteUserAccount(userId)
                                        } else {
                                            view.showError("Errore di autenticazione.")
                                        }
                                    }
                            } else {
                                view.showError("Password non valida")
                            }
                        }
                    } else if (userData.phoneNumber != null) {
                        startPhoneVerification(userData.phoneNumber)
                    }
                }
            }
        }
    }

    fun startPhoneVerification(phoneNumber: String) {
        this.phoneNumber = phoneNumber
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(view as Activity)
            .setCallbacks(verificationCallbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val verificationCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            view.showError("Errore di verifica telefonica: ${e.message}")
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            this@ProfilePresenter.verificationId = verificationId
            phoneNumber?.let {
                view.showPhoneVerificationDialog(it) { verificationCode ->
                    val cred = PhoneAuthProvider.getCredential(verificationId, verificationCode)
                    signInWithPhoneAuthCredential(cred)
                }
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                auth.uid?.let { deleteUserAccount(it) }
            } else {
                view.showError("Errore di autenticazione.")
            }
        }
    }

    private fun deleteUserAccount(userId: String) {
        auth.currentUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                userRepo.deleteAccount(userId) { success ->
                    if (success) {
                        view.showSuccess("Account eliminato")
                        view.navigateToHome()
                    } else {
                        view.showError("Errore nell'eliminazione.")
                    }
                }
            } else {
                view.showError("Errore nell'eliminazione dell'account.")
            }
        }
    }
}




