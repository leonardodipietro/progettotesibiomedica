package com.example.myapplication2

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

class InfoActivity:AppCompatActivity(), InfoView {

    private lateinit var presenter: InfoPresenter
    private lateinit var currentUser: Utente
    private lateinit var faqRecyclerView: RecyclerView
    private lateinit var faqAdapter: FaqAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activityinfo)




        presenter = InfoPresenter(this, UserRepo(), FaqRepo())
        currentUser = intent.getParcelableExtra("utente") ?: throw IllegalStateException("Utente non trovato")
        Log.d("siamo nell info","vediamo l utente $currentUser")
        //currentUser.id?.let { presenter.loadUserData(it) }


        faqRecyclerView = findViewById(R.id.faqRecyclerView)
        faqRecyclerView.layoutManager = LinearLayoutManager(this)

        presenter.loadFaqData()




        setupBottomNavigation()




    }

    override fun showFaqList(faqList: List<Faq>) {
        faqAdapter = FaqAdapter(faqList)
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


}