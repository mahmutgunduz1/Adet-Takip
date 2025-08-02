package com.mahmutgunduz.adettakip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mahmutgunduz.adettakip.R
import com.mahmutgunduz.adettakip.model.FaqItem

class FaqAdapter(
    private val onItemClick: (FaqItem) -> Unit
) : RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    private var faqList = listOf<FaqItem>()

    fun updateList(newList: List<FaqItem>) {
        faqList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_faq, parent, false)
        return FaqViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        holder.bind(faqList[position])
    }

    override fun getItemCount(): Int = faqList.size

    inner class FaqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvQuestion: TextView = itemView.findViewById(R.id.tvQuestion)
        private val tvAnswer: TextView = itemView.findViewById(R.id.tvAnswer)
        private val ivExpandIcon: ImageView = itemView.findViewById(R.id.ivExpandIcon)

        fun bind(faqItem: FaqItem) {
            tvQuestion.text = faqItem.question
            tvAnswer.text = faqItem.answer

            // Expand/collapse durumuna göre görünürlük ayarla
            if (faqItem.isExpanded) {
                tvAnswer.visibility = View.VISIBLE
                ivExpandIcon.rotation = 180f
            } else {
                tvAnswer.visibility = View.GONE
                ivExpandIcon.rotation = 0f
            }

            // Click listener
            itemView.setOnClickListener {
                onItemClick(faqItem)
            }
        }
    }
}