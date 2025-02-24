package com.example.myapplication2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication2.repository.UserRepo
import org.mindrot.jbcrypt.BCrypt
import java.util.Locale

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var resetButton: Button
    private lateinit var userRepo: UserRepo
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLocale()
        setContentView(R.layout.activityresetpsw)

        userRepo = UserRepo()
        newPasswordEditText = findViewById(R.id.newPasswordresetpage)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordresetpage)
        resetButton = findViewById(R.id.resetButtonresetpage)
        username = intent.getStringExtra("username") ?: ""

        if (username.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_error_username_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        resetButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (newPassword == confirmPassword) {
                saveNewPassword(newPassword)
            } else {
                Toast.makeText(this, getString(R.string.toast_error_passwords_not_matching), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun saveNewPassword(password: String) {
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        userRepo.changePassword(username, hashedPassword) { success ->
            if (success) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, getString(R.string.toast_error_saving_password), Toast.LENGTH_SHORT).show()
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

}