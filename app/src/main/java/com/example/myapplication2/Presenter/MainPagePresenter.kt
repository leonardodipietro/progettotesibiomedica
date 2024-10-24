package com.example.myapplication2.Presenter

import android.content.Context
import android.util.Log
import com.example.myapplication2.interfacepackage.MainPageView
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo

class MainPagePresenter(
    private val view: MainPageView,
    private val sintomoRepo: SintomoRepo,
    private val userRepo: UserRepo
) {
    fun loadUserData(intentUser: Utente?) {
        val user = intentUser ?: view.loadUserFromPreferences()
        Log.d("loadUserData", "Utente: ${user?.id ?: "Nessun utente"} - Ruolo: ${user?.ruolo ?: "Ruolo non disponibile"}")

        if (user != null) {
            view.saveUserToPreferences(user)
            Log.d("loadUserData", "Utente salvato nelle preferenze: ${user.username}")

            view.showUserWelcomeMessage(user.username ?: "")
            Log.d("loadUserData", "Messaggio mostr ${user.username}")
            //scheduleNotificationsIfNeeded(user.id)
        } else {
            view.showError("Utente non trovato, accesso negato.")

        }
    }

    fun loadSintomiList(context: Context) {
        sintomoRepo.fetchSintomi(context)
        sintomoRepo.sintomi.observeForever { sintomiList ->
            if (sintomiList != null) {
                view.updateSintomiList(sintomiList)
            } else {
                view.showError("Errore nel caricamento dei sintomi.")
            }
        }
    }

   /* fun submitSelectedSintomi(userId: String, selectedSintomi: List<Sintomo>, allSintomi: Int, distanzapasto: Int) {
        selectedSintomi.forEach { it.tempoTrascorsoUltimoPasto = distanzapasto }

        // Invia i sintomi selezionati
        userRepo.submitSintomi(userId, selectedSintomi)

        // Rimuovi sintomi deselezionati (quelli non inclusi nei sintomi selezionati)
        val selectedSintomiIds = selectedSintomi.map { it.id }
        val sintomiDaRimuovere = allSintomi.map { it.id }.minus(selectedSintomiIds)

        sintomiDaRimuovere.forEach { sintomoId ->
            userRepo.removeSintomo(userId, sintomoId)
        }
    }*/
    fun submitSelectedSintomi(userId: String, selectedSintomi: List<Sintomo>, allSintomi: List<Sintomo>, distanzapasto: Int) {
        selectedSintomi.forEach { it.tempoTrascorsoUltimoPasto = distanzapasto }

        // Invia i sintomi selezionati
        userRepo.submitSintomi(userId, selectedSintomi)

        // Rimuovi sintomi deselezionati
        val selectedSintomiIds = selectedSintomi.map { it.id }
        val sintomiDaRimuovere = allSintomi.map { it.id }.minus(selectedSintomiIds)
        sintomiDaRimuovere.forEach { sintomoId ->
            userRepo.removeSintomo(userId, sintomoId)
        }
    }


    private fun scheduleNotifications(userId: String?) {
        userId?.let {
            view.scheduleDailyNotification()
        }
    }
}
