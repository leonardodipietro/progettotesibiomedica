package com.example.myapplication2

import OnSwipeTouchListener
import android.app.Activity
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkManager
import com.example.myapplication2.Presenter.ProfilePresenter
import com.example.myapplication2.interfacepackage.ProfileView
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
import com.example.myapplication2.utility.UserExperience
import org.mindrot.jbcrypt.BCrypt
class ProfileActivity : AppCompatActivity(), ProfileView {
    private lateinit var presenter: ProfilePresenter
    private lateinit var currentUser: Utente
    private lateinit var currentPhoneNumber: String

    private lateinit var userExperience: UserExperience

    //
    // Variabili UI
    private lateinit var logoutButton: Button
    private lateinit var deleteButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var bottomNavigationAdmin: BottomNavigationView
    private lateinit var profileLayout: View
    private lateinit var emailEditText: EditText
   // private lateinit var phoneEditText: EditText
    private lateinit var phoneTextView: TextView

    private lateinit var oldPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var showOldPassword: ImageView
    private lateinit var showNewPassword: ImageView
    private lateinit var showConfirmPassword: ImageView
    private lateinit var saveButton: Button
    private lateinit var usernameEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var auth: FirebaseAuth
    // Flag per visibilità delle password
    private var isOldPasswordVisible = false
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        presenter = ProfilePresenter(this, UserRepo())
        currentUser = intent.getParcelableExtra("utente") ?: throw IllegalStateException("Utente non trovato")
        currentUser.id?.let { presenter.loadUserData(it) }
        setupUI()
        // Controlla il ruolo dell'utente e imposta la navigazione corretta
        when (currentUser.ruolo) {
            "admin", "superadmin" -> setupAdminNavigation()
            "user" -> setupUserNavigation()
            else -> throw IllegalStateException("Ruolo non riconosciuto")
        }

        auth=FirebaseAuth.getInstance()
        userExperience= UserExperience()

        setupListeners()
        //setupBottomNavigation()
    }

    private fun setupUI() {
        logoutButton = findViewById(R.id.logoutbutton)
        deleteButton = findViewById(R.id.deleteAccount)
        // Inizializza le BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationAdmin = findViewById(R.id.bottom_navigation_admin)
        profileLayout = findViewById(R.id.profilepageroot)
        emailEditText = findViewById(R.id.edit_email)
        //phoneEditText = findViewById(R.id.edit_phone)
        phoneTextView = findViewById(R.id.edit_phone)
        oldPasswordEditText = findViewById(R.id.edit_password_old)
        newPasswordEditText = findViewById(R.id.edit_password_new)
        confirmPasswordEditText = findViewById(R.id.edit_password_confirm)
        showOldPassword = findViewById(R.id.mostraVecchiaPasswordUser)
        showNewPassword = findViewById(R.id.mostraNuovaPasswordUser)
        showConfirmPassword = findViewById(R.id.mostraConfermaPasswordUser)
        saveButton = findViewById(R.id.button_save)
        usernameEditText = findViewById(R.id.edit_username)
        nameEditText = findViewById(R.id.edit_name)
        addressEditText = findViewById(R.id.edit_address)

    }

    private fun setupListeners() {
        logoutButton.setOnClickListener {
            showLogoutDialog()
        }
        deleteButton.setOnClickListener {
            showDeleteDialog()
        }
        saveButton.setOnClickListener {
            currentUser.id?.let { it1 ->
                presenter.saveUserData(
                    it1,
                    emailEditText.text.toString(),
                    //phoneEditText.text.toString(),
                    usernameEditText.text.toString(),
                    nameEditText.text.toString(),
                    addressEditText.text.toString(),
                    oldPasswordEditText.text.toString(),
                    newPasswordEditText.text.toString(),
                    confirmPasswordEditText.text.toString()
                )
            }
        }

        showOldPassword.setOnClickListener {
            isOldPasswordVisible = togglePasswordVisibility(oldPasswordEditText, isOldPasswordVisible)
        }
        showNewPassword.setOnClickListener {
            isNewPasswordVisible = togglePasswordVisibility(newPasswordEditText, isNewPasswordVisible)
        }
        showConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = togglePasswordVisibility(confirmPasswordEditText, isConfirmPasswordVisible)
        }

        // Click listener per la TextView del numero di telefono
        phoneTextView.setOnClickListener {
            showPhoneUpdateDialog()
        }
    }
    // Mostra il dialogo per inserire il vecchio e il nuovo numero di telefono
    fun showPhoneUpdateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_phone, null)
        val oldPhoneEditText = dialogView.findViewById<EditText>(R.id.edit_old_phone)
        val newPhoneEditText = dialogView.findViewById<EditText>(R.id.edit_new_phone)

        AlertDialog.Builder(this)
            .setTitle("Modifica numero di telefono")
            .setView(dialogView)
            .setPositiveButton("Conferma") { _, _ ->
                val oldPhone = oldPhoneEditText.text.toString().trim()
                val newPhone = newPhoneEditText.text.toString().trim()

                if (oldPhone.isNotEmpty() && newPhone.isNotEmpty()) {
                    currentUser.id?.let { userId ->
                        // Chiama il metodo per avviare la verifica per vecchio e nuovo numero
                        presenter.authenticateAndModifyPhoneNumber(userId, oldPhone, newPhone)
                    }
                } else {
                    showError("Inserisci il vecchio e il nuovo numero di telefono")
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // Implementa il metodo per mostrare il dialogo per l'inserimento del codice di verifica per il nuovo numero
    override fun showNewPhoneVerificationDialog(newPhone: String, onCodeEntered: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.second_dialogphone, null)
        val verificationCodeEditText = dialogView.findViewById<EditText>(R.id.edit_verification_code)

        AlertDialog.Builder(this)
            .setTitle("Verifica il nuovo numero: $newPhone")
            .setView(dialogView)
            .setPositiveButton("Conferma") { _, _ ->
                val verificationCode = verificationCodeEditText.text.toString().trim()
                if (verificationCode.isNotEmpty()) {
                    onCodeEntered(verificationCode) // Passa il codice di verifica
                } else {
                    showError("Inserisci il codice di verifica")
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    private fun setupAdminNavigation() {
        // Mostra solo la barra di navigazione admin
        bottomNavigationAdmin.visibility = View.VISIBLE
        bottomNavigationView.visibility = View.GONE

        bottomNavigationAdmin.selectedItemId = R.id.nav_profile
        bottomNavigationAdmin.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
                R.id.nav_admin -> {
                    val intent = Intent(this, AdminActivity::class.java).apply {
                        putExtra("utente", currentUser)
                    }
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupUserNavigation() {
        // Mostra solo la barra di navigazione utente
        bottomNavigationView.visibility = View.VISIBLE
        bottomNavigationAdmin.visibility = View.GONE

        bottomNavigationView.selectedItemId = R.id.nav_profile
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainPage::class.java).apply {
                        putExtra("utente", currentUser)
                    }
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> true
                R.id.nav_info -> {
                    val intent = Intent(this, InfoActivity::class.java).apply {
                        putExtra("utente", currentUser)
                    }
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }


    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Uscire dall'Account?")
            .setMessage("Sei sicuro di voler uscire dall'account?")
            .setPositiveButton("Sì") { _, _ ->
                presenter.logout()
                clearUserPreferences()
                stopNotification()
               // navigateToHome()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Eliminare l'account?")
            .setMessage("Sei sicuro di voler eliminare il tuo account?")
            .setPositiveButton("Sì") { _, _ ->
                presenter.deleteAccount(currentUser)
                clearUserPreferences()
                stopNotification()
                //navigateToHome()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
    override fun showPasswordDialog(email: String, hashedPassword: String, onPasswordConfirmed: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Conferma Password")
        builder.setMessage("Inserisci la tua password per confermare l'eliminazione dell'account")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Conferma") { _, _ ->
            val inputPassword = input.text.toString()
            onPasswordConfirmed(inputPassword)
        }
        builder.setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun showPhoneVerificationDialog(phoneNumber: String, onCodeEntered: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verifica Numero di Telefono")
        builder.setMessage("Inserisci il codice di verifica inviato a $phoneNumber")

        val input = EditText(this)
        input.hint = "Codice di verifica"
        builder.setView(input)

        builder.setPositiveButton("Verifica") { _, _ ->
            val verificationCode = input.text.toString()
            onCodeEntered(verificationCode)
        }
        builder.setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    override fun populateUserData(user: Utente) {
        emailEditText.hint = user.email
        //TODO FORSE CI METTIAMO ALTRO
        //phoneEditText.hint = user.phoneNumber
        usernameEditText.hint = user.username
        nameEditText.hint = user.name
        addressEditText.hint = user.address
    }
    override fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }



    override fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun requestPhoneVerification(phone: String) {
        TODO("Not yet implemented")
    }

    private fun togglePasswordVisibility(editText: EditText, isVisible: Boolean): Boolean {
        editText.inputType = if (isVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        editText.setSelection(editText.text.length)
        return !isVisible
    }

    override fun clearUserPreferences() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }
    override fun confirmDeletion() {
        presenter.confirmDeletion(currentUser)
    }

    override fun stopNotification() {
        WorkManager.getInstance(this).cancelUniqueWork("NotificaWorker")
        WorkManager.getInstance(this).cancelAllWorkByTag("daily_notification")

    }


}

