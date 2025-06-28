package com.osmanyildiz.scs.fragment

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
import com.osmanyildiz.scs.R
import com.osmanyildiz.scs.databinding.FragmentSignUpBinding


class SignUpFragment : Fragment() {


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
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            // Kayıt işlemi burada yapılacak
            val fullName = binding.etFullName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Şifreler eşleşmiyor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!binding.cbTerms.isChecked) {
                Toast.makeText(
                    requireContext(),
                    "Kullanım koşullarını kabul etmelisiniz",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Progress bar'ı göster
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSignUp.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
                // Progress bar'ı gizle
                binding.progressBar.visibility = View.GONE
                binding.btnSignUp.isEnabled = true
                
                Toast.makeText(requireContext(), "Kayıt başarılı", Toast.LENGTH_SHORT).show()
                
                // Kayıt sonrası kullanıcıyı çıkış yaptırıyoruz
                auth.signOut()

                findNavController().navigate(
                    R.id.action_signUpFragment_to_loginFragment
                )
            }.addOnFailureListener {
                // Progress bar'ı gizle
                binding.progressBar.visibility = View.GONE
                binding.btnSignUp.isEnabled = true
                
                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_SHORT).show()
            }


        }

        binding.tvSignIn.setOnClickListener {
            // Giriş ekranına geçiş
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}