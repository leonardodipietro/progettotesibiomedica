package com.example.myapplication2.repository

import android.content.Context
import android.util.Log

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.apache.commons.compress.harmony.archive.internal.nls.Messages.getString
import com.example.myapplication2.R

import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.Presenter.InfoPresenter
import com.example.myapplication2.adapter.FaqAdapter
import com.example.myapplication2.interfacepackage.InfoView
import com.example.myapplication2.model.Faq
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.FaqRepo
import com.example.myapplication2.repository.UserRepo
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class FaqRepo {

    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
    private val faqsRef = database.getReference("faqs")
    private val translationRepo=TranslationRepo()

    fun addFaq(question: String, answer: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        translationRepo.translate(question, "en") { translatedQuestion ->
            if (translatedQuestion != null) {
                translationRepo.translate(answer, "en") { translatedAnswer ->
                    if (translatedAnswer != null) {
                        val faqId = faqsRef.push().key ?: return@translate
                        val faq = Faq(id = faqId, question = question, translatedQuestion = translatedQuestion, answer = answer, translatedAnswer = translatedAnswer   )
                        faqsRef.child(faqId).setValue(faq)
                        .addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it) } }    } } } }

    fun deleteFaq(faqId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        faqsRef.child(faqId).removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateFaq(faqId: String, question: String, answer: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        translationRepo.translate(question, "en") { translatedQuestion ->
            if (translatedQuestion != null) {
                translationRepo.translate(answer, "en") { translatedAnswer ->
                    if (translatedAnswer != null) {
                        val faq = Faq(id = faqId,
                            question = question,
                            translatedQuestion = translatedQuestion,
                            answer = answer,
                            translatedAnswer = translatedAnswer
                        )
                        faqsRef.child(faqId).setValue(faq)
                            .addOnSuccessListener { onSuccess() } .addOnFailureListener { onFailure(it) }        } } } } }


    fun fetchFaqList(context: Context, callback: (List<Faq>) -> Unit) {
        val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("LANGUAGE", "it")
        faqsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val faqList = mutableListOf<Faq>()
                for (faqSnapshot in snapshot.children) {
                    val faq = faqSnapshot.getValue(Faq::class.java)
                    faq?.let {
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
