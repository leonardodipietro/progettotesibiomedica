package com.example.myapplication2.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.myapplication2.model.Sintomo
import com.google.firebase.Firebase
import java.util.UUID
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.util.concurrent.CountDownLatch


class SintomoRepo {

    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
    private val sintomiRef = database.getReference("sintomi")
    private val userRef=database.getReference("users")
    val sintomi: MutableLiveData<List<Sintomo>> = MutableLiveData()

    fun aggiungiSintomo(nomeSintomo: String, onComplete: (Boolean) -> Unit) {
        // Controlla se il sintomo esiste già nel database confrontando i nomi
        sintomiRef.orderByChild("nomeSintomo").equalTo(nomeSintomo).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Sintomo già esistente, quindi non viene aggiunto nulla
                    Log.d("sintrepo", "Sintomo già esistente")
                    onComplete(false)
                } else {
                    // Crea una mappa dei dati da salvare
                    val idSintomo = UUID.randomUUID().toString()
                    val sintomoData = mapOf(
                        "id" to idSintomo,
                        "nomeSintomo" to nomeSintomo
                    )

                    // Salva la mappa nel database
                    sintomiRef.child(idSintomo).setValue(sintomoData)
                        .addOnSuccessListener {
                            Log.d("sintrepo", "Sintomo aggiunto")
                            onComplete(true)
                        }
                        .addOnFailureListener { e ->
                            Log.d("sintrepo", "Errore aggiunta sintomo: $e")
                            onComplete(false)
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("sintrepo", "Errore: ${error.message}")
                onComplete(false)
            }
        })
    }



    fun fetchSintomi() {
        sintomiRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sintomiIds = snapshot.children.mapNotNull { it.key }
                retrieveNameSintomo(sintomiIds) { sintomiNames ->
                    val sintomiList = sintomiNames.mapIndexed { index, nome ->
                        Sintomo(sintomiIds[index], nome)
                    }
                    sintomi.postValue(sintomiList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "errore ${error.message}")
            }
        })
    }

        fun caricaSintomi(sintomiList: MutableList<String>,sintomiIdList: MutableList<String>, onComplete: () -> Unit) {
            sintomiRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    sintomiList.clear()
                    sintomiIdList.clear()
                    for (sintomoSnapshot in snapshot.children) {
                        val nomeSintomo = sintomoSnapshot.child("nomeSintomo").getValue(String::class.java)
                        val idSintomo = sintomoSnapshot.key
                        if (nomeSintomo != null && idSintomo != null) {
                            sintomiList.add(nomeSintomo)
                            sintomiIdList.add(idSintomo)
                        }
                    }
                    onComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("sintrepo", "Errore: ${error.message}")
                }
            })
        }



    fun retrieveNameSintomo(sintomiIds: List<String>, onComplete: (List<String>) -> Unit) {
        val names = mutableListOf<String>()
        //sincronizzano i thread dell applicazione con un contatore
        val countDownLatch = CountDownLatch(sintomiIds.size)

        sintomiIds.forEach { id ->
            sintomiRef.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("nomeSintomo").getValue(String::class.java) ?: ""
                    names.add(name)
                    Log.d("SintomiRepo", " Nome recuperato per $id: $name")
                    countDownLatch.countDown()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Errore nome  ${error.message}")
                    countDownLatch.countDown()
                }
            })
        }

        Thread {
            countDownLatch.await()
            Log.d("SintomiRepo", "roba ricacciata: $names")
            onComplete(names)
        }.start()
    }

    fun rimuoviSintomo(idSintomo: String, onComplete: (Boolean) -> Unit) {
        //val usersRef = FirebaseDatabase.getInstance().getReference("users")

        // Rimuovi il sintomo dal nodo dei sintomi
        sintomiRef.child(idSintomo).removeValue()
            .addOnSuccessListener {
                Log.d("sintrepo", "Sintomo rimosso dal nodo sintomi")

                // Ora rimuovi il sintomo anche dai nodi degli utenti
                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (userSnapshot in snapshot.children) {
                            val sintomiUserRef = userSnapshot.child("sintomi").child(idSintomo)
                            if (sintomiUserRef.exists()) {
                                userSnapshot.ref.child("sintomi").child(idSintomo).removeValue()
                                    .addOnSuccessListener {
                                        Log.d("sintrepo", "Sintomo $idSintomo rimosso per l'utente ${userSnapshot.key}")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("sintrepo", "Errore nella rimozione del sintomo $idSintomo per l'utente ${userSnapshot.key}: $e")
                                    }
                            }
                        }
                        onComplete(true)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("sintrepo", "Errore durante la rimozione dai nodi utenti: ${error.message}")
                        onComplete(false)
                    }
                })
            }
            .addOnFailureListener { e ->
                Log.e("sintrepo", "Errore rimozione sintomo: $e")
                onComplete(false)
            }
    }

    //Todo rimuovi sintomo da nodo user


}