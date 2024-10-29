package com.example.myapplication2.Presenter

import android.content.Context
import android.util.Log
import com.example.myapplication2.interfacepackage.MainPageView
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import java.util.UUID

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
                // Log del numero totale di sintomi caricati
                Log.d("MainPagePresenter", "Numero totale di sintomi caricati: ${sintomiList.size}")

                // Filtra per includere solo i sintomi "non personalizzati"
                val sintomiUniversali = sintomiList.filter { it.isPersonalizzato == false }

                // Log del numero di sintomi universali dopo il filtraggio
                Log.d("MainPagePresenter", "Numero di sintomi universali (non personalizzati): ${sintomiUniversali.size}")

                view.updateSintomiList(sintomiUniversali)
            } else {
                view.showError("Errore nel caricamento dei sintomi.")
            }
        }
    }


    fun aggiungiSintomoAggiuntivo(nomeSintomo: String, onComplete: (Boolean) -> Unit) {
        sintomoRepo.aggiungiSintomo(nomeSintomo, isPersonalizzato = true) { successo ->
            if (successo) {
                val userId = view.loadUserFromPreferences()?.id ?: return@aggiungiSintomo
                val nuovoSintomo = Sintomo(
                    id = UUID.randomUUID().toString(),
                    nomeSintomo = nomeSintomo,
                    gravità = 0,
                    tempoTrascorsoUltimoPasto = 0,
                    isPersonalizzato = true  // Imposta isPersonalizzato a true
                )
                userRepo.submitSintomi(userId, listOf(nuovoSintomo))
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }


    fun submitSelectedSintomi(userId: String, selectedSintomi: List<Sintomo>, allSintomi: List<Sintomo>, distanzapasto: Int) {
        Log.d("DEBUGSPINNER", "Distanza pasto trasmessa: $distanzapasto")
        // Filtra solo i sintomi universali (non personalizzati) per evitare l'inclusione dei sintomi aggiuntivi nella RecyclerView
        val sintomiUniversali = selectedSintomi.filter { it.isPersonalizzato != true }
        sintomiUniversali.forEach { it.tempoTrascorsoUltimoPasto = distanzapasto }

        // Invia i sintomi universali selezionati
        userRepo.submitSintomi(userId, sintomiUniversali)

        // Rimuovi sintomi deselezionati
        val selectedSintomiIds = sintomiUniversali.map { it.id }
        val sintomiDaRimuovere = allSintomi.filter { it.isPersonalizzato != true }.map { it.id }.minus(selectedSintomiIds)
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
