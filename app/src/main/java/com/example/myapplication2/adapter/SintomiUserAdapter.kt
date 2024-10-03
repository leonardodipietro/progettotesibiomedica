package com.example.myapplication2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.R
class SintomiUserAdapter(private val sintomi: List<Sintomo>) : RecyclerView.Adapter<SintomiUserAdapter.SintomoUserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SintomoUserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_sintomiutente, parent, false)
        return SintomoUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: SintomoUserViewHolder, position: Int) {
        val sintomo = sintomi[position]
        holder.bind(sintomo)
    }

    override fun getItemCount(): Int = sintomi.size

    class SintomoUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerViewDate = itemView.findViewById<RecyclerView>(R.id.recyclerViewDate)

        fun bind(sintomo: Sintomo) {
            // Popolare la RecyclerView delle date per il sintomo specifico
            val dateAdapter = DataAdapter(listOf(sintomo.toString()))
            recyclerViewDate.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)
            recyclerViewDate.adapter = dateAdapter
        }
    }
}
