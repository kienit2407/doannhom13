package com.example.doan13.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.doan13.databinding.ActivityResetPasswordBinding
import com.example.doan13.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResetPassword : AppCompatActivity() {
    private  lateinit var binding: ActivityResetPasswordBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnSend.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            if(email.isBlank()){
                Toast.makeText(this, "Please fill out full information!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email invalid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.resetPassword(binding.edtEmail.text.toString())

            authViewModel.resetSuccess.observe(this) { success ->
                if (success){
                    Toast.makeText(this, "Đã gửi link đến Email của bạn làm ơn check", Toast.LENGTH_LONG).show()
//                    startActivity(Intent(this, RegisterActivity::class.java))
                }
            }
        }
    }
}