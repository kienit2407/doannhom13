package com.example.doan13.utilities

import android.app.Application
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo Cloudinary
        val config = mapOf(
            "cloud_name" to "deyzjx3oq",
            "api_key" to "224795637491789",
            "api_secret" to "ccN7x_5rjx8YRIQFROcbMmgahvE"
        )
        MediaManager.init(this, config)
    }
}