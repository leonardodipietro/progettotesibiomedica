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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.Presenter.AdminPresenter
import com.example.myapplication2.Presenter.SuperAdminPresenter
import com.example.myapplication2.adapter.FaqAdapter
import com.example.myapplication2.interfacepackage.SuperAdminView
import com.example.myapplication2.model.Faq
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.ExportRepo
import com.example.myapplication2.repository.FaqRepo
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.Locale

class SuperAdminActivity: AppCompatActivity(),SuperAdminView {

    private lateinit var presenter: SuperAdminPresenter


    private lateinit var emailEditText: EditText
    private lateinit var usernameEditText: EditText

    private lateinit var passwordEditText: EditText
    private lateinit var confermaPasswordEditText: EditText

    private lateinit var faqRecyclerView: RecyclerView
    private lateinit var faqAdapter: FaqAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLocale()
        setContentView(R.layout.superadminactivity)

        presenter = SuperAdminPresenter(this)

        val user = intent.getParcelableExtra<Utente>("utente")
         // Carica i dati utente dal presenter
        user?.let {
            saveUserToPreferences(user)

            //presenter.scheduleNotifications(it.id)
        }

        emailEditText = findViewById(R.id.emailsuperadmin)
        usernameEditText = findViewById(R.id.usernamesuperadmin)
        //namesurnameEditText = findViewById(R.id.nomeecognomesuperadmin)
        //addressEditText = findViewById(R.id.indirizzosuperadmin)
        passwordEditText = findViewById(R.id.passwordsuperadmin)
        confermaPasswordEditText = findViewById(R.id.confermapasswordsuperadmin)
        val registerButton = findViewById<Button>(R.id.registerbuttonsuperadmin)
        val logoutButton=findViewById<Button>(R.id.logoutsuperadmin)

        // Elementi di input e pulsanti per le FAQ
        val questionEditText = findViewById<EditText>(R.id.questionEditText)
        val answerEditText = findViewById<EditText>(R.id.answerEditText)
        val addFaqButton = findViewById<Button>(R.id.addFaqButton)
        //val updateFaqButton = findViewById<Button>(R.id.updateFaqButton)
        //val deleteFaqButton = findViewById<Button>(R.id.deleteFaqButton)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val name = ""
            val address = ""
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

            questionEditText.text.clear()
            answerEditText.text.clear()
        }

        /*updateFaqButton.setOnClickListener {
            val faqId = "ID_FAQ"
            val question = questionEditText.text.toString()
            val answer = answerEditText.text.toString()
            presenter.updateFaq(faqId, question, answer)
        }

        deleteFaqButton.setOnClickListener {
            val faqId = "ID_FAQ" // Ottieni l'ID della FAQ da eliminare
            presenter.deleteFaq(faqId)
        }*/


        faqRecyclerView = findViewById(R.id.faqRecyclerView)
        faqRecyclerView.layoutManager = LinearLayoutManager(this)

        presenter.loadFaqData()



    }
    private fun loadLocale() {
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("LANGUAGE", "it")
        if (languageCode != null) {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)

            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val sharedPref = newBase.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("LANGUAGE", "it")
        val locale = Locale(languageCode ?: "it")
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
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

    override fun showFaqList(faqList: List<Faq>) {
        faqAdapter = FaqAdapter(faqList, true) { faq ->
            val dialog = AlertDialog.Builder(this)
                .setTitle("Opzioni FAQ")
                .setMessage("Vuoi modificare o eliminare questa FAQ?")
                .setPositiveButton("Modifica") { _, _ ->
                    val question = faq.question
                    val answer = faq.answer
                    faq.id?.let {
                        if (question != null) {
                            if (answer != null) {
                                showUpdateFaqDialog(it, question, answer)
                            }
                        }
                    }
                }
                .setNegativeButton("Elimina") { _, _ ->

                    faq.id?.let { showDeleteConfirmationDialog(it) }
                }
                .setNeutralButton("Annulla", null)
                .create()
            dialog.show()
        }
        faqRecyclerView.adapter = faqAdapter
    }

    private fun showDeleteConfirmationDialog(faqId: String) {
        AlertDialog.Builder(this)
            .setTitle("Conferma eliminazione")
            .setMessage("Sei sicuro di voler eliminare questa FAQ? Questa operazione è irreversibile.")
            .setPositiveButton("Conferma") { _, _ ->
                presenter.deleteFaq(faqId)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }


    private fun showUpdateFaqDialog(faqId: String, currentQuestion: String, currentAnswer: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialogupdatefaq, null)
        val questionEditText = dialogView.findViewById<EditText>(R.id.updateQuestion)
        val answerEditText = dialogView.findViewById<EditText>(R.id.updateAnswer)

        questionEditText.setText(currentQuestion)
        answerEditText.setText(currentAnswer)

        AlertDialog.Builder(this)
            .setTitle("Modifica FAQ")
            .setView(dialogView)
            .setPositiveButton("Salva") { _, _ ->
                val newQuestion = questionEditText.text.toString()
                val newAnswer = answerEditText.text.toString()
                presenter.updateFaq(faqId, newQuestion, newAnswer)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun saveUserToPreferences(user: Utente) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(user)
        editor.putString("utente", json)
        editor.putString("ruolo", "superadmin")
        editor.putBoolean("isLoggedIn", true)
        editor.apply()
    }

    override fun loadUserFromPreferences(): Utente? {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("utente", null)

        // Log per visualizzare il contenuto delle Shared Preferences
        Log.d("SharedPreferences", "Dati trovati  $json")

        return try {
            if (json != null) {
                // Controllo se il JSON è un oggetto valido per la deserializzazione
                if (json.startsWith("{") && json.endsWith("}")) {
                    val utente = Gson().fromJson(json, Utente::class.java)
                    Log.d("Deserializzazione", "Deserializzazione done $utente")
                    utente
                } else {
                    Log.e("Deserializzazione", "Il JSON non valido: $json")
                    null
                }
            } else {
                Log.d("SharedPreferences", "Nessun dato .")
                null
            }
        } catch (e: JsonSyntaxException) {
            // Log dell'errore di deserializzazione
            Log.e("Deserializzazione", "Errore ", e)

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


        passwordEditText.text.clear()
        confermaPasswordEditText.text.clear()
    }


    override fun getContext(): Context {
        return this // Restituisce il Contesto della page
    }


}