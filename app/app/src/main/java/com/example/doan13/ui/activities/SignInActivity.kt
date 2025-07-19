package com.example.doan13.ui.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.doan13.MainActivity
import com.example.doan13.R
import com.example.doan13.databinding.ActivitySignInBinding
import com.example.doan13.databinding.ToastCustomBinding
import com.example.doan13.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivitySignInBinding
    private val authViewModel: AuthViewModel by viewModels()
    private var isHide = false
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(Exception::class.java)
                authViewModel.signInWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Toast.makeText(this, "Lỗi khi lấy tài khoản: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnReset.setOnClickListener {
            startActivity(Intent(this, ResetPassword::class.java))
        }

        binding.btnSwitch.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }

        //khởi tạo
        authViewModel.initGoogle(this)

        authViewModel.loading.observe(this) { isLoaded ->
            if (isLoaded) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }

        authViewModel.loginSuccess.observe(this) { success ->
            if (success) {
//                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                showCustomToast(this, "Đăng nhập thành công!")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        authViewModel.errorMessage.observe(this) { error ->
            error?.let {

                binding.txtPasswordState.text = "Email hoặc mật khẩu bị sai"
            }
        }
        binding.imgVisibility.setOnClickListener {
            isHide = !isHide // Đảo ngược trạng thái của isHide

            if (isHide) {
                // Hiện mật khẩu
                binding.editPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.imgVisibility.setImageResource(R.drawable.visibility_on)
            } else {
                // Ẩn mật khẩu
                binding.editPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.imgVisibility.setImageResource(R.drawable.visibility_off)
            }

            // Làm mới EditText để áp dụng thay đổi inputType
            binding.editPassword.setSelection(binding.editPassword.text.length)
            binding.editPassword.refreshDrawableState() // Đảm bảo giao diện cập nhật
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.editPassword.text.toString().trim()
            if (email.isBlank() || password.isBlank()) {
//                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                binding.txtEmailState.text = "Vui lòng điền đầy đủ thông tin!"
                binding.txtPasswordState.text = "Vui lòng điền đầy đủ thông tin!"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                binding.txtEmailState.text = "Email không hợp lệ"
                return@setOnClickListener
            }
            authViewModel.login(email, password)
        }

        binding.btnSignInWithGG.setOnClickListener {
            signInLauncher.launch(authViewModel.getSignInIntent())
        }


    }
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

//        //kiểm tra trạng thái
//        authViewModel.loginSuccess.observe(this) { success ->
//            if (success) {
//                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
//                val intent = Intent(this, MainActivity::class.java)
//                startActivity(intent)
//                finish()
//            }
//        }
//        authViewModel.errolMessage.observe(this) { error ->
//            if (error != null) {
//                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
//            }
//        }
//        binding.btnSignIn.setOnClickListener {
//            val edtEmail = binding.edtEmail.text.toString().trim()
//            val edtPassword = binding.editPassword.text.toString().trim()
//            if(edtEmail.isBlank() || edtPassword.isBlank()){
//                Toast.makeText(this, "Please fill out full information!", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(edtEmail).matches()) {
//                Toast.makeText(this, "Email invalid", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            // Gọi hàm đăng ký
//            authViewModel.login(edtEmail, edtPassword)
//        }
//
//        binding.btnSignInWithGG.setOnClickListener {
//            signIn()
//        }
//    }
//
//    private fun signIn() {
//        val signInIntent = authViewModel.getSignInIntent()
////        val signInIntent = googleSignInClient.signInIntent
//        startActivityForResult(signInIntent, authViewModel.getRequestCode()) // mỏw màn hình đăng nhập gg
//        //hàm này dùng để mở một activity khac và tờ trả kết quả
//        //cần truyền vào intent: là ý định muoson hẹ thống thực hiện. ví dụ mở gg sign in để đăng nhập
////        requestcode :mã định danh để biết gọi từ đâu
//    }
//    //dùng hàm này để xử lý kết quả ở đâu
//    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
//    override fun onActivityResult(
//        requestCode: Int,
//        resultCode: Int,
//        data: Intent?,
//    ) {
//        super.onActivityResult(requestCode, resultCode, data)
//        println("onActivityResult called with requestCode: $requestCode")
//        if(requestCode == authViewModel.getRequestCode() && resultCode == Activity.RESULT_OK){ //kiểm tra mã yêu cầu
//            val task = GoogleSignIn.getSignedInAccountFromIntent(data) //lấy kết quả từ tài khoản lưu và obieens\
//            try {
//                val account = task.getResult(Exception::class.java) //lấy thông tin tìa khoản
//                println("Received account: ${account.email}, idToken: ${account.idToken}")
//                firebaseAuthWithGoogle(account.idToken!!) //gửi token đến fire base
//            }catch (e: Exception){
//                println("Error in task: ${e.message}")
//                Toast.makeText(this, "Lỗi khi lấy tài khoản: ${e.message}", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private fun firebaseAuthWithGoogle(idToken: kotlin.String?) {
//        print(idToken)
//        authViewModel.signInWithGoogle(idToken.toString())
//        authViewModel.loginSuccess.observe (this){ state->
//            if (state){
//                Toast.makeText(this, "Sign In Succesed", Toast.LENGTH_SHORT).show()
//                val intent = Intent(this, MainActivity::class.java)
//                startActivity(intent)
//                finish()
//            }else{
//                Toast.makeText(this, "Sign In failed. Please try again", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//    }
//    }
//}