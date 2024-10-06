package com.example.myapplication2.adapter

import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.R
import com.example.myapplication2.model.Sintomo


class DataAdapter (private val dates: List<String>) : RecyclerView.Adapter<DataAdapter.DateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_data, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val dataSegnalazione = dates[position]
       // holder.bind(dataSegnalazione)
    }

     override fun getItemCount(): Int = dates.size

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewData = itemView.findViewById<TextView>(R.id.textViewData)
        //private val textViewOra = itemView.findViewById<TextView>(R.id.textViewOra)

        /*fun bind(dataSegnalazione: DataSegnalazione) {
            textViewData.text = dataSegnalazione.data
            textViewOra.text = dataSegnalazione.ora
        }*/
    }
}
