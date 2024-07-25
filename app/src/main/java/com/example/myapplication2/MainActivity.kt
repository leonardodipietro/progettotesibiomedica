package com.example.myapplication2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.database.ValueEventListener
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var userRepo: UserRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        userRepo=UserRepo()


        val firebaseDatabase = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
        firebaseDatabase.setLogLevel(Logger.Level.DEBUG)//serve per il debug
        database = firebaseDatabase.reference
        auth = FirebaseAuth.getInstance()


        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Check if user still exists in Firebase Authentication
                    auth.currentUser?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            // User exists, proceed to SecondActivity
                            Log.d("testmain", "testmain ${currentUser.email}")
                            val intent = Intent(this, SecondActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // UTENTE NON ESISTE
                            auth.signOut()
                        }
                    }
                }
            }
        }


        // per ottenere i riferimenti agli elementi del layout
        val emailEditText = findViewById<EditText>(R.id.email)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val registerButton = findViewById<Button>(R.id.registerbutton)

        registerButton.setOnClickListener {
            //prendono i campi di email e password scritti da utente e li convertono in stringhe
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            //se i campi non sono vuoti esegue l istruzione
            if (email.isNotEmpty() && password.isNotEmpty()) {
                //metodo predefinito della libreria di firebase
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Registrazione riuscita
                            // viene lanciato un messaggio
                            Log.d("TESTSIGNIN","è ANDATA BENE")
                            Toast.makeText(this, "Registrazione riuscita!", Toast.LENGTH_SHORT).show()

                            userRepo.saveUserIdToFirebase()

                            //Apro la main page dell'app con un Intent
                            val intent = Intent(this, SecondActivity::class.java)
                            startActivity(intent)

                        } else {
                            // Registrazione fallita
                            Log.d("TESTSIGNIN","è ANDATA MALE:  ${task.exception?.message}")
                            Toast.makeText(this, "Registrazione fallita:", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Email e password non possono essere vuoti", Toast.LENGTH_SHORT).show()
            }
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

}
