package com.mahmutgunduz.adettakip.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.mahmutgunduz.adettakip.R
import com.mahmutgunduz.adettakip.databinding.ItemPeriodDateBinding
import com.mahmutgunduz.adettakip.model.PeriodDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PeriodDateAdapter(
    private var periodDateList: List<PeriodDate>,
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

    // Listeyi tamamen güncelle
    fun updateList(newList: List<PeriodDate>) {
        periodDateList = newList
        notifyDataSetChanged()
    }

    inner class PeriodDateViewHolder(private val binding: ItemPeriodDateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(periodDate: PeriodDate, position: Int) {
            // Tarih formatını ayarla
            val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("tr"))
            val dayFormat = SimpleDateFormat("EEEE", Locale("tr"))
            
            // Tarih bilgisini ayarla
            val dateText = dateFormat.format(periodDate.date)
            val dayText = dayFormat.format(periodDate.date)
            binding.tvDate.text = dateText
            binding.tvDay.text = dayText
            
            // Saat bilgisini ayarla
            val hour = periodDate.hour ?: 0
            binding.tvTime.text = "Saat: $hour:00"
            
            // Kaç gün önce olduğunu hesapla
            val daysAgo = calculateDaysAgo(periodDate.date)
            binding.tvDaysAgo.text = formatDaysAgo(daysAgo)
            binding.tvDaysAgo.setTextColor(binding.root.context.getColor(R.color.primary_teal))
            
            // İki tarih arasındaki süreyi saat olarak hesapla ve göster
            if (position < periodDateList.size - 1) {
                // Bir sonraki tarih varsa (yani bu tarih ilk tarih değilse)
                val nextPeriodDate = periodDateList[position + 1]
                val hoursBetween = calculateHoursBetween(periodDate.date, nextPeriodDate.date)
                binding.tvCycleHours.text = "Son adet tarihinizle bir önceki adet tarihinizin adet ortalaması $hoursBetween saattir."
                binding.tvCycleHours.visibility = android.view.View.VISIBLE
            } else if (periodDateList.size == 1) {
                // Sadece bir tarih varsa
                binding.tvCycleHours.text = "Tek tarih bilgisi olduğu için adet ortalaması 630 saat olarak varsayılacaktır."
                binding.tvCycleHours.visibility = android.view.View.VISIBLE
            } else {
                binding.tvCycleHours.visibility = android.view.View.GONE
            }

            // Silme butonu
            binding.btnDelete.setOnClickListener {
                showDeleteConfirmationDialog(position, dateText)
            }
        }
        

        private fun calculateHoursBetween(date1: Date, date2: Date): Long {
            val diffInMillis = Math.abs(date1.time - date2.time)
            return TimeUnit.MILLISECONDS.toHours(diffInMillis)
        }
        
        private fun calculateDaysAgo(date: Date): Int {
            val today = Calendar.getInstance()
            val dateCalendar = Calendar.getInstance()
            dateCalendar.time = date
            
            // Saat, dakika, saniye ve milisaniyeleri sıfırla
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            
            dateCalendar.set(Calendar.HOUR_OF_DAY, 0)
            dateCalendar.set(Calendar.MINUTE, 0)
            dateCalendar.set(Calendar.SECOND, 0)
            dateCalendar.set(Calendar.MILLISECOND, 0)
            
            // Milisaniye cinsinden farkı al ve güne çevir
            val diffInMillis = today.timeInMillis - dateCalendar.timeInMillis
            return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        }
        
        private fun formatDaysAgo(days: Int): String {
            return when {
                days == 0 -> "Bugün"
                days == 1 -> "Dün"
                days > 1 -> "$days gün önce"
                else -> ""
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