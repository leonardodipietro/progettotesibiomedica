package com.example.myapplication2
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        auth = FirebaseAuth.getInstance()
        val mailEditText=findViewById<EditText>(R.id.maillogin)
        val pswEditText=findViewById<EditText>(R.id.pswlogin)
        val logmailbutton=findViewById<Button>(R.id.loginconmail)
        logmailbutton.setOnClickListener {
            val email = mailEditText.text.toString()
            val password = pswEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Login riuscito, puoi avviare l'attività successiva (ad esempio, MainPage)
                            Log.d("LoginActivity", "Login effettuato con successo: ${auth.currentUser?.email}")
                            startMainPage()  // Funzione per avviare la nuova Activity dopo il login
                        } else {
                            // Login fallito, mostra un messaggio all'utente
                            Toast.makeText(this, "Login fallito: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            Log.d("LoginActivity", "Login fallito: ${task.exception?.message}")
                        }
                    }
            } else {
                Toast.makeText(this, "Email e password non possono essere vuote", Toast.LENGTH_SHORT).show()
            }
        }

        val phoneEditText = findViewById<EditText>(R.id.edtextphonenumber)
        val codeEditText = findViewById<EditText>(R.id.codeEditText)
        val sendCodeButton = findViewById<Button>(R.id.sendCodeButton)
        val verifyCodeButton = findViewById<Button>(R.id.verifyCodeButton)

        // Invia il codice di verifica
        sendCodeButton.setOnClickListener {
            val phoneNumber = phoneEditText.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                sendVerificationCode(phoneNumber)
            } else {
                Toast.makeText(this, "Inserisci un numero di telefono valido", Toast.LENGTH_SHORT).show()
            }
        }

        // Verifica il codice ricevuto
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
            .setActivity(this)                 // Activity corrente
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Autenticazione completata automaticamente
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@LoginActivity, "Verifica fallita: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    // Salva il verificationId per utilizzarlo durante la verifica
                    this@LoginActivity.verificationId = verificationId
                    Toast.makeText(this@LoginActivity, "Codice inviato", Toast.LENGTH_SHORT).show()

                    // Rendi visibile la parte di UI per inserire il codice
                    findViewById<EditText>(R.id.codeEditText).visibility = View.VISIBLE
                    findViewById<Button>(R.id.verifyCodeButton).visibility = View.VISIBLE
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyVerificationCode(code: String) {
        val verificationId = this.verificationId
        if (verificationId != null) {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithPhoneAuthCredential(credential)
        } else {
            Toast.makeText(this, "Codice non ancora inviato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Autenticazione riuscita
                    Toast.makeText(this, "Autenticazione riuscita", Toast.LENGTH_SHORT).show()
                    // Esegui il redirect all'activity principale
                    startMainPage()
                } else {
                    // Autenticazione fallita
                    Toast.makeText(this, "Autenticazione fallita", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startMainPage() {
        val intent = Intent(this, MainPage::class.java)
        startActivity(intent)
        finish()
    }
    }
