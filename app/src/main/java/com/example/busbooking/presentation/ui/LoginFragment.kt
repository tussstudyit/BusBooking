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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.busbooking.R
import com.example.busbooking.data.db.BusBookingDatabase
import com.example.busbooking.domain.repository.UserRepository
import com.example.busbooking.presentation.viewmodel.AuthViewModel
import com.example.busbooking.utils.UiState
import com.google.android.material.snackbar.Snackbar

class LoginFragment : Fragment() {
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    private val viewModel: AuthViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        val userRepository = UserRepository(db.userDao())
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(userRepository) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupListeners()
        setupObservers()
    }

    private fun initializeViews(view: View) {
        emailInput = view.findViewById(R.id.emailInput)
        passwordInput = view.findViewById(R.id.passwordInput)
        loginButton = view.findViewById(R.id.loginButton)
        registerButton = view.findViewById(R.id.registerButton)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
    }

    private fun setupListeners() {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            viewModel.loginUser(email, password)
        }

        registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    loginButton.isEnabled = false
                    errorText.visibility = View.GONE
                }
                is UiState.Success -> {
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true
                    val role = state.data.role
                    if (role == "ADMIN") {
                        findNavController().navigate(R.id.action_loginFragment_to_adminDashboardFragment)
                    } else {
                        findNavController().navigate(R.id.action_loginFragment_to_userDashboardFragment)
                    }
                }
                is UiState.Error -> {
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled = true
                    errorText.text = state.message
                    errorText.visibility = View.VISIBLE
                    Snackbar.make(loginButton, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.validationErrors.observe(viewLifecycleOwner) { errors ->
            errors.forEach { (field, message) ->
                when (field) {
                    "email" -> emailInput.error = message
                    "password" -> passwordInput.error = message
                }
            }
        }
    }
}

