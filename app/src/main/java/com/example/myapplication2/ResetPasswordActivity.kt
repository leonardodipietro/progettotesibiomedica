package com.example.myapplication2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication2.repository.UserRepo
import org.mindrot.jbcrypt.BCrypt

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var resetButton: Button
    private lateinit var userRepo: UserRepo
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activityresetpsw)

        userRepo = UserRepo()
        newPasswordEditText = findViewById(R.id.newPasswordresetpage)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordresetpage)
        resetButton = findViewById(R.id.resetButtonresetpage)
        username = intent.getStringExtra("username") ?: ""

        if (username.isEmpty()) {
            Toast.makeText(this, "Errore: Username non trovato", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        resetButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (newPassword == confirmPassword) {
                saveNewPassword(newPassword)
            } else {
                Toast.makeText(this, "Le password non coincidono", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveNewPassword(password: String) {
        // Cripta la password usando BCrypt
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

        userRepo.changePassword(username, hashedPassword) { success ->
            if (success) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Errore nel salvataggio della password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}