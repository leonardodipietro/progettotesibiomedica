package com.example.myapplication2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.UserRepo
import com.example.myapplication2.utility.UserExperience
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DatabaseReference
import java.util.Locale
import java.util.concurrent.TimeUnit


class TelephoneActivity: AppCompatActivity()   {


    private lateinit var auth: FirebaseAuth
    //codice che viene inviato all'utente
    private var code: String? = null
    private lateinit var userRepo: UserRepo
    private lateinit var showPasswordIcon: ImageView
    private lateinit var showConfirmPasswordIcon: ImageView
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private var isUpdatingPhone = false
    private lateinit var userExperience:UserExperience

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        loadLocale()
        // Nessun utente autenticato, mostra il layout di `MainActivity`
        setContentView(R.layout.telephoneactivity)


        auth = FirebaseAuth.getInstance()
        userRepo=UserRepo()
        userExperience=UserExperience()

            //TODO NON SO SE è DA CASTRARE


        val phoneEditText = findViewById<EditText>(R.id.phoneEditText)
        val namesurnameEditText=findViewById<EditText>(R.id.nomeecognomephone)
        val addressEditText=findViewById<EditText>(R.id.indirizzophone)
        val usernameEditText = findViewById<EditText>(R.id.usernamepercellulare)
        val passwordEditText = findViewById<EditText>(R.id.passwordTel)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confermapasswordtel)
        val codeEditText = findViewById<EditText>(R.id.codeEditText)
        val sendCodeButton = findViewById<Button>(R.id.sendCodeButton)
        val verifyCodeButton = findViewById<Button>(R.id.verifyCodeButton)
        // Gestione della visibilità delle password
        showPasswordIcon = findViewById(R.id.showPasswordTel)
        showConfirmPasswordIcon = findViewById(R.id.showConfirmPasswordTel)

        showPasswordIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            userExperience.togglePasswordVisibility(passwordEditText, showPasswordIcon, isPasswordVisible)
        }

        showConfirmPasswordIcon.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            userExperience.togglePasswordVisibility(confirmPasswordEditText, showConfirmPasswordIcon, isConfirmPasswordVisible)
        }

        userExperience.formatPhoneNumber(phoneEditText, (+39).toString())
        userExperience.normalizeInputs(namesurnameEditText,addressEditText,usernameEditText)

        sendCodeButton.setOnClickListener {
            val phoneNumber = phoneEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()

            val namesurname = namesurnameEditText.text.toString().trim()

            val address = addressEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (phoneNumber.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() &&
                namesurname.isNotEmpty() && address.isNotEmpty()) {

                if (password == confirmPassword) {
                    userRepo.checkUsernameExists(username) { exists ->
                        if (exists) {
                            Toast.makeText(this, "Username già in uso, selezionare un altro", Toast.LENGTH_SHORT).show()
                        } else {
                            // Invia il codice di verifica solo se lo username è disponibile
                            sendVerificationCode(phoneNumber)
                        }
                    }
                } else {
                    Toast.makeText(this, "Le password non corrispondono", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Inserisci tutti i campi", Toast.LENGTH_SHORT).show()
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

    override fun attachBaseContext(newBase: Context) {
        val sharedPref = newBase.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("LANGUAGE", "it")
        val locale = Locale(languageCode ?: "it")
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }


    private fun sendVerificationCode(phoneNumber: String) {
        Toast.makeText(this, "Codice inviato a $phoneNumber", Toast.LENGTH_SHORT).show()
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout per il codice di verifica
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks()
            {



                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {

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
        userRepo = UserRepo()
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Autenticazione riuscita", Toast.LENGTH_SHORT).show()

                    val username = findViewById<EditText>(R.id.usernamepercellulare).text.toString().trim()
                    val password = findViewById<EditText>(R.id.passwordTel).text.toString().trim()
                    val name = findViewById<EditText>(R.id.nomeecognomephone).text.toString().trim()
                    val address = findViewById<EditText>(R.id.indirizzophone).text.toString().trim()
                    val email= ""
                    val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())

                    // Crea l'oggetto Utente
                    val user = Utente(
                        id = auth.currentUser?.uid ?: "",
                        email = email,
                        name = name,
                        address = address,
                        username = username,
                        password = hashedPassword,
                        ruolo = "user",
                        phoneNumber = credential.toString()

                    )

                    // Salva l'utente nel database
                    userRepo.savePhoneUserToFirebase(username, name, address, hashedPassword,email)
                    startSecondActivity(user)
                } else {
                    Toast.makeText(this, "Autenticazione fallita", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun startSecondActivity(user: Utente) {
        val intent = Intent(this, MainPage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("utente", user)
        }
        startActivity(intent)
        finish()
    }


}

