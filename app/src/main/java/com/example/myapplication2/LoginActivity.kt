package com.example.myapplication2
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.UserRepo
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
    private lateinit var userRepo: UserRepo
    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        userRepo= UserRepo()
        val user: Utente
        auth = FirebaseAuth.getInstance()
      //  val mailEditText=findViewById<EditText>(R.id.maillogin)
        val usernameEditText=findViewById<EditText>(R.id.usernamelogin)
        val pswEditText=findViewById<EditText>(R.id.pswlogin)
        val logmailbutton=findViewById<Button>(R.id.loginconmail)
        val showPassword = findViewById<ImageView>(R.id.mostraPassword)
        var isPasswordVisible = false //variabile che usiamo per gestire visibilita della password
        val resetPassword=findViewById<TextView>(R.id.iniziaresetpsw)

        logmailbutton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = pswEditText.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                userRepo.verifyUserCredentials(username, password) { isSuccess, errorMessage, admin, user ->
                    if (isSuccess) {
                        if (admin == true) {
                            // Login come admin, reindirizza all'AdminActivity
                            Toast.makeText(this, "Accesso Admin riuscito", Toast.LENGTH_SHORT).show()
                            Log.d("Login", "Admin flag: $admin")

                            // Passa l'oggetto Utente all'AdminActivity
                            val intent = Intent(this, AdminActivity::class.java).apply {
                                putExtra("utente", user) // Passa l'oggetto Utente
                            }
                            startActivity(intent)
                        } else {
                            // Login come utente normale
                            Toast.makeText(this, "Login utente normale riuscito", Toast.LENGTH_SHORT).show()
                            Log.d("Login", "Admin flag: $admin")

                            // Passa l'oggetto Utente alla MainPage

                            val intent = Intent(this, MainPage::class.java).apply {
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra("utente", user) // Passa l'oggetto Utente
                            }
                            startActivity(intent)
                        }
                    } else {
                        // Mostra un messaggio di errore
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Inserisci username e password", Toast.LENGTH_SHORT).show()
            }
        }
        showPassword.setOnClickListener {
            if (isPasswordVisible) {
                // Nascondi la password
                pswEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                showPassword.setImageResource(R.drawable.passwordicon) // Cambia l'icona in "occhio chiuso"
            } else {
                // Mostra la password
                pswEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                showPassword.setImageResource(R.drawable.passwordicon) // Cambia l'icona in "occhio aperto"
            }
            // Posiziona il cursore alla fine del testo
            pswEditText.setSelection(pswEditText.text.length)
            isPasswordVisible = !isPasswordVisible
        }




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

        /*DA VEDERE POI
        resetPassword.setOnClickListener {
            val email = mailEditText.text.toString()

            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this, ResetPassword::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Errore nell'invio dell'email", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Inserisci l'email", Toast.LENGTH_SHORT).show()
            }
        }*/

      /*  val phoneEditText = findViewById<EditText>(R.id.edtextphonenumber)
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
        }*/
    }

   /* private fun sendVerificationCode(phoneNumber: String) {
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
    }*/




 */