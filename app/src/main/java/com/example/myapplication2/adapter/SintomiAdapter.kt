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


class SintomiAdapter(private var itemList: List<Sintomo>) : RecyclerView.Adapter<SintomiAdapter.ViewHolder>()  {


    private val selectedSintomi = mutableSetOf<Sintomo>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomesintomo: TextView = itemView.findViewById(R.id.nomesintomo)
        val sintomoCheckbox: CheckBox = itemView.findViewById(R.id.sintomocheckbox)


        fun bind(sintomo: Sintomo, isSelected: Boolean, onClick: (Sintomo, Boolean) -> Unit) {
            nomesintomo.text = sintomo.nomeSintomo
            sintomoCheckbox.isChecked = isSelected
            sintomoCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onClick(sintomo, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_sintomi, parent, false)
        return ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sintomo = itemList[position]
        val isSelected = selectedSintomi.contains(sintomo)
        holder.bind(sintomo, isSelected) { item, isChecked ->
            if (isChecked) {
                selectedSintomi.add(item)
            } else {
                selectedSintomi.remove(item)
            }
        }
    }

    override fun getItemCount() = itemList.size


    fun getSelectedSintomi(): List<Sintomo> {
        return selectedSintomi.toList()
    }

     fun submitlist(nuovoSintomo: List<Sintomo>) {
         itemList= nuovoSintomo
         notifyDataSetChanged()
         Log.d("TrackAdapter", "submitList() chiamato con ${itemList.size} nuove tracce")
         Log.d("TrackAdapter", "Nuova lista di tracce: $itemList")
    }
}