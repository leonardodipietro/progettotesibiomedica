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

            if (email.isNotEmpty()) {
                if (oldPassword.isNotEmpty()) {
                    // Step 1: Recupera l'email corrente
                    userRepo.getUserEmail(userId) { currentEmail ->
                        if (!currentEmail.isNullOrEmpty() && currentEmail != email) {
                            // Step 2: Autentica temporaneamente l'utente con l'email corrente e oldPassword
                            val credential = EmailAuthProvider.getCredential(currentEmail, oldPassword)
                            FirebaseAuth.getInstance().signInWithCredential(credential)
                                .addOnCompleteListener { authTask ->
                                    if (authTask.isSuccessful) {
                                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                                        firebaseUser?.verifyBeforeUpdateEmail(email)
                                            ?.addOnCompleteListener { updateTask ->
                                                if (updateTask.isSuccessful) {
                                                    userRepo.updateUserEmail(userId, email) { success ->
                                                        if (success) {
                                                            view.showSuccess("Email di verifica inviata. Controlla la nuova email per confermare il cambiamento.")
                                                        } else {
                                                            view.showError("Errore nell'aggiornamento dell'email nel database")
                                                        }
                                                    }
                                                } else {
                                                    view.showError("Errore nell'invio della richiesta di verifica")
                                                    Log.e("verifyBeforeUpdateEmail", "Errore: ${updateTask.exception?.message ?: "Errore sconosciuto"}")
                                                }
                                            }
                                    } else {
                                        //view.showError("Errore nell'autenticazione con le credenziali fornite")
                                        updateEmailDirectly(userId, email)
                                    }
                                }
                        } else if (currentEmail.isNullOrEmpty()) {
                            // Se l'email è vuota, chiama direttamente updateEmailDirectly
                            updateEmailDirectly(userId, email)
                        } else if (currentEmail == email) {
                            view.showError("La nuova email coincide con quella attuale")
                            updatePassword(userId, oldPassword, newPassword, confirmPassword)
                        } else {
                            view.showError("Impossibile recuperare l'email corrente")
                            updatePassword(userId, oldPassword, newPassword, confirmPassword)
                        }
                    }
                } else {
                    view.showError("Inserisci la vecchia password nell'apposito spazio")
                }
            } else {
                //view.showError("L'email non può essere vuota")
            }





        // Phone update
        userRepo.getUserPhoneNumber(userId) { currentPhone ->
            if (phone.isNotEmpty() && currentPhone != phone) {
                userRepo.checkPhoneNumberExists(phone) { exists ->
                    if (!exists) {
                        userRepo.updatePhoneNumber(userId, phone) { success ->
                            if (success) {
                                view.showSuccess("Numero di telefono aggiornato")
                            }
                        }
                    }
                }
            }
        }

        // Username update
        userRepo.getUsername(userId) { currentUsername ->
            if (username.isNotEmpty() && currentUsername != username) {
                userRepo.checkUsernameExists(username) { exists ->
                    if (!exists) {
                        userRepo.updateUsername(userId, username) { success ->
                            if (success) {
                                view.showSuccess("Username aggiornato")
                            }
                        }
                    }
                }
            }
        }

        // Name update
        userRepo.getName(userId) { currentName ->
            if (name.isNotEmpty() && currentName != name) {
                userRepo.updateName(userId, name) { success ->
                    if (success) {
                        view.showSuccess("Nome aggiornato")
                    }
                }
            }
        }

        // Address update
        userRepo.getAddress(userId) { currentAddress ->
            if (address.isNotEmpty() && currentAddress != address) {
                userRepo.updateAddress(userId, address) { success ->
                    if (success) {
                        view.showSuccess("Indirizzo aggiornato")
                    }
                }
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
// Funzione per aggiornare direttamente l'email nel database se l'autenticazione fallisce
private fun updateEmailDirectly(userId: String, email: String) {
    userRepo.updateUserEmail(userId, email) { success ->
        if (success) {
            view.showSuccess("Email aggiornata correttamente")
        } else {
            view.showError("Errore nell'aggiornamento dell'email nel database")
        }
    }
}

// Funzione per aggiornare la password
private fun updatePassword(userId: String, oldPassword: String, newPassword: String, confirmPassword: String) {
    if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
        userRepo.changePassword(userId, oldPassword, newPassword) { success ->
            if (success) view.showSuccess("Password aggiornata") else view.showError("Errore nel cambio password")
        }
    } else if (newPassword != confirmPassword) {
        view.showError("Le nuove password non coincidono")
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




