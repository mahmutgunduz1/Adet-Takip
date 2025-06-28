package com.osmanyildiz.scs.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.osmanyildiz.scs.R
import com.osmanyildiz.scs.databinding.ItemPeriodDateBinding
import com.osmanyildiz.scs.model.PeriodDate
import java.text.SimpleDateFormat
import java.util.Locale

class PeriodDateAdapter(
    private val periodDateList: List<PeriodDate>,
    private val onDeleteClick: (position: Int) -> Unit
) : RecyclerView.Adapter<PeriodDateAdapter.PeriodDateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeriodDateViewHolder {
        val binding = ItemPeriodDateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PeriodDateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PeriodDateViewHolder, position: Int) {
        val periodDate = periodDateList[position]
        holder.bind(periodDate, position)
    }

    override fun getItemCount(): Int = periodDateList.size

    inner class PeriodDateViewHolder(private val binding: ItemPeriodDateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(periodDate: PeriodDate, position: Int) {
            val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("tr"))
            binding.tvDate.text = dateFormat.format(periodDate.date)
            
            val hour = periodDate.hour ?: 0
            val minute = periodDate.minute ?: 0
            val formattedTime = String.format("%02d:%02d", hour, minute)
            binding.tvTime.text = formattedTime

            binding.btnDelete.setOnClickListener {
                showDeleteConfirmationDialog(position, dateFormat.format(periodDate.date))
            }
        }
        
        private fun showDeleteConfirmationDialog(position: Int, dateText: String) {
            AlertDialog.Builder(binding.root.context)
                .setTitle("Tarihi Sil")
                .setMessage("$dateText tarihini silmek istediğinize emin misiniz?")
                .setPositiveButton("Evet") { _, _ ->
                    onDeleteClick(position)
                }
                .setNegativeButton("Hayır", null)
                .setIcon(R.drawable.ic_warning)
                .show()
        }
    }
} 