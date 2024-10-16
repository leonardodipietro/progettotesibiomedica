package com.example.myapplication2.repository

import android.app.Activity
import android.util.Log
//import at.favre.lib.crypto.bcrypt.BCrypt
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
import org.mindrot.jbcrypt.BCrypt
import java.util.Calendar

class UserRepo {
    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
    private var auth = FirebaseAuth.getInstance()
    val usersRef = database.getReference("users")


    fun getUserEmail(userId: String, callback: (String?) -> Unit) {
        val userRef = usersRef.child(userId).child("email")

        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val email = task.result?.getValue(String::class.java)
                Log.d("getUserEmail", "Email recuperata con successo: $email")
                callback(email) // Restituisce l'email trovata
            } else {
                callback(null) // In caso di errore, restituisce null
            }
        }
    }
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
    // Funzione per recuperare l'email associata a uno username
    fun getEmailByUsername(username: String, callback: (String?, String?) -> Unit) {
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var emailFound: String? = null

                // Cerca l'email associata allo username
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(Utente::class.java)
                    if (user != null && user.username == username) {
                        emailFound = user.email
                        break
                    }
                }

                // Passa l'email trovata al callback
                if (emailFound != null) {
                    callback(emailFound, null)
                } else {
                    callback(null, "Username non trovato")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null, "Errore del database: ${error.message}")
            }
        })
    }
    fun checkUsernameExists(username: String, callback: (Boolean) -> Unit) {
        val usernameRef = usersRef.orderByChild("username").equalTo(username)
        usernameRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("UserRepo", "Numero di utenti trovati con lo username $username: ${snapshot.childrenCount}")
                callback(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserRepo", "Errore nella query dello username: ${error.message}")
                callback(false)
            }
        })
    }
//al posto del terzo callback c era un booleano con ?
    fun verifyUserCredentials(username: String, password: String, callback: (Boolean, String?, String?, Utente?) -> Unit) {
        //val usersRef = FirebaseDatabase.getInstance().getReference("users")

        Log.d("verifyUserCredentials", "Inizio verifica credenziali per username: $username")

        // Ricerca l'utente con il campo username specificato
        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    val user = userSnapshot.getValue(Utente::class.java)

                    if (user != null) {
                        Log.d("verifyUserCredentials", "Utente trovato per username. Verifica email o numero di telefono...")

                        // Controlla se ha una email
                        if (user.email.isNullOrEmpty()) {
                            // Email non trovata, verifica numero di telefono
                            if (!user.phoneNumber.isNullOrEmpty()) {
                                Log.d("verifyUserCredentials", "Numero di telefono trovato per utente, verifica password...")

                                // Verifica la password
                                if (BCrypt.checkpw(password, user.password)) {
                                    Log.d("verifyUserCredentials", "Password corretta. Accesso come admin: ${user.ruolo}")
                                    val ruolo = user.ruolo
                                    callback(true, null, ruolo, user)
                                    /*val isAdmin = user.admin ?: false
                                    callback(true, null, isAdmin, user)*/
                                    Log.d("verifyUserCredentials", "Callback chiamato con successo per admin: ${user.ruolo} e utente: $user")
                                } else {
                                    Log.d("verifyUserCredentials", "Password errata per numero di telefono.")
                                    callback(false, "Password errata", null, null)
                                }
                            } else {
                                Log.d("verifyUserCredentials", "Nessun numero di telefono trovato per questo utente.")
                                callback(false, "Utente non trovato", null, null)
                            }
                        } else {
                            // Email trovata, verifica la password
                            Log.d("verifyUserCredentials", "Email trovata per utente, verifica password...")

                            if (BCrypt.checkpw(password, user.password)) {
                                Log.d("verifyUserCredentials", "Password corretta. Accesso come UTENTE: ${user.ruolo}")

                                callback(true, null, user.ruolo, user)
                            } else {
                                Log.d("verifyUserCredentials", "Password errata per email trovata.")
                                callback(false, "Password errata", null, null)
                            }
                        }
                    } else {
                        Log.d("verifyUserCredentials", "Utente non trovato per username.")
                        callback(false, "Utente non trovato", null, null)
                    }
                } else {
                    Log.d("verifyUserCredentials", "Nessun utente trovato per questo username.")
                    callback(false, "Utente non trovato", null, null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("verifyUserCredentials", "Errore durante la ricerca per username: ${error.message}")
                callback(false, error.message, null, null)
            }
        })
    }


    fun checkPhoneNumberExists(phone: String, callback: (Boolean) -> Unit) {
        val phoneRef = usersRef.orderByChild("phoneNumber").equalTo(phone)
        phoneRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserRepo", "Errore nella query del numero di telefono: ${error.message}")
                callback(false)
            }
        })
    }

    fun initiateEmailUpdate(utente: Utente?, newEmail: String, onComplete: (Boolean) -> Unit) {
        val userUid = utente?.id
        Log.d("UserRepo", "ID utente passato: $userUid")
        Log.d("UserRepo", "Nuova email richiesta: $newEmail")

        if (userUid == null) {
            Log.e("UserRepo", "ID utente è null. Interrompo l'aggiornamento.")
            onComplete(false)
            return
        }

        val currentUser = auth.currentUser
        Log.d("UserRepo", "Utente autenticato corrente UID: ${currentUser?.uid}")

        if (currentUser != null && currentUser.uid == userUid) {
            if (!currentUser.isEmailVerified) {
                Log.d("UserRepo", "Email corrente non verificata, invio email di verifica.")
                currentUser.sendEmailVerification().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("UserRepo", "Email di verifica inviata all'email corrente. Verifica prima di continuare.")
                        onComplete(false)
                    } else {
                        Log.e("UserRepo", "Errore durante l'invio della verifica per l'email corrente: ${task.exception?.message}")
                        onComplete(false)
                    }
                }
                return
            }

            // Invia email di verifica alla nuova email
            currentUser.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener { verifyTask ->
                if (verifyTask.isSuccessful) {
                    Log.d("UserRepo", "Email di verifica inviata alla nuova email. Attendi la conferma.")

                    currentUser.updateEmail(newEmail).addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {
                            Log.d("UserRepo", "Aggiornamento email su Firebase Authentication riuscito.")

                            // Ora aggiorna l'email nel Realtime Database
                            usersRef.child(userUid).child("email").setValue(newEmail)
                                .addOnCompleteListener { dbTask ->
                                    if (dbTask.isSuccessful) {
                                        Log.d("UserRepo", "Aggiornamento email nel Realtime Database riuscito.")
                                        onComplete(true)
                                    } else {
                                        Log.e("UserRepo", "Errore durante l'aggiornamento dell'email nel Realtime Database: ${dbTask.exception?.message}")
                                        onComplete(false)
                                    }
                                }
                        } else {
                            Log.e("UserRepo", "Errore durante l'aggiornamento dell'email su Firebase Authentication: ${emailTask.exception?.message}")
                            onComplete(false)
                        }
                    }
                } else {
                    Log.e("UserRepo", "Errore durante l'invio dell'email di verifica: ${verifyTask.exception?.message}")
                    onComplete(false)
                }
            }
        } else {
            Log.e("UserRepo", "Utente autenticato non coincide con l'utente passato o non è autenticato. Impossibile aggiornare l'email.")
            onComplete(false)
        }
    }
    fun takePassword(oldPassword: String, newPassword: String, callback: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            val email = user.email
            if (email != null) {
                val credential = EmailAuthProvider.getCredential(email, oldPassword)

                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                // Aggiorna anche la password nel Realtime Database
                                usersRef.child(user.uid).child("password").setValue(newPassword).addOnCompleteListener { dbTask ->
                                    if (dbTask.isSuccessful) {
                                        callback(true) // Aggiornamento completato con successo
                                    } else {
                                        callback(false) // Errore nell'aggiornamento del database
                                    }
                                }
                            } else {
                                callback(false) // Errore nel cambio password su Authentication
                            }
                        }
                    } else {
                        callback(false) // Errore nella re-autenticazione
                    }
                }
            } else {
                callback(false) // L'email non è disponibile
            }
        } else {
            callback(false) // L'utente non è autenticato
        }
    }


    fun saveUserToFirebase(username: String,name:String,address:String, hashedPassword: String, ruolo:String) {
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
                    name = name,
                    address = address,
                    username = username,
                    password = hashedPassword,
                    //admin = false
                    ruolo= "user",
                    phoneNumber=""
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
        usersRef.child(uid).removeValue()
            .addOnCompleteListener { task ->
                callback(task.isSuccessful) // Restituisce true se l'eliminazione è riuscita, false altrimenti
            }
            .addOnFailureListener { e ->
                Log.e("UserRepo", "Errore nell'eliminazione dei dati dal database: ${e.message}", e)
                callback(false)
            }
    }
    fun savePhoneUserToFirebase(username: String,name:String,address:String, hashedPassword: String,email:String) {
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
                    name= name,
                    address = address,
                    username = username,
                    password = hashedPassword,
                    email = email,
                    ruolo = "user"
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

    fun submitSintomi(userId: String, sintomiList: List<Sintomo>) {
        // Riferimento al nodo dell'utente nel database
        val userSintomiRef = database.reference.child("users").child(userId).child("sintomi")

        // Ottieni la data corrente (es. "2024-03-01")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        // Ottieni l'ora corrente (es. "13:02")
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        // prende l anno e la settimana corrente dell anno tipo 42 43
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)

        // Nodo settimanale basato su anno e settimana
        val settimanaanno = "$currentYear/week-$currentWeek"

        for (sintomo in sintomiList) {
            sintomo.dataSegnalazione = currentDate
            sintomo.oraSegnalazione = currentTime

            val sintomoMap = mapOf(
                "gravità" to sintomo.gravità,
                "tempoTrascorsoUltimoPasto" to sintomo.tempoTrascorsoUltimoPasto,
                "dataSegnalazione" to sintomo.dataSegnalazione,  // Aggiungi la data
                "oraSegnalazione" to sintomo.oraSegnalazione    // Aggiungi l'ora
            )

            Log.d("SubmitSintomi", "Dati da inviare per sintomo ${sintomo.nomeSintomo}: $sintomoMap")


            // Salva i dati del sintomo nella struttura desiderata
            userSintomiRef.child(sintomo.id)
                .child(settimanaanno)
                .child(currentDate)  // Nodo per la data
                .child(currentTime)  // Nodo per l'ora
                .setValue(sintomoMap)  // Dati associati al sintomo
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("SubmitSintomi", "Sintomo ${sintomo.id} inviato per $userId con data $currentDate e ora $currentTime")
                    } else {
                        Log.e("SubmitSintomi", "Errore nell'invio del sintomo ${sintomo.id}", task.exception)
                    }
                }
        }
    }

    // Aggiornamento dell'email
    fun updateUserEmail(userId: String, newEmail: String, callback: (Boolean) -> Unit) {
        usersRef.child(userId).child("email").setValue(newEmail)
            .addOnCompleteListener { task ->
                callback(task.isSuccessful)
            }
    }

    // Aggiornamento del numero di telefono
    fun updatePhoneNumber(userId: String, newPhone: String, callback: (Boolean) -> Unit) {
        usersRef.child(userId).child("phoneNumber").setValue(newPhone)
            .addOnCompleteListener { task ->
                callback(task.isSuccessful)
            }
    }

    // Aggiornamento dello username
    fun updateUsername(userId: String, newUsername: String, callback: (Boolean) -> Unit) {
        usersRef.child(userId).child("username").setValue(newUsername)
            .addOnCompleteListener { task ->
                callback(task.isSuccessful)
            }
    }

    // Aggiornamento del nome
    fun updateName(userId: String, newName: String, callback: (Boolean) -> Unit) {
        usersRef.child(userId).child("name").setValue(newName)
            .addOnCompleteListener { task ->
                callback(task.isSuccessful)
            }
    }

    // Aggiornamento dell'indirizzo
    fun updateAddress(userId: String, newAddress: String, callback: (Boolean) -> Unit) {
        usersRef.child(userId).child("address").setValue(newAddress)
            .addOnCompleteListener { task ->
                callback(task.isSuccessful)
            }
    }


    fun changePassword(userId: String, oldPassword: String, newPassword: String, callback: (Boolean) -> Unit) {
        Log.d("ChangePassword", "Inizio procedura di cambio password per utente con ID: $userId")

        usersRef.child(userId).get().addOnSuccessListener { snapshot ->
            val email = snapshot.child("email").getValue(String::class.java)
            Log.d("ChangePassword", "Email trovata: $email")

            if (email.isNullOrEmpty()) {
                Log.d("ChangePassword", "Nessuna email trovata. Procedo con aggiornamento solo nel Realtime Database.")
                // Se non c'è email, aggiorna solo nel Realtime Database con BCrypt
                val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
                usersRef.child(userId).child("password").setValue(hashedPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("ChangePassword", "Password aggiornata con successo nel Realtime Database.")
                        } else {
                            Log.e("ChangePassword", "Errore nell'aggiornamento della password nel Realtime Database.")
                        }
                        callback(task.isSuccessful)
                    }
            } else {
                Log.d("ChangePassword", "Email trovata. Procedo con il cambio password su Firebase Authentication $oldPassword.")
                // Se c'è email, cambia la password su Firebase Authentication e nel Realtime Database
                auth.signInWithEmailAndPassword(email, oldPassword).addOnCompleteListener { loginTask ->
                    if (loginTask.isSuccessful) {
                        Log.d("ChangePassword", "Login avvenuto con successo per l'utente con email: $email")
                        auth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.d("ChangePassword", "Password aggiornata con successo in Firebase Authentication.")
                                // Aggiorna anche nel Realtime Database
                                val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
                                usersRef.child(userId).child("password").setValue(hashedPassword)
                                    .addOnCompleteListener { dbTask ->
                                        if (dbTask.isSuccessful) {
                                            Log.d("ChangePassword", "Password aggiornata anche nel Realtime Database.")
                                            // Logout da Firebase Authentication
                                            auth.signOut()
                                            Log.d("ChangePassword", "Logout eseguito con successo.")
                                        } else {
                                            val dbException = dbTask.exception
                                            Log.e("ChangePassword", "Dettagli errore: ${dbException?.message}")
                                            Log.e("ChangePassword", "Tipo eccezione: ${dbException?.javaClass?.name}")
                                            Log.e("ChangePassword", "Dettagli localizzati: ${dbException?.localizedMessage}")
                                            Log.e("ChangePassword", "Errore durante l'aggiornamento della password nel Realtime Database.")
                                        }
                                        callback(dbTask.isSuccessful)
                                    }
                            } else {
                                val updateException = updateTask.exception
                                Log.e("ChangePassword", "Errore durante l'aggiornamento della password in Firebase Authentication: ${updateException?.message}")
                                Log.e("ChangePassword", "Dettagli eccezione: ${updateException?.localizedMessage}")
                                Log.e("ChangePassword", "Tipo eccezione: ${updateException?.javaClass?.name}")
                                callback(false)

                            }
                        }
                    } else {
                        Log.e("ChangePassword", "Login fallito. Verifica la vecchia password.")
                        callback(false)
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("ChangePassword", "Errore nel recupero delle informazioni utente: ${exception.message}")
            callback(false)
        }
    }



    /*  fun syncUsers() {
        val auth = FirebaseAuth.getInstance()

        // Step 1: Recupera tutti gli ID utente dal Realtime Database
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usersInDatabase = snapshot.children.mapNotNull { it.key }.toSet()

                // Step 2: Recupera gli utenti da Firebase Authentication
                auth.listUsers(null)
                    .addOnSuccessListener { result ->
                        val usersInAuth = result.users.map { it.uid }.toSet()

                        // Step 3: Trova utenti in Authentication ma non nel Database
                        val usersToDelete = usersInAuth - usersInDatabase

                        // Step 4: Elimina utenti non presenti nel Database
                        usersToDelete.forEach { uid ->
                            auth.deleteUser(uid)
                                .addOnSuccessListener {
                                    println("Utente con UID: $uid eliminato da Firebase Authentication")
                                }
                                .addOnFailureListener { exception ->
                                    println("Errore durante l'eliminazione dell'utente con UID: $uid. Errore: ${exception.message}")
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        println("Errore nel recupero degli utenti da Firebase Authentication. Errore: ${exception.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Errore nel recupero degli utenti dal Realtime Database. Errore: ${error.message}")
            }
        })
    }
*/
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