package com.example.doan13.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.doan13.ui.activities.MainActivity
import com.example.doan13.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val authFirebaseAuth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_page)

        if (authFirebaseAuth.currentUser != null) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish() //đóng cái nàyu
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                val intent = Intent(this@SplashActivity, RegisterActivity::class.java)
                startActivity(intent)
                finish() //đóng cái nàyu
            }
        }
    }
}
