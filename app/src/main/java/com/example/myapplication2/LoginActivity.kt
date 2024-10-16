package com.example.myapplication2
import android.content.Context
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
import com.example.myapplication2.Presenter.LoginPresenter
import com.example.myapplication2.interfacepackage.LoginInterface
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import java.util.concurrent.TimeUnit


    class LoginActivity : AppCompatActivity(), LoginInterface {
//TODO RIVEDERE NUMERO DI TELEFONO PER ADMIN
        private lateinit var presenter: LoginPresenter
        private lateinit var usernameEditText: EditText
        private lateinit var passwordEditText: EditText
        private lateinit var showPasswordImageView: ImageView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_login)

            val userRepo = UserRepo()
            presenter = LoginPresenter(this, userRepo)

            usernameEditText = findViewById(R.id.usernamelogin)
            passwordEditText = findViewById(R.id.pswlogin)
            showPasswordImageView = findViewById(R.id.mostraPassword)

            findViewById<Button>(R.id.loginconmail).setOnClickListener {
                onLoginClicked(usernameEditText.text.toString(), passwordEditText.text.toString())
            }

            findViewById<TextView>(R.id.iniziaresetpsw).setOnClickListener {
                onResetPasswordClicked(usernameEditText.text.toString())
            }

            showPasswordImageView.setOnClickListener {
                onShowPasswordClicked()
            }

        }

        /*override fun showLoginSuccess(admin: Boolean, user: Utente?) {
            val intent = Intent(this, if (admin) AdminActivity::class.java else MainPage::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("utente", user)
            }
            startActivity(intent)
        }*/
        override fun showLoginSuccess(ruolo: String, user: Utente?) {
            Log.d("showLoginSuccess", "Ruolo ricevuto: $ruolo")
            Log.d("showLoginSuccess", "Utente ricevuto: ${user?.username ?: "Nessun utente"}")
            Log.d("showLoginSuccess", "Utente ricevuto: ${user?.id ?: "Nessun utente"}")
            val targetActivity = when (ruolo) {
                "superadmin" -> {
                    Log.d("showLoginSuccess", "Navigazione verso SuperAdminActivity con ${user?.username ?: "Nessun utente"}")
                    SuperAdminActivity::class.java
                }
                "admin" -> {
                    Log.d("showLoginSuccess", "Navigazione verso AdminActivity con ${user?.username ?: "Nessun utente"}")
                    AdminActivity::class.java
                }
                else -> {
                    Log.d("showLoginSuccess", "Navigazione verso MainPage (utente normale)con ${user?.username ?: "Nessun utente"}")
                    MainPage::class.java
                }
            }

            val intent = Intent(this, targetActivity).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("utente", user)
                Log.d("IntentCreation", "Aggiungo all'Intent - Utente: ${user?.username ?: "Nessun utente"} - Ruolo: ${user?.ruolo ?: "Ruolo non disponibile"}")

            }

            Log.d("showLoginSuccess", "Inizio l'activity con intent per ${targetActivity.simpleName}")
            startActivity(intent)
        }



        override fun showLoginFailure(errorMessage: String) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }

        override fun showAccountLocked(remainingTime: Long) {
            Toast.makeText(this, "Account bloccato. Riprova tra $remainingTime secondi.", Toast.LENGTH_LONG).show()
        }

        override fun showResetPasswordEmailSent() {
            Toast.makeText(this, "Email di reset inviata. Controlla la tua posta.", Toast.LENGTH_SHORT).show()
        }

        override fun showResetPasswordError(errorMessage: String) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }

        override fun promptForUsername() {
            Toast.makeText(this, "Inserisci il tuo username.", Toast.LENGTH_SHORT).show()
        }

        override fun togglePasswordVisibility(isVisible: Boolean) {
            passwordEditText.inputType = if (isVisible) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordEditText.setSelection(passwordEditText.text.length)
            showPasswordImageView.setImageResource(if (isVisible) R.drawable.passwordicon else R.drawable.passwordicon)
        }

        override fun onLoginClicked(username: String, password: String) {
            presenter.handleLogin(username, password)
        }

        override fun onResetPasswordClicked(username: String) {
            presenter.handleResetPassword(username)
        }


        override fun onShowPasswordClicked() {
            presenter.handleShowPassword()
        }
    }

    //todo vedere occhio
    //TODO CREAREICONA
    /*private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private lateinit var userRepo: UserRepo
    private var loginfalliti = 0
    private var bloccoutenteinizio: Long = 0
    private var duratablocco: Long = 0

    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        userRepo= UserRepo()
        val user: Utente
        //auth = FirebaseAuth.getInstance()

      //  val mailEditText=findViewById<EditText>(R.id.maillogin)
        val usernameEditText=findViewById<EditText>(R.id.usernamelogin)
        val pswEditText=findViewById<EditText>(R.id.pswlogin)
        val logmailbutton=findViewById<Button>(R.id.loginconmail)
        val showPassword = findViewById<ImageView>(R.id.mostraPassword)
        var isPasswordVisible = false //variabile che usiamo per gestire visibilita della password
        val resetPassword=findViewById<TextView>(R.id.iniziaresetpsw)



        resetPassword.setOnClickListener {
            val username = usernameEditText.text.toString()

            if (username.isNotEmpty()) {
                userRepo.getEmailByUsername(username) { email, error ->
                    if (email != null) {

                        // Invia l'email di reset
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this@LoginActivity, "Email di reset inviata. Controlla la tua posta.", Toast.LENGTH_SHORT).show()
                                    Log.d("ResetPassword", "Email di reset inviata a: $email")
                                } else {
                                    Toast.makeText(this@LoginActivity, "Errore nell'invio dell'email di reset. Controlla l'email associata.", Toast.LENGTH_SHORT).show()
                                    Log.d("ResetPassword", "Errore nell'invio dell'email di reset: ${task.exception?.message}")
                                }
                            }
                    } else {
                        Toast.makeText(this@LoginActivity, error ?: "Errore sconosciuto", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Inserisci il tuo username per procedere.", Toast.LENGTH_SHORT).show()
            }
        }


        logmailbutton.setOnClickListener {
            if (isAccountLocked()) {
                val remainingTime = (bloccoutenteinizio + duratablocco - System.currentTimeMillis()) / 1000
                Toast.makeText(this, "Account bloccato. Riprova tra $remainingTime secondi.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val username = usernameEditText.text.toString()
            val password = pswEditText.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                userRepo.verifyUserCredentials(username, password) { isSuccess, errorMessage, admin, user ->
                    if (isSuccess) {
                        loginfalliti = 0
                        duratablocco = 0
                        bloccoutenteinizio = 0
                        if (admin == true) {
                            Toast.makeText(this, "Accesso Admin riuscito", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, AdminActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra("utente", user)

                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Login utente normale riuscito", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainPage::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra("utente", user)
                                if (user != null) {
                                    saveUserToPreferences(user)
                                }
                            }
                            startActivity(intent)
                        }
                    } else {
                        gestioneloginfalliti()
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

    // Metodo per verificare se l'account è bloccato
    private fun isAccountLocked(): Boolean {
        val currentTime = System.currentTimeMillis()
        // Verifica se il tempo di blocco è scaduto
        if (currentTime >= bloccoutenteinizio + duratablocco) {
            // Reimposta il contatore dei tentativi se il tempo di blocco è scaduto
            loginfalliti = 0
            duratablocco = 0
            bloccoutenteinizio = 0
            return false
        }
        return true
    }
    fun saveUserToPreferences(user: Utente) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(user)
        editor.putString("utente", json)
        editor.putBoolean("isLoggedIn", true)
        editor.apply()
    }

    // Metodo per gestire il blocco in base ai tentativi
    private fun gestioneloginfalliti() {
        loginfalliti++
        if (loginfalliti == 5) {
            duratablocco = 10 * 60 * 1000 // Blocco di 10 minuti
            bloccoutenteinizio= System.currentTimeMillis()
        } else if (loginfalliti == 10) {
            duratablocco= 60 * 60 * 1000 // Blocco di 1 ora
            bloccoutenteinizio = System.currentTimeMillis()
        } else if (loginfalliti == 15) {
            duratablocco = 24 * 60 * 60 * 1000 // Blocco di 1 giorno
            bloccoutenteinizio = System.currentTimeMillis()
        }
    }

}
*/




