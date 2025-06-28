package com.mahmutgunduz.adettakip.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.mahmutgunduz.adettakip.R
import com.mahmutgunduz.adettakip.databinding.FragmentSignUpBinding


class SignUpFragment : Fragment() {

    // Firebase ve view binding değişkenleri
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Fragment görünümü oluşturuluyor
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Firebase servisleri başlatılıyor
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Kayıt ol butonuna tıklandığında
        binding.btnSignUp.setOnClickListener {
            // Form bilgileri alınıyor
            val fullName = binding.etFullName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val phoneNumber = binding.etPhoneNumber.text.toString()
            
            println(
                "Full Name: $fullName, Email: $email, Password: $password, Confirm Password: $confirmPassword, Phone Number: $phoneNumber"
            )

            // Form alanları boş mu kontrol ediliyor
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // İnternet bağlantısı kontrolü
            if (!com.mahmutgunduz.adettakip.utils.NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(), "İnternet bağlantınızı kontrol edin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Şifreler eşleşiyor mu kontrol ediliyor
            if (password != confirmPassword) {
                Toast.makeText(requireContext(), getString(R.string.passwords_not_match), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kullanım koşulları kabul edildi mi kontrol ediliyor
            if (!binding.cbTerms.isChecked) {
                Toast.makeText(requireContext(), getString(R.string.accept_terms), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            try {
                // İlerleme çubuğu gösteriliyor ve buton devre dışı bırakılıyor
                binding.progressBar.visibility = View.VISIBLE
                binding.btnSignUp.isEnabled = false
                
                // Telefon numarası formatlanıyor (ülke kodu ekleniyor)
                var formattedPhoneNumber = phoneNumber
                if (!phoneNumber.startsWith("+")) {
                    formattedPhoneNumber = "+90$phoneNumber" // Türkiye ülke kodu ekleniyor
                }
                
                // Direkt kullanıcı oluşturma işlemi yapılıyor (telefon doğrulama olmadan)
                createUserWithEmailAndPassword(fullName, email, password, formattedPhoneNumber)
            } catch (e: Exception) {
                // Hata durumunda kullanıcıya bilgi veriliyor
                binding.progressBar.visibility = View.GONE
                binding.btnSignUp.isEnabled = true
                Toast.makeText(requireContext(), "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                println("Hata: ${e.message}")
                e.printStackTrace()
            }
        }

        // Giriş yap butonuna tıklandığında
        binding.tvSignIn.setOnClickListener {
            // Giriş ekranına yönlendiriliyor
            findNavController().popBackStack()
        }
    }
    
    // E-posta ve şifre ile kullanıcı oluşturan fonksiyon
    private fun createUserWithEmailAndPassword(fullName: String, email: String, password: String, phoneNumber: String) {
        try {
            println("E-posta ve şifre ile kullanıcı oluşturuluyor")
            
            // Firebase ile e-posta ve şifre kullanıcısı oluşturuluyor
            auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
                val userId = auth.currentUser?.uid

                if (userId != null) {
                    println("Kullanıcı oluşturuldu, Firestore'a kaydediliyor")
                    
                    // Kullanıcı bilgileri Firestore'a kaydediliyor
                    val userMap = hashMapOf(
                        "fullName" to fullName,
                        "email" to email,
                        "phoneNumber" to phoneNumber
                    )
                    
                    firestore.collection("users").document(userId).set(userMap)
                        .addOnSuccessListener {
                            println("Kullanıcı bilgileri Firestore'a kaydedildi")
                            
                            binding.progressBar.visibility = View.GONE
                            binding.btnSignUp.isEnabled = true
                            
                            Toast.makeText(requireContext(), getString(R.string.registration_success), Toast.LENGTH_SHORT).show()
                            
                            // Oturum kapatılıyor ve giriş ekranına yönlendiriliyor
                            auth.signOut()
                            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
                        }
                        .addOnFailureListener { e ->
                            println("Firestore kayıt hatası: ${e.message}")
                            
                            binding.progressBar.visibility = View.GONE
                            binding.btnSignUp.isEnabled = true
                            
                            val errorMessage = when {
                                e.message?.contains("PERMISSION_DENIED") == true -> 
                                    "Veri kaydetme izni reddedildi"
                                e.message?.contains("UNAVAILABLE") == true -> 
                                    "İnternet bağlantınızı kontrol edin"
                                else -> "Kullanıcı bilgileri kaydedilemedi: ${e.localizedMessage}"
                            }
                            
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        }
                }
            }.addOnFailureListener { e ->
                println("Kullanıcı oluşturma hatası: ${e.message}")
                
                binding.progressBar.visibility = View.GONE
                binding.btnSignUp.isEnabled = true
                
                val errorMessage = when {
                    e.message?.contains("WEAK_PASSWORD") == true -> 
                        "Şifre çok zayıf. En az 6 karakter olmalıdır."
                    e.message?.contains("INVALID_EMAIL") == true -> 
                        "Geçersiz e-posta adresi"
                    e.message?.contains("EMAIL_ALREADY_IN_USE") == true -> 
                        "Bu e-posta adresi zaten kullanımda"
                    e.message?.contains("NETWORK_ERROR") == true -> 
                        "İnternet bağlantınızı kontrol edin"
                    else -> "Kullanıcı oluşturma hatası: ${e.localizedMessage}"
                }
                
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            binding.btnSignUp.isEnabled = true
            Toast.makeText(requireContext(), "Kayıt hatası: ${e.message}", Toast.LENGTH_LONG).show()
            println("Kullanıcı oluşturma hatası: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}