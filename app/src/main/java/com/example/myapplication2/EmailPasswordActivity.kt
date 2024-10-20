package com.example.myapplication2

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.UserRepo
import com.example.myapplication2.utility.UserExperience
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth


class EmailPasswordActivity : AppCompatActivity() {


    //todo gestire meglio toast

    private lateinit var auth: FirebaseAuth
    private lateinit var userRepo: UserRepo

    private lateinit var showPasswordIcon: ImageView
    private lateinit var showConfirmPasswordIcon: ImageView
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private lateinit var userExperience: UserExperience

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.emailpasswordactivity)


        auth = FirebaseAuth.getInstance()
        userRepo = UserRepo()
        userExperience=UserExperience()

        val emailEditText = findViewById<EditText>(R.id.email)
        val usernameEditText = findViewById<EditText>(R.id.usernameregistrazione)
        val namesurnameEditText=findViewById<EditText>(R.id.nomeecognome)
        val addressEditText=findViewById<EditText>(R.id.indirizzo)

        val passwordEditText = findViewById<EditText>(R.id.password)
        val confermaPasswordEditText = findViewById<EditText>(R.id.confermapassword)
        showPasswordIcon = findViewById(R.id.showPassword)
        showConfirmPasswordIcon = findViewById(R.id.showConfirmPassword)



        showPasswordIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(passwordEditText, showPasswordIcon, isPasswordVisible)
        }

        showConfirmPasswordIcon.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(confermaPasswordEditText, showConfirmPasswordIcon, isConfirmPasswordVisible)
        }
        val registerButton = findViewById<Button>(R.id.registerbutton)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val name = namesurnameEditText.text.toString()
            val address = addressEditText.text.toString()
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confermaPassword = confermaPasswordEditText.text.toString()
            val phoneNumber=""

            /*Inserire qui condizioni sintattiche per i campi mail telefono username*/



            userExperience.normalizeInputs(usernameEditText, emailEditText, addressEditText)
            userExperience.validateEmailInput(emailEditText)

            if (email.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty() && confermaPassword.isNotEmpty() &&
                name.isNotEmpty() && address.isNotEmpty()) {


                if (password == confermaPassword) {
                    userRepo.checkUsernameExists(username) { exists ->
                        if (exists) {
                            Toast.makeText(this, "Username già in uso, selezionare un altro", Toast.LENGTH_SHORT).show()
                        } else {

                            // Procedi con la registrazione su Firebase Authentication
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        val userId = auth.currentUser?.uid ?: ""
                                        try {
                                            val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
                                            val user = Utente(
                                                id = userId,
                                                email = email,
                                                name = name,
                                                address = address,
                                                username = username,
                                                password = hashedPassword,
                                                //admin = false,
                                                ruolo = "user",
                                                phoneNumber=phoneNumber
                                            )
                                            userRepo.saveUserToFirebase(username, name, address, hashedPassword,ruolo="user")
                                            val intent = Intent(this, MainPage::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                putExtra("utente", user)
                                            }
                                            startActivity(intent)
                                            finish()
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Errore durante il salvataggio dell'utente: ${e.message}")
                                        }
                                    } else {
                                        Toast.makeText(this, "Registrazione fallita: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    }
                } else {
                    Toast.makeText(this, "Le password non corrispondono", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Email, password e conferma password non possono essere vuoti", Toast.LENGTH_SHORT).show()
            }
        }


        /* FirebaseApp.initializeApp(this)
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
                             //startSecondActivity()
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
         */

    }

    /*private fun startSecondActivity(user) {
        val intent = Intent(this, MainPage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("utente", user)
        }
        startActivity(intent)
        finish()
        }*/

    private fun togglePasswordVisibility(editText: EditText, icon: ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        }
        // Mantenere il cursore alla fine del testo
        editText.setSelection(editText.text.length)
    }
}





    /*Log.d("PROVIAMO", "Database reference initialized: ${database.root}")
    Log.d("PROVIAMO", "Database URL: ${FirebaseDatabase.getInstance().reference.database.getReference()}")
    writeTestData()
    readDataFromDatabase()*/


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


