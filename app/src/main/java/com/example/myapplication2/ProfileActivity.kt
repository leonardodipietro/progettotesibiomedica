package com.example.myapplication2

import OnSwipeTouchListener
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_profile // Set the default selection

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Passa alla MainPage
                    startActivity(Intent(this, MainPage::class.java))
                    true
                }
                R.id.nav_profile -> {
                    // Sei già su questa Activity, non fare nulla
                    true
                }
                else -> false
            }
        }




        val profileLayout: View = findViewById(R.id.profilepageroot) // Il layout radice della tua Activity

        // Rileva il movimento di swipe
        profileLayout.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeRight() {
                // Naviga alla MainPage
                val intent = Intent(this@ProfileActivity, MainPage::class.java)
                startActivity(intent)
            }
        })
    }

}