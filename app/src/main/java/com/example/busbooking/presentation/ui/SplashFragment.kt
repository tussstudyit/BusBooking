package com.example.busbooking.presentation.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.busbooking.R
import com.example.busbooking.utils.SessionManager

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Simulate loading time
        Handler(Looper.getMainLooper()).postDelayed({
            navigateBasedOnSession()
        }, 2000) // 2 seconds
    }

    private fun navigateBasedOnSession() {
        if (SessionManager.isSessionActive()) {
            val role = SessionManager.getCurrentUserRole()
            if (role == "ADMIN") {
                findNavController().navigate(R.id.action_splashFragment_to_adminDashboardFragment)
            } else {
                findNavController().navigate(R.id.action_splashFragment_to_userDashboardFragment)
            }
        } else {
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }
    }
}
