package com.example.myapplication2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.R
import com.example.myapplication2.model.Utente

class UtenteAdapter(private val users: List<Utente>) : RecyclerView.Adapter<UtenteAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = users.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerViewSintomi = itemView.findViewById<RecyclerView>(R.id.recyclerViewSintomi)

        fun bind(user: Utente) {
            // Popolare la RecyclerView dei sintomi per l'utente specifico
            //val sintomiAdapter = SintomiAdapter(user.sintomi)
            recyclerViewSintomi.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)
            //recyclerViewSintomi.adapter = sintomiUserAdapter
        }
    }
}
