package com.example.doan13.utilities.common

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import com.example.doan13.databinding.ToastCustomBinding

object ToastCustom { //không cần tạo intance
    fun showCustomToast(context: Context, message: String) {
        // Inflate layout với View Binding
        val binding = ToastCustomBinding.inflate(LayoutInflater.from(context))

        // Gán nội dung cho TextView
        binding.message.text = message

        // Tạo Toast
        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = binding.root // Sử dụng root view từ binding
        toast.show()
    }
}