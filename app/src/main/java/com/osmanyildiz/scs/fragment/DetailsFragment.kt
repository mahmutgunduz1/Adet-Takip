package com.osmanyildiz.scs.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.osmanyildiz.scs.R
import com.osmanyildiz.scs.databinding.FragmentDetailsBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Firebase tanımlama
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        setupToolbar()
        loadPeriodDates()
    }
    
    private fun setupToolbar() {
        binding.toolbar.title = ""
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
    }
    
    private fun loadPeriodDates() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Lütfen önce giriş yapın")
            findNavController().popBackStack()
            return
        }

        // Yükleniyor animasyonu başlat
        setupAnimations()
        
        // Firestore'dan tarih verilerini çek
        firestore.collection("periodDates")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showToast("Henüz kayıtlı adet tarihi bulunmamaktadır")
                    return@addOnSuccessListener
                }
                
                // En az 2 tarih olmalı
                if (documents.size() < 2) {
                    binding.tvInfoDate.text = "Döngü hesaplaması için en az 2 adet tarihi gereklidir"
                    return@addOnSuccessListener
                }
                
                // Tarihleri listeye al
                val periodDates = documents.mapNotNull { doc -> 
                    doc.getTimestamp("date")?.toDate() 
                }
                
                // Hesaplamaları yap
                calculateAndShowCycleInfo(periodDates)
            }
            .addOnFailureListener { e ->
                showToast("Veriler yüklenirken hata oluştu: ${e.localizedMessage}")
            }
    }
    
    private fun calculateAndShowCycleInfo(periodDates: List<Date>) {
        // Tarih listesi zaten tarih sırasına göre azalan şekilde sıralanmış olmalı
        // En son adet tarihi
        val lastPeriodDate = periodDates[0]
        
        // Bilgi tarihi - şuanki tarih
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd MMMM yyyy EEEE, 'Saat:' HH:mm:ss", Locale("tr", "TR"))
        binding.tvInfoDate.text = dateFormat.format(currentDate)
        
        // Ortalama döngü uzunluğunu hesapla
        val cycleLengths = mutableListOf<Long>()
        
        // Tüm ardışık tarih çiftleri için döngü uzunluklarını hesapla
        for (i in 0 until periodDates.size - 1) {

                            // TimeUnit.MILLISECONDS.toDays(...) → Bu iki tarih arasındaki farkı güne çevirir.
            val daysBetween = TimeUnit.MILLISECONDS.toDays(periodDates[i].time - periodDates[i + 1].time)
            cycleLengths.add(daysBetween)
        }
        
        // Ortalama döngü uzunluğunu hesapla
        val averageCycleLength = if (cycleLengths.isNotEmpty()) {
            //cycleLengths.average() → Yukarıda hesaplanan tüm gün farklarının ortalamasını alır.
            cycleLengths.average().roundToInt()
        } else {
            28 // Eğer hesaplanamıyorsa varsayılan değer
        }
        
        // SCS saati hesaplama (yumurtlama günü)
        val scsDate = Calendar.getInstance()
        scsDate.time = lastPeriodDate
        //Kadınlar, ortalama olarak bir sonraki adetlerinden 14 gün önce yumurtlar.
        //Yani: 1 Temmuz + (28 - 14) = 15 Temmuz → Bu gün yumurtlama (SCS) günü.
        scsDate.add(Calendar.DAY_OF_MONTH, averageCycleLength - 14) // Son adet + ortalama döngü - 14 (yumurtlama)
        
        // Riskli dönem başlangıcı (SCS - 3 gün)
        val riskStartDate = Calendar.getInstance()
        riskStartDate.time = scsDate.time
        //Sperm birkaç gün yaşayabildiği için, 1 gün öncesi ve 1 gün sonrası da risklidir.
        riskStartDate.add(Calendar.DAY_OF_MONTH, -1)
        
        // Riskli dönem bitişi (SCS + 1 gün)
        val riskEndDate = Calendar.getInstance()
        riskEndDate.time = scsDate.time
        riskEndDate.add(Calendar.DAY_OF_MONTH, 1)
        
        // SCS saati formatı
        val scsDateText = SimpleDateFormat("dd MMMM yyyy EEEE, 'Saat:' HH 'civarı'", Locale("tr", "TR"))
            .format(scsDate.time)
        binding.tvScsTime.text = scsDateText
        
        // Uyarı metni oluşturma
        val riskStartText = SimpleDateFormat("dd MMMM yyyy EEEE 'Saat:' HH", Locale("tr", "TR"))
            .format(riskStartDate.time)
        val riskEndText = SimpleDateFormat("dd MMMM yyyy EEEE 'Saat:' HH", Locale("tr", "TR"))
            .format(riskEndDate.time)
            
        val warningMessage = "$riskStartText saati ve tarihinden başlayarak $riskEndText saati ve tarihine " +
                "kadar lütfen korununuz. Çünkü bu tarihler arasında girilecek ilişki sonucu hamile kalacak " +
                "olursanız çocuğunuzun sorunlu olma ihtimali vardır. " +
                "Adet ortalamanız ${if (averageCycleLength in 25..35) "sağlıklı bir bayanın olması gereken adet süresi ortalamasına çok yakın. " +
                "Sağlıklı bir anne adayı olabilirsiniz." else "normal değerlerden biraz farklı olabilir, bir doktora danışmanız önerilir."}\n\n" +
                "Buradan sizlere verilen bilgilere güvenebilirsiniz. Sağlığınıza dikkat ettiğiniz için insanlık adına sizlere teşekkür ederiz."
        
        binding.tvWarningContent.text = warningMessage
        
        // Güvenli günler - SCS öncesi
        val safeStart1 = Calendar.getInstance()
        safeStart1.time = lastPeriodDate
        safeStart1.add(Calendar.DAY_OF_MONTH, 5) // Adet sonrası 5 gün
        
        val safeEnd1 = Calendar.getInstance()
        safeEnd1.time = riskStartDate.time
        safeEnd1.add(Calendar.DAY_OF_MONTH, -1)
        
        // Güvenli günler - SCS sonrası
        val safeStart2 = Calendar.getInstance()
        safeStart2.time = riskEndDate.time
        safeStart2.add(Calendar.DAY_OF_MONTH, 1)
        
        val safeEnd2 = Calendar.getInstance()
        safeEnd2.time = lastPeriodDate
        safeEnd2.add(Calendar.DAY_OF_MONTH, averageCycleLength - 1)
        
        // Güvenli günleri göster - Tek bir metin olarak
        val dateFormatShort = SimpleDateFormat("dd MMMM yyyy EEEE 'Saat:' HH", Locale("tr", "TR"))
        val safePeriodText = "${dateFormatShort.format(safeStart2.time)} civarından başlayarak " +
                "${dateFormatShort.format(safeEnd2.time)} saatine kadar hiç bir şekilde hamile kalma ihtimaliniz yoktur."
        
        // Güvenli gün aralığını TextView'a yerleştir
        binding.llSafeDays.removeAllViews()
        
        // Güvenli dönem
        val safePeriodView = layoutInflater.inflate(R.layout.item_safe_day, binding.llSafeDays, false)
        val safePeriodTextView = safePeriodView.findViewById<android.widget.TextView>(R.id.tvSafePeriod)
        safePeriodTextView.text = safePeriodText
        binding.llSafeDays.addView(safePeriodView)
        
        // Tahmini yeni adet tarihi
        val nextPeriodDate = Calendar.getInstance()
        nextPeriodDate.time = lastPeriodDate
        nextPeriodDate.add(Calendar.DAY_OF_MONTH, averageCycleLength)
        
        val nextPeriodText = SimpleDateFormat("dd MMMM yyyy EEEE, 'Saat:' HH 'civarı'", Locale("tr", "TR"))
            .format(nextPeriodDate.time)
        binding.tvNextPeriodDate.text = nextPeriodText
    }
    
    private fun setupAnimations() {
        // Tüm kartlar için animasyon
        val cards = listOf(
            binding.cvTitle,
            binding.cvInfoDate,
            binding.cvWarning,
            binding.cvScsTime,
            binding.cvSafeDays,
            binding.cvNextPeriod
        )
        
        // Her kart için animasyon, sırayla
        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 50f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay((index * 100).toLong())
                .start()
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 