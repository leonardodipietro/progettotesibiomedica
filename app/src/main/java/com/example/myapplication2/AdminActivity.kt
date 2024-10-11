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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication2.adapter.SpinnerSintomoAdapter
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.ExportRepo
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson

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
        //auth = FirebaseAuth.getInstance()ù
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
