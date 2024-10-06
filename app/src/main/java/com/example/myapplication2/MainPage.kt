package com.example.myapplication2

import OnSwipeTouchListener
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication2.adapter.SintomiAdapter
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.NotificaWorker
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class MainPage : AppCompatActivity() {
    private lateinit var sintomoRepo: SintomoRepo
    private lateinit var sintadapter: SintomiAdapter
    private lateinit var userRepo: UserRepo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainpageactivity)


        //todo salvare l utente con il nodo su firebase
        //todo bloccare fatta loa registrazione il ritorno alla pagina di signin
        //todo verificare effetto della recycler sulla navbar
        //todo vedere in basso
        val utente = intent.getParcelableExtra<Utente>("utente")
        val userid= utente?.id

        Log.d("funziona","funziona $userid")
        // Inizializza il repository
        sintomoRepo = SintomoRepo()
        userRepo= UserRepo()

        val spinnerDistanza: Spinner = findViewById(R.id.spinnerDistanzaUltimoPasto)

        val inviaButton: Button = findViewById(R.id.inviadati)
        inviaButton.setOnClickListener {

            // Recupera la distanza selezionata dallo spinner
            val distanzapasto = when (spinnerDistanza.selectedItem.toString()) {
                "Meno di 1 ora" -> 0
                "1 ora" -> 1
                "2 ore" -> 2
                "3 ore" -> 3
                "Più di 3 ore" -> 4
                else -> 0
            }
            Log.d("SpinnerDistanza", "Distanza selezionata dallo spinner: $distanzapasto")


            val selectedSintomi = sintadapter.getSelectedSintomi()
            Log.d("InviaButton", "Selected Sintomi e Gravità: $selectedSintomi")

            // Aggiungi la distanza dall'ultimo pasto a ogni sintomo selezionato
            selectedSintomi.forEach { sintomo ->
                sintomo.tempoTrascorsoUltimoPasto = distanzapasto

                Log.d("Sintomo", "Sintomo ${sintomo.nomeSintomo} ha tempoTrascorsoUltimoPasto: ${sintomo.tempoTrascorsoUltimoPasto}")
            }



            if (userid != null) {

                // Invia i sintomi selezionati
                userRepo.submitSintomi(userid, selectedSintomi)

                // Rimuovi i sintomi deselezionati (quelli che non sono più nella lista selectedSintomi)
                val allSintomi = sintadapter.getAllSintomi()
                val allSintomiIds = allSintomi.map { it.id }
                val selectedSintomiIds = selectedSintomi.map { it.id }
                val sintomiDaRimuovere = allSintomiIds.minus(selectedSintomiIds)

                sintomiDaRimuovere.forEach { sintomoId ->
                    userRepo.removeSintomo(userid, sintomoId)
                }
            } else {
                Log.d("InviaButton", "Nessun utente autenticato.")
            }
        }
        val recyclerView: RecyclerView = findViewById(R.id.recyclerSintomi)
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeLeft() {
                // Naviga alla ProfileActivity
                startActivity(Intent(this@MainPage, ProfileActivity::class.java))
            }
        })
       /* val recyclerView: SwipeableRecyclerView = findViewById(R.id.recyclerSintomi)
        recyclerView.setSwipeEnabled(false)*/

        sintadapter = SintomiAdapter(emptyList())
        recyclerView.adapter = sintadapter

        sintomoRepo.fetchSintomi()

        sintomoRepo.sintomi.observe(this, Observer { sintomiList ->
            Log.d("Recuperoactivit", "Listasintomi: $sintomiList")
            sintadapter.submitlist(sintomiList)
        })


        sintomoRepo.fetchSintomi()



        // Osserva i cambiamenti nella lista dei sintomi
        sintomoRepo.sintomi.observe(this, Observer { sintomiList ->
            Log.d("Recuperoactivity", "Listasintomi: $sintomiList")
            sintadapter.submitlist(sintomiList)


        })


       /* userRepo.fetchSelectedSintomiForUser(currentUser!!.uid) { sintomiList ->
            Log.d("FetchSintomo", "Numero di sintomi recuperati: ${sintomiList.size}")

            for (sintomo in sintomiList) {
                Log.d("FetchSintomo", "Nome: ${sintomo.nomeSintomo}")
            }

            Log.d("FetchSintomo", "Fine gestione")
        }*/


        //Usare per ogni sintomo
        sintomoRepo.aggiungiSintomo("paura") { success ->
            if (success) {
                Log.d("AGGIUNTASECONDACTIVIY","AGGIUNTA RIUSCITA")
            }
        }

        //METODO PER RIMUOVERE UN SINTOMO DAL DATABSE
        //copiare id da dashboard database
        sintomoRepo.rimuoviSintomo("074df670-63fd-4ebd-81e1-f70113e7440c") { success ->
            if (success) {
                Log.d("Rimozione riuscita","rimozione riuscita")
            }
        }


        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_home // Set the default selection

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Sei già su questa Activity, non fare nulla
                    true
                }
                R.id.nav_profile -> {
                    // Passa alla ProfileActivity
                    val intent = Intent(this, ProfileActivity::class.java).apply {
                        putExtra("utente", utente) // Passa l'oggetto Utente
                    }
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
        val mainpageLayout: View = findViewById(R.id.mainpageroot)

        // Rileva il movimento di swipe
        mainpageLayout.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeLeft() {
                val intent = Intent(this@MainPage, ProfileActivity::class.java).apply {
                    putExtra("utente", utente) // Passa l'oggetto Utente
                }
            }
    })

        // Configurazione notifiche giornaliere se l'utente è loggato
        if (userid != null) {
            scheduleDailyNotification()
            // Crea il canale di notifica ùù
            createNotificationChannel()
        }

        if (userid != null) {
            scheduleTestNotification()  //Test ogni 15 sec sarà da rimuovere
            // Crea il canale di notifica ùù
            createNotificationChannel()
        }

}
    private fun scheduleDailyNotification() {
        val workRequest = PeriodicWorkRequestBuilder<NotificaWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun scheduleTestNotification() {
        Log.d("MainPage", "Scheduling the notification worker")

        val workRequest = OneTimeWorkRequestBuilder<NotificaWorker>()
            .setInitialDelay(10, TimeUnit.MINUTES)  // 10 min atest
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork("DailyNotificationWork", ExistingWorkPolicy.REPLACE, workRequest)

    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Notification Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("daily_notification", name, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

        }
    }
}


