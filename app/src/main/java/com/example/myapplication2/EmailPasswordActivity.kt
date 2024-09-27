package com.example.myapplication2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class EmailPasswordActivity : AppCompatActivity() {


    //todo gestire meglio toast
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepo: UserRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Ricarica l'utente e controlla se è ancora autenticato
            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            // L'utente esiste, avvia `SecondActivity`
                            Log.d("MainActivity", "User è autenticato: ${currentUser.email}")
                            startSecondActivity()
                        } else {
                            // L'utente non esiste
                            auth.signOut()

                            setContentView(R.layout.emailpasswordactivity)
                            setupUIAndRegister()
                        }
                    }
                } else {
                    // Ricaricamento fallito, esegui il logout e mostra il layout di `MainActivity`
                    auth.signOut()
                    setContentView(R.layout.emailpasswordactivity)
                    setupUIAndRegister()
                }
            }
        } else {
            // Nessun utente autenticato, mostra il layout di `MainActivity`
            setContentView(R.layout.emailpasswordactivity)
            setupUIAndRegister()
        }
    }

    private fun setupUIAndRegister() {
        userRepo = UserRepo()

        val firebaseDatabase = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
        database = firebaseDatabase.reference

        val emailEditText = findViewById<EditText>(R.id.email)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val confermaPasswordEditText = findViewById<EditText>(R.id.confermapassword)
        val registerButton = findViewById<Button>(R.id.registerbutton)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confermaPassword = confermaPasswordEditText.text.toString()

            // Controllo se i campi non sono vuoti
            if (email.isNotEmpty() && password.isNotEmpty() && confermaPassword.isNotEmpty()) {
                // Verifica se la password e la conferma della password corrispondono
                if (password == confermaPassword) {
                    // Procedi con la registrazione su Firebase Authentication
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                Log.d("MainActivity", "Registrazione avvenuta con successo")
                                userRepo.saveUserIdToFirebase()
                                startSecondActivity()
                            } else {
                                Toast.makeText(this, "Registrazione fallita: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                Log.d("MainActivity", "Errore registrazione: ${task.exception?.message}")
                            }
                        }
                } else {
                    // Mostra un messaggio di errore se le password non corrispondono
                    Toast.makeText(this, "Le password non corrispondono", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Mostra un messaggio di errore se uno dei campi è vuoto
                Toast.makeText(this, "Email, password e conferma password non possono essere vuoti", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startSecondActivity() {
        val intent = Intent(this, MainPage::class.java)
        startActivity(intent)
        finish()
    }





    /*Log.d("PROVIAMO", "Database reference initialized: ${database.root}")
    Log.d("PROVIAMO", "Database URL: ${FirebaseDatabase.getInstance().reference.database.getReference()}")
    writeTestData()
    readDataFromDatabase()*/
    }

    /*private fun writeTestData() {
        Log.d("PROVIAMO", "Writing data to database")
        database.child("message").setValue("Hello, World!")
            .addOnSuccessListener {
                Log.d("PROVIAMO", "Data written successfully")
                Toast.makeText(applicationContext, "Data written successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("PROVIAMO", "Failed to write data", e)
                Toast.makeText(applicationContext, "Failed to write data", Toast.LENGTH_SHORT).show()
            }
    }
    private fun readDataFromDatabase() {
        database.child("message").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val value = dataSnapshot.getValue(String::class.java)
                Log.d("PROVIAMO", "Value is: $value")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("PROVIAMO", "Failed to read value.", error.toException())
            }
        })
    }*/


