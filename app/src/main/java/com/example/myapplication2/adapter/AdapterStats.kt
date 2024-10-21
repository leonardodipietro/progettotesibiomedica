package com.example.myapplication2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.R
import com.example.myapplication2.model.Sintomo
class AdapterStats(private val sintomiList: List<Pair<Sintomo, String>>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.statsrecyclerheader, parent, false) // Layout per l'header
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.statsrecycler, parent, false) // Layout per i dati
            SintomoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SintomoViewHolder && position > 0) {
            val (sintomo, nomeUtente) = sintomiList[position - 1]
            holder.bind(sintomo, nomeUtente)
        }
    }

    override fun getItemCount(): Int {
        return sintomiList.size + 1 // Aggiungi 1 per l'header
    }

    inner class SintomoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dataSegnalazione: TextView = itemView.findViewById(R.id.data_segnalazionestats)
        private val nomeUtente: TextView = itemView.findViewById(R.id.nome_utentestats)
        private val oraSegnalazione: TextView = itemView.findViewById(R.id.ora_segnalazionestats)
        private val gravita: TextView = itemView.findViewById(R.id.gravitastats)
        private val tempoUltimoPasto: TextView = itemView.findViewById(R.id.tempo_ultimo_pastostats)
        private val nomeSintomo: TextView = itemView.findViewById(R.id.nome_sintomostats)

        fun bind(sintomo: Sintomo, nomeUtenteStr: String) {

            dataSegnalazione.text = sintomo.dataSegnalazione
            nomeUtente.text = nomeUtenteStr
            oraSegnalazione.text = sintomo.oraSegnalazione
            gravita.text = sintomo.gravità.toString()
            tempoUltimoPasto.text = sintomo.tempoTrascorsoUltimoPasto.toString()


            nomeSintomo.text = sintomo.nomeSintomo
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }
}




