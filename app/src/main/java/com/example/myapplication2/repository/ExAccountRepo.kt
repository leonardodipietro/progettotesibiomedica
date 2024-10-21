package com.example.myapplication2.repository

import android.util.Log
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.UtenteEliminato
import com.google.firebase.database.FirebaseDatabase

class ExAccountRepo {

        private val exAccountRef = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app").reference
    fun creaUtenteEliminato(userId: String, callback: (Boolean) -> Unit) {

        val userRef = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users/$userId/sintomi")

        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val sintomiSnapshot = task.result
                val sintomiMap = mutableMapOf<String, MutableMap<String, MutableMap<String, MutableMap<String, MutableMap<String, Sintomo>>>>>() // Mappa corretta con Sintomo

                if (sintomiSnapshot.exists()) {

                    for (idSintomoSnapshot in sintomiSnapshot.children) {
                        val sintomoId = idSintomoSnapshot.key ?: ""
                        if (sintomoId.isNotEmpty()) {
                            for (annoSnapshot in idSintomoSnapshot.children) {
                                val annoKey = annoSnapshot.key ?: ""

                                for (settimanaSnapshot in annoSnapshot.children) {
                                    val settimanaKey = settimanaSnapshot.key ?: ""

                                    for (giornoSnapshot in settimanaSnapshot.children) {
                                        val giornoKey = giornoSnapshot.key ?: ""

                                        for (oraSnapshot in giornoSnapshot.children) {
                                            val oraKey = oraSnapshot.key ?: ""

                                            val sintomoMapData = oraSnapshot.value as? Map<String, Any>

                                            if (sintomoMapData != null) {
                                                val sintomo = Sintomo(
                                                    id = sintomoId, // Usa l'ID esistente del sintomo
                                                    gravità = sintomoMapData["gravità"]?.toString()?.toInt() ?: 0,
                                                    tempoTrascorsoUltimoPasto = sintomoMapData["tempoTrascorsoUltimoPasto"]?.toString()?.toInt() ?: 0,
                                                    dataSegnalazione = sintomoMapData["dataSegnalazione"]?.toString() ?: "",
                                                    oraSegnalazione = sintomoMapData["oraSegnalazione"]?.toString() ?: ""
                                                )


                                                if (!sintomiMap.containsKey(sintomoId)) {
                                                    sintomiMap[sintomoId] = mutableMapOf()
                                                }
                                                if (!sintomiMap[sintomoId]!!.containsKey(annoKey)) {
                                                    sintomiMap[sintomoId]!![annoKey] = mutableMapOf()
                                                }
                                                if (!sintomiMap[sintomoId]!![annoKey]!!.containsKey(settimanaKey)) {
                                                    sintomiMap[sintomoId]!![annoKey]!![settimanaKey] = mutableMapOf()
                                                }
                                                if (!sintomiMap[sintomoId]!![annoKey]!![settimanaKey]!!.containsKey(giornoKey)) {
                                                    sintomiMap[sintomoId]!![annoKey]!![settimanaKey]!![giornoKey] = mutableMapOf()
                                                }

                                                sintomiMap[sintomoId]!![annoKey]!![settimanaKey]!![giornoKey]!![oraKey] = sintomo
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val nuovoUtenteId = exAccountRef.child("exaccount").push().key

                if (nuovoUtenteId != null) {
                    val utenteEliminato = UtenteEliminato(
                        id = nuovoUtenteId,
                        nome = "Utente eliminato $nuovoUtenteId",
                        sintomi = sintomiMap
                    )

                    // Salva l'utente eliminato nel nodo exaccount con la struttura temporale
                    exAccountRef.child("exaccount").child(nuovoUtenteId)
                        .setValue(utenteEliminato)
                        .addOnCompleteListener { saveTask ->
                            if (saveTask.isSuccessful) {
                                Log.d("ExAccountRepo", "ex utente slavato $nuovoUtenteId")
                                callback(true)
                            } else {
                                Log.e("ExAccountRepo", "Errore salvatggio $nuovoUtenteId")
                                callback(false)
                            }
                        }
                } else {
                    Log.e("ExAccountRepo", "Errore id ute ekli.")
                    callback(false)
                }
            } else {
                Log.e("ExAccountRepo", "Errore nel fetch sintomi ${task.exception?.message}")
                callback(false)
            }
        }
    }



}

