package com.example.myapplication2.interfacepackage

import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.Utente

interface MainPageView {
    fun showUserWelcomeMessage(username: String)
    fun updateSintomiList(sintomiList: List<Sintomo>)
    fun showError(message: String)
    fun navigateToProfile(user: Utente)
    fun scheduleDailyNotification()
    fun stopNotification()
    //fun scheduleNotifications(user.id: Utente)
    fun saveUserToPreferences(user: Utente)
    fun loadUserFromPreferences(): Utente?
}