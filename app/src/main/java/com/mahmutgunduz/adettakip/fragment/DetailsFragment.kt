package com.mahmutgunduz.adettakip.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mahmutgunduz.adettakip.R
import com.mahmutgunduz.adettakip.databinding.FragmentDetailsBinding

import com.mahmutgunduz.adettakip.adapter.PeriodDateAdapter

import com.mahmutgunduz.adettakip.model.PeriodDate

class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var periodDateAdapter: PeriodDateAdapter
    private val periodDateList = mutableListOf<PeriodDate>()
    private val documentIdList = mutableListOf<String>() // Firestore belge ID'lerini saklamak için

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
        setupRecyclerView()
        loadAllPeriodDates()

    }
    
    private fun setupToolbar() {

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
    }
    
    private fun setupRecyclerView() {
        periodDateAdapter = PeriodDateAdapter(emptyList(), onDeleteClick = { position ->
            deletePeriodDate(position)
        })

        binding.rvAllPeriodDates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = periodDateAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun loadAllPeriodDates() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Lütfen önce giriş yapın")
            findNavController().popBackStack()
            return
        }

        // Yükleniyor animasyonu göster
        binding.progressBar.visibility = View.VISIBLE
        
        // Firestore'dan verileri çek
        firestore.collection("periodDates")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                periodDateList.clear()
                documentIdList.clear()

                for (document in documents) {
                    val date = document.getDate("date")
                    val hour = document.getLong("hour")?.toInt()


                    if (date != null) {
                        // Varsayılan döngü süresi 28 gün olarak ayarlandı
                        periodDateList.add(PeriodDate(date, 28, hour))
                        documentIdList.add(document.id) // Belge ID'sini kaydet
                    }
                }

                // Tüm tarihleri göster - en son eklenen tarih en üstte
                periodDateAdapter.updateList(periodDateList)
                binding.progressBar.visibility = View.GONE
                
                // Eğer en az 2 tarih varsa, döngü hesaplamalarını göster
            }
            .addOnFailureListener { e ->
                showToast("Veri yüklenirken hata: ${e.localizedMessage}")
                binding.progressBar.visibility = View.GONE

            }
    }
    
    private fun deletePeriodDate(position: Int) {
        if (position in 0 until periodDateList.size && position in 0 until documentIdList.size) {
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
                    periodDateAdapter.updateList(periodDateList)
                    

                    
                    showToast("Adet tarihi silindi")
                    binding.progressBar.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    showToast("Silme işlemi başarısız: ${e.localizedMessage}")
                    binding.progressBar.visibility = View.GONE
                }
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