package com.example.busbooking.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.busbooking.R
import com.example.busbooking.data.db.BusBookingDatabase
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.utils.SessionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    private val authRepository by lazy {
        AuthRepository(BusBookingDatabase.getInstance(requireContext()).userDao())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emailInput = view.findViewById(R.id.emailInput)
        passwordInput = view.findViewById(R.id.passwordInput)
        loginButton = view.findViewById(R.id.loginButton)
        registerButton = view.findViewById(R.id.registerButton)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)

        registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            if (email.isBlank() || password.isBlank()) {
                showError("Email and password are required")
                return@setOnClickListener
            }
            doLogin(email, password)
        }
    }

    private fun doLogin(email: String, password: String) {
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        loginButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = authRepository.loginUser(email, password)) {
                is Result.Success -> {
                    SessionManager.saveSession(result.data)
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true
                    if (result.data.role == "ADMIN") {
                        findNavController().navigate(R.id.action_loginFragment_to_adminDashboardFragment)
                    } else {
                        findNavController().navigate(R.id.action_loginFragment_to_userDashboardFragment)
                    }
                }
                is Result.Error -> {
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true
                    showError(result.message)
                }
            }
        }
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }
}

