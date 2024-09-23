package com.example.myapplication2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger

class MainActivity: AppCompatActivity()  {
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Ricarica l'utente e controlla se è ancora autenticato
            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            // L'utente esiste, avvia `SecondActivity`
                            Log.d("MainActivity", "User è autenticato: ${currentUser.email}")
                            startsMainPage()
                        } else {
                            // L'utente non esiste
                            auth.signOut()

                            setContentView(R.layout.activitymain)
                            interfacciagrafica()

                        }
                    }
                } else {

                    auth.signOut()
                    setContentView(R.layout.activitymain)
                    interfacciagrafica()

                }
            }
        } else {
            // Nessun utente autenticato, mostra il layout di `MainActivity`
            setContentView(R.layout.activitymain)
            interfacciagrafica()
        }



    }

    private fun interfacciagrafica() {
       val mailpswdbutton = findViewById<Button>(R.id.pulsantemail)
       val phonebutton=findViewById<Button>(R.id.pulsantetel)

        mailpswdbutton.setOnClickListener {
            val intent1 = Intent(this, EmailPasswordActivity::class.java)
            startActivity(intent1)
            finish()
        }
        phonebutton.setOnClickListener{
            val intent2 = Intent(this, TelephoneActivity::class.java)
            startActivity(intent2)
            finish()
        }
    }

    private fun startsMainPage() {
        val intent = Intent(this, MainPage::class.java)
        startActivity(intent)
        finish()
    }


}