package com.mahmutgunduz.adettakip.fragment

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mahmutgunduz.adettakip.R
import com.mahmutgunduz.adettakip.adapter.NotesAdapter
import com.mahmutgunduz.adettakip.databinding.FragmentMyNotesBinding
import com.mahmutgunduz.adettakip.model.NoteItem
import java.text.SimpleDateFormat
import java.util.*

class MyNotesFragment : Fragment() {

    private var _binding: FragmentMyNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var notesAdapter: NotesAdapter
    private val notesList = mutableListOf<NoteItem>()
    private val documentIdList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase tanımlama
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadNotes()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Notlarım"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onEditClick = { position ->
                editNote(position)
            },
            onDeleteClick = { position ->
                deleteNote(position)
            }
        )

        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notesAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddNote.setOnClickListener {
            showAddNoteDialog()
        }
    }

    private fun loadNotes() {
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

        binding.progressBar.visibility = View.VISIBLE

        // Önce basit sorgu deneyelim (orderBy olmadan)
        firestore.collection("notes")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                
                notesList.clear()
                documentIdList.clear()

                // Verileri listeye ekle
                val tempList = mutableListOf<Pair<NoteItem, String>>()
                
                for (document in documents) {
                    try {
                        val title = document.getString("title") ?: ""
                        val content = document.getString("content") ?: ""
                        val timestamp = document.getTimestamp("timestamp")?.toDate() ?: Date()

                        val noteItem = NoteItem(title, content, timestamp)
                        tempList.add(Pair(noteItem, document.id))
                    } catch (e: Exception) {
                        android.util.Log.e("MyNotesFragment", "Document parse error: ${e.message}")
                    }
                }

                // Timestamp'e göre sırala (en yeni önce)
                tempList.sortByDescending { it.first.timestamp }
                
                // Sıralı listeyi ana listeye aktar
                for (pair in tempList) {
                    notesList.add(pair.first)
                    documentIdList.add(pair.second)
                }

                updateUI()
                android.util.Log.d("MyNotesFragment", "Notlar başarıyla yüklendi: ${notesList.size} adet")
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                
                android.util.Log.e("MyNotesFragment", "Firestore error: ${e.message}", e)
                
                val errorMessage = when {
                    e.message?.contains("PERMISSION_DENIED") == true -> {
                        android.util.Log.e("MyNotesFragment", "Permission denied - User: ${currentUser.uid}")
                        "Firebase kuralları güncelleniyor. Lütfen birkaç dakika bekleyin."
                    }
                    e.message?.contains("UNAVAILABLE") == true -> 
                        "İnternet bağlantınızı kontrol edin."
                    e.message?.contains("FAILED_PRECONDITION") == true ->
                        "Firestore index eksik. Basit sorgulama yapılıyor..."
                    else -> "Notlar yüklenirken hata oluştu: ${e.localizedMessage}"
                }
                
                showErrorMessage(errorMessage)
                
                // Index hatası durumunda basit sorgu dene
                if (e.message?.contains("FAILED_PRECONDITION") == true) {
                    loadNotesSimple()
                }
            }
    }
    
    private fun loadNotesSimple() {
        val currentUser = auth.currentUser ?: return
        
        android.util.Log.d("MyNotesFragment", "Basit sorgu deneniyor...")
        
        firestore.collection("notes")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                notesList.clear()
                documentIdList.clear()

                for (document in documents) {
                    try {
                        val title = document.getString("title") ?: ""
                        val content = document.getString("content") ?: ""
                        val timestamp = document.getTimestamp("timestamp")?.toDate() ?: Date()

                        notesList.add(NoteItem(title, content, timestamp))
                        documentIdList.add(document.id)
                    } catch (e: Exception) {
                        android.util.Log.e("MyNotesFragment", "Document parse error: ${e.message}")
                    }
                }

                updateUI()
                android.util.Log.d("MyNotesFragment", "Basit sorgu başarılı: ${notesList.size} adet")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MyNotesFragment", "Basit sorgu da başarısız: ${e.message}")
                showErrorMessage("Notlar yüklenemedi. Lütfen daha sonra tekrar deneyin.")
                updateUI() // Boş liste göster
            }
    }

    private fun updateUI() {
        if (notesList.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvNotes.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvNotes.visibility = View.VISIBLE
            notesAdapter.updateList(notesList)
        }
    }

    private fun showAddNoteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_note, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etNoteTitle)
        val etContent = dialogView.findViewById<EditText>(R.id.etNoteContent)

        AlertDialog.Builder(requireContext())
            .setTitle("Yeni Not Ekle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()

                if (title.isNotEmpty() && content.isNotEmpty()) {
                    saveNote(title, content)
                } else {
                    showErrorMessage("Başlık ve içerik boş olamaz")
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun saveNote(title: String, content: String) {
        val currentUser = auth.currentUser ?: return

        val noteData = hashMapOf(
            "userId" to currentUser.uid,
            "title" to title,
            "content" to content,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("notes")
            .add(noteData)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                showSuccessMessage("Not başarıyla kaydedildi")
                loadNotes() // Listeyi yenile
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                
                val errorMessage = when {
                    e.message?.contains("PERMISSION_DENIED") == true -> 
                        "Firebase kuralları güncelleniyor. Lütfen birkaç dakika bekleyin."
                    e.message?.contains("UNAVAILABLE") == true -> 
                        "İnternet bağlantınızı kontrol edin."
                    else -> "Not kaydedilirken hata oluştu: ${e.localizedMessage}"
                }
                
                showErrorMessage(errorMessage)
                android.util.Log.e("MyNotesFragment", "Save note error: ${e.message}")
            }
    }

    private fun editNote(position: Int) {
        val note = notesList[position]
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_note, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etNoteTitle)
        val etContent = dialogView.findViewById<EditText>(R.id.etNoteContent)

        etTitle.setText(note.title)
        etContent.setText(note.content)

        AlertDialog.Builder(requireContext())
            .setTitle("Notu Düzenle")
            .setView(dialogView)
            .setPositiveButton("Güncelle") { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()

                if (title.isNotEmpty() && content.isNotEmpty()) {
                    updateNote(position, title, content)
                } else {
                    showErrorMessage("Başlık ve içerik boş olamaz")
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun updateNote(position: Int, title: String, content: String) {
        val documentId = documentIdList[position]

        val updateData = hashMapOf(
            "title" to title,
            "content" to content,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("notes").document(documentId)
            .update(updateData as Map<String, Any>)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                showSuccessMessage("Not başarıyla güncellendi")
                loadNotes() // Listeyi yenile
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                showErrorMessage("Not güncellenirken hata oluştu: ${e.localizedMessage}")
            }
    }

    private fun deleteNote(position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Notu Sil")
            .setMessage("Bu notu silmek istediğinize emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                val documentId = documentIdList[position]

                binding.progressBar.visibility = View.VISIBLE

                firestore.collection("notes").document(documentId)
                    .delete()
                    .addOnSuccessListener {
                        binding.progressBar.visibility = View.GONE
                        showSuccessMessage("Not başarıyla silindi")
                        loadNotes() // Listeyi yenile
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        showErrorMessage("Not silinirken hata oluştu: ${e.localizedMessage}")
                    }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showSuccessMessage(message: String) {
        val view = view ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(resources.getColor(R.color.success, null))
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}