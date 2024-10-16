package com.example.myapplication2

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication2.Presenter.AdminPresenter
import com.example.myapplication2.adapter.SpinnerSintomoAdapter
import com.example.myapplication2.interfacepackage.AdminView
import com.example.myapplication2.interfacepackage.PasswordType
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.ExportRepo
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class AdminActivity : AppCompatActivity(), AdminView {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001 // Aggiungi qui il PERMISSION_REQUEST_CODE
    }

    private lateinit var presenter: AdminPresenter
    private lateinit var auth: FirebaseAuth

    // Viste UI
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var oldPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var showOldPassword: ImageView
    private lateinit var showNewPassword: ImageView
    private lateinit var showConfirmPassword: ImageView
    private lateinit var modifyButton: Button
    private lateinit var logoutButton: Button
    private lateinit var generateExcelButton: Button
    private lateinit var aggiungiSintButton: Button
    private lateinit var removeSintButton: Button
    private lateinit var writeSintomo: EditText
    private lateinit var inviosintomo: Button
    private lateinit var spinnerRimuoviSint: Spinner

    // Adapter per lo spinner
    private lateinit var spinnerSintAdapter: SpinnerSintomoAdapter
    private val sintomiList = mutableListOf<String>()
    private val sintomiIdList = mutableListOf<String>()

    // Stato visibilità password
    private var isOldPasswordVisible = false
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        //clearUserPreferences()

        presenter = AdminPresenter(this, UserRepo(), SintomoRepo(), ExportRepo(), this)

        //var utente = intent.getParcelableExtra<Utente>("utente")
       /* saveUserToPreferences(utente!!)
        Log.d("Lifecycle", "onCreate avviato in AdminActivity $utente")
        if (utente == null) {
            utente = loadUserFromPreferences()
        } else {
            saveUserToPreferences(utente)
        }*/

        auth=FirebaseAuth.getInstance()
        val user = intent.getParcelableExtra<Utente>("utente")
        presenter.loadUserData(user) // Carica i dati utente dal presenter
        user?.let {
            saveUserToPreferences(user)

        //presenter.scheduleNotifications(it.id)
        }


        /*utente?.id?.let {
            presenter.loadUserData(it)
        }*/
        //presenter = AdminPresenter(this, UserRepo(), SintomoRepo(), ExportRepo())

        // Inizializzazione viste
        emailEditText = findViewById(R.id.editemailadmin)
        phoneEditText = findViewById(R.id.editphoneadmin)
        usernameEditText = findViewById(R.id.editusernameadmin)
        oldPasswordEditText = findViewById(R.id.editpswadminold)
        newPasswordEditText = findViewById(R.id.editpswadmindnew)
        confirmPasswordEditText = findViewById(R.id.editpswadminconferm)
        showOldPassword = findViewById(R.id.mostraVecchiaPassword)
        showNewPassword = findViewById(R.id.mostraNuovaPassword)
        showConfirmPassword = findViewById(R.id.mostraConfermaPassword)
        modifyButton = findViewById(R.id.buttonmodifyadmin)
        logoutButton = findViewById(R.id.logoutadmin)
        generateExcelButton = findViewById(R.id.exporttoexcel)
        aggiungiSintButton = findViewById(R.id.aggiungisintomo)
        writeSintomo = findViewById(R.id.editaggiuntasintomo)
        removeSintButton = findViewById(R.id.rimuovisintomo)
        inviosintomo = findViewById(R.id.buttonaggiuntanuovosintomo)
        spinnerRimuoviSint = findViewById(R.id.spinnerrimuovisintomo)

        // Configura adapter e spinner
        spinnerSintAdapter = SpinnerSintomoAdapter(this, sintomiList)
        spinnerRimuoviSint.adapter = spinnerSintAdapter

        // Event listeners
        aggiungiSintButton.setOnClickListener {
            if (writeSintomo.visibility == View.GONE) {
                writeSintomo.visibility = View.VISIBLE
                inviosintomo.visibility = View.VISIBLE
            } else {
                writeSintomo.visibility = View.GONE
                inviosintomo.visibility = View.GONE
            }
        }

        inviosintomo.setOnClickListener {
            val nomeSintomo = writeSintomo.text.toString().trim()
            presenter.addSintomo(nomeSintomo)
        }

        removeSintButton.setOnClickListener {
            if (spinnerRimuoviSint.visibility == View.VISIBLE) {
                spinnerRimuoviSint.visibility = View.GONE  // Nascondi lo spinner se è già visibile
            } else {
                spinnerRimuoviSint.visibility = View.VISIBLE  // Mostra lo spinner se è nascosto
                presenter.loadSintomi()  // Carica i sintomi tramite il presenter solo quando diventa visibile
            }
        }

// Gestisce la selezione di un elemento nello Spinner
        // Gestisce la selezione di un elemento nello Spinner
        spinnerRimuoviSint.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position >= 0 && position < sintomiIdList.size) {
                    val idSintomo = sintomiIdList[position]
                    AlertDialog.Builder(this@AdminActivity)
                        .setTitle("Conferma Rimozione")
                        .setMessage("Vuoi rimuovere il sintomo selezionato?")
                        .setPositiveButton("Sì") { _, _ ->
                            presenter.removeSintomo(idSintomo)  // Rimuovi il sintomo tramite il presenter
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    Log.e("sintrepo", "Indice fuori dai limiti: $position per la lista degli ID con lunghezza ${sintomiIdList.size}")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Nessuna azione necessaria
            }
        }




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
            val email = emailEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val username = usernameEditText.text.toString()
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()
            user?.id?.let { it1 ->
                presenter.saveUserData(
                    it1,
                    email,
                    phone,
                    username,
                    oldPassword,
                    newPassword,
                    confirmPassword)
            }

                }

        logoutButton.setOnClickListener {
            presenter.logout()
        }

        generateExcelButton.setOnClickListener {
            presenter.exportToExcel(this)
        }
    }

    override fun showUserData(email: String?, phone: String?, username: String?) {
        emailEditText.setText(email ?: "")
        phoneEditText.setText(phone ?: "")
        usernameEditText.setText(username ?: "")
    }

    override fun showUserNotFoundError() {
        Toast.makeText(this, "Nessun dato utente trovato", Toast.LENGTH_SHORT).show()
    }

    override fun showAddSintomoSuccess() {
        Toast.makeText(this, "Sintomo aggiunto con successo", Toast.LENGTH_SHORT).show()
        writeSintomo.visibility = View.GONE
        inviosintomo.visibility = View.GONE
    }

    override fun showAddSintomoError() {
        Toast.makeText(this, "Sintomo già esistente o errore", Toast.LENGTH_SHORT).show()
    }
    override fun saveUserToPreferences(user: Utente) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(user)
        editor.putString("utente", json)
        editor.putString("ruolo", "admin")
        editor.putBoolean("isLoggedIn", true)
        editor.apply()
    }

    override fun loadUserFromPreferences(): Utente? {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("utente", null)
        Log.d("Lifecycle", "onCreate avviato in AdminActivity chiamata load")
        // Log per visualizzare il contenuto delle Shared Preferences
        Log.d("SharedPreferences", "Dati trovati nelle Shared Preferences: $json")

        return try {
            if (json != null) {
                // Controllo se il JSON è un oggetto valido per la deserializzazione
                if (json.startsWith("{") && json.endsWith("}")) {
                    val utente = Gson().fromJson(json, Utente::class.java)
                    Log.d("Deserializzazione", "Deserializzazione completata con successo: $utente")
                    utente
                } else {
                    Log.e("Deserializzazione", "Il JSON non è un oggetto valido: $json")
                    null
                }
            } else {
                Log.d("SharedPreferences", "Nessun dato utente salvato.")
                null
            }
        } catch (e: JsonSyntaxException) {
            // Log dell'errore di deserializzazione
            Log.e("Deserializzazione", "Errore nella deserializzazione dei dati utente", e)

            // Cancella i dati corrotti dalle Shared Preferences
            //clearUserPreferences()
            null
        }
    }

    /*override fun saveUserToPreferences(user: Any?) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(user)
        editor.putString("utente", json)
        editor.apply()
    }*/

    override fun clearUserPreferences() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    override fun showRemoveSintomoSuccess() {
        Toast.makeText(this, "Sintomo rimosso con successo", Toast.LENGTH_SHORT).show()
    }

    override fun showRemoveSintomoError() {
        Toast.makeText(this, "Errore nella rimozione del sintomo", Toast.LENGTH_SHORT).show()
    }


    override fun showSintomiList(nomiSintomi: List<String>, idSintomi: List<String>) {
        sintomiList.clear()
        sintomiList.addAll(nomiSintomi)

        sintomiIdList.clear()
        sintomiIdList.addAll(idSintomi)

        spinnerSintAdapter.notifyDataSetChanged()
    }


    override fun showUpdateSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showUpdateError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun togglePasswordVisibility(passwordType: PasswordType, isVisible: Boolean) {
        val editText = when (passwordType) {
            PasswordType.OLD_PASSWORD -> oldPasswordEditText
            PasswordType.NEW_PASSWORD -> newPasswordEditText
            PasswordType.CONFIRM_PASSWORD -> confirmPasswordEditText
        }
        val imageView = when (passwordType) {
            PasswordType.OLD_PASSWORD -> showOldPassword
            PasswordType.NEW_PASSWORD -> showNewPassword
            PasswordType.CONFIRM_PASSWORD -> showConfirmPassword
        }
        editText.inputType = if (isVisible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        editText.setSelection(editText.text.length)
        imageView.setImageResource(R.drawable.passwordicon)
        when (passwordType) {
            PasswordType.OLD_PASSWORD -> isOldPasswordVisible = !isVisible
            PasswordType.NEW_PASSWORD -> isNewPasswordVisible = !isVisible
            PasswordType.CONFIRM_PASSWORD -> isConfirmPasswordVisible = !isVisible
        }
    }

    override fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Uscire dall'Account?")
            .setMessage("Sei sicuro di voler uscire dall'account?")
            .setPositiveButton("Sì") { _, _ ->
                presenter.confirmLogout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun returnToMain() {
        clearUserPreferences() // Cancella le Shared Preferences
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    override fun requestWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        } else {
            presenter.onRequestPermissionsResult(true,this)
        }
    }
    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    override fun showPermissionDeniedError() {
        Log.e("Permission", "Permesso di scrittura negato")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            presenter.onRequestPermissionsResult(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED,this)
        }
    }
    override fun hasWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    override fun showExportSuccessMessage() {
        Toast.makeText(this, "Esportazione completata con successo", Toast.LENGTH_SHORT).show()
    }

    override fun showExportErrorMessage() {
        Toast.makeText(this, "Errore durante l'esportazione", Toast.LENGTH_SHORT).show()
    }
   /* override fun showUserWelcomeMessage(username: String) {
        findViewById<TextView>(R.id.titolo).text = "Benvenuto $username, come ti senti oggi?"
    }*/






}


/*
class AdminActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var export:ExportRepo
    private lateinit var sintomorepo: SintomoRepo
    private lateinit var userrepo:UserRepo
    private val gson = Gson()
    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)


        Log.d("MainActivity", "sono nell admin?.")

        var utente = intent.getParcelableExtra<Utente>("utente")
        if (utente == null) {
            utente = loadUserFromPreferences() // Prova a caricare dalle Shared Preferences
        } else {
            saveUserToPreferences(utente) // Salva l'utente nelle Shared Preferences se presente nell'intent
        }

        export= ExportRepo()
        sintomorepo=SintomoRepo()
        userrepo=UserRepo()
        val sintomiList = mutableListOf<String>()
        val spinnerSintAdapter = SpinnerSintomoAdapter(this, sintomiList)
        val sintomiIdList = mutableListOf<String>()

        val generateExcelButton = findViewById<Button>(R.id.exporttoexcel)
        val logoutButton = findViewById<Button>(R.id.logoutadmin)
        val aggiungiSintButton=findViewById<Button>(R.id.aggiungisintomo)
        val writeSintomo=findViewById<EditText>(R.id.editaggiuntasintomo)
        val removeSintButton=findViewById<Button>(R.id.rimuovisintomo)
        val inviosintomo=findViewById<Button>(R.id.buttonaggiuntanuovosintomo)
        var isOldPasswordVisible = false
        var isNewPasswordVisible = false
        var isConfirmPasswordVisible = false
        val spinnerRimuoviSint= findViewById<Spinner>(R.id.spinnerrimuovisintomo)
        spinnerRimuoviSint.adapter = spinnerSintAdapter


        val emailEditText = findViewById<EditText>(R.id.editemailadmin)
        val phoneEditText = findViewById<EditText>(R.id.editphoneadmin)
        val usernameEditText=findViewById<EditText>(R.id.editusernameadmin)
        val oldPasswordEditText = findViewById<EditText>(R.id.editpswadminold)
        val newPasswordEditText = findViewById<EditText>(R.id.editpswadmindnew)
        val confirmPasswordEditText = findViewById<EditText>(R.id.editpswadminconferm)
        val showOldPassword = findViewById<ImageView>(R.id.mostraVecchiaPassword)
        val showNewPassword = findViewById<ImageView>(R.id.mostraNuovaPassword)
        val showConfirmPassword = findViewById<ImageView>(R.id.mostraConfermaPassword)
        val modifyButton=findViewById<Button>(R.id.buttonmodifyadmin)
        utente!!.id?.let {
            userrepo.getUserData(it) { utente ->
                Log.d("ProfileActivity", "Dati utente recuperati dal database: ${utente.toString()}")
                if (utente != null) {
                    emailEditText.setText(utente.email ?: "")
                    phoneEditText.setText(utente.phoneNumber ?: "")
                    usernameEditText.setText(utente.username ?: "")
                    //nameEditText.setText(utente.name ?: "")
                    //addressEditText.setText(utente.address ?: "")
                } else {
                    // Gestisci il caso in cui i dati non siano presenti nel database
                    // Log.d("ProfileActivity", "Nessun dato utente trovato per UID=${user.uid}")
                    Toast.makeText(this, "Nessun dato utente trovato", Toast.LENGTH_SHORT).show()
                }
            }
        }



        writeSintomo.visibility = View.GONE
        inviosintomo.visibility = View.GONE



        aggiungiSintButton.setOnClickListener {
            if (writeSintomo.visibility == View.GONE) {
                // Rendi visibili EditText e bottone accanto
                writeSintomo.visibility = View.VISIBLE
                inviosintomo.visibility = View.VISIBLE
            } else {
                // Nascondi EditText e bottone se sono già visibili
                writeSintomo.visibility = View.GONE
                inviosintomo.visibility = View.GONE
            }
        }
        inviosintomo.setOnClickListener {
            val nomeSintomo = writeSintomo.text.toString().trim()
            if (nomeSintomo.isNotEmpty()) {
                sintomorepo.aggiungiSintomo(nomeSintomo) { success ->
                    if (success) {
                        Toast.makeText(this, "Sintomo aggiunto con successo", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Sintomo già esistente o errore", Toast.LENGTH_SHORT).show()
                    }
                    // Nascondi di nuovo EditText e bottone accanto
                    writeSintomo.visibility = View.GONE
                    inviosintomo.visibility = View.GONE
                }
            } else {
                Toast.makeText(this, "Inserisci un sintomo", Toast.LENGTH_SHORT).show()
            }
        }

        sintomorepo.caricaSintomi(sintomiList, sintomiIdList) {
            // Aggiorna l'adapter dopo il caricamento dei dati
            spinnerSintAdapter.notifyDataSetChanged()
        }
        removeSintButton.setOnClickListener {
            spinnerRimuoviSint.visibility = View.VISIBLE
            sintomorepo.caricaSintomi(sintomiList, sintomiIdList) {
                // Aggiorna l'adapter dopo aver caricato i dati
                spinnerSintAdapter.notifyDataSetChanged()
            }
        }

        // Gestisce la selezione di un elemento nello Spinner
        spinnerRimuoviSint.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                // Controlla che l'indice sia valido prima di accedere all'elemento
                if (position < sintomiIdList.size) {
                    val idSintomo = sintomiIdList[position]

                    // Finestra di dialogo di conferma
                    AlertDialog.Builder(this@AdminActivity)
                        .setTitle("Conferma Rimozione")
                        .setMessage("Vuoi rimuovere il sintomo selezionato?")
                        .setPositiveButton("Sì") { _, _ ->
                            sintomorepo.rimuoviSintomo(idSintomo) { success ->
                                if (success) {
                                    Toast.makeText(this@AdminActivity, "Sintomo rimosso con successo", Toast.LENGTH_SHORT).show()
                                    // Ricarica i dati per aggiornare lo Spinner
                                    sintomorepo.caricaSintomi(sintomiList, sintomiIdList) {
                                        spinnerSintAdapter.notifyDataSetChanged()
                                    }
                                } else {
                                    Toast.makeText(this@AdminActivity, "Errore nella rimozione del sintomo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    Log.e("sintrepo", "Indice fuori dai limiti: $position per la lista degli ID con lunghezza ${sintomiIdList.size}")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Nessuna azione necessaria
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


        modifyButton.setOnClickListener {
                val newEmail = emailEditText.text.toString()
                val newPhone = phoneEditText.text.toString()
                val newUsername = usernameEditText.text.toString()
                //val newName = nameEditText.text.toString()
                //val newAddress = addressEditText.text.toString()
                val oldPassword = oldPasswordEditText.text.toString()
                val newPassword = newPasswordEditText.text.toString()
                val confirmPassword = confirmPasswordEditText.text.toString()

                // Aggiorna Email
                if (newEmail.isNotEmpty()) {
                    utente.id?.let { it1 ->
                        userrepo.updateUserEmail(it1, newEmail) { success ->
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
                        userrepo.updatePhoneNumber(it1, newPhone) { success ->
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
                        userrepo.updateUsername(it1, newUsername) { success ->
                            if (success) {
                                Toast.makeText(this, "Username aggiornato con successo", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Errore nell'aggiornamento dell'username", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // Aggiorna Nome
                /*if (newName.isNotEmpty()) {
                    utente.id?.let { it1 ->
                        userRepo.updateName(it1, newName) { success ->
                            if (success) {
                                Toast.makeText(this, "Nome aggiornato con successo", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Errore nell'aggiornamento del nome", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }*/

                // Aggiorna Indirizzo
                /*if (newAddress.isNotEmpty()) {
                    utente.id?.let { it1 ->
                        userRepo.updateAddress(it1, newAddress) { success ->
                            if (success) {
                                Toast.makeText(this, "Indirizzo aggiornato con successo", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Errore nell'aggiornamento dell'indirizzo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }*/
                if (newPassword.isNotEmpty()) {
                    if (newPassword == confirmPassword) {
                        // Aggiungi qui i parametri oldPassword e newPassword
                        utente.id?.let { it1 ->
                            userrepo.changePassword(userId = it1, oldPassword = oldPassword, newPassword = newPassword) { success ->
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
        logoutButton.setOnClickListener {
            // Effettua il logout
            //auth.signOut()

            // Torna alla MainActivity
            /*val intent = Intent(this, MainActivity::class.java)
            //serve per rimuovere la main page dallo stack di memoria
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()  // Chiude l'activity corrente*/
            showLogoutDialog()
        }

        generateExcelButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    val permissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permissions, PERMISSION_REQUEST_CODE)
                } else {
                    // Chiamare qui fetchDataAndGenerateExcel() se i permessi sono già concessi
                    export.fetchDataAndGenerateExcel(this)
                }
            } else {
                // Chiamare qui fetchDataAndGenerateExcel() se la versione di Android è minore di M
                export.fetchDataAndGenerateExcel(this)
            }
        }
    }


    // Funzione per salvare l'utente nelle Shared Preferences
    private fun saveUserToPreferences(user: Utente) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = gson.toJson(user)
        editor.putString("utente", json)
        editor.apply()
    }

    // Funzione per caricare l'utente dalle Shared Preferences
    private fun loadUserFromPreferences(): Utente? {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("utente", null)
        return if (json != null) {
            gson.fromJson(json, Utente::class.java)
        } else {
            null
        }
    }
    // Gestisci il risultato della richiesta di permesso
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permesso concesso, possiamo procedere
                export.fetchDataAndGenerateExcel(this)
            } else {
                // Permesso negato, gestire il caso qui
                Log.e("Permission", "Permesso di scrittura negato")
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Uscire dall'Account?")
            .setMessage("Sei sicuro di voler uscire dall'account?")
            .setPositiveButton("Sì") { dialog, which ->


                // Annulla la notifica
                //stopNotification()

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
}
*/