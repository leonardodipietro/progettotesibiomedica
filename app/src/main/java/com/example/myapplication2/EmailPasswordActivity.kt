package com.example.myapplication2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale


class EmailPasswordActivity : AppCompatActivity() {


    //todo gestire meglio toast

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepo: UserRepo

    private lateinit var showPasswordIcon: ImageView
    private lateinit var showConfirmPasswordIcon: ImageView
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private lateinit var userExperience: UserExperience

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLocale()
        setContentView(R.layout.emailpasswordactivity)


        auth = FirebaseAuth.getInstance()
        userRepo = UserRepo()
        userExperience=UserExperience()

        val emailEditText = findViewById<EditText>(R.id.email)
        val usernameEditText = findViewById<EditText>(R.id.usernameregistrazione)
        /*val namesurnameEditText=findViewById<EditText>(R.id.nomeecognome)
        val addressEditText=findViewById<EditText>(R.id.indirizzo)*/

        val passwordEditText = findViewById<EditText>(R.id.password)
        val confermaPasswordEditText = findViewById<EditText>(R.id.confermapassword)
        showPasswordIcon = findViewById(R.id.showPassword)
        showConfirmPasswordIcon = findViewById(R.id.showConfirmPassword)
        showPasswordIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(passwordEditText, showPasswordIcon, isPasswordVisible)
        }

        showConfirmPasswordIcon.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(confermaPasswordEditText, showConfirmPasswordIcon, isConfirmPasswordVisible)
        }
        val registerButton = findViewById<Button>(R.id.registerbutton)
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confermaPassword = confermaPasswordEditText.text.toString()
            val phoneNumber=""
            userExperience.validateEmailInput(emailEditText)
            if (email.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty() && confermaPassword.isNotEmpty()) {
                if (password == confermaPassword) { userRepo.checkUsernameExists(username) { exists ->
                        if (exists) {
                            Toast.makeText(this, getString(R.string.username_in_use), Toast.LENGTH_SHORT).show() }
                        else { //Qui inizia la registrazione su Firebase Authentication
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this) { task -> if (task.isSuccessful) {
                                        val userId = auth.currentUser?.uid ?: ""
                                        try {
                                            val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
                                            val user = Utente(id = userId, email = email, username = username,
                                                password = hashedPassword, ruolo = "user", phoneNumber=phoneNumber )
                                            userRepo.saveUserToFirebase(username, hashedPassword,ruolo="user")
                                            val intent = Intent(this, MainPage::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                putExtra("utente", user) }
                                            startActivity(intent)
                                            finish() } catch (e: Exception) {
                                            Log.e("Eccezione", "Errore ${e.message}") } } else {
                                     Toast.makeText(this, getString(R.string.registration_failed), Toast.LENGTH_SHORT).show() } } } } } else {
                    Toast.makeText(this, getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show() } } else {
                Toast.makeText(this, getString(R.string.fields_cannot_be_empty), Toast.LENGTH_SHORT).show() } }


    }

    /*private fun startSecondActivity(user) {
        val intent = Intent(this, MainPage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("utente", user)
        }
        startActivity(intent)
        finish()
        }*/
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

    private fun togglePasswordVisibility(editText: EditText, icon: ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        }
        // Mantenere il cursore alla fine del testo
        editText.setSelection(editText.text.length)
    }
}




