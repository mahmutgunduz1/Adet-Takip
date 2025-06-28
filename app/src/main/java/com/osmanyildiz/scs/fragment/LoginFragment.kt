package com.osmanyildiz.scs.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.osmanyildiz.scs.R
import com.osmanyildiz.scs.databinding.FragmentLoginBinding



class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    // SharedPreferences için sabitler
    private val PREF_NAME = "login_pref"
    private val KEY_EMAIL = "email"
    private val KEY_PASSWORD = "password"
    private val KEY_REMEMBER_ME = "remember_me"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        
        // SharedPreferences'dan kayıtlı bilgileri kontrol et ve form alanlarını doldur
        loadSavedCredentials()
        
        // Kullanıcı zaten giriş yapmışsa ana sayfaya yönlendir
        val currentUser = auth.currentUser
        if (currentUser != null) {
            findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
        }
        
        setupClickListeners()
    }
    
    private fun loadSavedCredentials() {
        val sharedPref = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rememberMe = sharedPref.getBoolean(KEY_REMEMBER_ME, false)
        
        if (rememberMe) {
            val savedEmail = sharedPref.getString(KEY_EMAIL, "")
            val savedPassword = sharedPref.getString(KEY_PASSWORD, "")
            
            binding.etUsername.setText(savedEmail)
            binding.etPassword.setText(savedPassword)
            binding.cbRememberMe.isChecked = true
        }
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            // Giriş işlemi burada yapılacak
            val email = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Giriş başarılı olduğunda yapılacak işlemler
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSignIn.isEnabled = false
            
            // Firebase ile giriş işlemi
            auth.signInWithEmailAndPassword(
                email,
                password
            ).addOnSuccessListener {
                // Beni hatırla seçeneği işaretliyse bilgileri kaydet
                saveLoginCredentials(email, password, binding.cbRememberMe.isChecked)
                
                binding.progressBar.visibility = View.GONE
                binding.btnSignIn.isEnabled = true
                Toast.makeText(requireContext(), "Giriş başarılı", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.btnSignIn.isEnabled = true
                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.tvSignUp.setOnClickListener {
            // Kayıt ekranına geçiş
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }
        
        binding.tvForgotPassword.setOnClickListener {
            // Şifremi unuttum diyaloğunu göster
            showForgotPasswordDialog()
        }
    }
    
    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_forgot_password, null)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.etForgotPasswordEmail)
        val tilEmail = dialogView.findViewById<TextInputLayout>(R.id.tilForgotPasswordEmail)
        
        // Eğer giriş formunda e-posta adresi varsa, diyalogda da göster
        val currentEmail = binding.etUsername.text.toString()
        if (currentEmail.isNotEmpty()) {
            etEmail.setText(currentEmail)
        }
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Şifremi Unuttum")
            .setView(dialogView)
            .setPositiveButton("Şifre Sıfırlama Bağlantısı Gönder", null)
            .setNegativeButton("İptal", null)
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val email = etEmail.text.toString().trim()
                
                if (email.isEmpty()) {
                    tilEmail.error = "E-posta adresinizi girin"
                    return@setOnClickListener
                }
                
                // Firebase ile şifre sıfırlama e-postası gönder
                sendPasswordResetEmail(email, dialog)
            }
        }
        
        dialog.show()
    }
    
    private fun sendPasswordResetEmail(email: String, dialog: AlertDialog) {
        binding.progressBar.visibility = View.VISIBLE
        
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                dialog.dismiss()
                showSuccessMessage("Şifre sıfırlama bağlantısı e-posta adresinize gönderildi")
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                showErrorMessage("Şifre sıfırlama bağlantısı gönderilemedi: ${e.localizedMessage}")
            }
    }
    
    private fun showSuccessMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(R.color.primary_teal, null))
            .setTextColor(resources.getColor(R.color.white, null))
            .show()
    }
    
    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(R.color.error, null))
            .setTextColor(resources.getColor(R.color.white, null))
            .show()
    }
    
    private fun saveLoginCredentials(email: String, password: String, rememberMe: Boolean) {
        val sharedPref = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        
        if (rememberMe) {
            editor.putString(KEY_EMAIL, email)
            editor.putString(KEY_PASSWORD, password)
            editor.putBoolean(KEY_REMEMBER_ME, true)
        } else {
            // Beni hatırla işaretli değilse bilgileri temizle
            editor.clear()
        }
        
        editor.apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}