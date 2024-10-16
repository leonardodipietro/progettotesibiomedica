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
import org.mindrot.jbcrypt.BCrypt
class ProfileActivity : AppCompatActivity(), ProfileView {
    private lateinit var presenter: ProfilePresenter
    private lateinit var currentUser: Utente
    private lateinit var currentPhoneNumber: String


    //
    // Variabili UI
    private lateinit var logoutButton: Button
    private lateinit var deleteButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var profileLayout: View
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
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
        auth=FirebaseAuth.getInstance()
        setupUI()
        setupListeners()
        setupBottomNavigation()
    }

    private fun setupUI() {
        logoutButton = findViewById(R.id.logoutbutton)
        deleteButton = findViewById(R.id.deleteAccount)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        profileLayout = findViewById(R.id.profilepageroot)
        emailEditText = findViewById(R.id.edit_email)
        phoneEditText = findViewById(R.id.edit_phone)
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
                    phoneEditText.text.toString(),
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
    }

    private fun setupBottomNavigation() {
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
        emailEditText.setText(user.email)
        phoneEditText.setText(user.phoneNumber)
        usernameEditText.setText(user.username)
        nameEditText.setText(user.name)
        addressEditText.setText(user.address)
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


/*
class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepo: UserRepo
    private lateinit var verificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var database: DatabaseReference

    private lateinit var currentUser: Utente
    private lateinit var currentPhoneNumber: String

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

        var isOldPasswordVisible = false
        var isNewPasswordVisible = false
        var isConfirmPasswordVisible = false

        val oldPasswordEditText = findViewById<EditText>(R.id.edit_password_old)
        val newPasswordEditText = findViewById<EditText>(R.id.edit_password_new)
        val confirmPasswordEditText = findViewById<EditText>(R.id.edit_password_confirm)
        val showOldPassword = findViewById<ImageView>(R.id.mostraVecchiaPasswordUser)
        val showNewPassword = findViewById<ImageView>(R.id.mostraNuovaPasswordUser)
        val showConfirmPassword = findViewById<ImageView>(R.id.mostraConfermaPasswordUser)

        val saveButton = findViewById<Button>(R.id.button_save)

        val usernameEditText = findViewById<EditText>(R.id.edit_username)
        val nameEditText = findViewById<EditText>(R.id.edit_name)
        val addressEditText = findViewById<EditText>(R.id.edit_address)

        // Recupera l'UID dell'utente
       // val currentUser = auth.currentUser
        Log.d("ProfileActivity", "Utente corrente trovato: ${auth.currentUser}")

        utente!!.id?.let {
            userRepo.getUserData(it) { utente ->
                Log.d("ProfileActivity", "Dati utente recuperati dal database: ${utente.toString()}")
                if (utente != null) {
                    emailEditText.setText(utente.email ?: "")
                    phoneEditText.setText(utente.phoneNumber ?: "")
                    usernameEditText.setText(utente.username ?: "")
                    nameEditText.setText(utente.name ?: "")
                    addressEditText.setText(utente.address ?: "")
                } else {
                    // Gestisci il caso in cui i dati non siano presenti nel database
                   // Log.d("ProfileActivity", "Nessun dato utente trovato per UID=${user.uid}")
                    Toast.makeText(this, "Nessun dato utente trovato", Toast.LENGTH_SHORT).show()
                }
            }
        }
        showOldPassword.setOnClickListener {
            if (isOldPasswordVisible) {
                oldPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                showOldPassword.setImageResource(R.drawable.passwordicon) // Cambia l'icona in "occhio chiuso"
            } else {
                oldPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                showOldPassword.setImageResource(R.drawable.passwordicon) // Cambia l'icona in "occhio aperto"
            }
            oldPasswordEditText.setSelection(oldPasswordEditText.text.length)
            isOldPasswordVisible = !isOldPasswordVisible
        }

        showNewPassword.setOnClickListener {
            if (isNewPasswordVisible) {
                newPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                showNewPassword.setImageResource(R.drawable.passwordicon) // Cambia l'icona in "occhio chiuso"
            } else {
                newPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                showNewPassword.setImageResource(R.drawable.passwordicon) // Cambia l'icona in "occhio aperto"
            }
            newPasswordEditText.setSelection(newPasswordEditText.text.length)
            isNewPasswordVisible = !isNewPasswordVisible
        }
        showConfirmPassword.setOnClickListener {
            if (isConfirmPasswordVisible) {
                confirmPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                showConfirmPassword.setImageResource(R.drawable.passwordicon) // Cambia l'icona in "occhio chiuso"
            } else {
                confirmPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                showConfirmPassword.setImageResource(R.drawable.passwordicon) // Cambia l'icona in "occhio aperto"
            }
            confirmPasswordEditText.setSelection(confirmPasswordEditText.text.length)
            isConfirmPasswordVisible = !isConfirmPasswordVisible
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

            // Aggiorna Email
            if (newEmail.isNotEmpty()) {
                utente.id?.let { it1 ->
                    userRepo.updateUserEmail(it1, newEmail) { success ->
                        if (success) {
                            Toast.makeText(this, "Email aggiornata con successo", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Errore nell'aggiornamento dell'email", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // Aggiorna Numero di Telefono
            if (newPhone.isNotEmpty()) {
                utente.id?.let { it1 ->
                    userRepo.updatePhoneNumber(it1, newPhone) { success ->
                        if (success) {
                            Toast.makeText(this, "Numero di telefono aggiornato con successo", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Errore nell'aggiornamento del numero di telefono", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // Aggiorna Username
            if (newUsername.isNotEmpty()) {
                utente.id?.let { it1 ->
                    userRepo.updateUsername(it1, newUsername) { success ->
                        if (success) {
                            Toast.makeText(this, "Username aggiornato con successo", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Errore nell'aggiornamento dell'username", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // Aggiorna Nome
            if (newName.isNotEmpty()) {
                utente.id?.let { it1 ->
                    userRepo.updateName(it1, newName) { success ->
                        if (success) {
                            Toast.makeText(this, "Nome aggiornato con successo", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Errore nell'aggiornamento del nome", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // Aggiorna Indirizzo
            if (newAddress.isNotEmpty()) {
                utente.id?.let { it1 ->
                    userRepo.updateAddress(it1, newAddress) { success ->
                        if (success) {
                            Toast.makeText(this, "Indirizzo aggiornato con successo", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Errore nell'aggiornamento dell'indirizzo", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            if (newPassword.isNotEmpty()) {
                if (newPassword == confirmPassword) {
                    // Aggiungi qui i parametri oldPassword e newPassword
                    utente.id?.let { it1 ->
                        userRepo.changePassword(userId = it1, oldPassword = oldPassword, newPassword = newPassword) { success ->
                            if (success) {
                                Toast.makeText(this, "Password aggiornata con successo", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Errore nel cambio password. Controlla le credenziali.", Toast.LENGTH_SHORT).show()
                            }
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
            showDeleteDialog(utente)
        }


            bottomNavigationView.selectedItemId = R.id.nav_profile // Set the default selection

            bottomNavigationView.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        Log.d("BottomNavigation", "Navigating to Home") // Log per il passaggio a MainPage

                        // Controlla se l'oggetto utente è nullo
                        if (utente == null) {
                            Log.e("BottomNavigation", "Utente è nullo, impossibile navigare")
                        } else {
                            Log.d("BottomNavigation", "Utente presente: ${utente.username}") // Log dell'utente corrente
                        }

                        // Passa alla MainPage
                        val intent = Intent(this, MainPage::class.java).apply {
                            putExtra("utente", utente) // Passa l'oggetto Utente
                        }
                        startActivity(intent)
                        true
                    }
                    R.id.nav_profile -> {
                        Log.d("BottomNavigation", "Already on Profile") // Log per quando sei già su questa Activity
                        true
                    }
                    else -> {
                        Log.d("BottomNavigation", "Unknown item selected: ${item.itemId}") // Log per gli altri elementi
                        false
                    }
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
    private fun showDeleteDialog(utente: Utente) {
        AlertDialog.Builder(this)
            .setTitle("Eliminare l'account?")
            .setMessage("Sei sicuro di voler eliminare il tuo account? Questa operazione non può essere annullata.")
            .setPositiveButton("Sì") { dialog, _ ->
                deleteAccount(utente)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
    private fun deleteAccount(utente:Utente) {
        val userId = utente?.id ?: return
        userRepo.getUserData(userId) { user ->
            val email = user?.email
            val phoneNumber = user?.phoneNumber
            val hashedPassword = user?.password

            if (!email.isNullOrEmpty()) {
                if (!email.isNullOrEmpty() && !hashedPassword.isNullOrEmpty()) {
                    showPasswordDialog(email, hashedPassword, userId)
                }
            } else if (!phoneNumber.isNullOrEmpty()) {
                // Se l'utente ha un numero di telefono, procedi con la verifica del numero
                //showPhoneVerificationDialog(phoneNumber,utente)
                startDeleteProcess(phoneNumber,utente)
            }
        }
    }
    private fun showPasswordDialog(email: String, hashedPassword: String, userId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Conferma Password")
        builder.setMessage("Inserisci la tua password per confermare l'eliminazione dell'account")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Conferma") { dialog, _ ->
            val inputPassword = input.text.toString()

            // Verifica la password con BCrypt
            if (BCrypt.checkpw(inputPassword, hashedPassword)) {
                // Effettua il login con email e password
                auth.signInWithEmailAndPassword(email, inputPassword).addOnCompleteListener { loginTask ->
                    if (loginTask.isSuccessful) {
                        auth.currentUser?.delete()?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                userRepo.deleteAccount(userId) { success ->
                                    handleDeleteAccountResult(success)
                                }
                            } else {
                                Log.d("ProfileActivity", "Errore nell'eliminazione da Firebase Authentication")
                            }
                        }
                    } else {
                        Log.d("ProfileActivity", "Errore nel login per l'eliminazione con email e password: ${loginTask.exception?.message}")
                        Toast.makeText(this, "Errore nel login. Controlla le credenziali.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.d("ProfileActivity", "Password non valida")
                Toast.makeText(this, "Password non valida", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Annulla") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }


    private fun startDeleteProcess(phoneNumber: String, utente: Utente) {
        currentUser = utente
        currentPhoneNumber = phoneNumber
        showPhoneVerificationDialog(phoneNumber, utente)
    }

    private fun showPhoneVerificationDialog(phoneNumber: String, utente: Utente) {
        startPhoneNumberVerification(phoneNumber)

        val builder = AlertDialog.Builder(this)
        val input = EditText(this)
        input.hint = "Inserisci il codice di verifica"
        builder.setView(input)

        builder.setPositiveButton("Verifica") { _, _ ->
            val verificationCode = input.text.toString()
            val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)
            signInWithPhoneAuthCredential(credential, utente)
        }
        builder.setNegativeButton("Annulla") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(verificationCallbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, utente: Utente) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            utente.id?.let {
                                userRepo.deleteAccount(it) { result ->
                                    handleDeleteAccountResult(result)
                                }
                            }
                        } else {
                            Log.d("ProfileActivity", "Errore nell'eliminazione da Firebase Authentication")
                        }
                    }
                } else {
                    Log.e("ProfileActivity", "Errore di autenticazione: ${task.exception?.message}")
                    Toast.makeText(this, "Autenticazione fallita", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private val verificationCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential, currentUser)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e("ProfileActivity", "Errore di verifica telefonica: ${e.message}")
            Toast.makeText(this@ProfileActivity, "Verifica fallita", Toast.LENGTH_SHORT).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            this@ProfileActivity.verificationId = verificationId
            this@ProfileActivity.resendToken = token
            showPhoneVerificationDialog(currentPhoneNumber, currentUser)
        }
    }
    private fun handleDeleteAccountResult(success: Boolean) {
        if (success) {
            stopNotification()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Log.d("ProfileActivity", "Errore nell'eliminazione dei dati dal database")
        }
    }


    private fun stopNotification() {
        Log.d("MainPage", "fermata la notifica")

        WorkManager.getInstance(this).cancelUniqueWork("NotificaWorker")
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
*/
