package com.example.myapplication2.repository

import com.example.myapplication2.model.Faq
import com.google.firebase.database.FirebaseDatabase

class FaqRepo {

    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
    private val faqsRef = database.getReference("faqs")

    fun addFaq(question: String, answer: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val faqId = faqsRef.push().key ?: return
        val faq = Faq(id = faqId, question = question, answer = answer)

        faqsRef.child(faqId).setValue(faq)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateFaq(faqId: String, question: String, answer: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val faq = Faq(id = faqId, question = question, answer = answer)

        faqsRef.child(faqId).setValue(faq)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteFaq(faqId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        faqsRef.child(faqId).removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
