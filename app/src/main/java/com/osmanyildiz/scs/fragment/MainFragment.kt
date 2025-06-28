package com.osmanyildiz.scs.fragment

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
import com.osmanyildiz.scs.R
import com.osmanyildiz.scs.adapter.PeriodDateAdapter
import com.osmanyildiz.scs.databinding.FragmentMainBinding
import com.osmanyildiz.scs.model.PeriodDate

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
        // Çıkış butonuna tıklama olayı ekle
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
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
        periodDateAdapter = PeriodDateAdapter(
            periodDateList,
            onDeleteClick = { position ->
                deletePeriodDate(position)
            }
        )
        
        binding.rvPeriodDates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = periodDateAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun loadPeriodDatesFromFirebase() {
        // Kullanıcı giriş yapmış mı kontrol et
        val currentUser = auth.currentUser
        if (currentUser == null) {
            updateEmptyState()
            return
        }
        
        // Yükleme göstergesini göster
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
                    val minute = document.getLong("minute")?.toInt()
                    
                    if (date != null) {
                        // Varsayılan döngü süresi 28 gün olarak ayarlandı
                        periodDateList.add(PeriodDate(date, 28, hour, minute))
                        documentIdList.add(document.id) // Belge ID'sini kaydet
                    }
                }
                
                periodDateAdapter.notifyDataSetChanged()
                updateEmptyState()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Veri yüklenirken hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                println(e.localizedMessage)
                binding.progressBar.visibility = View.GONE
                updateEmptyState()
            }
    }
    
    private fun updateEmptyState() {
        if (periodDateList.isEmpty()) {
            binding.rvPeriodDates.visibility = View.GONE
            binding.cvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvPeriodDates.visibility = View.VISIBLE
            binding.cvEmptyState.visibility = View.GONE
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
                    periodDateAdapter.notifyItemRemoved(position)
                    updateEmptyState()
                    
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
            // Yeni adet tarihi ekleme fragment'ına geçiş
            findNavController().navigate(R.id.action_mainFragment_to_addPeriodFragment)
        }
        
        binding.btnAddFirstPeriod.setOnClickListener {
            // Boş durum ekranındaki butona tıklandığında da aynı sayfaya yönlendir
            findNavController().navigate(R.id.action_mainFragment_to_addPeriodFragment)
        }
        
        // Bilgi butonu tıklama olayı
        binding.fabInfo.setOnClickListener {
            navigateToDetailsFragment()
        }
    }
    
    private fun navigateToDetailsFragment() {
        // Detay fragment'a geçiş
        findNavController().navigate(R.id.action_mainFragment_to_detailsFragment)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}