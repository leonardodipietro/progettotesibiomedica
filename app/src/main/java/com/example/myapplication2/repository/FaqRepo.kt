package com.example.myapplication2.repository

import android.util.Log
import com.example.myapplication2.model.Faq
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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

    fun fetchFaqList(callback: (List<Faq>) -> Unit) {

            faqsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val faqList = mutableListOf<Faq>()
                    for (faqSnapshot in snapshot.children) {
                        val faq = faqSnapshot.getValue(Faq::class.java)
                        faq?.let { faqList.add(it) }
                    }
                    callback(faqList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FaqRepo", "Error fetching data", error.toException())
                }
            })
    }


}
