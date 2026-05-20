package com.example.busbooking

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.busbooking.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        SessionManager.initialize(this)

        // Lấy NavController từ NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Gắn BottomNav với NavController
        bottomNav = findViewById(R.id.bottomNavigation)
        bottomNav.setupWithNavController(navController)
        bottomNav.visibility = View.GONE

        val hideBottomNavDestinations = setOf(
            R.id.splashFragment,
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.adminDashboardFragment,
            R.id.routeListAdminFragment,
            R.id.routeFormAdminFragment,
            R.id.busListAdminFragment,
            R.id.busFormAdminFragment,
            R.id.seatManagementFragment,
            R.id.tripListAdminFragment,
            R.id.tripFormAdminFragment,
            R.id.ticketListAdminFragment,
            R.id.userListAdminFragment,
            R.id.analyticsFragment,
            R.id.adminProfileFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in hideBottomNavDestinations) {
                bottomNav.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
            }
        }
    }
}