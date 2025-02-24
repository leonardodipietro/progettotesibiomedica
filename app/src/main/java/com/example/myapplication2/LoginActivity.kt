package com.example.myapplication2
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.appcompat.app.AlertDialog

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication2.Presenter.LoginPresenter
import com.example.myapplication2.interfacepackage.LoginInterface
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.UserRepo
import com.example.myapplication2.utility.UserExperience
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import java.util.Locale
import java.util.concurrent.TimeUnit



class LoginActivity : AppCompatActivity(), LoginInterface {

        private lateinit var presenter: LoginPresenter
        private lateinit var credentialEditText: EditText
        private lateinit var userExperience: UserExperience
        private lateinit var passwordEditText: EditText
        private lateinit var showPasswordImageView: ImageView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            loadLocale()
            setContentView(R.layout.activity_login)

            val userRepo = UserRepo()
            presenter = LoginPresenter(this, userRepo,this)
            userExperience= UserExperience()
            credentialEditText = findViewById(R.id.usernamelogin)
            passwordEditText = findViewById(R.id.pswlogin)
            showPasswordImageView = findViewById(R.id.mostraPassword)
            findViewById<Button>(R.id.loginconmail).setOnClickListener {
                onLoginClicked(credentialEditText.text.toString(), passwordEditText.text.toString())
            }
            findViewById<TextView>(R.id.iniziaresetpsw).setOnClickListener {
                onResetPasswordClicked(credentialEditText.text.toString())
            }
            showPasswordImageView.setOnClickListener {
                onShowPasswordClicked()
            }

        }
        override fun showLoginSuccess(ruolo: String, user: Utente?) {
            Log.d("showLoginSuccess", "Utente ricevuto: $ruolo")
            Log.d("showLoginSuccess", "Utente ricevuto: ${user?.username ?: "Nessun utente"}")
            Log.d("showLoginSuccess", "Utente ricevuto: ${user?.id ?: "Nessun utente"}")
            val targetActivity = when (ruolo) {
                "superadmin" -> {
                    Log.d("showLoginSuccess", "SuperAdmin va  con ${user?.username ?: "Nessun utente"}")
                    SuperAdminActivity::class.java
                }
                "admin" -> {
                    Log.d("showLoginSuccess", "AdminActivity con ${user?.username ?: "Nessun utente"}")
                    AdminActivity::class.java
                }
                else -> {
                    Log.d("showLoginSuccess", "mainpage va con ${user?.username ?: "Nessun utente"}")
                    MainPage::class.java //Pagina dello User
                }
            }

            val intent = Intent(this, targetActivity).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("utente", user)


            }

            startActivity(intent)
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
        override fun showLoginFailure(errorMessage: String) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }

        override fun showAccountLocked(remainingTime: Long) {
            val minutes = remainingTime / 60
            val seconds = remainingTime  % 60
            Toast.makeText(this, "Account bloccato. Riprova tra $minutes minuti.", Toast.LENGTH_LONG).show()
        }

        override fun showResetPasswordEmailSent() {
            Toast.makeText(this, "Email di reset inviata. Controlla la tua posta.", Toast.LENGTH_SHORT).show()
        }

        override fun showResetPasswordError(errorMessage: String) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }

        override fun promptForUsername() {
            Toast.makeText(this, "Inserisci il tuo username.", Toast.LENGTH_SHORT).show()
        }

        override fun togglePasswordVisibility(isVisible: Boolean) {
            passwordEditText.inputType = if (isVisible) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordEditText.setSelection(passwordEditText.text.length)
            showPasswordImageView.setImageResource(if (isVisible) R.drawable.passwordicon else R.drawable.passwordicon)
        }

        override fun onLoginClicked(username: String, password: String) {
            presenter.handleLogin(username, password)
        }

        override fun onResetPasswordClicked(username: String) {
            presenter.handleResetPassword(username)
            presenter.handleResetPasswordWithPhone(username)
        }


        override fun onShowPasswordClicked() {
            presenter.handleShowPassword()
        }

    override fun showVerificationDialog(username: String, verificationId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.dialog_verification_title))
        builder.setMessage(getString(R.string.dialog_verification_message))
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        builder.setView(input)
        builder.setPositiveButton(getString(R.string.dialog_button_verify)) { _, _ ->
            val code = input.text.toString()
            if (code.isNotEmpty()) {
                verifyPhoneCode(verificationId, code) { success ->
                    if (success) {
                        // Reindirizza l'utente alla ResetPasswordActivity
                        val intent = Intent(this, ResetPasswordActivity::class.java).apply {
                            putExtra("username", username)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.toast_error_verification_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, getString(R.string.toast_error_code_empty), Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton(getString(R.string.dialog_button_cancel)) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun verifyPhoneCode(verificationId: String, code: String, callback: (Boolean) -> Unit) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
    }


}

/*override fun showVerificationDialog(username: String, verificationId: String) {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(getString(R.string.dialog_verification_title)) // Titolo dialogo
    builder.setMessage(getString(R.string.dialog_verification_message)) // Messaggio dialogo

    // Campo di input per il codice OTP
    val input = EditText(this)
    input.inputType = InputType.TYPE_CLASS_NUMBER
    builder.setView(input)

    builder.setPositiveButton(getString(R.string.dialog_button_verify)) { _, _ ->
        val code = input.text.toString()
        if (code.isNotEmpty()) {
            verifyPhoneCode(verificationId, code) { success ->
                if (success) {
                    // Reindirizza l'utente alla ResetPasswordActivity
                    val intent = Intent(this, ResetPasswordActivity::class.java).apply {
                        putExtra("username", username)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.toast_error_verification_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.toast_error_code_empty), Toast.LENGTH_SHORT).show()
        }
    }

    builder.setNegativeButton(getString(R.string.dialog_button_cancel)) { dialog, _ ->
        dialog.cancel()
    }

    builder.show()
}

      /*override fun showLoginSuccess(admin: Boolean, user: Utente?) {
            val intent = Intent(this, if (admin) AdminActivity::class.java else MainPage::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("utente", user)
            }
            startActivity(intent)
        }*/


private fun verifyPhoneCode(verificationId: String, code: String, callback: (Boolean) -> Unit) {
    val credential = PhoneAuthProvider.getCredential(verificationId, code)
    FirebaseAuth.getInstance().signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true)
            } else {
                callback(false)
            }
        }
}






*/

/*CANALE VECCHIO DI NOTIFICA
    override fun showResetPasswordNotification(username: String) {
        // Crea il canale di notifica (solo per Android 8.0 e superiori)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "reset_password_channel"
            val channelName = "Reset Password Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Canale per notifiche di reset password"
            }
            // Crea il canale nel NotificationManager
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val intent = Intent(this, ResetPasswordActivity::class.java).apply {
            putExtra("username", username)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "reset_password_channel")
            .setSmallIcon(R.drawable.notificaicona)
            .setContentTitle(getString(R.string.notification_reset_password_title)) // Usa la stringa localizzata
            .setContentText(getString(R.string.notification_reset_password_text)) // Usa la stringa localizzata
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(this).notify(1, notification)

    }
 */


//todo vedere occhio
    //TODO CREAREICONA
