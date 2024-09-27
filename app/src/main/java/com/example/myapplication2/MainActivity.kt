package com.example.myapplication2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity: AppCompatActivity()  {

    private lateinit var auth: FirebaseAuth
    //todo capire perche ci sta l url per forza
    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = currentUser.uid
                    checkUserTypeAndRedirect(uid)  // Verifica il tipo di utente dal database
                } else {
                    auth.signOut()
                    setContentView(R.layout.activitymain)
                    interfacciagrafica()
                }
            }
        } else {
            setContentView(R.layout.activitymain)
            interfacciagrafica()
        }
    }
    private fun checkUserTypeAndRedirect(uid: String) {
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
    }
    private fun interfacciagrafica() {
       val mailpswdbutton = findViewById<Button>(R.id.pulsantemail)
       val phonebutton=findViewById<Button>(R.id.pulsantetel)
       val loginroot=findViewById<TextView>(R.id.vaialoginactivity)

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
        loginroot.setOnClickListener{
            val intent3 = Intent(this, LoginActivity::class.java)
            startActivity(intent3)
            finish()
        }

    }

    private fun startsMainPage() {
        val intent = Intent(this, MainPage::class.java)
        startActivity(intent)
        finish()
    }


}