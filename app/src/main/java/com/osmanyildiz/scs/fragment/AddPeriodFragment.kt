package com.osmanyildiz.scs.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.osmanyildiz.scs.R
import com.osmanyildiz.scs.databinding.FragmentAddPeriodBinding
import com.osmanyildiz.scs.model.PeriodDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddPeriodFragment : Fragment() {

    private var _binding: FragmentAddPeriodBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val MAX_PERIOD_DATES = 9 // Maksimum tarih sayısı


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPeriodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase tanımlama
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupToolbar()
        setupDatePicker()
        setupTimePicker()
        setupClickListeners()
        setupAnimations()
    }

    private fun setupToolbar() {
        binding.toolbar.title = ""
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
    }

    private fun setupDatePicker() {
        // Tarih seçicinin maksimum değerini bugün olarak ayarla (gelecek tarih seçilemesin)
        val today = Calendar.getInstance()
        binding.datePicker.maxDate = today.timeInMillis

        // Türkçe dil ayarı (XML'de de locale="tr_TR" ekledik)
        try {
            val locale = Locale("tr", "TR")
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupTimePicker() {
        // TimePicker için gerekli ayarlar
        binding.timePicker.setIs24HourView(true) // 24 saat formatını kullan
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {


            checkPeriodDateCount()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnInfo.setOnClickListener {
            showInfoDialog()
        }
    }

    private fun checkPeriodDateCount() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showErrorMessage("Lütfen önce giriş yapın")
            return
        }

        // Mevcut tarih sayısını kontrol et
        firestore.collection("periodDates")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                val currentCount = documents.size()

                if (currentCount >= MAX_PERIOD_DATES) {
                    // Maksimum sayıya ulaşıldı, uyarı göster
                    showMaxDateLimitDialog()
                } else {
                    // Limit aşılmadı, kaydetmeye devam et
                    showDialogAddPeriod()

                }
            }
            .addOnFailureListener { e ->
                // Hata durumunda yine de kaydetmeye çalış
                showErrorMessage("Veri sayısı kontrol edilirken hata oluştu, yine de kaydediliyor")
                saveSelectedDate()
            }
    }

    private fun showMaxDateLimitDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Maksimum Tarih Sınırı")
            .setMessage("En fazla 9 adet tarih kaydedebilirsiniz. Yeni tarih eklemek için önce eski tarihlerden bazılarını silmelisiniz.")
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Tamam") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Ana Sayfaya Dön") { _, _ ->
                findNavController().popBackStack()
            }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun showDialogAddPeriod() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_period, null)
        builder.setView(dialogView)
        val alertDialog = builder.create()

        val date = dialogView.findViewById<TextView>(R.id.tvSelectedDate)
        val time = dialogView.findViewById<TextView>(R.id.tvSelectedTime)

        date.text =
            binding.datePicker.dayOfMonth.toString() + "." + binding.datePicker.month.toString() + "." + binding.datePicker.year.toString()
        time.text = binding.timePicker.hour.toString() + ":" + binding.timePicker.minute.toString()

        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener {
            alertDialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.btnConfirm).setOnClickListener {
            saveSelectedDate()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun showInfoDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_period_info, null)

        builder.setView(dialogView)
        val alertDialog = builder.create()

        // Dialog arka planını yuvarlak yapmak için
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Kapatma butonu
        dialogView.findViewById<View>(R.id.btnClose).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun setupAnimations() {
        // Kart animasyonu
        binding.cvContent.alpha = 0f
        binding.cvContent.translationY = 50f
        binding.cvContent.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    private fun saveSelectedDate() {
        // Fragment'in context'e bağlı olup olmadığını kontrol et
        val context = context ?: return

        // Seçilen tarihi al
        val day = binding.datePicker.dayOfMonth
        val month = binding.datePicker.month
        val year = binding.datePicker.year

        // Seçilen saati al
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute

        val selectedDate = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // Gelecek tarih kontrolü (ek güvenlik)
        val today = Calendar.getInstance().time

        if (selectedDate.after(today)) {
            showErrorMessage(getString(R.string.future_date_error))
            return
        }

        // Firebase'e kaydet
        saveToFirebase(selectedDate)
    }

    private fun saveToFirebase(date: Date) {
        // Kullanıcı giriş yapmış mı kontrol et
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showErrorMessage("Lütfen önce giriş yapın")
            return
        }

        // Veri hazırlama
        val periodData = hashMapOf(
            "userId" to currentUser.uid,
            "date" to date,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "hour" to binding.timePicker.hour,
            "minute" to binding.timePicker.minute
        )

        // Firestore'a kaydet
        firestore.collection("periodDates")
            .add(periodData)
            .addOnSuccessListener {
                showSuccessMessage(getString(R.string.period_added_success))
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                showErrorMessage("Kayıt başarısız: ${e.localizedMessage}")
            }
    }

    private fun showErrorMessage(message: String) {
        val view = view ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(resources.getColor(R.color.error, null))
            .setTextColor(resources.getColor(R.color.white, null))
            .show()
    }

    private fun showSuccessMessage(message: String) {
        val view = view ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(resources.getColor(R.color.success, null))
            .setTextColor(resources.getColor(R.color.white, null))
            .show()
    }

    private fun checkDuplicateDate(date: Date): Boolean {
        // Gerçek uygulamada veritabanından kontrol edilmeli
        // Bu örnekte her zaman false döndürerek, tarih kontrolü yapılmadığını varsayıyoruz
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 