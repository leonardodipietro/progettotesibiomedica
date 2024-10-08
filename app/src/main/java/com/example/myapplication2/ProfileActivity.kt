package com.example.myapplication2

import OnSwipeTouchListener
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.WorkManager
import com.example.myapplication2.repository.UserRepo
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DatabaseReference
import java.util.concurrent.TimeUnit
import com.example.myapplication2.model.Utente

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepo: UserRepo

    private lateinit var database: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Inizializza FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Recupera l'oggetto Utente dalla ProfileActivity
        val utente = intent.getParcelableExtra<Utente>("utente")

        Log.d("ProfileActivityAAAAAAA", "Utente corrente trovato oggetto: ${utente?.id}")
        Log.d("ProfileActivityAAAAAAA", "Utente corrente trovato firebase: ${auth.currentUser?.uid}")
        userRepo= UserRepo()

        userRepo.usersRef
        val logoutButton = findViewById<Button>(R.id.logoutbutton)
        val deleteButton =findViewById<Button>(R.id.deleteAccount)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val profileLayout: View = findViewById(R.id.profilepageroot)

        val emailEditText = findViewById<EditText>(R.id.edit_email)
        val phoneEditText = findViewById<EditText>(R.id.edit_phone)

        val oldPasswordEditText = findViewById<EditText>(R.id.edit_password_old)
        val newPasswordEditText = findViewById<EditText>(R.id.edit_password_new)
        val confirmPasswordEditText = findViewById<EditText>(R.id.edit_password_confirm)
        val saveButton = findViewById<Button>(R.id.button_save)

        val usernameEditText = findViewById<EditText>(R.id.edit_username)
        val nameEditText = findViewById<EditText>(R.id.edit_name)
        val addressEditText = findViewById<EditText>(R.id.edit_address)

        // Recupera l'UID dell'utente
        val currentUser = auth.currentUser
        Log.d("ProfileActivity", "Utente corrente trovato: ${auth.currentUser}")

        // Se l'utente è loggato, recupera i dati dal database
        currentUser?.let { user ->
            Log.d("ProfileActivity", "Utente corrente trovato: UID=${user.uid}")
            userRepo.getUserData(user.uid) { utente ->
                Log.d("ProfileActivity", "Dati utente recuperati dal database: ${utente.toString()}")
                if (utente != null) {
                    emailEditText.setText(utente.email ?: "")
                    phoneEditText.setText(utente.phoneNumber ?: "")
                    usernameEditText.setText(utente.username ?: "")
                    nameEditText.setText(utente.name ?: "")
                    addressEditText.setText(utente.address ?: "")

                    // Popola altri campi se presenti
                } else {
                    // Gestisci il caso in cui i dati non siano presenti nel database
                    Log.d("ProfileActivity", "Nessun dato utente trovato per UID=${user.uid}")
                    Toast.makeText(this, "Nessun dato utente trovato", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // Pulsante Salva le modifiche
        saveButton.setOnClickListener {
            val newEmail = emailEditText.text.toString()
            val newPhone = phoneEditText.text.toString()
            val newUsername = usernameEditText.text.toString()
            val newName = nameEditText.text.toString()
            val newAddress = addressEditText.text.toString()
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (newEmail.isNotEmpty()) {
                Log.d("ProfileActivity", "Tentativo di aggiornamento dell'email a $newEmail")
                userRepo.initiateEmailUpdate(utente, newEmail) { success ->
                    if (success) {
                        Log.d("ProfileActivity", "Email aggiornata con successo")
                        Toast.makeText(this, "Email aggiornata con successo", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("ProfileActivity", "Errore nell'aggiornamento dell'email")
                        Toast.makeText(this, "Errore nell'aggiornamento dell'email", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (newPhone.isNotEmpty()) {
                updatePhoneNumber(newPhone, this)
            }
            if (newUsername.isNotEmpty()) {
                userRepo.updateUsername(newUsername) { success ->
                    if (success) {
                        Toast.makeText(this, "Username aggiornato con successo", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Errore nell'aggiornamento dell'username", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (newName.isNotEmpty()) {
                userRepo.updateName(newName) { success ->
                    if (success) {
                        Toast.makeText(this, "Nome aggiornato con successo", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Errore nell'aggiornamento del nome", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (newAddress.isNotEmpty()) {
                userRepo.updateAddress(newAddress) { success ->
                    if (success) {
                        Toast.makeText(this, "Indirizzo aggiornato con successo", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Errore nell'aggiornamento dell'indirizzo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            // Verifica e cambio password
            if (newPassword.isNotEmpty()) {
                if (newPassword == confirmPassword) {
                    userRepo.changePassword(oldPassword, newPassword) { success ->
                        if (success) {
                            Toast.makeText(this, "Password aggiornata con successo", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Errore nel cambio password. Controlla le credenziali.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Le password non coincidono!", Toast.LENGTH_SHORT).show()
                }
            }


        }


       /* showPasswordButton.setOnClickListener {
            passwordEditText.visibility = if (passwordEditText.visibility == View.GONE) View.VISIBLE else View.GONE
        }*/


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
                    val intent = Intent(this, MainPage::class.java).apply {
                        putExtra("utente", utente) // Passa l'oggetto Utente
                    }
                    startActivity(intent)
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
                val intent = Intent(this@ProfileActivity, MainPage::class.java).apply {
                    putExtra("utente", utente) // Passa l'oggetto Utente
                }
                startActivity(intent)
            }
        })
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Uscire dall'Account?")
            .setMessage("Sei sicuro di voler uscire dall'account?")
            .setPositiveButton("Sì") { dialog, which ->
                // Effettua il logout da Firebase
                auth.signOut()

                // Annulla la notifica
                stopNotification()

                // Elimina le Shared Preferences dell'utente
                clearUserPreferences()

                // Torna alla MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Chiude l'activity corrente
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss() // Chiude la finestra di dialogo
            }
            .create()
            .show()
    }
    private fun clearUserPreferences() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Rimuove tutti i dati
        editor.apply()
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
        Log.d("ProfileActivityyyyyy","valore ${auth.currentUser?.uid}")

        user?.let { currentUser ->
            // Cancella l'account utente da Firebase Authentication
            currentUser.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Cancella i dati dal Realtime Database
                        userRepo.deleteAccount(currentUser.uid) { success ->
                            if (success) {
                                stopNotification() // Annulla le notifiche
                                // Torna alla MainActivity e cancella la memoria delle Activity precedenti
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                Log.d("ProfileActivity", "Errore nell'eliminazione dei dati dal database")
                            }
                        }
                    } else {
                        Log.d("ProfileActivity", "Errore nell'eliminazione dell'account da Firebase Authentication")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Errore: ${e.message}", e)
                }
        } ?: Log.d("ProfileActivity", "L'utente non è autenticato")
    }

    private fun stopNotification() {
        Log.d("MainPage", "fermata la notifica")

        WorkManager.getInstance(this).cancelUniqueWork("DailyNotificationWork")
    }

    private fun updateEmail(newEmail: String) {
        val user = auth.currentUser
        user?.updateEmail(newEmail)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Email aggiornata correttamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Errore nell'aggiornamento dell'email", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updatePhoneNumber(newPhone: String, activity: Activity) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(newPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.currentUser?.updatePhoneNumber(credential)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(activity, "Numero di telefono aggiornato", Toast.LENGTH_SHORT).show()
                            }
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(activity, "Errore nella verifica del numero", Toast.LENGTH_SHORT).show()
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun updatePassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser
        val credential = EmailAuthProvider.getCredential(user?.email!!, oldPassword)

        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Toast.makeText(this, "Password aggiornata", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Errore nell'aggiornamento della password", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "La vecchia password non è corretta", Toast.LENGTH_SHORT).show()
            }
        }
    }

}

