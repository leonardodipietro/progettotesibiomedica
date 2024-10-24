package com.example.myapplication2.Presenter

import com.example.myapplication2.InfoActivity
import com.example.myapplication2.repository.FaqRepo
import com.example.myapplication2.repository.UserRepo

class InfoPresenter(private val view: InfoActivity, private val userRepo: UserRepo,private val faqRepo: FaqRepo) {


    fun loadFaqData() {
        val context = view.getContext() // Assicurati che la tua view possa restituire il Context
        faqRepo.fetchFaqList(context) { faqList ->
            view.showFaqList(faqList)
        }
    }

   /* fun loadUserData(userId: String) {
        userRepo.getUserData(userId) { user ->
            if (user != null) {
                view.populateUserData(user)
            } else {
                view.showError("Dati utente non trovati")
            }
        }
    }*/


}