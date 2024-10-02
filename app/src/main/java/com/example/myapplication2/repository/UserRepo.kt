package com.example.myapplication2.repository

import android.app.Activity
import android.util.Log
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.Utente
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class UserRepo {
    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
    private var auth = FirebaseAuth.getInstance()
    val usersRef = database.getReference("users")


    fun getUserData(uid: String, callback: (Utente?) -> Unit) {
        Log.d("UserRepo", "Inizio recupero dati per utente con UID: $uid")

        usersRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("UserRepo", "Dati trovati per l'utente con UID: $uid")
                    val utente = snapshot.getValue(Utente::class.java)
                    Log.d("UserRepo", "Dati utente: ${utente?.toString()}")
                    callback(utente)
                } else {
                    Log.d("UserRepo", "Nessun dato trovato per l'utente con UID: $uid")
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserRepo", "Errore nel recupero dei dati per l'utente con UID: $uid. Errore: ${error.message}")
                callback(null)
            }
        })
    }
    fun verifyUserCredentials(username: String, password: String, callback: (Boolean, String?) -> Unit) {
        Log.d("UserRepo", "Tentativo di login per utente: $username")

        // Esegui la query per recuperare tutti gli utenti
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var userFound = false

                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(Utente::class.java)

                        if (user != null && user.username == username) {
                            userFound = true
                            Log.d("UserRepo", "Username trovato nel database: ${user.username}")

                            // Verifica se la password hashata è corretta usando BCrypt
                            val hashedPassword = user.password
                            val isPasswordCorrect = BCrypt.verifyer().verify(password.toCharArray(), hashedPassword).verified
                            if (isPasswordCorrect) {
                                Log.d("UserRepo", "Password corretta per $username")

                                // Controlla se l'utente ha una email in Firebase Authentication
                                if (!user.email.isNullOrEmpty()) {
                                    Log.d("UserRepo", "Login tramite email: ${user.email}")
                                    signInWithFirebaseEmail(user.email, password, callback)
                                } else if (!user.phoneNumber.isNullOrEmpty()) {
                                    Log.d("UserRepo", "Login tramite numero di telefono: ${user.phoneNumber}")
                                    signInWithUsernameAndPassword(user.username, password, callback)
                                } else {
                                    callback(false, "Credenziali non valide")
                                }
                            } else {
                                callback(false, "Password errata")
                            }
                            return
                        }
                    }

                    if (!userFound) {
                        callback(false, "Username non trovato")
                    }
                } else {
                    callback(false, "Nessun utente trovato nel database")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false, "Errore del database: ${error.message}")
            }
        })
    }


    // Metodo per il login tramite email
    private fun signInWithFirebaseEmail(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        Log.d("UserRepo", "Login con telefono chiamata")
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("UserRepo", "Login con email riuscito")
                    callback(true, null)
                } else {
                    Log.e("UserRepo", "Errore nel login tramite email: ${task.exception?.message}")
                    callback(false, "Errore nel login tramite email")
                }
            }
    }

    private fun signInWithUsernameAndPassword(username: String, password: String, callback: (Boolean, String?) -> Unit) {
        // Effettua una verifica del numero di telefono tramite il backend, senza richiedere la verifica SMS
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Autenticazione anonima per gestire la sessione senza la verifica SMS
                    val user = FirebaseAuth.getInstance().currentUser
                    Log.d("Login", "Utente autenticato anonimamente: ${user?.uid}")
                    callback(true, null)
                } else {
                    callback(false, "Errore nel login anonimo: ${task.exception?.message}")
                    Log.d("TentativoLogin", "Utente autenticato anonimamente: ${task.exception?.message}")
                }
            }
    }

    fun saveUserToFirebase(username: String, hashedPassword: String) {
        Log.d("userrepo", "funzione chiamata")
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("userrepo", "CurrentUser non è null")
                val userRef = database.reference.child("users").child(currentUser.uid)

                // Creazione dell'oggetto utente con email, username e password hashata
                val nuovoUser = Utente(
                    id = currentUser.uid,
                    email = currentUser.email.toString(),
                    username = username,
                    password = hashedPassword
                )
                Log.d("userrepo", "Creazione oggetto completata: $nuovoUser")

                // Salva l'utente nel Realtime Database
                userRef.setValue(nuovoUser)
                    .addOnSuccessListener {
                        Log.d("userrepo", "Utente aggiunto con successo")
                    }
                    .addOnFailureListener { e ->
                        Log.d("userrepo", "Errore durante l'aggiunta dell'utente: $e")
                    }
            } else {
                Log.d("userrepo", "CurrentUser è null")
            }
        } catch (e: Exception) {
            Log.e("userrepo", "Eccezione: ${e.message}")
        }
    }


    fun deleteAccount(uid: String, callback: (Boolean) -> Unit) {
        // Elimina i dati dell'utente dal Realtime Database
        usersRef.child(uid).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true) // Dati eliminati con successo dal database
            } else {
                callback(false) // Errore nell'eliminazione dal database
            }
        }
    }
    fun savePhoneUserToFirebase(username: String, hashedPassword: String) {
        Log.d("userrepo", "Funzione chiamata per l'autenticazione con numero di telefono")
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("userrepo", "CurrentUser non è null (telefono)")
                val userRef = database.reference.child("users").child(currentUser.uid)

                // Aggiungiamo username e password hashata all'oggetto utente
                val nuovoUser = Utente(
                    id = currentUser.uid,
                    phoneNumber = currentUser.phoneNumber,
                    username = username,
                    password = hashedPassword
                )
                Log.d("userrepo", "Creazione oggetto completata $nuovoUser")
                userRef.setValue(nuovoUser)
                    .addOnSuccessListener {
                        Log.d("userrepo", "Utente con numero di telefono aggiunto")
                    }
                    .addOnFailureListener { e ->
                        Log.d("userrepo", "Utente non aggiunto: $e")
                    }
            } else {
                Log.d("userrepo", "CurrentUser è null (telefono)")
            }
        } catch (e: Exception) {
            Log.e("userrepo", "Eccezione: ${e.message}")
        }
    }


    /*
    fun submitSintomi(userId: String, sintomiList: List<Sintomo>) {

        // Estrai solo gli ID dei sintomi
        val sintomiIdList = sintomiList.map { it.id }

        val userSintomiRef = database.reference.child("users").child(userId).child("selectedSintomi")

        userSintomiRef.setValue(sintomiIdList).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("SubmitSintomi", "Sintomi inviati al db per: $userId")
            } else {
                Log.e("SubmitSintomi", "Errore nell'invio al db per : $userId", task.exception)
            }
        }
    }*/
    /*fun submitSintomi(userId: String, sintomiList: List<Sintomo>) {
        val userSintomiRef = database.reference.child("users").child(userId).child("selectedSintomi")

        // Ottieni il timestamp corrente nel formato desiderato
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val currentTime = dateFormat.format(Date())

        // Crea una mappa dei sintomi con il loro ID, gravità e timestamp
        val sintomiMap = sintomiList.associate { sintomo ->
            sintomo.id to mapOf(
                "gravita" to sintomo.gravita,
                "timestamp" to currentTime
            )
        }

        Log.d("SubmitSintomi", "Invio dei sintomi al DB: $sintomiMap")

        userSintomiRef.setValue(sintomiMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("SubmitSintomi", "Sintomi, gravità e timestamp inviati al db per: $userId")
            } else {
                Log.e("SubmitSintomi", "Errore nell'invio al db per: $userId", task.exception)
            }
        }
    }*/

    fun submitSintomi(userId: String, sintomiList: List<Sintomo>) {
        // Ottieni il riferimento alla posizione del database con il timestamp corrente
        val userSintomiRef = database.reference.child("users").child(userId).child("storicoSintomi")

        // Ottieni il timestamp corrente nel formato desiderato (es. 2024-09-29T15:30:00Z)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = dateFormat.format(Date())

        // Crea una mappa dei sintomi con il loro ID, gravità e timestamp
        val sintomiMap = sintomiList.associate { sintomo ->
            sintomo.id to mapOf(
                "gravita" to sintomo.gravita,
                "timestamp" to currentTime
            )
        }

        // Salva i sintomi con l'ID del sintomo come chiave nel nodo "storicoSintomi"
        userSintomiRef.child(currentTime).setValue(sintomiMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("SubmitSintomi", "Sintomi inviati al db per $userId nel timestamp $currentTime")
            } else {
                Log.e("SubmitSintomi", "Errore nell'invio al db per $userId", task.exception)
            }
        }
    }
    // Modifica dell'email
    fun updateEmail(newEmail: String) {
        val user = auth.currentUser
        user?.updateEmail(newEmail)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ProfileActivity", "Email aggiornata correttamente")
                    // Puoi aggiornare anche l'email nel database Realtime Firebase se necessario
                    val userRef = database.reference.child("users").child(user.uid)
                    userRef.child("email").setValue(newEmail)
                } else {
                    Log.d("ProfileActivity", "Errore nell'aggiornamento dell'email: ${task.exception?.message}")
                }
            }
    }

    // Modifica della password
    fun updatePassword(newPassword: String) {
        val user = auth.currentUser
        user?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ProfileActivity", "Password aggiornata correttamente")
                } else {
                    Log.d("ProfileActivity", "Errore nell'aggiornamento della password: ${task.exception?.message}")
                }
            }
    }
    // Inizia la verifica del numero di telefono
    fun updatePhoneNumber(newPhoneNumber: String, activity: Activity) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(newPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Collegare il nuovo numero di telefono all'utente
                    auth.currentUser?.updatePhoneNumber(credential)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("ProfileActivity", "Numero di telefono aggiornato correttamente")
                            } else {
                                Log.d("ProfileActivity", "Errore nell'aggiornamento del numero: ${task.exception?.message}")
                            }
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("ProfileActivity", "Errore nella verifica del numero: ${e.message}")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    // Gestisci l'invio del codice e richiedi all'utente di inserire il codice di verifica
                    // Puoi memorizzare `verificationId` e `token` per usarli successivamente
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun linkWithPhoneNumber(credential: PhoneAuthCredential) {
        val user = auth.currentUser
        user?.linkWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ProfileActivity", "Account collegato correttamente")
                } else {
                    Log.d("ProfileActivity", "Errore nel collegamento dell'account: ${task.exception?.message}")
                }
            }
    }

    fun linkWithEmailPassword(email: String, password: String) {
        val credential = EmailAuthProvider.getCredential(email, password)
        val user = auth.currentUser
        user?.linkWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ProfileActivity", "Account email/password collegato correttamente")
                } else {
                    Log.d("ProfileActivity", "Errore nel collegamento dell'account: ${task.exception?.message}")
                }
            }
    }



    fun removeSintomo(userId: String, sintomoId: String) {
        val userSintomiRef = database.reference.child("users").child(userId).child("selectedSintomi").child(sintomoId)

        userSintomiRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("RemoveSintomo", "Sintomo rimosso dal db per: $userId")
            } else {
                Log.e("RemoveSintomo", "Errore nella rimozione del sintomo dal db per: $userId", task.exception)
            }
        }
    }

    fun fetchSelectedSintomiForUser(userId: String, callback: (List<Sintomo>) -> Unit) {
        Log.d("fetchSelectedSintomi", "Inizio recupero per  $userId")

        val userSintomiRef = database.reference.child("users").child(userId).child("selectedSintomi")

        userSintomiRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val selectedSintomiIds = task.result.children.map { it.getValue(Long::class.java)?.toString() ?: "" }.filter { it.isNotEmpty() }

                //val selectedSintomiIds = task.result.children.map { it.getValue(String::class.java) ?: "" }.filter { it.isNotEmpty() }
                Log.d("fetchSelectedSintomi", "Recuperati ID dei sintomi selezionati: $selectedSintomiIds")

                val sintomiList = mutableListOf<Sintomo>()
                val sintomiRef = database.reference.child("sintomi")

                var count = 0
                if (selectedSintomiIds.isEmpty()) {
                    Log.d("fetchSelectedSintomi", "Nessun sintomo selezionato trovato per l'utente con ID: $userId")
                    callback(sintomiList)
                    return@addOnCompleteListener
                }

                for (sintomoId in selectedSintomiIds) {
                    Log.d("fetchSelectedSintomi", "Inizio recupero dati per il sintomo con ID: $sintomoId")

                    sintomiRef.child(sintomoId).get().addOnCompleteListener { sintomoTask ->
                        if (sintomoTask.isSuccessful) {
                            val sintomo = sintomoTask.result.getValue(Sintomo::class.java)
                            if (sintomo != null) {
                                Log.d("fetchSelectedSintomi", "Sintomo Nome: ${sintomo.nomeSintomo}")
                                sintomiList.add(sintomo)
                            } else {
                                Log.d("fetchSelectedSintomi", "Sintomo non trogvato")
                            }
                        } else {
                            Log.e("fetchSelectedSintomi", "Sintomo non trovato : $sintomoId", sintomoTask.exception)
                        }
                        count++
                        if (count == selectedSintomiIds.size) {
                            Log.d("fetchSelectedSintomi", "Fatto e riuscito")
                            callback(sintomiList)
                        }
                    }
                }
            } else {
                Log.e("fetchSelectedSintomi", "Errore nel recupero id")
                callback(emptyList())
            }
        }
    }



}