package com.example.myapplication2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.Presenter.InfoPresenter
import com.example.myapplication2.adapter.FaqAdapter
import com.example.myapplication2.interfacepackage.InfoView
import com.example.myapplication2.model.Faq
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.FaqRepo
import com.example.myapplication2.repository.UserRepo
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class InfoActivity:AppCompatActivity(), InfoView {

    private lateinit var presenter: InfoPresenter
    private lateinit var currentUser: Utente
    private lateinit var faqRecyclerView: RecyclerView
    private lateinit var faqAdapter: FaqAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadLocale()
        setContentView(R.layout.activityinfo)




        presenter = InfoPresenter(this, UserRepo(), FaqRepo())
        currentUser = intent.getParcelableExtra("utente") ?: throw IllegalStateException("Utente non trovato")
        Log.d("siamo nell info","vediamo l utente $currentUser")
        //currentUser.id?.let { presenter.loadUserData(it) }


        faqRecyclerView = findViewById(R.id.faqRecyclerView)
        faqRecyclerView.layoutManager = LinearLayoutManager(this)

        presenter.loadFaqData()


/*
                    userRepo.updateUserEmail(userId,email) { success ->
                        if (success) {
                            view.showError("email aggiunta con successo ")
                        } else {
                           view.showError("Errore nell'aggiunta della mail")
                            }
                        }*/

        setupBottomNavigation()




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

    override fun showFaqList(faqList: List<Faq>) {
        faqAdapter = FaqAdapter(faqList, false)
        faqRecyclerView.adapter = faqAdapter
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_info
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainPage::class.java).apply {
                        putExtra("utente", currentUser)
                    }
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java).apply {
                        putExtra("utente", currentUser)
                    }
                    startActivity(intent)
                    true
                }
                R.id.nav_info -> true
                else -> false
            }
        }
    }

    override fun getContext(): Context {
        return this // Restituisce il Contesto della page
    }


}