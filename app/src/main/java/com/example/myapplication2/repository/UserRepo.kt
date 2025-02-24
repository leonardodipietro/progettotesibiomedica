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

    fun getUserByUsername(username: String, callback: (Utente?) -> Unit) {
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var userFound: Utente? = null

                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(Utente::class.java)
                    if (user != null && user.username == username) {
                        userFound = user
                        break
                    }
                }

                if (userFound != null) {
                    callback(userFound) // Restituisce l'utente trovato
                } else {
                    callback(null) // Nessun utente trovato
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserRepo", "Errore del database: ${error.message}")
                callback(null) // Restituisce null in caso di errore
            }
        })
    }


    fun getUserEmail(userId: String, callback: (String?) -> Unit) {
        val userRef = usersRef.child(userId).child("email")

        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val email = task.result?.getValue(String::class.java)
                Log.d("getUserEmail", "Email  $email")
                callback(email) // Restituisce l'email trovata
            } else {
                callback(null) // In caso di errore, restituisce null
            }
        }
    }
    fun getUserData(uid: String, callback: (Utente?) -> Unit) {
        Log.d("UserRepo", "daje ")

        usersRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("UserRepo", "Dati trovati per  $uid")
                    val utente = snapshot.getValue(Utente::class.java)
                    Log.d("UserRepo", "Dati utente: ${utente?.toString()}")
                    callback(utente)
                } else {
                    Log.d("UserRepo", "Nessun dato uid: $uid")
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserRepo", "Errore di $uid. Errore: ${error.message}")
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
    fun getUsername(userId: String, callback: (String?) -> Unit) {
        usersRef.child("users").child(userId).child("username").get()
            .addOnSuccessListener { snapshot ->
                callback(snapshot.value as? String)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    // Recupera il nome dell'utente
    fun getName(userId: String, callback: (String?) -> Unit) {
        usersRef.child("users").child(userId).child("name").get()
            .addOnSuccessListener { snapshot ->
                callback(snapshot.value as? String)
            }
            .addOnFailureListener {
                callback(null)
            }
    }
    fun getPhoneNumber(userId: String, callback: (String?) -> Unit) {


        // Riferimento al nodo dell'utente nel database
        usersRef.child(userId).child("phoneNumber").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val phoneNumber = snapshot.getValue(String::class.java)
                if (phoneNumber.isNullOrEmpty()) {
                    Log.d("UserRepo", "Nessun numero di telefono trovato per l'utente con ID: $userId")
                } else {
                    Log.d("UserRepo", "Numero di telefono trovato: $phoneNumber per l'utente con ID: $userId")
                }
                callback(phoneNumber)
            }

            override fun onCancelled(error: DatabaseError) {

                callback(null)
            }
        })


    }

    fun getAddress(userId: String, callback: (String?) -> Unit) {
        usersRef.child("users").child(userId).child("address").get()
            .addOnSuccessListener { snapshot ->
                callback(snapshot.value as? String)
            }
            .addOnFailureListener {
                callback(null)
            }
    }
    // Recupera il numero di telefono dell'utente
// Recupera il numero di telefono dell'utente
    fun getUserPhoneNumber(userId: String, callback: (String?) -> Unit) {
        Log.d("PhoneUpdate", "Tentativo di recuperare il numero di telefono per l'utente con ID: $userId")

        usersRef.child(userId).child("phoneNumber").get()
            .addOnSuccessListener { snapshot ->
                val phoneNumber = snapshot.value as? String
                if (phoneNumber != null) {
                    Log.d("PhoneUpdate", "Numero di telefono recuperato con successo: $phoneNumber")
                } else {
                    Log.w("PhoneUpdate", "Numero di telefono non trovato per l'utente con ID: $userId")
                }
                callback(phoneNumber)
            }
            .addOnFailureListener { exception ->
                Log.e("PhoneUpdate", "Errore durante il recupero del numero di telefono: ${exception.message}")
                callback(null)
            }
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

    fun verifyUserByEmail(email: String, password: String, callback: (Boolean, String?, String?, Utente?) -> Unit) {
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.children.first().getValue(Utente::class.java)
                    if (user != null) {
                        if (BCrypt.checkpw(password, user.password)) {
                            callback(true, null, user.ruolo, user)
                        } else { callback(false, "Password errata", null, null) }
                    } else {  callback(false, "Utente non trovato", null, null) }
                } else { callback(false, "Utente non trovato", null, null) } }
            override fun onCancelled(error: DatabaseError) {callback(false, error.message, null, null) } })
    }
    fun verifyUserByPhone(phoneNumber: String, password: String, callback: (Boolean, String?, String?, Utente?) -> Unit) {
        usersRef.orderByChild("phoneNumber").equalTo(phoneNumber).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.children.first().getValue(Utente::class.java)
                    if (user != null) {
                        if (BCrypt.checkpw(password, user.password)) {
                            callback(true, null, user.ruolo, user)
                        } else {
                            callback(false, "Password errata", null, null) }
                    } else { callback(false, "Utente non trovato", null, null) }
                } else { callback(false, "Utente non trovato", null, null) }
            }
            override fun onCancelled(error: DatabaseError) {
                callback(false, error.message, null, null) } })
    }
    fun verifyUserCredentials(username: String, password: String, callback: (Boolean, String?, String?, Utente?) -> Unit) {
        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    val user = userSnapshot.getValue(Utente::class.java)
                    if (user != null) {
                     // Controlla se ha una email
                        if (user.email.isNullOrEmpty()) {
                            if (!user.phoneNumber.isNullOrEmpty()) {
                                if (BCrypt.checkpw(password, user.password)) {
                                    Log.d("verifyUserCredentials", "Password corretta. Accesso : ${user.ruolo}")
                                    val ruolo = user.ruolo
                                    callback(true, null, ruolo, user)
                                } else { callback(false, "Password errata", null, null) }
                            } else { callback(false, "Utente non trovato", null, null) }
                        } else {
                            if (BCrypt.checkpw(password, user.password)) {
                                Log.d("verifyUserCredentials", "Password corretta. Accesso: ${user.ruolo}")
                                callback(true, null, user.ruolo, user)
                            } else {
                                Log.d("verifyUserCredentials", "Password errata per email trovata.")
                                callback(false, "Password errata", null, null) } }
                    } else {
                        callback(false, "Utente non trovato", null, null) }
                } else {
                    callback(false, "Utente non trovato", null, null) } }

            override fun onCancelled(error: DatabaseError) {
                Log.e("verifyUserCredentials", "Errore durante la ricerca per username: ${error.message}")
                callback(false, error.message, null, null)
            }
        })
    }
    fun changePassword(username: String, newPassword: String, callback: (Boolean) -> Unit) {
        val userQuery = usersRef.orderByChild("username").equalTo(username)
        userQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        userSnapshot.ref.child("password").setValue(newPassword)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    callback(true)
                                } else {
                                    callback(false)
                                }
                            }
                    }
                } else {
                    callback(false)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                callback(false)
            }
        })
    }



    fun saveUserToFirebase(username: String, hashedPassword: String, ruolo:String) {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userRef = database.reference.child("users").child(currentUser.uid)
                // Creazione dell'oggetto utente
                val nuovoUser = Utente(
                    id = currentUser.uid,
                    email = currentUser.email.toString(),
                    username = username,
                    password = hashedPassword,
                    ruolo= ruolo,
                    phoneNumber=""
                )
                // Salva l'utente nel db
                userRef.setValue(nuovoUser)
                    .addOnSuccessListener {
                        Log.d("userrepo", "Utente aggiunto")
                    }
                    .addOnFailureListener { e ->
                        Log.d("userrepo", "Eccezione numero 1 $e")
                    }
            } else {
                Log.d("userrepo", "CurrentUser  null")
            }
        } catch (e: Exception) {
            Log.e("userrepo", "Eccezione numero 2 ${e.message}")
        }
    }


    fun deleteAccount(uid: String, callback: (Boolean) -> Unit) {
        // Elimina i dati dell'utente dal Realtime Database
        usersRef.child(uid).removeValue()
            .addOnCompleteListener { task ->
                callback(task.isSuccessful) // Restituisce true se l'eliminazione è riuscita, false altrimenti
            }
            .addOnFailureListener { e ->
                Log.e("UserRepo", "Errore ${e.message}", e)
                callback(false)
            }
    }

    fun savePhoneUserToFirebase(username: String, hashedPassword: String,email:String) {
        Log.d("userrepo", "Funzione chiamata")
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                //userRef è il riferimento al nodo users del database
                val userRef = database.reference.child("users").child(currentUser.uid)
                // Aggiungiamo username e password hashata all'oggetto utente
                val nuovoUser = Utente(
                    id = currentUser.uid,
                    phoneNumber = currentUser.phoneNumber,
                    username = username,
                    password = hashedPassword,
                    email = email,
                    ruolo = "user"
                )
                Log.d("userrepo", "Creazione $nuovoUser")
                userRef.setValue(nuovoUser)
                    .addOnSuccessListener {
                        Log.d("userrepo", "Utente con TEL aggiunto")
                    }
                    .addOnFailureListener { e ->
                        Log.d("userrepo", "Utente non aggiunto: $e")
                    }
            } else {
                Log.d("userrepo", "CurrentUsernull")
            }
        } catch (e: Exception) {
            Log.e("userrepo", "Eccezione: ${e.message}")
        }
    }

    // Funzione per recuperare i sintomi dell'utente

    fun submitSintomi(userId: String, sintomiList: List<Sintomo>) {
        val userSintomiRef = database.reference.child("users").child(userId).child("sintomi")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val settimanaanno = "$currentYear/week-$currentWeek"
        for (sintomo in sintomiList) {
            sintomo.dataSegnalazione = currentDate
            sintomo.oraSegnalazione = currentTime
            val sintomoMap = mapOf("gravità" to sintomo.gravità,
                "tempoTrascorsoUltimoPasto" to sintomo.tempoTrascorsoUltimoPasto,
                "dataSegnalazione" to sintomo.dataSegnalazione, "oraSegnalazione" to sintomo.oraSegnalazione,)
            userSintomiRef.child(sintomo.id)
                .child(settimanaanno).child(currentDate).child(currentTime)
                .setValue(sintomoMap)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                    } else {

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
        usersRef.child(userId).get().addOnSuccessListener { snapshot ->
            val email = snapshot.child("email").getValue(String::class.java)
            if (email.isNullOrEmpty()) { // Se non c'è email su Autentication , aggiorna solo nel db
                val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
                usersRef.child(userId).child("password").setValue(hashedPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) { Log.d("ChangePassword", "Password aggiornata.") }
                        else { Log.e("ChangePassword", "Errore nell'aggiornamento.") }
                        callback(task.isSuccessful) } } else {
                // Se c'è email, cambia la password su Firebase Authentication e nel db
                auth.signInWithEmailAndPassword(email, oldPassword).addOnCompleteListener { loginTask ->
                    if (loginTask.isSuccessful) {
                        auth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.d("ChangePassword", "Password aggiornata su auth fir.")
                                val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
                                usersRef.child(userId).child("password").setValue(hashedPassword)
                                    .addOnCompleteListener { dbTask -> if (dbTask.isSuccessful) {
                                            auth.signOut() } // Logout da Firebase Auth
                                        callback(dbTask.isSuccessful) } } else {
                                callback(false) } } } else { Log.e("ChangePassword", "Login fallito")
                        callback(false) } } } }.addOnFailureListener { exception ->
            Log.e("ChangePassword", "Errore nel recupero utente ${exception.message}")
            callback(false)
        }
    }



    fun removeSintomo(userId: String, sintomoId: String) {
        val userSintomiRef = database.reference.child("users").child(userId).child("selectedSintomi").child(sintomoId)

        userSintomiRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("RemoveSintomo", "Sintomo rimosso dal db per: $userId")
            } else {
                Log.e("RemoveSintomo", "Errore rimozione sint $userId", task.exception)
            }
        }
    }




}