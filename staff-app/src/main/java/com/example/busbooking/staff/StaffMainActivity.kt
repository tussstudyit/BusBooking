package com.example.busbooking.staff

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.busbooking.staff.data.api.StaffServerConfig
import com.example.busbooking.staff.session.StaffSessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class StaffMainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StaffServerConfig.initialize(this)
        StaffSessionManager.initialize(this)
        setContentView(R.layout.activity_staff_main)

        val host = supportFragmentManager.findFragmentById(R.id.staffNavHost) as NavHostFragment
        navController = host.navController
        val hostView = findViewById<View>(R.id.staffNavHost)
        bottomNav = findViewById(R.id.staffBottomNav)
        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hideBottomNav = destination.id == R.id.loginFragment
            bottomNav.visibility = if (hideBottomNav) View.GONE else View.VISIBLE
            val params = hostView.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            params.bottomMargin = if (hideBottomNav) 0 else resources.getDimensionPixelSize(R.dimen.staff_bottom_nav_height)
            hostView.layoutParams = params
        }
    }
}
