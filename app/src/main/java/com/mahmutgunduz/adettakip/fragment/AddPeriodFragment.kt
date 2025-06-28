package com.mahmutgunduz.adettakip.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mahmutgunduz.adettakip.R
import com.mahmutgunduz.adettakip.databinding.FragmentAddPeriodBinding

import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AddPeriodFragment : Fragment() {

    private var _binding: FragmentAddPeriodBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val MAX_PERIOD_DATES = 120 // Maksimum tarih sayısı
    private val MIN_CYCLE_DAYS = 20 // Minimum döngü günü
    private val MAX_CYCLE_DAYS = 35 // Maximum döngü günü

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
        // NumberPicker için saat ayarları (0-23 arası)
        binding.hourPicker.minValue = 0
        binding.hourPicker.maxValue = 23
        
        // Geçerli saati ayarla
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.hourPicker.value = currentHour
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

        // Yükleme göstergesini göster
        binding.progressBar?.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        // Mevcut tarih sayısını kontrol et
        firestore.collection("periodDates")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                // Yükleme göstergesini gizle
                binding.progressBar?.visibility = View.GONE
                binding.btnSave.isEnabled = true
                
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
                // Yükleme göstergesini gizle
                binding.progressBar?.visibility = View.GONE
                binding.btnSave.isEnabled = true
                
                val errorMessage = when {
                    e.message?.contains("PERMISSION_DENIED") == true -> 
                        "Veri erişim izni reddedildi. Lütfen tekrar giriş yapın."
                    e.message?.contains("UNAVAILABLE") == true -> 
                        "İnternet bağlantınızı kontrol edin."
                    e.message?.contains("FAILED_PRECONDITION") == true ->
                        "Firestore index eksik. Lütfen geliştiriciyle iletişime geçin."
                    e.message?.contains("DEADLINE_EXCEEDED") == true ->
                        "Bağlantı zaman aşımına uğradı. Tekrar deneyin."
                    else -> "Veri kontrolü yapılırken hata oluştu: ${e.localizedMessage}"
                }
                showErrorMessage(errorMessage)
            }
    }

    private fun showMaxDateLimitDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Maksimum Tarih Sınırı")
            .setMessage("En fazla 120 adet tarih kaydedebilirsiniz. Yeni tarih eklemek için önce eski tarihlerden bazılarını silmelisiniz.")
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
        // Seçilen tarihi oluştur
        val selectedDate = createSelectedDate()
        
        // Önce mevcut tarihleri kontrol et ve geçerli döngü kontrolü yap
        checkValidCycle(selectedDate) { isValid, daysBetween ->
            val builder = AlertDialog.Builder(requireContext())
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_period, null)
            builder.setView(dialogView)
            val alertDialog = builder.create()

            val date = dialogView.findViewById<TextView>(R.id.tvSelectedDate)
            val time = dialogView.findViewById<TextView>(R.id.tvSelectedTime)
            val noteTextView = dialogView.findViewById<TextView>(R.id.tvNote)
            val confirmButton = dialogView.findViewById<View>(R.id.btnConfirm)
            val dialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
            val confirmationMessage = dialogView.findViewById<TextView>(R.id.tvConfirmationMessage)
            val detailsContainer = dialogView.findViewById<View>(R.id.cvDetailsContainer)

            date.text =
                binding.datePicker.dayOfMonth.toString() + "." + (binding.datePicker.month + 1).toString() + "." + binding.datePicker.year.toString()
            time.text = binding.hourPicker.value.toString() + ":00"

            // Geçerli döngü kontrolü - eğer önceki tarih ile arasında 20-35 gün yoksa uyarı göster
            if (!isValid && daysBetween != null) {
                // Uyarı mesajını göster
                noteTextView.text = "UYARI: Son adet tarihiniz ile bu tarih arasında $daysBetween gün var. " +
                        "Normal adet döngüsü $MIN_CYCLE_DAYS-$MAX_CYCLE_DAYS gün arasında olmalıdır. " +
                        "Bu aralık dışındaki döngüler için bir doktora danışmanız önerilir."
                noteTextView.setTextColor(resources.getColor(R.color.error, null))
                noteTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0)
                noteTextView.compoundDrawableTintList = resources.getColorStateList(R.color.error, null)
                
                // Dialog başlığını ve mesajını güncelle
                dialogTitle.text = "Geçersiz Döngü Uyarısı"
                dialogTitle.setTextColor(resources.getColor(R.color.error, null))
                
                // Özel mesaj oluştur
                val detailedMessage = if (daysBetween < MIN_CYCLE_DAYS) {
                    "Son adet tarihinizden bu yana sadece $daysBetween gün geçmiş. " +
                    "Normal adet döngüsü en az $MIN_CYCLE_DAYS gün olmalıdır. " +
                    "Lütfen doğru tarihi girdiğinizden emin olun veya daha sonra tekrar deneyin."
                } else {
                    "Son adet tarihinizden bu yana $daysBetween gün geçmiş. " +
                    "Normal adet döngüsü en fazla $MAX_CYCLE_DAYS gün olmalıdır. " +
                    "Lütfen doğru tarihi girdiğinizden emin olun veya bir kadın doğum uzmanına danışın."
                }
                
                confirmationMessage.text = detailedMessage
                
                // Detay container arka plan rengini değiştir
                detailsContainer.setBackgroundColor(resources.getColor(R.color.error, null))
                
                // Onay butonunu devre dışı bırak ve rengini değiştir
                confirmButton.isEnabled = false
                confirmButton.alpha = 0.5f

            }

            dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener {
                alertDialog.dismiss()
            }
            
            confirmButton.setOnClickListener {
                // Geçerli döngü kontrolü yapıldığından, buraya sadece geçerli tarihler ulaşabilir
                saveSelectedDate()
                alertDialog.dismiss()
            }

            alertDialog.show()
        }
    }
    

    private fun createSelectedDate(): Date {
        val day = binding.datePicker.dayOfMonth
        val month = binding.datePicker.month
        val year = binding.datePicker.year
        val hour = binding.hourPicker.value
        val minute = 0

        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
    

     //Yeni tarih ile en son kaydedilen tarih arasındaki döngünün geçerli olup olmadığını kontrol eder
   // newDate Yeni eklenen tarih
   // callback Kontrol sonucunu döndüren callback (geçerli mi, gün farkı)

    private fun checkValidCycle(newDate: Date, callback: (Boolean, Long?) -> Unit) {
        val currentUser = auth.currentUser ?: return callback(true, null) // Kullanıcı yoksa kontrol yapma
        
        // Yükleme göstergesini göster
        binding.progressBar?.visibility = View.VISIBLE
        
        // En son kaydedilen tarihi bul - geçici olarak basitleştirilmiş sorgu
        firestore.collection("periodDates")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                // Yükleme göstergesini gizle
                binding.progressBar?.visibility = View.GONE
                
                if (documents.isEmpty) {
                    // Önceki tarih yoksa geçerli kabul et (ilk kayıt)
                    callback(true, null)
                    return@addOnSuccessListener
                }
                
                // Tüm tarihleri al ve en son olanı bul (geçici çözüm)
                var lastPeriodDate: Date? = null
                for (document in documents) {
                    val date = document.getDate("date")
                    if (date != null && (lastPeriodDate == null || date.after(lastPeriodDate))) {
                        lastPeriodDate = date
                    }
                }
                
                if (lastPeriodDate == null) {
                    callback(true, null)
                    return@addOnSuccessListener
                }
                
                // Tarihler arasındaki farkı hesapla (mutlak değer)
                val daysBetween = if (newDate.before(lastPeriodDate)) {
                    TimeUnit.MILLISECONDS.toDays(lastPeriodDate.time - newDate.time)
                } else {
                    TimeUnit.MILLISECONDS.toDays(newDate.time - lastPeriodDate.time)
                }
                
                // Geçerli döngü kontrolü (20-35 gün arasında olmalı)
                val isValidCycle = daysBetween in MIN_CYCLE_DAYS..MAX_CYCLE_DAYS
                
                // Sonucu döndür
                callback(isValidCycle, daysBetween)
                
                // Geçersiz döngü ise kullanıcıya bilgi ver
                if (!isValidCycle) {
                    val message = if (daysBetween < MIN_CYCLE_DAYS) {
                        "Son adet tarihinizden bu yana sadece $daysBetween gün geçmiş. Normal adet döngüsü en az $MIN_CYCLE_DAYS gün olmalıdır."
                    } else {
                        "Son adet tarihinizden bu yana $daysBetween gün geçmiş. Normal adet döngüsü en fazla $MAX_CYCLE_DAYS gün olmalıdır."
                    }
                    showErrorMessage(message)
                }
            }
            .addOnFailureListener { e ->
                // Yükleme göstergesini gizle
                binding.progressBar?.visibility = View.GONE
                
                val errorMessage = when {
                    e.message?.contains("PERMISSION_DENIED") == true -> 
                        "Veri erişim izni reddedildi. Lütfen tekrar giriş yapın."
                    e.message?.contains("UNAVAILABLE") == true -> 
                        "İnternet bağlantınızı kontrol edin."
                    e.message?.contains("FAILED_PRECONDITION") == true ->
                        "Firestore index eksik. Lütfen geliştiriciyle iletişime geçin."
                    e.message?.contains("DEADLINE_EXCEEDED") == true ->
                        "Bağlantı zaman aşımına uğradı. Tekrar deneyin."
                    else -> "Tarih kontrolü yapılırken hata oluştu: ${e.localizedMessage}"
                }
                showErrorMessage(errorMessage)
                callback(true, null)
            }
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


        // Seçilen tarihi al
        val selectedDate = createSelectedDate()

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

        // İnternet bağlantısı kontrolü
        if (!com.mahmutgunduz.adettakip.utils.NetworkUtils.isNetworkAvailable(requireContext())) {
            showErrorMessage("İnternet bağlantınızı kontrol edin")
            return
        }

        // Veri hazırlama
        val periodData = hashMapOf(
            "userId" to currentUser.uid,
            "date" to date,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "hour" to binding.hourPicker.value,
            "minute" to 0
        )

        // Firestore'a kaydet
        firestore.collection("periodDates")
            .add(periodData)
            .addOnSuccessListener {
                showSuccessMessage(getString(R.string.period_added_success))
                
                // Ana ekrana geri dön
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                val errorMessage = when {
                    e.message?.contains("PERMISSION_DENIED") == true -> 
                        "Veri kaydetme izni reddedildi. Lütfen giriş yapın."
                    e.message?.contains("UNAVAILABLE") == true -> 
                        "İnternet bağlantınızı kontrol edin."
                    e.message?.contains("DEADLINE_EXCEEDED") == true -> 
                        "Bağlantı zaman aşımına uğradı. Tekrar deneyin."
                    else -> "Kayıt başarısız: ${e.localizedMessage}"
                }
                showErrorMessage(errorMessage)
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



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 