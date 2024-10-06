package com.example.myapplication2

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication2.repository.ExportRepo
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.auth.FirebaseAuth

class AdminActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var export:ExportRepo
    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)


        Log.d("MainActivity", "sono nell admin?.")

    //todo rivedere la logica di accesso alla pagina di default all apertura dell'app


        val logoutButton = findViewById<Button>(R.id.logoutadmin)
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

        auth = FirebaseAuth.getInstance()
        export= ExportRepo()


        val generateExcelButton = findViewById<Button>(R.id.exporttoexcel)
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
