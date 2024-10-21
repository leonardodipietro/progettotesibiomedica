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
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.myapplication2.R
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.repository.ExAccountRepo
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator

class ProfilePresenter(private val view: ProfileView, private val userRepo: UserRepo) {
    private var verificationId: String? = null
    private var phoneNumber: String? = null
    private val exAccountRepo=ExAccountRepo()
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
    fun checkPhoneNumber(userId: String) {
        userRepo.getPhoneNumber(userId) { phoneNumber ->
            if (phoneNumber.isNullOrEmpty()) {
                Log.d("PhoneCheck", "Nessun numero di telefono trovato : $userId")
                view.showPhoneEditText()
            } else {
                Log.d("PhoneCheck", "Numero di telefono trovato: $phoneNumber")
                view.showPhoneNumber(phoneNumber) // Mostra il numero esistente
            }
        }
    }

    fun updatePhoneNumber(userId: String, newPhone: String) {
        if (newPhone.isNotEmpty()) {
            userRepo.updatePhoneNumber(userId, newPhone) { success ->
                if (success) {
                    view.showSuccess("Numero di telefono aggiornato con successo")
                } else {
                    view.showError("Errore nell'aggiornamento del numero di telefono")
                }
            }
        } else {
            view.showError("Inserisci un numero di telefono valido")
        }
    }


    private fun updatePhoneNumberInDatabase(userId: String, newPhone: String) {
        userRepo.updatePhoneNumber(userId, newPhone) { success ->
            if (success) {
                view.showSuccess("Numero di telefono aggiornato con successo ")

            } else {
                view.showError("Errore nell'aggiornamento del numero di telefono")

            }
        }
    }
    fun authenticateAndModifyPhoneNumber(userId: String, oldPhone: String, newPhone: String) {



        startPhoneVerificationModify(oldPhone, userId, true) { oldPhoneVerified ->
            if (oldPhoneVerified) {

                startPhoneVerificationModify(newPhone, userId, false) { newPhoneVerified ->
                    if (newPhoneVerified) {


                        // Step 3: Aggiorna il numero di telefono nel database
                        userRepo.updatePhoneNumber(userId, newPhone) { success ->
                            if (success) {
                                view.showSuccess("Numero di telefono aggiornato con successo")

                            } else {
                                view.showError("Errore nell'aggiornamento del numero di telefono")

                            }
                        }
                    } else {
                        view.showError("Verifica del nuovo numero fallita.")

                    }
                }
            } else {
                //view.showError("Verifica del vecchio numero fallita.")

                //Log.e("PhoneUpdate", "Verifica del vecchio numero fallita: $oldPhone")
            }
        }
    }


    private fun startPhoneVerificationModify(phoneNumber: String, userId: String, isOldPhone: Boolean, onPhoneVerified: (Boolean) -> Unit = {}) {


        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(view as Activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {


                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("PhoneUpdate", "Autenticazione completata $phoneNumber")
                            onPhoneVerified(true)
                        } else {
                            view.showError("Errore nell'autenticazione.")
                            Log.e("PhoneUpdate", "Errore: ${task.exception?.message}")
                            onPhoneVerified(false)
                        }
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {

                    Log.e("PhoneUpdate", "Errore di verifica per $phoneNumber: ${e.message}")
                    //onPhoneVerified(false)
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {


                    // Mostra il dialogo solo dopo che il codice è stato inviato
                    view.showNewPhoneVerificationDialog(phoneNumber) { verificationCode ->
                        val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)

                        Log.d("PhoneUpdate", "Tentativo di verifica  $verificationCode")
                        signInWithPhoneAuthCredentialForModify(credential, userId, phoneNumber) { verified ->
                            Log.d("PhoneUpdate", "Verifica completata $phoneNumber")
                            onPhoneVerified(verified)
                        }
                    }
                }
            })
            .build()

        Log.d("PhoneUpdate", "Chiamo PhoneAuthProvider.verifyPhoneNumber per $phoneNumber")
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    private fun signInWithPhoneAuthCredentialForModify(
        credential: PhoneAuthCredential,
        userId: String,
        phoneNumber: String,
        onPhoneVerified: (Boolean) -> Unit
    ) {


        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                onPhoneVerified(true)
                Log.d("PhoneUpdate", "Accesso completato  $phoneNumber")
            } else {
                // Se l'autenticazione con il vecchio numero fallisce
                Log.e("PhoneUpdate", "Errore : $phoneNumber - ${task.exception?.message}")


                Log.d("siamo arrivati qui","Il vecchio numero non è stato trovato in authfirebase Aggiorno comunque il numero.")

                updatePhoneNumberInDatabase(userId, phoneNumber)

                onPhoneVerified(false)
            }
        }
    }



    fun saveUserData(
        userId: String,
        email: String,
        //phone: String,
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
        /*userRepo.getUserPhoneNumber(userId) { currentPhone ->
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
        }*/

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
                    Log.d("deleteAccount", "Dati utente recuperati: $userData")

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
                    } else {
                        Log.e("deleteAccount", "Nessuna possibilita di auth")
                    }
                } else {
                    Log.e("deleteAccount", "Dati utente non recuperati ")
                }
            }
        } ?: run {
            Log.e("deleteAccount", "ID utente non disponibile")
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
        val exAccountRepo = ExAccountRepo()

        exAccountRepo.creaUtenteEliminato(userId) { success ->
            if (success) {
                auth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) {
                        Log.d("deleteUserAccount", "Utente eliminato da Firebase Authentication: $userId")
                        // Elimina anche l'account dal Realtime Database
                        userRepo.deleteAccount(userId) { success ->
                            if (success) {
                                Log.d("deleteUserAccount", "Account eliminato dal db $userId")
                                view.showSuccess("Account eliminato")
                                view.navigateToHome()
                            } else {
                                view.showError("Errore nell'eliminazione dell'account.")
                            }
                        }
                    } else {
                        view.showError("Errore nell'eliminazione dell'account.")
                    }
                }
            } else {
                Log.d("deleteUserAccount", "boh")
            }
        }
    }


}



