package com.example.myapplication2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class AdminActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)


        Log.d("MainActivity", "sono nell admin?.")

    //todo rivedere la logica di accesso alla pagina di default all apertura dell'app


        auth = FirebaseAuth.getInstance()

        val logoutButton = findViewById<Button>(R.id.logoutadmin)
        logoutButton.setOnClickListener {
            // Effettua il logout
            auth.signOut()

            // Torna alla MainActivity
            val intent = Intent(this, MainActivity::class.java)
            //serve per rimuovere la main page dallo stack di memoria
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()  // Chiude l'activity corrente
        }







    }
}