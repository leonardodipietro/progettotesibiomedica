package com.example.myapplication2

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ResetPassword: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.resetpassword)

        val backButton = findViewById<Button>(R.id.backToLoginPage)

        backButton.setOnClickListener {
            finish()
        }
    }
}