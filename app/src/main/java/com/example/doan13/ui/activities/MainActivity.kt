package com.example.doan13.ui.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.doan13.R
import com.example.doan13.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding // tạp lớp tự động từ activimanin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) //chuyển thành view và
        setContentView(binding.root)
        //setup bottom nav
            try {
                //Khi mà ta nhấp bào home của bottom là nó sẽ khớp với bottom menu, mà các item có cùng id với id nav controller nên
//                là nó sẽ nhận là homefragment. sau đó ta phải tìm được hostframment. và lấy ra navcontroller của host đó để setfragment
//                lên host đó
                //Lợi ích dùn navgraph là nó lưu tự động vào back stack và nó sẽ không gây huỷ state. ví dụ: vị trí cuộn của từng màn hinh
                //hay là state của từng màn hình
                //tìm navhost của fragment host thông qua supportFrament: quản lý tất cả các fragment
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_host) as NavHostFragment
                // Lấy NavController từ NavHostFragment
                val navController = navHostFragment.navController
                // Kết nối BottomNavigationView với NavController
                binding.bottomNavigation.setupWithNavController(navController)

//                // Ẩn/hiện BottomNavigationView dựa trên destination
//                navController.addOnDestinationChangedListener { _, destination, _ ->
//                    Log.d("Navigation", "Destination changed to: ${destination.label}")
//                    binding.bottomNavigation.visibility = when (destination.id) {
//                        R.id.uploadFragment -> View.GONE
//                        else -> View.VISIBLE
//                    }
//                }
            }catch (e: IllegalStateException){
                Log.e("Mainactivity", "Navigation errol: ${e.message}")
            }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                // Kiểm tra nếu đây là activity đầu tiên trong stack
                if (isTaskRoot) {
                    Toast.makeText(this@MainActivity, "Không thể quay lại!", Toast.LENGTH_SHORT).show()
                } else {
                    // Nếu không phải activity đầu, cho phép back
                    finish()
                }
            }
        })

    }

}
