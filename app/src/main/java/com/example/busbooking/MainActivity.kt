package com.example.busbooking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.busbooking.domain.repository.ServerConfig
import com.example.busbooking.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServerConfig.initialize(this)
        SessionManager.initialize(this)
        setContentView(R.layout.activity_main)

        // LÃ¡ÂºÂ¥y NavController tÃ¡Â»Â« NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // GÃ¡ÂºÂ¯n BottomNav vÃ¡Â»â€ºi NavController
        bottomNav = findViewById(R.id.bottomNavigation)
        bottomNav.setupWithNavController(navController)
        bottomNav.visibility = View.GONE

        val hideBottomNavDestinations = setOf(
            R.id.splashFragment,
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.seatSelectionFragment,
            R.id.bookingConfirmationFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in hideBottomNavDestinations) {
                bottomNav.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
            }
        }

        handlePaymentReturnIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentReturnIntent(intent)
    }

    private fun handlePaymentReturnIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "busbooking" || data.host != "payment-return") return

        window.decorView.post {
            runCatching {
                val destination = if (SessionManager.getCurrentUserId() > 0) {
                    R.id.myTicketsFragment
                } else {
                    R.id.loginFragment
                }
                navController.navigate(destination)
                Toast.makeText(this, "Da cap nhat ket qua thanh toan", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


