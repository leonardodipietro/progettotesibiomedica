package com.example.myapplication2.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.myapplication2.model.Sintomo
import java.util.UUID
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.CountDownLatch


class SintomoRepo {

    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
    private val sintomiRef = database.getReference("sintomi")
    private val exAccountRef=database.getReference("exaccount")
    private val userRef=database.getReference("users")
    val sintomi: MutableLiveData<List<Sintomo>> = MutableLiveData()
    fun aggiungiSintomo(nomeSintomo: String, onComplete: (Boolean) -> Unit) {
        sintomiRef.orderByChild("nomeSintomo").equalTo(nomeSintomo).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("sintrepo", "Sintomo già esistente")
                    onComplete(false)
                } else {
                    val idSintomo = UUID.randomUUID().toString()
                    val sintomoData = mapOf(
                        "id" to idSintomo,
                        "nomeSintomo" to nomeSintomo
                    )

                    // Salva il sintomo nel database
                    sintomiRef.child(idSintomo).setValue(sintomoData)
                        .addOnSuccessListener {
                            Log.d("sintrepo", "Sintomo aggiunto")

                            // Aggiungi la traduzione tramite TranslationRepo
                            val translationRepo = TranslationRepo()
                            translationRepo.translate(nomeSintomo, "en") { translatedText ->
                                if (translatedText != null) {
                                    Log.d("sintrepo", "Traduzione del sintomo: $translatedText")
                                    // Salva il nome tradotto nel database
                                    sintomiRef.child(idSintomo).child("nomeSintomoTradotto").setValue(translatedText)
                                        .addOnSuccessListener {
                                            Log.d("sintrepo", "Traduzione salvata nel database")
                                            onComplete(true)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.d("sintrepo", "Errore nel salvataggio della traduzione: $e")
                                            onComplete(false)
                                        }
                                } else {
                                    Log.d("sintrepo", "Errore nella traduzione del sintomo")
                                    onComplete(false)
                                }
                            }
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

    fun fetchSintomi(context: Context) {
        // Recupera la lingua dalle SharedPreferences
        val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("LANGUAGE", "it") // 'it' come default

        sintomiRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sintomiIds = snapshot.children.mapNotNull { it.key }
                retrieveNameSintomo(sintomiIds) { sintomiNames ->
                    val sintomiList = snapshot.children.map { childSnapshot ->
                        val sintomoId = childSnapshot.key ?: ""
                        val nomeSintomo = if (languageCode == "it") {
                            childSnapshot.child("nomeSintomo").getValue(String::class.java) ?: ""
                        } else {
                            childSnapshot.child("nomeSintomoTradotto").getValue(String::class.java) ?: ""
                        }

                        Sintomo(sintomoId, nomeSintomo)
                    }
                    sintomi.postValue(sintomiList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "errore ${error.message}")
            }
        })
    }
    fun caricaSintomi(
        sintomiList: MutableList<String>,
        sintomiIdList: MutableList<String>,
        onComplete: () -> Unit  // Rimosso parametro Any?
    ) {
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
                onComplete()  // Nessun argomento necessario qui
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

                        // Dopo aver rimosso il sintomo dal nodo users, rimuovilo anche dal nodo exaccount
                        exAccountRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(exSnapshot: DataSnapshot) {
                                for (exUserSnapshot in exSnapshot.children) {
                                    val sintomiExUserRef = exUserSnapshot.child("sintomi").child(idSintomo)
                                    if (sintomiExUserRef.exists()) {
                                        exUserSnapshot.ref.child("sintomi").child(idSintomo).removeValue()
                                            .addOnSuccessListener {
                                                Log.d("sintrepo", "Sintomo $idSintomo rimosso per l'ex utente ${exUserSnapshot.key}")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("sintrepo", "Errore nella rimozione del sintomo $idSintomo per l'ex utente ${exUserSnapshot.key}: $e")
                                            }
                                    }
                                }
                                onComplete(true)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("sintrepo", "Errore durante la rimozione dai nodi exaccount: ${error.message}")
                                onComplete(false)
                            }
                        })
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

}