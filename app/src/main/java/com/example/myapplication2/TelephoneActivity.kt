package com.example.myapplication2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DatabaseReference
import java.util.concurrent.TimeUnit

class TelephoneActivity: AppCompatActivity()   {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    //codice che viene inviato all'utente
    private var code: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

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
                            startSecondActivity()
                        } else {
                            // L'utente non esiste
                            auth.signOut()

                            setContentView(R.layout.telephoneactivity)
                            setupUIAndRegister()
                        }
                    }
                } else {
                    // Ricaricamento fallito, esegui il logout e mostra il layout di `MainActivity`
                    auth.signOut()
                    setContentView(R.layout.telephoneactivity)
                    setupUIAndRegister()
                }
            }
        }
        else {
            //TODO NON SO SE è DA CASTRARE

            // Nessun utente autenticato, mostra il layout di `MainActivity`
            setContentView(R.layout.telephoneactivity)
            setupUIAndRegister()
        }

    }

    private fun setupUIAndRegister() {

        val phoneEditText = findViewById<EditText>(R.id.phoneEditText)
        val codeEditText = findViewById<EditText>(R.id.codeEditText)
        val sendCodeButton = findViewById<Button>(R.id.sendCodeButton)
        val verifyCodeButton = findViewById<Button>(R.id.verifyCodeButton)
        sendCodeButton.setOnClickListener {
            val phoneNumber = phoneEditText.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                sendVerificationCode(phoneNumber)
            } else {
                Toast.makeText(this, "Inserisci un numero di telefono valido", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "errore nella funzione: $phoneEditText ;;; $phoneNumber")
            }
        }

        verifyCodeButton.setOnClickListener {
            val code = codeEditText.text.toString().trim()
            if (code.isNotEmpty()) {
                verifyVerificationCode(code)
            } else {
                Toast.makeText(this, "Inserisci il codice di verifica", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Numero di telefono
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout per il codice di verifica
            .setActivity(this)                 // Activity
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@TelephoneActivity, "Verifica fallita: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity", "qualcosa non va $${e.message}")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@TelephoneActivity.code = verificationId
                    Toast.makeText(this@TelephoneActivity, "Codice inviato", Toast.LENGTH_SHORT).show()
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyVerificationCode(code: String) {
        if (this.code != null) {
            val credential = PhoneAuthProvider.getCredential(this.code!!, code)
            signInWithPhoneAuthCredential(credential)
        } else {
            Toast.makeText(this, "Attendi che l'sms venga inviato", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "verificationId è null. Attendi che l'SMS venga inviato.")
        }
    }


    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    Toast.makeText(this, "Autenticazione riuscita", Toast.LENGTH_SHORT).show()
                    startSecondActivity()
                } else {
                    Toast.makeText(this, "Autenticazione fallita", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startSecondActivity() {
        val intent = Intent(this, MainPage::class.java)
        startActivity(intent)
        finish()
    }

}