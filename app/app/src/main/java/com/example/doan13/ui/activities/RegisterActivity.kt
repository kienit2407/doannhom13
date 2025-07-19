package com.example.doan13.ui.activities

import android.app.AlertDialog
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputBinding
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.doan13.R
import com.example.doan13.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnRegister.setOnClickListener {
           val signup = Intent(this@RegisterActivity, SignUpActivity::class.java)
            startActivity(signup)
        }

        binding.btnSignIn.setOnClickListener {
            val signin = Intent(this@RegisterActivity, SignInActivity::class.java)
            startActivity(signin)
        }
    }
}