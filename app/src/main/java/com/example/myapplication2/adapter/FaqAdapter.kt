package com.example.myapplication2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.R
import com.example.myapplication2.model.Faq

class FaqAdapter(private val faqList: List<Faq>,
                 private val isClickable: Boolean,
                 private val itemClickListener: ((Faq) -> Unit)? = null
) : RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_faq, parent, false)
        return FaqViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        val faq = faqList[position]
        holder.bind(faq, isClickable, itemClickListener)
    }

    override fun getItemCount(): Int = faqList.size

    class FaqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionTextView: TextView = itemView.findViewById(R.id.questionTextView)
        private val answerTextView: TextView = itemView.findViewById(R.id.answerTextView)

        fun bind(faq: Faq, isClickable: Boolean, itemClickListener: ((Faq) -> Unit)?) {
            questionTextView.text = faq.question
            answerTextView.text = faq.answer

            if (isClickable) {
                itemView.setOnClickListener {
                    itemClickListener?.invoke(faq)
                }
            } else {
                itemView.setOnClickListener(null)  // Disabilita il click
            }
        }
    }
}
