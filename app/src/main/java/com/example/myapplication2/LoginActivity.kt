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
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    //todo vedere occhio
    //TODO CREAREICONA
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
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
                            // Recupera l'utente autenticato
                            val user = auth.currentUser
                            if (user != null) {
                                // Verifica se l'utente è admin o utente normale
                                checkUserTypeAndRedirect(user.uid)
                            }
                        } else {
                            Toast.makeText(this, "Login fallito", Toast.LENGTH_SHORT).show()
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

    fun checkUserTypeAndRedirect(uid: String) {
        val userRef = database.reference.child("users").child(uid)
        Log.d("checkUserTypeAndRedirect", "Recupero dati utente per UID: $uid")  // Log per verificare l'UID dell'utente

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Log per confermare che i dati dell'utente sono stati trovati nel database
                Log.d("checkUserTypeAndRedirect", "Dati utente trovati: ${snapshot.value}")

                // Estrae il valore di isAdmin dal database
                val isAdmin = snapshot.child("admin").getValue(Boolean::class.java) ?: false
                Log.d("checkUserTypeAndRedirect", "isAdmin: $isAdmin")  // Log per controllare il valore di isAdmin

                if (isAdmin) {
                    // Se l'utente è admin, reindirizza alla AdminActivity
                    Log.d("checkUserTypeAndRedirect", "L'utente è un amministratore. Reindirizzamento alla AdminActivity.")
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Se è un utente normale, reindirizza alla MainPage
                    Log.d("checkUserTypeAndRedirect", "L'utente non è un amministratore. Reindirizzamento alla MainPage.")
                    val intent = Intent(this, MainPage::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                // Log per il caso in cui l'utente non sia trovato nel database
                Log.d("checkUserTypeAndRedirect", "Dati utente non trovati per UID: $uid")
                Toast.makeText(this, "Dati utente non trovati", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            // Log per errori durante il recupero dei dati
            Log.e("checkUserTypeAndRedirect", "Errore nel recupero dati utente: ${exception.message}")
            Toast.makeText(this, "Errore nel recupero dati utente", Toast.LENGTH_SHORT).show()
        }
    }


    private fun startMainPage() {
        val intent = Intent(this, MainPage::class.java)
        startActivity(intent)
        finish()
    }
    }

/*
pswEditText.setOnTouchListener { _, event ->
    if (event.action == MotionEvent.ACTION_UP) {
        // Verifica se l'utente ha toccato l'icona
        if (event.rawX >= (pswEditText.right - pswEditText.compoundDrawables[2].bounds.width())) {
            // Alterna la visibilità della password
            if (isPasswordVisible) {
                // Nascondi la password
                pswEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                pswEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0)
            } else {
                // Mostra la password
                pswEditText.transformationMethod = null
                pswEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_closed, 0)
            }
            isPasswordVisible = !isPasswordVisible
            // Sposta il cursore alla fine del testo
            pswEditText.setSelection(pswEditText.text.length)
            return@setOnTouchListener true
        }
    }
    false
}
 */