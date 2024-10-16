package com.example.myapplication2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication2.Presenter.AdminPresenter
import com.example.myapplication2.Presenter.SuperAdminPresenter
import com.example.myapplication2.interfacepackage.SuperAdminView
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.ExportRepo
import com.example.myapplication2.repository.FaqRepo
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class SuperAdminActivity: AppCompatActivity(),SuperAdminView {

    private lateinit var presenter: SuperAdminPresenter


    private lateinit var emailEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var namesurnameEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confermaPasswordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.superadminactivity)

        presenter = SuperAdminPresenter(this)

        emailEditText = findViewById(R.id.emailsuperadmin)
        usernameEditText = findViewById(R.id.usernamesuperadmin)
        namesurnameEditText = findViewById(R.id.nomeecognomesuperadmin)
        addressEditText = findViewById(R.id.indirizzosuperadmin)
        passwordEditText = findViewById(R.id.passwordsuperadmin)
        confermaPasswordEditText = findViewById(R.id.confermapasswordsuperadmin)
        val registerButton = findViewById<Button>(R.id.registerbuttonsuperadmin)
        val logoutButton=findViewById<Button>(R.id.logoutsuperadmin)

        // Elementi di input e pulsanti per le FAQ
        val questionEditText = findViewById<EditText>(R.id.questionEditText)
        val answerEditText = findViewById<EditText>(R.id.answerEditText)
        val addFaqButton = findViewById<Button>(R.id.addFaqButton)
        val updateFaqButton = findViewById<Button>(R.id.updateFaqButton)
        val deleteFaqButton = findViewById<Button>(R.id.deleteFaqButton)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val name = namesurnameEditText.text.toString()
            val address = addressEditText.text.toString()
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confermaPassword = confermaPasswordEditText.text.toString()

            presenter.registerAdmin(email, username, name, address, password, confermaPassword)
        }

        logoutButton.setOnClickListener {
            presenter.logout()
        }

        // Eventi di click per i pulsanti di gestione delle FAQ
        addFaqButton.setOnClickListener {
            val question = questionEditText.text.toString()
            val answer = answerEditText.text.toString()
            presenter.addFaq(question, answer)
        }

        updateFaqButton.setOnClickListener {
            val faqId = "ID_FAQ"
            val question = questionEditText.text.toString()
            val answer = answerEditText.text.toString()
            presenter.updateFaq(faqId, question, answer)
        }

        deleteFaqButton.setOnClickListener {
            val faqId = "ID_FAQ" // Ottieni l'ID della FAQ da eliminare
            presenter.deleteFaq(faqId)
        }

    }



    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    override fun clearUserPreferences() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    override fun clearInputFields() {
        emailEditText.text.clear()
        usernameEditText.text.clear()
        namesurnameEditText.text.clear()
        addressEditText.text.clear()
        passwordEditText.text.clear()
        confermaPasswordEditText.text.clear()
    }


}