package com.example.myapplication2

import OnSwipeTouchListener
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.adapter.SintomiAdapter
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainPage : AppCompatActivity() {
    private lateinit var sintomoRepo: SintomoRepo
    private lateinit var sintadapter: SintomiAdapter
    private lateinit var userRepo: UserRepo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainpageactivity)




        //todo bloccare fatta loa registrazione il ritorno alla pagina di signin
        //todo verificare effetto della recycler sulla navbar


        val currentUser = FirebaseAuth.getInstance().currentUser
        // Inizializza il repository
        sintomoRepo = SintomoRepo()
        userRepo= UserRepo()

        val inviaButton: Button = findViewById(R.id.inviadati)
        inviaButton.setOnClickListener {
            val selectedSintomi = sintadapter.getSelectedSintomi()
            Log.d("InviaButton", "Selected Sintomi:$selectedSintomi ")


            Log.d("InviaButton", "CurrentUser: ${currentUser?.uid}")

            if (currentUser != null) {
                val userId = currentUser.uid
                Log.d("InviaButton", "User ID: $userId")

                userRepo.submitSintomi(userId, selectedSintomi)
            } else {
                Log.d("InviaButton", "Nessun utente autenticato.")
            }
        }



        val recyclerView: RecyclerView = findViewById(R.id.recyclerSintomi)
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeLeft() {
                // Naviga alla ProfileActivity
                startActivity(Intent(this@MainPage, ProfileActivity::class.java))
            }
        })
       /* val recyclerView: SwipeableRecyclerView = findViewById(R.id.recyclerSintomi)
        recyclerView.setSwipeEnabled(false)*/

        sintadapter = SintomiAdapter(emptyList())
        recyclerView.adapter = sintadapter

        sintomoRepo.fetchSintomi()

        sintomoRepo.sintomi.observe(this, Observer { sintomiList ->
            Log.d("Recuperoactivit", "Listasintomi: $sintomiList")
            sintadapter.submitlist(sintomiList)
        })


        sintomoRepo.fetchSintomi()



        // Osserva i cambiamenti nella lista dei sintomi
        sintomoRepo.sintomi.observe(this, Observer { sintomiList ->
            Log.d("Recuperoactivity", "Listasintomi: $sintomiList")
            sintadapter.submitlist(sintomiList)


        })


        userRepo.fetchSelectedSintomiForUser(currentUser!!.uid) { sintomiList ->
            Log.d("FetchSintomo", "Numero di sintomi recuperati: ${sintomiList.size}")

            for (sintomo in sintomiList) {
                Log.d("FetchSintomo", "Nome: ${sintomo.nomeSintomo}")
            }

            Log.d("FetchSintomo", "Fine gestione")
        }


        //Usare per ogni sintomo
        sintomoRepo.aggiungiSintomo("paura") { success ->
            if (success) {
                Log.d("AGGIUNTASECONDACTIVIY","AGGIUNTA RIUSCITA")
            }
        }

        //METODO PER RIMUOVERE UN SINTOMO DAL DATABSE
        //copiare id da dashboard database
        sintomoRepo.rimuoviSintomo("074df670-63fd-4ebd-81e1-f70113e7440c") { success ->
            if (success) {
                Log.d("Rimozione riuscita","rimozione riuscita")
            }
        }


        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_home // Set the default selection

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Sei già su questa Activity, non fare nulla
                    true
                }
                R.id.nav_profile -> {
                    // Passa alla ProfileActivity
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        val mainpageLayout: View = findViewById(R.id.mainpageroot) // Il layout radice della tua Activity

        // Rileva il movimento di swipe
        mainpageLayout.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeLeft() {
                // Naviga alla ProfileActivity
                val intent = Intent(this@MainPage, ProfileActivity::class.java)
                startActivity(intent)
            }
    })
}
}
