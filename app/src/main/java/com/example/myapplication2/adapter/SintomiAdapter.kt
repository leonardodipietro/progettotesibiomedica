package com.example.myapplication2.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.R
import com.example.myapplication2.model.Sintomo

class SintomiAdapter(private var itemList: List<Sintomo>) : RecyclerView.Adapter<SintomiAdapter.ViewHolder>() {

    private val selectedSintomi = mutableSetOf<Sintomo>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomesintomo: TextView = itemView.findViewById(R.id.nomesintomo)
        //val sintomoCheckbox: CheckBox = itemView.findViewById(R.id.sintomocheckbox)
        val gravita1: CheckBox = itemView.findViewById(R.id.cbgravita1)
        val gravita2: CheckBox = itemView.findViewById(R.id.cbgravita2)
        val gravita3: CheckBox = itemView.findViewById(R.id.cbgravita3)
        val gravita4: CheckBox = itemView.findViewById(R.id.cbgravita4)

        fun bind(sintomo: Sintomo, isSelected: Boolean, onSintomoClick: (Sintomo, Boolean) -> Unit, onGravitaSelected: (Sintomo, Int) -> Unit) {
            nomesintomo.text = sintomo.nomeSintomo
            //sintomoCheckbox.isChecked = isSelected
            // Resetta tutte le checkbox prima di impostare lo stato
            gravita1.isChecked = false
            gravita2.isChecked = false
            gravita3.isChecked = false
            gravita4.isChecked = false

            // Seleziona la gravità corrente
            when (sintomo.gravita) {
                1 -> gravita1.isChecked = true
                2 -> gravita2.isChecked = true
                3 -> gravita3.isChecked = true
                4 -> gravita4.isChecked = true
            }
            // Listener per ogni checkbox, permettendo la selezione esclusiva di una gravità
            gravita1.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    deselectOthers(gravita2, gravita3, gravita4)
                    onGravitaSelected(sintomo, 1)
                }
            }
            gravita2.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    deselectOthers(gravita1, gravita3, gravita4)
                    onGravitaSelected(sintomo, 2)
                }
            }
            gravita3.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    deselectOthers(gravita1, gravita2, gravita4)
                    onGravitaSelected(sintomo, 3)
                }
            }
            gravita4.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    deselectOthers(gravita1, gravita2, gravita3)
                    onGravitaSelected(sintomo, 4)
                }
            }
        }
        private fun deselectOthers(vararg others: CheckBox) {
            others.forEach { it.isChecked = false }
        }





        // Imposta l'azione del checkbox per il sintomo
            /*sintomoCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onSintomoClick(sintomo, isChecked)
            }

            // Imposta i listener per la selezione della gravità
            gravita1.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) onGravitaSelected(sintomo, 1)
            }
            gravita2.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) onGravitaSelected(sintomo, 2)
            }
            gravita3.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) onGravitaSelected(sintomo, 3)
            }
            gravita4.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) onGravitaSelected(sintomo, 4)
            }*/
        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_sintomi, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sintomo = itemList[position]

        // Controlliamo se il sintomo è già stato selezionato
        val isSelected = selectedSintomi.any { it.id == sintomo.id }

        holder.bind(sintomo, isSelected, { item, isChecked ->
            // Logica per l'aggiunta o rimozione del sintomo dalla lista, basata sulla gravità
            if (isChecked) {
                selectedSintomi.add(item)
            } else {
                selectedSintomi.remove(item)
            }
        }) { item, gravita ->
            // Aggiorna la gravità e aggiungi il sintomo alla lista selezionati
            item.gravita = gravita

            // Aggiungiamo il sintomo alla lista solo se una gravità è stata selezionata
            if (!selectedSintomi.contains(item)) {
                selectedSintomi.add(item)
            } else {
                // Se il sintomo è già selezionato, aggiorna la sua gravità
                selectedSintomi.find { it.id == item.id }?.gravita = gravita
            }

            // Log per verificare se l'aggiornamento è corretto
            Log.d("SintomiAdapter", "Sintomo aggiornato: ${item.nomeSintomo}, Gravità: $gravita")
        }
    }

    override fun getItemCount() = itemList.size

    fun getSelectedSintomi(): List<Sintomo> {
        return selectedSintomi.toList()
    }

    fun submitlist(nuovoSintomo: List<Sintomo>) {
        itemList = nuovoSintomo
        notifyDataSetChanged()
    }
    fun getAllSintomi(): List<Sintomo> {
        return itemList
    }
}
