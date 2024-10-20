package com.example.myapplication2

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication2.Presenter.AdminPresenter
import com.example.myapplication2.Presenter.ProfileAdminPresenter
import com.example.myapplication2.interfacepackage.AdminView
import com.example.myapplication2.interfacepackage.PasswordType
import com.example.myapplication2.interfacepackage.ProfileAdminView
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileAdminActivity : AppCompatActivity(), ProfileAdminView {

    // Dichiarazioni delle variabili
    private lateinit var presenter: ProfileAdminPresenter
    private lateinit var emailEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var oldPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var showOldPassword: ImageView
    private lateinit var showNewPassword: ImageView
    private lateinit var showConfirmPassword: ImageView
    private lateinit var modifyButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var currentUser: Utente

    private lateinit var userRepo: UserRepo



    // Stato visibilità password
    private var isOldPasswordVisible = false
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profileadmin)

        userRepo = UserRepo()
        // Associa il presenter e inizializza l'utente
        presenter = ProfileAdminPresenter(this,userRepo )
        // Verifica se l'intent contiene l'utente
        currentUser = intent.getParcelableExtra("utente") ?: throw IllegalStateException("Utente non trovato")
        Log.d("ProfileAdminActivity", "Utente trovato: $currentUser")

        // Inizializza le viste
        initViews()

        // Carica i dati dell'utente dal presenter
        currentUser.id?.let {
            Log.d("ProfileAdminActivity", "Caricamento dati utente con id: $it")
            presenter.loadUserData(it)
        } ?: Log.e("ProfileAdminActivity", "ID utente non trovato")


        setupEventListeners()
    }

    private fun initViews() {
        emailEditText = findViewById(R.id.editemailadmin)
        usernameEditText = findViewById(R.id.editusernameadmin)
        oldPasswordEditText = findViewById(R.id.editpswadminold)
        newPasswordEditText = findViewById(R.id.editpswadmindnew)
        confirmPasswordEditText = findViewById(R.id.editpswadminconferm)
        showOldPassword = findViewById(R.id.mostraVecchiaPassword)
        showNewPassword = findViewById(R.id.mostraNuovaPassword)
        showConfirmPassword = findViewById(R.id.mostraConfermaPassword)
        modifyButton = findViewById(R.id.buttonmodifyadmin)
        bottomNavigationView = findViewById(R.id.bottom_navigation_profileadmin)
    }

    private fun setupEventListeners() {
        showOldPassword.setOnClickListener {
            togglePasswordVisibility(PasswordType.OLD_PASSWORD, isOldPasswordVisible)
        }

        showNewPassword.setOnClickListener {
            togglePasswordVisibility(PasswordType.NEW_PASSWORD, isNewPasswordVisible)
        }

        showConfirmPassword.setOnClickListener {
            togglePasswordVisibility(PasswordType.CONFIRM_PASSWORD, isConfirmPasswordVisible)
        }

        modifyButton.setOnClickListener {
            saveUserData()
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.nav_admin) {
                navigateToAdminHome()
                true
            } else {
                false
            }
        }
    }

    private fun saveUserData() {
        val email = emailEditText.text.toString()
        val username = usernameEditText.text.toString()
        val oldPassword = oldPasswordEditText.text.toString()
        val newPassword = newPasswordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        val userId = currentUser.id

        userId?.let { presenter.saveUserData(it, email, "", username, oldPassword, newPassword, confirmPassword) }
    }

    private fun navigateToAdminHome() {
        val intent = Intent(this, AdminActivity::class.java).apply {
            putExtra("utente", currentUser)
        }
        startActivity(intent)
    }

    override fun togglePasswordVisibility(passwordType: PasswordType, isVisible: Boolean) {
        val editText = when (passwordType) {
            PasswordType.OLD_PASSWORD -> oldPasswordEditText
            PasswordType.NEW_PASSWORD -> newPasswordEditText
            PasswordType.CONFIRM_PASSWORD -> confirmPasswordEditText
        }

        editText.inputType = if (isVisible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        editText.setSelection(editText.text.length)

        when (passwordType) {
            PasswordType.OLD_PASSWORD -> isOldPasswordVisible = !isVisible
            PasswordType.NEW_PASSWORD -> isNewPasswordVisible = !isVisible
            PasswordType.CONFIRM_PASSWORD -> isConfirmPasswordVisible = !isVisible
        }
    }



    override fun populateUserData(user: Utente) {
        emailEditText.hint = user.email ?: ""
        usernameEditText.hint = user.username ?: ""
    }



    override fun showUpdateSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showUpdateError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    override fun showUserData(email: String?, phone: String?, username: String?) {
        TODO("Not yet implemented")
    }

    override fun showUserNotFoundError() {
        TODO("Not yet implemented")
    }




    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun saveUserToPreferences(user: Utente) {
        TODO("Not yet implemented")
    }

    override fun clearUserPreferences() {
        TODO("Not yet implemented")
    }

}
