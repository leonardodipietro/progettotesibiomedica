package com.example.myapplication2

import OnSwipeTouchListener
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication2.repository.UserRepo
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepo: UserRepo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)


        // Inizializza FirebaseAuth
        auth = FirebaseAuth.getInstance()
        userRepo= UserRepo()
        val logoutButton = findViewById<Button>(R.id.logoutbutton)
        val deleteButton =findViewById<Button>(R.id.deleteAccount)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val profileLayout: View = findViewById(R.id.profilepageroot)



        logoutButton.setOnClickListener {
           showLogoutDialog()
        }

        deleteButton.setOnClickListener {
            showDeleteDialog()
        }


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

        // Rileva il movimento di swipe
        profileLayout.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeRight() {
                // Naviga alla MainPage
                val intent = Intent(this@ProfileActivity, MainPage::class.java)
                startActivity(intent)
            }
        })
    }


    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Uscire dall'Account?")
            .setMessage("Sei sicuro di voler uscire dall account?")
            .setPositiveButton("Sì") { dialog, which ->
                // Effettua il logout
                auth.signOut()

                // Torna alla MainActivity
                val intent = Intent(this, MainActivity::class.java)
                //serve per rimuovere la main page dallo stack di memoria
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()  // Chiude l'activity corrente

            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss() // Chiudi la finestra di dialogo
            }
            .create()
            .show()
    }
    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Eliminare l'account?")
            .setMessage("Sei sicuro di voler eliminare il tuo account? Questa operazione non può essere annullata.")
            .setPositiveButton("Sì") { dialog, which ->
                deleteAccount()
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss() // Chiudi la finestra di dialogo
            }
            .create()
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser

        user?.let {

            it.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        userRepo.deleteAccount(it.uid) { success ->
                            if (success) {
                                // Torna alla MainActivity cancelandom la memoria
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                Log.d("ProfileActivity","Eliminazione non riuscita 1")

                            }
                        }
                    } else {
                        Log.d("ProfileActivity","Eliminazione non riuscita 2")
                    }
                }
        }
    }
}