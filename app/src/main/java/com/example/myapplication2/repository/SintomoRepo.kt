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
    val sintomi: MutableLiveData<List<Sintomo>> = MutableLiveData()


    fun aggiungiSintomo(nomeSintomo: String, onComplete: (Boolean) -> Unit) {
        // Controlla se il sintomo esiste già nel db confrontandone i nomi
        sintomiRef.orderByChild("nomeSintomo").equalTo(nomeSintomo).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Sintomo già esistente quindi non viene aggiunto nulla
                    Log.d("sintrepo", "Sintomo già esistente")
                    onComplete(false)
                } else {
                    // Aggiunta il nuovo sintomo
                    val idSintomo = UUID.randomUUID().toString()
                    val nuovoSintomo = Sintomo(id = idSintomo, nomeSintomo = nomeSintomo)

                    sintomiRef.child(idSintomo).setValue(nuovoSintomo)
                        .addOnSuccessListener {
                            Log.d("sintrepo", "Sintomo aggiunto")
                            onComplete(true)
                        }
                        .addOnFailureListener { e ->
                            Log.d("sintrepo", "Errore aggiunta sintomob AAAAAA: $e")
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

    fun saveSintomoInUsersNode () {
        

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
        sintomiRef.child(idSintomo).removeValue()
            .addOnSuccessListener {
                Log.d("sintrepo", "Sintomo rimosso con successo")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.d("sintrepo", "Errore rimozione sintomo: $e")
                onComplete(false)
            }
    }

    //Todo rimuovi sintomo da nodo user


}