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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import com.example.myapplication2.repository.NotificaWorker
import java.util.Locale

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
        loadLocale()
        setContentView(R.layout.mainpageactivity)


        // Controllo se l'alert deve essere mostrato
        val skipAlert = intent.getBooleanExtra("skipAlert", false)
        if (!skipAlert) {
            showSymptomAlertDialog()
        }


         //HINT TEXT DENTRO LO SPINNER
        // Inizializza il presenter
        userRepo = UserRepo()
        sintomoRepo = SintomoRepo()
        presenter = MainPagePresenter(this, sintomoRepo, userRepo)


        setupNotificationChannelAndPermissions()


        val intentUser = intent.getParcelableExtra<Utente>("utente")
        Log.d("IntentData", "Utente ripreso ${intentUser?.id?: "Nessun utent.id"} ${intentUser?.username ?: "Nessun utente"} - Ruolo: ${intentUser?.ruolo ?: "Ruolo non disponibile"}")
        presenter.loadUserData(intentUser)
        intentUser?.let {
            //presenter.scheduleNotifications(it.id)
        }

        // Inizializza la UI e l'adapter
        setupUI()
        setupListeners()
        setupBottomNavigation()

        presenter.loadSintomiList(this)

        scheduleDailyNotification()
    }
    private fun showSymptomAlertDialog() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.alert_title_cisonosintomi))
            .setMessage(getString(R.string.alert_message_cisonosintomi))
            .setPositiveButton(getString(R.string.alert_yes)) { dialog, _ ->
                // Azione per "Sì"
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.alert_no)) { dialog, _ ->
                // Azione per "No"
                dialog.dismiss()
            }
            .create()
        alertDialog.show()
    }

    override fun attachBaseContext(newBase: Context) {
        val sharedPref = newBase.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("LANGUAGE", "it")
        val locale = Locale(languageCode ?: "it")
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    private fun loadLocale() {
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("LANGUAGE", "it")
        if (languageCode != null) {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)

            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
    private fun setupUI() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerSintomi)
        recyclerView.layoutManager = LinearLayoutManager(this)
        sintadapter = SintomiAdapter(emptyList())
        recyclerView.adapter = sintadapter
    }
    private fun setupListeners() {
        val inviaButton: Button = findViewById(R.id.inviadati)
        val spinnerDistanza: Spinner = findViewById(R.id.spinnerDistanzaUltimoPasto)
        val editTextSintomoAggiuntivo: EditText = findViewById(R.id.editTextSintomoAggiuntivo)
        inviaButton.setOnClickListener {
            val distanzapasto = getDistanzaPastoFromSpinner(spinnerDistanza)
            val selectedSintomi = sintadapter.getSelectedSintomi()
            val allSintomi = sintadapter.getAllSintomi()
            val userId = loadUserFromPreferences()?.id
            val sintomoAggiuntivo = editTextSintomoAggiuntivo.text.toString().trim()
            if (userId != null) {
                // Invio dei sintomi selezionati da lista
                presenter.submitSelectedSintomi(userId, selectedSintomi, allSintomi, distanzapasto)
                Toast.makeText(this, getString(R.string.sintomi_inviati), Toast.LENGTH_SHORT).show()
                // Mostra l'Alert per sintomi con gravità elevata
                val hasHighSeveritySintomi = selectedSintomi.any { it.gravità == 3 || it.gravità == 4 }
                if (hasHighSeveritySintomi) {
                    val alertDialog = AlertDialog.Builder(this)
                        .setMessage(getString(R.string.contatta_medico))
                        .setCancelable(false)
                        .create()
                    alertDialog.show()
                    Handler(Looper.getMainLooper()).postDelayed({ alertDialog.dismiss() }, 5000)
                }
                if (sintomoAggiuntivo.isNotEmpty()) {
                    presenter.aggiungiSintomoAggiuntivo(sintomoAggiuntivo) { successo ->
                        if (successo) {
                            editTextSintomoAggiuntivo.text.clear()
                        }
                    }
                }
            } else {
                showError(getString(R.string.errore_utente_non_autenticato))
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    val utente = loadUserFromPreferences()
                    utente?.let { navigateToProfile(it) }
                    true
                }
                R.id.nav_home -> {
                    // Rimanere sulla MainPage
                    true
                }
                R.id.nav_info -> {
                    val utente = loadUserFromPreferences()
                    val intent = Intent(this, InfoActivity::class.java).apply {
                        putExtra("utente", utente)
                    }
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun getDistanzaPastoFromSpinner(spinner: Spinner): Int {
        val selectedText = spinner.selectedItem.toString()
        Log.d("DEBUGSPINNER", "Valore selezionato: $selectedText")
        return when (spinner.selectedItem.toString()) {
            "Meno di 1 ora" -> 1
            "Da 1 a 3 ore" -> 2
            "Più di 3 ore" -> 3
            else -> 0
        }
    }

    // Implementazione dell'interfaccia MainPageView
    override fun showUserWelcomeMessage(username: String) {
        findViewById<TextView>(R.id.titolo).text = getString(R.string.mainpage_welcome_message) // Usa la stringa localizzata

    }

    override fun updateSintomiList(sintomiList: List<Sintomo>) {
        sintadapter.submitlist(sintomiList)  // Carica solo i sintomi filtrati
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
            val name = "Daily Notification"
            val descriptionText = "Channel for daily notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("daily_notification", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            scheduleDailyNotification()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
            } else {
                scheduleDailyNotification()
            }
        } else {
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

                scheduleDailyNotification()

            } else {

            }
        }
    }
    override fun scheduleDailyNotification() {
        Log.d("WorkManager", "Checking if periodic work is already scheduled")

        // Verifica sincrona sullo stato del lavoro
        val workInfos = WorkManager.getInstance(this)
            .getWorkInfosForUniqueWork("UniqueDailyNotificationWork")
            .get() // Ottiene i risultati in modo sincrono

        if (workInfos.isNullOrEmpty() || workInfos.none { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }) {

            val periodicWorkRequest = PeriodicWorkRequestBuilder<NotificaWorker>(15, TimeUnit.MINUTES)
                .addTag("daily_notification")
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "UniqueDailyNotificationWork",
                ExistingPeriodicWorkPolicy.KEEP, // Mantiene il lavoro esistente se già attivo
                periodicWorkRequest
            )
        } else {

        }
    }





    override fun saveUserToPreferences(user: Utente) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(user)

        editor.putString("utente", json)
        editor.putString("ruolo", user.ruolo)
        editor.putBoolean("isLoggedIn", true)
        editor.apply()
    }

    override fun loadUserFromPreferences(): Utente? {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("utente", null)

        Log.d("loadUserFromPreferences", "Dati utente $json")

        return if (json != null) {
            Gson().fromJson(json, Utente::class.java)
        } else {
            Log.d("loadUserFromPreferences", "Nessun utente salvato.")
            null
        }
    }

    override fun stopNotification() {
      //  WorkManager.getInstance(this).cancelUniqueWork("NotificaWorker")
    }



}



