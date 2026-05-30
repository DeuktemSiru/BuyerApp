package com.example.deuktemsiru_buyer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.deuktemsiru_buyer.databinding.ActivityMainBinding
import com.example.deuktemsiru_buyer.network.Push
import com.example.deuktemsiru_buyer.network.RetrofitClient
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

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
        val session = (application as DeuktemsiruBuyerApp).session
        session.restoreToken()
        if (session.isLoggedIn()) enablePush()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        RetrofitClient.onSessionExpired = {
            runOnUiThread {
                session.clear()
                navController.navigate(
                    R.id.onboardingFragment,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build(),
                )
            }
        }

        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = if (destination.id in bottomNavDestinations) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    fun enablePush() {
        Push.registerCurrentToken()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onDestroy() {
        RetrofitClient.onSessionExpired = null
        super.onDestroy()
    }
}
