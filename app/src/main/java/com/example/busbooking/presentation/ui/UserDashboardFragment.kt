package com.example.busbooking.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.busbooking.R
import com.example.busbooking.utils.SessionManager

class UserDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchRoutesButton: Button = view.findViewById(R.id.searchRoutesButton)
        val myTicketsButton: Button = view.findViewById(R.id.myTicketsButton)
        val bookingHistoryButton: Button = view.findViewById(R.id.bookingHistoryButton)
        val profileButton: Button = view.findViewById(R.id.profileButton)
        val logoutButton: Button = view.findViewById(R.id.logoutButton)

        searchRoutesButton.setOnClickListener {
            findNavController().navigate(R.id.action_userDashboardFragment_to_routeSearchFragment)
        }

        myTicketsButton.setOnClickListener {
            findNavController().navigate(R.id.action_userDashboardFragment_to_myTicketsFragment)
        }

        bookingHistoryButton.setOnClickListener {
            findNavController().navigate(R.id.action_userDashboardFragment_to_bookingHistoryFragment)
        }

        profileButton.setOnClickListener {
            findNavController().navigate(R.id.action_userDashboardFragment_to_userProfileFragment)
        }

        logoutButton.setOnClickListener {
            SessionManager.clearSession()
            findNavController().navigate(R.id.action_userDashboardFragment_to_loginFragment)
        }
    }
}
