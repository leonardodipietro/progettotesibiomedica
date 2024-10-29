package com.example.myapplication2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import java.util.Locale

class MainActivity: AppCompatActivity()  {

    private lateinit var auth: FirebaseAuth
    //todo capire perche ci sta l url per forza
    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLocale()

        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val ruolo = sharedPreferences.getString("ruolo", "user") // "user" come valore predefinito

        Log.d("SharedPreferences", "Dati utente recuperati: $isLoggedIn, Ruolo: $ruolo")
        if (isLoggedIn) {
            val utenteJson = sharedPreferences.getString("utente", null)
            val utente = Gson().fromJson(utenteJson, Utente::class.java)

            when (ruolo) {
                "admin" -> {
                    Log.d("RoleCheck", "Utente è admin, avvio AdminActivity")
                    val intent = Intent(this, AdminActivity::class.java).apply {
                        putExtra("utente", utente)
                        loadLocale()
                    }
                    startActivity(intent)
                }
                "superadmin" -> {
                    Log.d("RoleCheck", "Utente è superadmin, avvio SuperAdminActivity")
                    val intent = Intent(this, SuperAdminActivity::class.java).apply {
                        putExtra("utente", utente)
                        loadLocale()
                    }
                    startActivity(intent)
                }
                else -> {
                    Log.d("RoleCheck", "Utente è user, avvio MainPage")
                    val intent = Intent(this, MainPage::class.java).apply {
                        loadLocale()
                        putExtra("utente", utente)
                    }
                    startActivity(intent)
                }
            }
            finish()
        } else {
            loadLocale()
            setContentView(R.layout.activitymain)

            interfacciagrafica()

        }
    }

    private fun loadLocale() {
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("LANGUAGE", "it") // Imposta 'it' come default
        Log.d("Locale", "Lingua salvata $languageCode") // Log della lingua salvata

        if (languageCode != null) {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            Log.d("Locale", "impostato: ${Locale.getDefault()}") // Log del locale impostato

            val config = resources.configuration
            config.setLocale(locale)
            Log.d("Locale", "Configurazione aggiornata: ${config.locales.get(0)}") // Log della configurazione

            resources.updateConfiguration(config, resources.displayMetrics)
            Log.d("Locale", "Risorse aggiornate ${resources.configuration.locales.get(0)}") // Log delle risorse aggiornate
        }
    }

    /*private fun checkUserTypeAndRedirect(uid: String) {
        val userRef = database.getReference("users").child(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Recupera il campo 'admin' dall'utente nel Realtime Database
                val isAdmin = snapshot.child("admin").getValue(Boolean::class.java) ?: false
                if (isAdmin) {
                    // Se l'utente è un amministratore
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Se l'utente è un utente normale
                    val intent = Intent(this, MainPage::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                Log.e("MainActivity", "Utente non trovato nel database")
            }
        }.addOnFailureListener { exception ->
            Log.e("MainActivity", "Errore nel recupero dei dati: ${exception.message}")
        }
    }*/
    private fun interfacciagrafica() {
       val mailpswdbutton = findViewById<Button>(R.id.btn_email)
       val phonebutton=findViewById<Button>(R.id.btn_phone)
       val loginroot=findViewById<Button>(R.id.btn_gotologin)

        // Configura i bottoni per la selezione della lingua
        val italianButton = findViewById<Button>(R.id.btn_italian)
        val englishButton = findViewById<Button>(R.id.btn_english)

        italianButton.setOnClickListener {
            setLocale("it")
        }

        englishButton.setOnClickListener {
            setLocale("en")
        }
        mailpswdbutton.setOnClickListener {
            val intent1 = Intent(this, EmailPasswordActivity::class.java)
            startActivity(intent1)

        }
        phonebutton.setOnClickListener{
            val intent2 = Intent(this, TelephoneActivity::class.java)
            startActivity(intent2)

        }
        loginroot.setOnClickListener{
            val intent3 = Intent(this, LoginActivity::class.java)
            startActivity(intent3)

        }

    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)

        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("LANGUAGE", languageCode)
        editor.apply()

        resources.updateConfiguration(config, resources.displayMetrics)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
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


    private fun startsMainPage() {
        val intent = Intent(this, MainPage::class.java)
        startActivity(intent)
        finish()
    }


}