package com.mahmutgunduz.adettakip.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

import com.mahmutgunduz.adettakip.adapter.PeriodDateAdapter

import com.mahmutgunduz.adettakip.model.PeriodDate
import java.util.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import android.widget.TextView
import kotlin.math.roundToInt
import android.widget.CheckBox
import android.widget.Button
import android.content.Context
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.mahmutgunduz.adettakip.R
import com.mahmutgunduz.adettakip.databinding.FragmentMainBinding


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var periodDateAdapter: PeriodDateAdapter
    private val periodDateList = mutableListOf<PeriodDate>()
    private val documentIdList = mutableListOf<String>() // Firestore belge ID'lerini saklamak için

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase tanımlama
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()



        setupToolbar()
        setupRecyclerView()
        loadPeriodDatesFromFirebase()
        setupClickListeners()
    }

    private fun setupToolbar() {
        // Menü butonuna tıklama olayı ekle
        binding.btnMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(binding.navView)) {
                binding.drawerLayout.closeDrawer(binding.navView)
            } else {
                binding.drawerLayout.openDrawer(binding.navView)
            }
        }

        // Toolbar'a menü ekle
        val menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        showLogoutConfirmationDialog()
                        true
                    }

                    else -> false
                }
            }
        }

        // MenuProvider'ı lifecycle'a bağla
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Çıkış Yap")
            .setMessage("Hesabınızdan çıkış yapmak istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                logoutUser()
            }
            .setNegativeButton("Hayır", null)
            .setIcon(R.drawable.ic_logout)
            .show()
    }

    private fun logoutUser() {
        // Firebase Authentication'dan çıkış yap
        auth.signOut()

        // Kullanıcıya bildir
        showSuccessMessage("Çıkış yapıldı")

        // Login ekranına yönlendir
        findNavController().navigate(R.id.action_mainFragment_to_loginFragment)
    }

    private fun setupRecyclerView() {
        periodDateAdapter = PeriodDateAdapter(emptyList(), onDeleteClick = { position ->
            deletePeriodDate(position)
        })

        binding.rvPeriodDates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = periodDateAdapter
            setHasFixedSize(true)
        }
    }


    private fun loadPeriodDatesFromFirebase() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showErrorMessage("Lütfen önce giriş yapın")
            // Login sayfasına yönlendir
            findNavController().navigate(R.id.action_mainFragment_to_loginFragment)
            return
        }

        // İnternet bağlantısı kontrolü
        if (!com.mahmutgunduz.adettakip.utils.NetworkUtils.isNetworkAvailable(requireContext())) {
            showErrorMessage("İnternet bağlantınızı kontrol edin")
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        // Debug için log ekle
        android.util.Log.d("MainFragment", "Veri çekme başladı - User ID: ${currentUser.uid}")

        // Index aktif - normal sorgu ile tarih sıralı veri çek
        firestore.collection("periodDates")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("MainFragment", "Firestore başarılı - Döküman sayısı: ${documents.size()}")
                
                periodDateList.clear()
                documentIdList.clear()
                val dateList = mutableListOf<Date>()

                for (document in documents) {
                    android.util.Log.d("MainFragment", "Döküman ID: ${document.id}, Data: ${document.data}")
                    
                    val date = document.getDate("date")
                    val hour = document.getLong("hour")?.toInt()

                    if (date != null) {
                        periodDateList.add(PeriodDate(date, 28, hour))
                        documentIdList.add(document.id)
                        dateList.add(date)
                        android.util.Log.d("MainFragment", "Tarih eklendi: $date")
                    } else {
                        android.util.Log.w("MainFragment", "Null tarih bulundu: ${document.id}")
                    }
                }

                android.util.Log.d("MainFragment", "Toplam eklenen tarih sayısı: ${periodDateList.size}")

                // Boş durum kontrolü
                if (dateList.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    
                    // RecyclerView'i güncelle
                    updateRecyclerView()
                    
                    // Adapter'a veri gönderildiğini kontrol et
                    android.util.Log.d("MainFragment", "RecyclerView güncellendi - Adapter item count: ${periodDateAdapter.itemCount}")

                    // En az 2 tarih girilmişse döngü hesaplamalarını yap
                    if (dateList.size >= 2) {
                        checkShouldShowWarningDialog(dateList)
                    } else {
                        // Yeterli veri yoksa bilgi kartlarını gizle
                        binding.cvInfoDate.visibility = View.GONE
                        binding.cvWarning.visibility = View.GONE
                        binding.cvScsTime.visibility = View.GONE
                        binding.cvSafeDays.visibility = View.GONE
                        binding.cvNextPeriod.visibility = View.GONE
                        
                        showWelcomeMessage("Döngü hesaplaması için en az 2 adet tarihi girmelisiniz.")
                    }
                }

                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MainFragment", "Firestore hatası: ${e.message}", e)
                
                val errorMessage = when {
                    e.message?.contains("PERMISSION_DENIED") == true -> 
                        "Veri erişim izni reddedildi. Lütfen tekrar giriş yapın."
                    e.message?.contains("UNAVAILABLE") == true -> 
                        "Sunucuya bağlanılamıyor. İnternet bağlantınızı kontrol edin."
                    e.message?.contains("DEADLINE_EXCEEDED") == true -> 
                        "Bağlantı zaman aşımına uğradı. Tekrar deneyin."
                    e.message?.contains("FAILED_PRECONDITION") == true ->
                        "Firestore indeksi eksik. Lütfen geliştirici ile iletişime geçin."
                    else -> "Veri yüklenirken hata oluştu: ${e.localizedMessage}"
                }
                
                showErrorMessage(errorMessage)
                
                // Kimlik doğrulama hatası durumunda kullanıcıyı login sayfasına yönlendir
                if (e.message?.contains("PERMISSION_DENIED") == true || 
                    e.message?.contains("UNAUTHENTICATED") == true) {
                    auth.signOut()
                    findNavController().navigate(R.id.action_mainFragment_to_loginFragment)
                }
                
                binding.progressBar.visibility = View.GONE
            }
    }


    private fun checkShouldShowWarningDialog(dateList: List<Date>) {
        // SharedPreferences'dan "bir daha gösterme" seçeneğini kontrol et
        val sharedPrefs = requireActivity().getSharedPreferences("SCSPrefs", Context.MODE_PRIVATE)
        val shouldShowWarning = sharedPrefs.getBoolean("shouldShowWarning", true)

        if (shouldShowWarning) {
            // Uyarı dialogunu göster
            showWarningDialog(dateList)
        } else {
            // Direkt hesaplamaya geç
            calculateAndShowCycleInfo(dateList)
            showCycleInfoUI()
        }
    }


    private fun showWarningDialog(dateList: List<Date>) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Kullanıcı giriş yapmamışsa uyarı ver veya direkt hesaplama yap
            calculateAndShowCycleInfo(dateList)
            showCycleInfoUI()
            return
        }

        firestore.collection("users")
            .document(currentUser.uid) // Giriş yapan kullanıcının UID'si ile veri çek
            .get()
            .addOnSuccessListener { document ->
                var fullName = "Kullanıcı"
                var phoneNumber = "05*********"

                if (document.exists()) {
                    fullName = document.getString("fullName") ?: fullName
                    phoneNumber = document.getString("phoneNumber") ?: phoneNumber
                }

                val builder = AlertDialog.Builder(requireContext())
                val dialogView = layoutInflater.inflate(R.layout.dialog_scs_warning, null)
                builder.setView(dialogView)
                val alertDialog = builder.create()
                alertDialog.setCancelable(false)

                val warningMessage = dialogView.findViewById<TextView>(R.id.tvWarningMessage)
                val phoneNumberMasked = if (phoneNumber.length >= 7)
                    phoneNumber.replaceRange(3, 7, "****")
                else
                    phoneNumber

                warningMessage.text =
                    " Sayın $fullName, sizden SCS saati ile ilgili uyarı bilgileriniz bu metni okuduktan sonraki bölümdedir.\n\n" +
                            "SCS saati SCS=AOs(Saat olarak adet ortalaması)-365 yardımı ile hesaplanmıştır. Bu hesabı sizler de kendiniz yapabilirsiniz.\n\n" +
                            "Eğer bu bilgileri kullanmaktan memnun iseniz lütfen çevrenizdeki anne adaylarına bu haklarını kullanabilmeleri için bilgi veriniz."

                val checkBoxDontShowAgain = dialogView.findViewById<CheckBox>(R.id.cbDontShowAgain)
                val btnContinue = dialogView.findViewById<Button>(R.id.btnContinue)
                binding.tvUserName.text = fullName


                btnContinue.setOnClickListener {
                    val sharedPrefs =
                        requireActivity().getSharedPreferences("SCSPrefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit()
                        .putBoolean("shouldShowWarning", !checkBoxDontShowAgain.isChecked).apply()

                    alertDialog.dismiss()

                    calculateAndShowCycleInfo(dateList)
                    showCycleInfoUI()
                }

                alertDialog.show()

                val displayMetrics = resources.displayMetrics
                val width = (displayMetrics.widthPixels * 0.9).toInt()
                alertDialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Veriler alınırken hata oluştu: ${it.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                // Hata durumunda da hesaplamaya devam edelim
                calculateAndShowCycleInfo(dateList)
                showCycleInfoUI()
            }
    }


    // Döngü bilgisi UI elemanlarını gösterir
    private fun showCycleInfoUI() {
        binding.cvInfoDate.visibility = View.VISIBLE
        binding.cvWarning.visibility = View.VISIBLE
        binding.cvScsTime.visibility = View.VISIBLE
        binding.cvSafeDays.visibility = View.VISIBLE
        binding.cvNextPeriod.visibility = View.VISIBLE
    }


    // Adet döngüsü hesaplamalarını yapr ve Kullanıcıya gösteren fonkisyon


    private fun calculateAndShowCycleInfo(periodDates: List<Date>) {
        // 1. En son adet tarihini al (liste azalan sırada olduğu için ilk eleman)
        val lastPeriodDate = periodDates[0]

        // 2. Bilgi tarihi - bugünün tarihini ve saatini göster
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd MMMM yyyy EEEE, 'Saat:' HH:mm:ss", Locale("tr", "TR"))
        binding.tvInfoDate.text = dateFormat.format(currentDate)
        println(dateFormat.format(currentDate))

        // 3. Ortalama döngü uzunluğunu hesapla
        //Adet döngüsü uzunluğunu hesaplamak için her iki tarih arası farkları bu listeye atacağız
        val cycleLengths = mutableListOf<Long>()

        // 4. Tüm ardışık tarih çiftleri için döngü uzunluklarını hesapla
        // Tarihler arasındaki gün farkını hesaplarken saat bilgisine de dikkat ediuyoruz...
        // until anahtar kelimesi, sondaki değeri dahil etmeden 0'dan başlayıp size - 1’e kadar bir sayı listesi oluşturur.

        for (i in 0 until periodDates.size - 1) {
            // İki tarih arasındaki milisaniye farkını güne çevir
            //1 gün = 86.400.000 milisaniyedir.
            val daysBetween =
                TimeUnit.MILLISECONDS.toDays(periodDates[i].time - periodDates[i + 1].time)

            // 5. Çok kısa süreli döngüleri filtrele (muhtemelen hatalı veri)
            if (daysBetween > 10) {
                cycleLengths.add(daysBetween)
            }
        }

        // 6. Ortalama döngü uzunluğunu hesapla ve 20-35 gün aralığında tut
        val averageCycleLength = if (cycleLengths.isNotEmpty()) {
            //   roundToInt: Ondalıklı sayıyı en yakın tam sayıya yuvarlar
            //  coerceIn(20, 35): Sonucun minimum 20, maksimum 35 olmasını sağlar
            cycleLengths.average().roundToInt().coerceIn(20, 35)
        } else {
            28 // Eğer hesaplanamıyorsa varsayılan değer
        }

        // 7. SCS saati hesaplama (yumurtlama günü)
        // Son adet tarihine ortalama döngü uzunluğundan 14 gün çıkararak hesaplanır
        val scsDate = Calendar.getInstance()
        scsDate.time = lastPeriodDate
        scsDate.add(Calendar.DAY_OF_MONTH, averageCycleLength - 14)

        // 8. Riskli dönem başlangıcı (SCS - 5 gün)
        // Yumurtlama öncesi 5 gün de riskli dönem içerisindedir
        val riskStartDate = Calendar.getInstance()
        riskStartDate.time = scsDate.time
        riskStartDate.add(Calendar.DAY_OF_MONTH, -5)

        // 9. Riskli dönem bitişi (SCS + 1 gün)
        // Yumurtlama sonrası 1 gün de riskli dönem içerisindedir
        val riskEndDate = Calendar.getInstance()
        riskEndDate.time = scsDate.time
        riskEndDate.add(Calendar.DAY_OF_MONTH, 1)

        // 10. SCS saati formatını oluştur ve göster
        // Saat bilgisini de ekleyerek göster
        val scsDateText =
            SimpleDateFormat("dd MMMM yyyy EEEE, 'Saat:' HH 'civarı'", Locale("tr", "TR"))
                .format(scsDate.time)
        binding.tvScsTime.text = scsDateText

        // 11. Riskli dönem için uyarı metni oluştur
        val riskStartText = SimpleDateFormat("dd MMMM yyyy EEEE 'Saat:' HH", Locale("tr", "TR"))
            .format(riskStartDate.time)
        val riskEndText = SimpleDateFormat("dd MMMM yyyy EEEE 'Saat:' HH", Locale("tr", "TR"))
            .format(riskEndDate.time)

        // 12. Uyarı metnini oluştur ve sağlık durumu hakkında bilgi ver
        val warningMessage =
            "Risk penceresi: Ovülasyon döneminde (yumurtlama) ve öncesindeki 5 gün hamile kalma ihtimalinin yüksek olduğu dönemdir. " +
                    "Bu dönemde korunmasız ilişki, istenmeyen gebelik riskini artırır. Ayrıca, genetik faktörler de döngünüzü etkileyebilir. " +
                    "Ailenizde erken menopoz veya polikistik over sendromu gibi durumlar varsa, döngünüz düzensiz olabilir ve tahminler daha az güvenilir olabilir. " +
                    "Her zaman ek korunma yöntemleri kullanmanız önerilir.\n\n" +
                    "Riskli dönem: $riskStartText - $riskEndText"

        // 13. Uyarı metnini göster
        binding.tvWarningContent.text = warningMessage


        // 14. Güvenli günler hesaplama - İlk güvenli dönem (adet sonrası 5. günden riskli dönem başlangıcına kadar)
        val safeStart1 = Calendar.getInstance()
        safeStart1.time = lastPeriodDate
        safeStart1.add(Calendar.DAY_OF_MONTH, 5) // Adet sonrası 5 gün

        val safeEnd1 = Calendar.getInstance()
        safeEnd1.time = riskStartDate.time
        safeEnd1.add(Calendar.DAY_OF_MONTH, -1)

        // 15. Güvenli günler hesaplama - İkinci güvenli dönem (riskli dönem bitişinden bir sonraki adet başlangıcına kadar)
        val safeStart2 = Calendar.getInstance()
        safeStart2.time = riskEndDate.time
        safeStart2.add(Calendar.DAY_OF_MONTH, 1)

        val safeEnd2 = Calendar.getInstance()
        safeEnd2.time = lastPeriodDate
        safeEnd2.add(Calendar.DAY_OF_MONTH, averageCycleLength - 1)

        // 16. Güvenli günleri formatlayarak göster - Saat bilgisini de ekleyerek
        val dateFormatShort = SimpleDateFormat("dd MMMM yyyy EEEE 'Saat:' HH", Locale("tr", "TR"))
        val safePeriodText = "1. Güvenli Dönem: ${dateFormatShort.format(safeStart1.time)} - ${
            dateFormatShort.format(safeEnd1.time)
        }\n" +
                "2. Güvenli Dönem: ${dateFormatShort.format(safeStart2.time)} - ${
                    dateFormatShort.format(
                        safeEnd2.time
                    )
                }"

        // 17. Güvenli günleri UI'da göster
        binding.llSafeDays.removeAllViews()
        val safePeriodView =
            layoutInflater.inflate(R.layout.item_safe_day, binding.llSafeDays, false)
        val safePeriodTextView = safePeriodView.findViewById<TextView>(R.id.tvSafePeriod)
        safePeriodTextView.text = safePeriodText
        binding.llSafeDays.addView(safePeriodView)

        // 18. Tahmini yeni adet tarihini hesapla (son adet + ortalama döngü uzunluğu)
        val nextPeriodDate = Calendar.getInstance()
        nextPeriodDate.time = lastPeriodDate
        nextPeriodDate.add(Calendar.DAY_OF_MONTH, averageCycleLength)

        // 19. Tahmini yeni adet tarihini göster - Saat bilgisini de ekleyerek
        val nextPeriodText =
            SimpleDateFormat("dd MMMM yyyy EEEE, 'Saat:' HH 'civarı'", Locale("tr", "TR"))
                .format(nextPeriodDate.time)
        binding.tvNextPeriodDate.text = nextPeriodText
    }

    // RecyclerView'i güncelle - sadece son 3 tarihi göster
    private fun updateRecyclerView() {
        android.util.Log.d("MainFragment", "updateRecyclerView çağrıldı - periodDateList size: ${periodDateList.size}")
        
        val lastThreeDates = periodDateList.take(3) // Zaten tarihler azalan sırada olduğu için ilk 3'ü al
        android.util.Log.d("MainFragment", "Son 3 tarih alındı - lastThreeDates size: ${lastThreeDates.size}")
        
        periodDateAdapter.updateList(lastThreeDates)
        
        // RecyclerView'in görünür olduğundan emin ol
        binding.rvPeriodDates.visibility = View.VISIBLE
        
        android.util.Log.d("MainFragment", "Adapter güncellendi - yeni item count: ${periodDateAdapter.itemCount}")
    }

    private fun deletePeriodDate(position: Int) {
        // RecyclerView'daki pozisyon ile gerçek liste pozisyonunu eşleştir
        // Sadece son 3 tarih gösterildiğinden, position değeri doğrudan kullanılamaz
        if (position >= 0 && position < periodDateList.size) {
            val documentId = documentIdList[position]

            // Yükleme göstergesini göster
            binding.progressBar.visibility = View.VISIBLE

            // Firebase'den veriyi sil
            firestore.collection("periodDates").document(documentId)
                .delete()
                .addOnSuccessListener {
                    // Başarıyla silindiğinde listeden kaldır
                    periodDateList.removeAt(position)
                    documentIdList.removeAt(position)

                    // RecyclerView'i güncelle
                    updateRecyclerView()

                    showSuccessMessage("Adet tarihi silindi")
                    binding.progressBar.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    showErrorMessage("Silme işlemi başarısız: ${e.localizedMessage}")
                    binding.progressBar.visibility = View.GONE
                }
        }
    }

    private fun showSuccessMessage(message: String) {
        val view = view ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(resources.getColor(R.color.primary_teal, null))
            .setTextColor(resources.getColor(R.color.white, null))
            .show()
    }

    private fun showErrorMessage(message: String) {
        val view = view ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(resources.getColor(R.color.error, null))
            .setTextColor(resources.getColor(R.color.white, null))
            .show()
    }

    private fun setupClickListeners() {
        binding.fabAddPeriod.setOnClickListener {
            // Animasyon efekti
            it.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
            
            // Yeni adet tarihi ekleme fragment'ına geçiş
            findNavController().navigate(R.id.action_mainFragment_to_addPeriodFragment)
        }

        // Bilgi butonu tıklama olayı
        binding.fabInfo.setOnClickListener {
            navigateToDetailsFragment()
        }


        // Navigation Drawer menü öğeleri
        binding.layoutFaq.setOnClickListener {
            //soru cevap alanına tıklanınca faq sayfasına geçiş
            findNavController().navigate(R.id.action_mainFragment_to_faqFragment)

            binding.drawerLayout.closeDrawer(binding.navView)
        }

        binding.layoutNotes.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_myNotesFragment)
            binding.drawerLayout.closeDrawer(binding.navView)
        }

        binding.layoutSignOut.setOnClickListener {
            binding.drawerLayout.closeDrawer(binding.navView)
            showLogoutConfirmationDialog()
        }

        // Kullanıcı bilgilerini göster
        updateUserInfo()
        
        // Boş durum kartlarına click listener ekle
        setupEmptyStateClickListeners()
    }

    private fun updateUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {

            binding.tvUserEmail.text = currentUser.email ?: ""
        }
    }

    private fun showInfoMessage(message: String) {
        val view = view ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(resources.getColor(R.color.info, null))
            .setTextColor(resources.getColor(R.color.white, null))
            .show()
    }

    private fun showEmptyState() {
        // Fragment destroy olmuşsa işlem yapma
        if (_binding == null || !isAdded) return
        
        // Boş durum layout'unu göster
        try {
            val emptyStateLayout = binding.root.findViewById<View>(R.id.layoutEmptyState)
            emptyStateLayout?.visibility = View.VISIBLE
            
            // Diğer kartları gizle
            binding.cvRecyclerView.visibility = View.GONE
            binding.cvInfoDate.visibility = View.GONE
            binding.cvWarning.visibility = View.GONE
            binding.cvScsTime.visibility = View.GONE
            binding.cvSafeDays.visibility = View.GONE
            binding.cvNextPeriod.visibility = View.GONE
            
            // Kullanıcı adını güncelle
            updateWelcomeMessage()
            
            // Animasyon ekle
            startArrowAnimation()
        } catch (e: Exception) {
            android.util.Log.e("MainFragment", "showEmptyState error: ${e.message}")
        }
    }

    private fun hideEmptyState() {
        // Fragment destroy olmuşsa işlem yapma
        if (_binding == null || !isAdded) return
        
        // Boş durum layout'unu gizle
        try {
            val emptyStateLayout = binding.root.findViewById<View>(R.id.layoutEmptyState)
            emptyStateLayout?.visibility = View.GONE
            
            // RecyclerView kartını göster
            binding.cvRecyclerView.visibility = View.VISIBLE
        } catch (e: Exception) {
            android.util.Log.e("MainFragment", "hideEmptyState error: ${e.message}")
        }
    }

    private fun updateWelcomeMessage() {
        // Fragment destroy olmuşsa işlem yapma
        if (_binding == null || !isAdded) return
        
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Kullanıcı profil bilgilerini al
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    // Fragment hala aktifse devam et
                    if (_binding == null || !isAdded) return@addOnSuccessListener
                    
                    val fullName = if (document.exists()) {
                        document.getString("fullName") ?: "Kullanıcı"
                    } else {
                        "Kullanıcı"
                    }
                    
                    // Hoşgeldin mesajını güncelle
                    try {
                        val welcomeTitle = view?.findViewById<TextView>(R.id.tvWelcomeTitle)
                        val welcomeMessage = view?.findViewById<TextView>(R.id.tvWelcomeMessage)
                        
                        welcomeTitle?.text = "Hoşgeldiniz $fullName!"
                        welcomeMessage?.text = "Adet takip uygulamasına hoşgeldiniz. İlk adet tarihinizi ekleyerek başlayın."
                    } catch (e: Exception) {
                        android.util.Log.e("MainFragment", "updateWelcomeMessage error: ${e.message}")
                    }
                }
                .addOnFailureListener {
                    // Fragment hala aktifse devam et
                    if (_binding == null || !isAdded) return@addOnFailureListener
                    
                    // Hata durumunda varsayılan mesaj
                    try {
                        val welcomeTitle = view?.findViewById<TextView>(R.id.tvWelcomeTitle)
                        welcomeTitle?.text = "Hoşgeldiniz!"
                    } catch (e: Exception) {
                        android.util.Log.e("MainFragment", "updateWelcomeMessage error: ${e.message}")
                    }
                }
        }
    }

    private fun startArrowAnimation() {
        try {
            // Fragment destroy olmuşsa animasyonu başlatma
            if (_binding == null || !isAdded) return
            
            val arrowView = view?.findViewById<ImageView>(R.id.ivArrowDown)
            
            // Yukarı aşağı animasyon
            arrowView?.animate()
                ?.translationY(20f)
                ?.setDuration(1000)
                ?.withEndAction {
                    // Fragment hala aktifse devam et
                    if (_binding != null && isAdded) {
                        arrowView.animate()
                            .translationY(0f)
                            .setDuration(1000)
                            .withEndAction {
                                // Animasyonu tekrarla - fragment hala aktifse
                                if (_binding != null && isAdded) {
                                    try {
                                        val emptyStateLayout = binding.root.findViewById<View>(R.id.layoutEmptyState)
                                        if (emptyStateLayout?.visibility == View.VISIBLE) {
                                            startArrowAnimation()
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainFragment", "Animation repeat error: ${e.message}")
                                    }
                                }
                            }
                            .start()
                    }
                }
                ?.start()
        } catch (e: Exception) {
            android.util.Log.e("MainFragment", "startArrowAnimation error: ${e.message}")
        }
    }

    private fun showWelcomeMessage(message: String) {
        val view = view ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(R.color.primary_teal, null))
            .setTextColor(resources.getColor(R.color.white, null))
            .setAction("Ekle") {
                // + butonuna tıklama işlemi
                binding.fabAddPeriod.performClick()
            }
            .show()
    }

    private fun setupEmptyStateClickListeners() {
        // Fragment destroy olmuşsa işlem yapma
        if (_binding == null || !isAdded) return
        
        // Boş durum kartlarına click listener ekle
        try {
            view?.findViewById<View>(R.id.cvGetStarted)?.setOnClickListener {
                // Fragment hala aktifse devam et
                if (_binding != null && isAdded) {
                    // + butonuna tıklama efekti
                    binding.fabAddPeriod.performClick()
                }
            }
            
            view?.findViewById<View>(R.id.cvWelcome)?.setOnClickListener {
                // Fragment hala aktifse devam et
                if (_binding != null && isAdded) {
                    // Hoşgeldin kartına tıklandığında bilgi göster
                    showWelcomeMessage("İlk adet tarihinizi ekleyerek döngü takibine başlayın!")
                }
            }
            
            view?.findViewById<View>(R.id.cvFeatures)?.setOnClickListener {
                // Fragment hala aktifse devam et
                if (_binding != null && isAdded) {
                    // Özellikler kartına tıklandığında detay sayfasına git
                    navigateToDetailsFragment()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainFragment", "setupEmptyStateClickListeners error: ${e.message}")
        }
    }

    private fun showQuickTips() {
        val tips = listOf(
            "💡 İpucu: Düzenli adet takibi sağlığınız hakkında önemli bilgiler verir.",
            "📅 İpucu: En az 3 ay düzenli takip yaparak daha doğru tahminler alabilirsiniz.",
            "⏰ İpucu: Adet tarihinizi her ay aynı saatlerde kaydetmeye çalışın.",
            "🔔 İpucu: Düzensiz döngüleriniz varsa doktorunuza danışın.",
            "📊 İpucu: Uygulamadaki verilerinizi doktorunuzla paylaşabilirsiniz."
        )
        
        val randomTip = tips.random()
        showWelcomeMessage(randomTip)
    }

    private fun checkForAppUpdates() {
        // Uygulama güncellemesi kontrolü (gelecekte eklenebilir)
        val currentVersion = "1.0.0"
        binding.tvVersion.text = "Versiyon $currentVersion"
    }

    private fun showDataSummary() {
        if (periodDateList.isNotEmpty()) {
            val totalDates = periodDateList.size
            val oldestDate = periodDateList.maxByOrNull { it.date }?.date
            val newestDate = periodDateList.minByOrNull { it.date }?.date
            
            if (oldestDate != null && newestDate != null) {
                val daysBetween = TimeUnit.MILLISECONDS.toDays(newestDate.time - oldestDate.time)
                val summary = "📊 Toplam $totalDates adet tarihi kaydettiniz. " +
                        "İlk kayıt: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(oldestDate)}, " +
                        "Son kayıt: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(newestDate)} " +
                        "($daysBetween gün takip)"
                
                showWelcomeMessage(summary)
            }
        }
    }

    private fun navigateToDetailsFragment() {
        // Detay fragment'a geçiş
        findNavController().navigate(R.id.action_mainFragment_to_detailsFragment)
    }

    override fun onResume() {
        super.onResume()
        // Sayfa her açıldığında verileri yeniden yükle
        loadPeriodDatesFromFirebase()
        
        // Uygulama güncellemesi kontrolü
        checkForAppUpdates()
        
        // Rastgele ipucu göster (sadece boş durumdaysa)
        if (periodDateList.isEmpty()) {
            // 3 saniye sonra ipucu göster
            binding.root.postDelayed({
                try {
                    // Fragment hala aktifse devam et
                    if (_binding != null && isAdded) {
                        val emptyStateLayout = binding.root.findViewById<View>(R.id.layoutEmptyState)
                        if (emptyStateLayout?.visibility == View.VISIBLE) {
                            showQuickTips()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainFragment", "onResume tips error: ${e.message}")
                }
            }, 3000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // Animasyonları durdur
        try {
            val arrowView = view?.findViewById<ImageView>(R.id.ivArrowDown)
            arrowView?.animate()?.cancel()
        } catch (e: Exception) {
            android.util.Log.e("MainFragment", "onDestroyView animation cancel error: ${e.message}")
        }
        
        _binding = null
    }
    

}