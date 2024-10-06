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

            // Resetta tutte le checkbox prima di impostare lo stato
            gravita1.isChecked = false
            gravita2.isChecked = false
            gravita3.isChecked = false
            gravita4.isChecked = false

            // Variabili per tenere traccia dello stato di selezione
            var gravita1Selected = false
            var gravita2Selected = false
            var gravita3Selected = false
            var gravita4Selected = false

            // Seleziona la gravità corrente e imposta lo stato selezionato
            when (sintomo.gravità) {
                1 -> {
                    gravita1.isChecked = true
                    gravita1Selected = true
                }
                2 -> {
                    gravita2.isChecked = true
                    gravita2Selected = true
                }
                3 -> {
                    gravita3.isChecked = true
                    gravita3Selected = true
                }
                4 -> {
                    gravita4.isChecked = true
                    gravita4Selected = true
                }
            }

            // Definiamo una funzione per gestire il secondo click
            fun handleSecondClick(
                checkBox: CheckBox,
                gravita: Int,
                isSelected: Boolean,
                setSelected: (Boolean) -> Unit,
                vararg others: CheckBox
            ) {
                if (checkBox.isChecked) {
                    if (isSelected) {
                        Log.d("SintomiAdapter", "Deselezionando gravità $gravita per sintomo ${sintomo.nomeSintomo}")
                        // Se già selezionato, deseleziona al secondo click
                        checkBox.isChecked = false
                        onGravitaSelected(sintomo, 0) // Gravità annullata
                        setSelected(false) // Aggiorna lo stato selezionato
                    } else {
                        // Deseleziona le altre checkbox e seleziona la corrente
                        Log.d("SintomiAdapter", "Selezionata gravità $gravita per sintomo ${sintomo.nomeSintomo}")
                        deselectOthers(*others)
                        onGravitaSelected(sintomo, gravita) // Imposta la gravità
                        setSelected(true) // Segna che questa checkbox è selezionata
                    }
                } else {
                    // Aggiungiamo un controllo per quando la checkbox viene deselezionata manualmente
                    Log.d("SintomiAdapter", "Checkbox gravità $gravita deselezionata per sintomo ${sintomo.nomeSintomo}")
                    onGravitaSelected(sintomo, 0) // Aggiorna la gravità a 0 quando viene deselezionata
                    setSelected(false) // Aggiorna lo stato a deselezionato
                }
            }

            // Gestione del click e deselezione su ciascuna checkbox
            gravita1.setOnCheckedChangeListener { _, isChecked ->
                Log.d("SintomiAdapter", "Checkbox Gravità 1: $isChecked per sintomo ${sintomo.nomeSintomo}")
                handleSecondClick(gravita1, 1, gravita1Selected, { gravita1Selected = it }, gravita2, gravita3, gravita4)
            }
            gravita2.setOnCheckedChangeListener { _, isChecked ->
                Log.d("SintomiAdapter", "Checkbox Gravità 2: $isChecked per sintomo ${sintomo.nomeSintomo}")
                handleSecondClick(gravita2, 2, gravita2Selected, { gravita2Selected = it }, gravita1, gravita3, gravita4)
            }
            gravita3.setOnCheckedChangeListener { _, isChecked ->
                Log.d("SintomiAdapter", "Checkbox Gravità 3: $isChecked per sintomo ${sintomo.nomeSintomo}")
                handleSecondClick(gravita3, 3, gravita3Selected, { gravita3Selected = it }, gravita1, gravita2, gravita4)
            }
            gravita4.setOnCheckedChangeListener { _, isChecked ->
                Log.d("SintomiAdapter", "Checkbox Gravità 4: $isChecked per sintomo ${sintomo.nomeSintomo}")
                handleSecondClick(gravita4, 4, gravita4Selected, { gravita4Selected = it }, gravita1, gravita2, gravita3)
            }
        }

        private fun deselectOthers(vararg others: CheckBox) {
            others.forEach { it.isChecked = false }
        }




        // Imposta l'azione del checkbox per il sintomo

        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_sintomi, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sintomo = itemList[position]

        // Controlliamo se il sintomo è già stato selezionato
        //ritorna true se si
        val isSelected = selectedSintomi.any { it.id == sintomo.id }

        holder.bind(sintomo, isSelected, { item, isChecked ->
            // se il sintomo è selezionato
            if (isChecked) {
                selectedSintomi.add(item)
                //isChecked==false
            } else {
                selectedSintomi.remove(item)
            }
        }) //prende il sintomo selezionatom e la sua gravita
        { item, gravita ->
            // Se gravità è deselezionata (ad esempio impostiamo gravita = 0 o altro valore per indicare deselezione)
            if (gravita == 0) {
                // Rimuovi il sintomo dalla lista se la gravità è stata deselezionata
                selectedSintomi.remove(item)
                Log.d("SintomiAdapter", "Sintomo rimosso: ${item.nomeSintomo}")
            } else {
                // Se la gravità è stata selezionata, aggiorna l'oggetto
                item.gravità = gravita
                if (!selectedSintomi.contains(item)) {
                    selectedSintomi.add(item)
                } else {
                    // Se il sintomo è già presente nella lista, aggiorna solo la sua gravità
                    selectedSintomi.find { it.id == item.id }?.gravità = gravita
                }
                Log.d("SintomiAdapter", "Sintomo aggiornato: ${item.nomeSintomo}, Gravità: $gravita")
            }
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
