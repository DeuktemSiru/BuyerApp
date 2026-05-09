package com.example.deuktemsiru_buyer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale.KOREAN)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val bottomNavDestinations = setOf(
        R.id.homeFragment,
        R.id.mapFragment,
        R.id.wishlistFragment,
        R.id.ordersFragment,
        R.id.myPageFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 저장된 토큰을 RetrofitClient에 복원 (로그인 상태 유지)
        SessionManager(this).restoreToken()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = if (destination.id in bottomNavDestinations) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }
}
