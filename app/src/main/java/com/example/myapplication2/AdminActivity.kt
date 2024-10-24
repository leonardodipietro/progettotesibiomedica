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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.Presenter.AdminPresenter
import com.example.myapplication2.adapter.AdapterStats
import com.example.myapplication2.adapter.SpinnerSintomoAdapter
import com.example.myapplication2.interfacepackage.AdminView
import com.example.myapplication2.interfacepackage.PasswordType
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.ExportRepo
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.example.myapplication2.utility.UserExperience
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.Locale

class AdminActivity : AppCompatActivity(), AdminView {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001 // Aggiungi qui il PERMISSION_REQUEST_CODE
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var sintomoAdapter: AdapterStats

    private lateinit var presenter: AdminPresenter
    private lateinit var auth: FirebaseAuth
    private lateinit var userExperience: UserExperience


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
    private var isUserInteractingWithSpinner = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLocale()
        setContentView(R.layout.activity_admin)
        //clearUserPreferences()

        presenter = AdminPresenter(this, UserRepo(), SintomoRepo(), ExportRepo(), this)
        userExperience=UserExperience()


        auth=FirebaseAuth.getInstance()
        val user = intent.getParcelableExtra<Utente>("utente")
        user!!.id?.let { presenter.loadUserData(it) } // Carica i dati utente dal presenter
        user?.let {
            saveUserToPreferences(user)

        }
        presenter.fetchSintomiUltimaSettimana()


        recyclerView = findViewById(R.id.recycler_view_statistiche)
        recyclerView.layoutManager = LinearLayoutManager(this)

        generateExcelButton = findViewById(R.id.exporttoexcel)
        aggiungiSintButton = findViewById(R.id.aggiungisintomo)
        writeSintomo = findViewById(R.id.editaggiuntasintomo)
        removeSintButton = findViewById(R.id.rimuovisintomo)
        inviosintomo = findViewById(R.id.buttonaggiuntanuovosintomo)
        spinnerRimuoviSint = findViewById(R.id.spinnerrimuovisintomo)

        // Configura adapter e spinner
        spinnerSintAdapter = SpinnerSintomoAdapter(this, sintomiList)
        spinnerRimuoviSint.adapter = spinnerSintAdapter


        val sintomiList = listOf(
            Sintomo(
                id = "1",
                nomeSintomo = "depressione",
                gravità = 3,
                tempoTrascorsoUltimoPasto = 4,
                dataSegnalazione = "2024-10-19",
                oraSegnalazione = "22:02"
            ),

        )






        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_admin)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {  // Naviga verso AdminActivity (la home admin)
                    val user = loadUserFromPreferences()
                    val intent = Intent(this, ProfileActivity::class.java).apply {
                        putExtra("utente", user)
                    }
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }



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

                spinnerRimuoviSint.visibility = View.GONE
                findViewById<ImageView>(R.id.freccettaadmin).visibility = View.GONE
            } else {
                // Mostra lo spinner e la freccetta
                spinnerRimuoviSint.visibility = View.VISIBLE
                findViewById<ImageView>(R.id.freccettaadmin).visibility = View.VISIBLE
                presenter.loadSintomi()  // Carica i sintomi tramite il presenter solo quando diventa visibile
            }
        }
// Gestisce la selezione di un elemento nello Spinner
        spinnerRimuoviSint.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Ignora la selezione automatica al momento in cui lo spinner diventa visibile
                if (isUserInteractingWithSpinner) {
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
                    }
                }
                isUserInteractingWithSpinner = false // Reset flag dopo la selezione
            }
            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        spinnerRimuoviSint.setOnTouchListener { _, _ ->
            isUserInteractingWithSpinner = true
            false
        }




        generateExcelButton.setOnClickListener {
            presenter.exportToExcel(this)
        }
    }

    override fun showUserData(email: String?, phone: String?, username: String?) {
        TODO("Not yet implemented")
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
    override fun attachBaseContext(newBase: Context) {
        val sharedPref = newBase.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("LANGUAGE", "it")
        val locale = Locale(languageCode ?: "it")
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
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




    override fun showSintomiListUser(sintomi: List<Pair<Sintomo, String>>) {
        sintomoAdapter = AdapterStats(sintomi)
        recyclerView.adapter = sintomoAdapter
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

