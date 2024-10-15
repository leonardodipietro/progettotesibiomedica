package com.example.myapplication2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication2.Presenter.MainPagePresenter
import com.example.myapplication2.adapter.SintomiAdapter
import com.example.myapplication2.interfacepackage.MainPageView
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.Utente
//import com.example.myapplication2.repository.NotificaWorker
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import android.Manifest
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import com.example.myapplication2.repository.NotificaWorker


class MainPage : AppCompatActivity(), MainPageView {
    companion object {
        private const val REQUEST_CODE = 101
    }
    private lateinit var presenter: MainPagePresenter
    private lateinit var sintadapter: SintomiAdapter
    private lateinit var userRepo: UserRepo
    private lateinit var sintomoRepo: SintomoRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainpageactivity)


        // Inizializza il presenter
        userRepo = UserRepo()
        sintomoRepo = SintomoRepo()
        presenter = MainPagePresenter(this, sintomoRepo, userRepo)


        setupNotificationChannelAndPermissions()

        // Inizializza la UI e l'adapter
        setupUI()
        setupListeners()

        val intentUser = intent.getParcelableExtra<Utente>("utente")
        presenter.loadUserData(intentUser)
        intentUser?.let {
            //presenter.scheduleNotifications(it.id)
        }

        presenter.loadSintomiList()

        scheduleDailyNotification()
    }

    private fun setupUI() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerSintomi)
        recyclerView.layoutManager = LinearLayoutManager(this)
        sintadapter = SintomiAdapter(emptyList())
        recyclerView.adapter = sintadapter

        // Setup della navigation bar
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    val utente = loadUserFromPreferences()
                    utente?.let { navigateToProfile(it) }
                    true
                }
                else -> false
            }
        }
    }
    private fun setupListeners() {
        val inviaButton: Button = findViewById(R.id.inviadati)
        val spinnerDistanza: Spinner = findViewById(R.id.spinnerDistanzaUltimoPasto)

        inviaButton.setOnClickListener {
            val distanzapasto = getDistanzaPastoFromSpinner(spinnerDistanza)
            val selectedSintomi = sintadapter.getSelectedSintomi()
            val allSintomi = sintadapter.getAllSintomi()  // Ottieni la lista completa dei sintomi
            val userId = loadUserFromPreferences()?.id

            if (userId != null) {
                presenter.submitSelectedSintomi(userId, selectedSintomi, allSintomi, distanzapasto)
            } else {
                showError("Errore: Utente non autenticato.")
            }
        }
    }

    private fun getDistanzaPastoFromSpinner(spinner: Spinner): Int {
        return when (spinner.selectedItem.toString()) {
            "Meno di 1 ora" -> 0
            "1 ora" -> 1
            "2 ore" -> 2
            "3 ore" -> 3
            "Più di 3 ore" -> 4
            else -> 0
        }
    }

    // Implementazione dell'interfaccia MainPageView
    override fun showUserWelcomeMessage(username: String) {
        findViewById<TextView>(R.id.titolo).text = "Benvenuto $username, come ti senti oggi?"
    }

    override fun updateSintomiList(sintomiList: List<Sintomo>) {
        sintadapter.submitlist(sintomiList)
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToProfile(user: Utente) {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            putExtra("utente", user)
        }
        startActivity(intent)
    }
    private fun setupNotificationChannelAndPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("NotificationSetup", "Checking if notification channel needs to be created")
            val name = "Daily Notification"
            val descriptionText = "Channel for daily notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("daily_notification", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            scheduleDailyNotification()
            Log.d("NotificationSetup", "Notification channel created")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d("NotificationSetup", "Checking notification permission for Android 13+")
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("NotificationSetup", "Permission not granted, requesting permission")
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
            } else {
                Log.d("NotificationSetup", "Permission already granted, scheduling notification")
                scheduleDailyNotification()
            }
        } else {
            Log.d("NotificationSetup", "Android version below 13, no permission required, scheduling notification")
            scheduleDailyNotification()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("NotificationPermission", "Notification permission granted: ${checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED}")

                scheduleDailyNotification()

            } else {
                Log.d("NotificationPermission", "Permesso per le notifiche negato")
            }
        }
    }

    override fun scheduleDailyNotification() {
        /*val workRequest = PeriodicWorkRequestBuilder<NotificaWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueue(workRequest)*/

       /* val workRequest = PeriodicWorkRequestBuilder<NotificaWorker>(20, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(workRequest)*/
        Log.d("WorkManager", "Scheduling daily notification")
        val periodicWorkRequest = PeriodicWorkRequestBuilder<NotificaWorker>(15, TimeUnit.MINUTES)
            .addTag("daily_notification")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "UniqueDailyNotificationWork", // Nome univoco per il lavoro
            ExistingPeriodicWorkPolicy.REPLACE, // Politica per sostituire lavori esistenti con lo stesso nome
            periodicWorkRequest
        )

        // Log per verificare lo stato del WorkRequest
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(periodicWorkRequest.id).observe(this) { workInfo ->
            if (workInfo != null) {
                Log.d("WorkManager", "Work ID: ${periodicWorkRequest.id}, State: ${workInfo.state}")

                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> Log.d("WorkManager", "Periodic work is enqueued")
                    WorkInfo.State.RUNNING -> Log.d("WorkManager", "Periodic work is running")
                    WorkInfo.State.SUCCEEDED -> Log.d("WorkManager", "Periodic work succeeded")
                    WorkInfo.State.FAILED -> Log.d("WorkManager", "Periodic work failed")
                    WorkInfo.State.BLOCKED -> Log.d("WorkManager", "Periodic work is blocked")
                    WorkInfo.State.CANCELLED -> Log.d("WorkManager", "Periodic work is cancelled")
                    else -> Log.d("WorkManager", "Unknown work state")
                }
            } else {
                Log.d("WorkManager", "WorkInfo is null")
            }
        }







        /*val workRequest = OneTimeWorkRequestBuilder<NotificaWorker>()
            //.addTag("daily_notification")
            //.setInitialDelay(10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "UniqueNotificationWork",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        //WorkManager.getInstance(this).enqueue(workRequest)

        // Log per verificare lo stato del WorkRequest
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.id).observe(this) { workInfo ->
            if (workInfo != null) {
                Log.d("WorkManager", "Work ID: ${workRequest.id}, State: ${workInfo.state}")

                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> Log.d("WorkManager", "Work is enqueued")
                    WorkInfo.State.RUNNING -> Log.d("WorkManager", "Work is running")
                    WorkInfo.State.SUCCEEDED -> Log.d("WorkManager", "Work succeeded")
                    WorkInfo.State.FAILED -> Log.d("WorkManager", "Work failed")
                    WorkInfo.State.BLOCKED -> Log.d("WorkManager", "Work is blocked")
                    WorkInfo.State.CANCELLED -> Log.d("WorkManager", "Work is cancelled")
                    else -> Log.d("WorkManager", "Unknown work state")
                }
            } else {
                Log.d("WorkManager", "WorkInfo is null")
            }
        }*/
    }

    override fun saveUserToPreferences(user: Utente) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(user)
        editor.putString("utente", json)
        editor.putBoolean("isAdmin", false)
        editor.putBoolean("isLoggedIn", true)
        editor.apply()
    }

    override fun loadUserFromPreferences(): Utente? {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("utente", null)
        return if (json != null) Gson().fromJson(json, Utente::class.java) else null
    }






    override fun stopNotification() {
      //  WorkManager.getInstance(this).cancelUniqueWork("NotificaWorker")
    }



}


/*class MainPage : AppCompatActivity() {
    private lateinit var sintomoRepo: SintomoRepo
    private lateinit var sintadapter: SintomiAdapter
    private lateinit var userRepo: UserRepo
    private val gson = Gson()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainpageactivity)

        //todo salvare l utente con il nodo su firebase
        //todo bloccare fatta loa registrazione il ritorno alla pagina di signin
        //todo verificare effetto della recycler sulla navbar
        //todo vedere in basso
        var utente = intent.getParcelableExtra<Utente>("utente")
        val userid= utente?.id
        val benvenutoTextView: TextView = findViewById(R.id.titolo)

        Log.d("funziona","funziona $userid")
            utente = loadUserFromPreferences() // Prova a caricare dalle Shared Preferences

            saveUserToPreferences(utente!!) // Salva l'utente nelle Shared Preferences se presente nell'intent


        if (utente != null) {
            val userId = utente.id
            Log.d("funziona", "Utente trovato con ID: $userId")
            benvenutoTextView.text = "Benvenuto ${utente.username}, come ti senti oggi?"
        } else {
            // Se l'utente non è presente, torna alla MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


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



            if (utente != null) {
                val userId = utente.id

                // Aggiungi un controllo di nullità per userId
                if (userId != null) {
                    Log.d("submitSintomi", "Inviando sintomi per utente con ID: $userId")

                    // Invia i sintomi selezionati
                    userRepo.submitSintomi(userId, selectedSintomi)

                    // Rimuovi i sintomi deselezionati (quelli che non sono più nella lista selectedSintomi)
                    val allSintomi = sintadapter.getAllSintomi()
                    val allSintomiIds = allSintomi.map { it.id }
                    val selectedSintomiIds = selectedSintomi.map { it.id }
                    val sintomiDaRimuovere = allSintomiIds.minus(selectedSintomiIds)

                    sintomiDaRimuovere.forEach { sintomoId ->
                        userRepo.removeSintomo(userId, sintomoId)
                    }
                } else {
                    Log.e("submitSintomi", "L'ID dell'utente è null. Impossibile inviare i sintomi.")
                }
            } else {
                Log.e("InviaButton", "L'oggetto utente è null. Nessun utente autenticato.")
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
            .enqueueUniqueWork("NotificaWorker", ExistingWorkPolicy.REPLACE, workRequest)

    }




    // Funzione per salvare l'utente nelle Shared Preferences
    private fun saveUserToPreferences(user: Utente) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = gson.toJson(user)
        editor.putString("utente", json)
        editor.putBoolean("isLoggedIn", true)
        editor.apply()

        // Log per verificare il salvataggio
        Log.d("debuglogin", "Utente salvato nelle Shared Preferences: $json")
        Log.d("debuglogin", "isLoggedIn salvato come true")
    }

    // Funzione per caricare l'utente dalle Shared Preferences
    private fun loadUserFromPreferences(): Utente? {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("utente", null)

        // Log per verificare i dati caricati
        if (json != null) {
            Log.d("debuglogin", "Utente caricato dalle Shared Preferences: $json")
            return gson.fromJson(json, Utente::class.java)
        } else {
            Log.d("debuglogins", "Nessun utente trovato nelle Shared Preferences")
            return null
        }
    }

}
*/

