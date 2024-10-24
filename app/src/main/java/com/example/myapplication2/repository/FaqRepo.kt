package com.example.myapplication2.repository

import android.content.Context
import android.util.Log
import com.example.myapplication2.model.Faq
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FaqRepo {

    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
    private val faqsRef = database.getReference("faqs")
    private val translationRepo=TranslationRepo()
    fun addFaq(question: String, answer: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {

        translationRepo.translate(question, "en") { translatedQuestion ->
            if (translatedQuestion != null) {
                // Dopo che la domanda è stata tradotta, traduci la risposta
                translationRepo.translate(answer, "en") { translatedAnswer ->
                    if (translatedAnswer != null) {

                        val faqId = faqsRef.push().key ?: return@translate
                        val faq = Faq(
                            id = faqId,
                            question = question,  // Domanda originale in italiano
                            translatedQuestion = translatedQuestion,  // Domanda tradotta in inglese
                            answer = answer,  // Risposta originale in italiano
                            translatedAnswer = translatedAnswer  // Risposta tradotta in inglese
                        )

                        // Salva la FAQ nel database con la traduzione
                        faqsRef.child(faqId).setValue(faq)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure(it) }
                    } else {
                        onFailure(Exception("Errore nella traduzione della risposta"))
                    }
                }
            } else {
                onFailure(Exception("Errore nella traduzione della domanda"))
            }
        }
    }


    fun updateFaq(faqId: String, question: String, answer: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Prima traduci la domanda
        translationRepo.translate(question, "en") { translatedQuestion ->
            if (translatedQuestion != null) {

                translationRepo.translate(answer, "en") { translatedAnswer ->
                    if (translatedAnswer != null) {

                        val faq = Faq(
                            id = faqId,
                            question = question,  // Domanda originale in italiano
                            translatedQuestion = translatedQuestion,  // Domanda tradotta in inglese
                            answer = answer,  // Risposta originale in italiano
                            translatedAnswer = translatedAnswer  // Risposta tradotta in inglese
                        )


                        faqsRef.child(faqId).setValue(faq)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFailure(it) }
                    } else {
                        onFailure(Exception("Errore nella traduzione della risposta"))
                    }
                }
            } else {
                onFailure(Exception("Errore nella traduzione della domanda"))
            }
        }
    }


    fun deleteFaq(faqId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        faqsRef.child(faqId).removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun fetchFaqList(context: Context, callback: (List<Faq>) -> Unit) {
        // Recupera la lingua dalle SharedPreferences
        val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("LANGUAGE", "it") // 'it' come default

        faqsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val faqList = mutableListOf<Faq>()
                for (faqSnapshot in snapshot.children) {
                    val faq = faqSnapshot.getValue(Faq::class.java)
                    faq?.let {
                        // Se la lingua è italiana, usa le domande e risposte in italiano, altrimenti in inglese
                        if (languageCode == "it") {
                            faq.question = faq.question
                            faq.answer = faq.answer
                        } else {
                            faq.question = faq.translatedQuestion
                            faq.answer = faq.translatedAnswer
                        }
                        faqList.add(it)
                    }
                }
                callback(faqList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FaqRepo", "Error fetching data", error.toException())
            }
        })
    }


}
