package com.example.myapplication2.repository

import android.util.Log
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.Utente
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UserRepo {
    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
    private var auth = FirebaseAuth.getInstance()







    fun saveUserIdToFirebase() {
        Log.d("userrepo", "funzione chiamata ")
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("userrepo", "CurrentUser non è null")
                val userRef = database.reference.child("users").child(currentUser.uid)
                val nuovoUser = Utente(id = currentUser.uid, email = currentUser.email.toString())
                Log.d("userrepo", "Creazione oggettocompletata  $nuovoUser")
                userRef.setValue(nuovoUser)
                    .addOnSuccessListener {
                        Log.d("userrepo", "Utente aggiunto")
                    }
                    .addOnFailureListener { e ->
                        Log.d("userrepo", "Utente non aggiunto: $e")
                    }
            } else {
                Log.d("userrepo", "CurrentUser è null")
            }
        } catch (e: Exception) {
            Log.e("userrepo", "Eccezione ${e.message}")
        }
    }


    fun savePhoneUserToFirebase() {
        Log.d("userrepo", "Funzione chiamata per l'autenticazione con numero di telefono")
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("userrepo", "CurrentUser non è null (telefono)")
                val userRef = database.reference.child("users").child(currentUser.uid)
                val nuovoUser = Utente(id = currentUser.uid, phoneNumber = currentUser.phoneNumber)
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
    }

    fun fetchSelectedSintomiForUser(userId: String, callback: (List<Sintomo>) -> Unit) {
        Log.d("fetchSelectedSintomi", "Inizio recupero per  $userId")

        val userSintomiRef = database.reference.child("users").child(userId).child("selectedSintomi")

        userSintomiRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val selectedSintomiIds = task.result.children.map { it.getValue(String::class.java) ?: "" }.filter { it.isNotEmpty() }
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