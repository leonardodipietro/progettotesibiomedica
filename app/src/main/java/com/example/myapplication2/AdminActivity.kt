package com.example.myapplication2

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication2.adapter.SpinnerSintomoAdapter
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.repository.ExportRepo
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.auth.FirebaseAuth

class AdminActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var export:ExportRepo
    private lateinit var sintomorepo: SintomoRepo
    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)


        Log.d("MainActivity", "sono nell admin?.")
        auth = FirebaseAuth.getInstance()
        export= ExportRepo()
        sintomorepo=SintomoRepo()
        val sintomiList = mutableListOf<String>()
        val spinnerSintAdapter = SpinnerSintomoAdapter(this, sintomiList)
        val sintomiIdList = mutableListOf<String>()

        val generateExcelButton = findViewById<Button>(R.id.exporttoexcel)
        val logoutButton = findViewById<Button>(R.id.logoutadmin)
        val aggiungiSintButton=findViewById<Button>(R.id.aggiungisintomo)
        val writeSintomo=findViewById<EditText>(R.id.editaggiuntasintomo)
        val removeSintButton=findViewById<Button>(R.id.rimuovisintomo)
        val inviosintomo=findViewById<Button>(R.id.buttonaggiuntanuovosintomo)
        val spinnerRimuoviSint= findViewById<Spinner>(R.id.spinnerrimuovisintomo)
        spinnerRimuoviSint.adapter = spinnerSintAdapter


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

        logoutButton.setOnClickListener {
            // Effettua il logout
            auth.signOut()

            // Torna alla MainActivity
            val intent = Intent(this, MainActivity::class.java)
            //serve per rimuovere la main page dallo stack di memoria
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()  // Chiude l'activity corrente
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
}
