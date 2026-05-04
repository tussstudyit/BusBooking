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

class RegisterFragment : Fragment() {
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var backButton: Button
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
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupListeners()
        setupObservers()
    }

    private fun initializeViews(view: View) {
        nameInput = view.findViewById(R.id.nameInput)
        emailInput = view.findViewById(R.id.emailInput)
        phoneInput = view.findViewById(R.id.phoneInput)
        passwordInput = view.findViewById(R.id.passwordInput)
        confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput)
        registerButton = view.findViewById(R.id.registerButton)
        backButton = view.findViewById(R.id.backButton)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
    }

    private fun setupListeners() {
        registerButton.setOnClickListener {
            val name = nameInput.text.toString()
            val email = emailInput.text.toString()
            val phone = phoneInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (password != confirmPassword) {
                Snackbar.make(registerButton, "Passwords do not match", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.registerUser(name, email, password, phone)
        }

        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {
        viewModel.registerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    registerButton.isEnabled = false
                    errorText.visibility = View.GONE
                }
                is UiState.Success -> {
                    progressBar.visibility = View.GONE
                    registerButton.isEnabled = true
                    Snackbar.make(registerButton, "Registration successful! Please login.", Snackbar.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                }
                is UiState.Error -> {
                    progressBar.visibility = View.GONE
                    registerButton.isEnabled = true
                    errorText.text = state.message
                    errorText.visibility = View.VISIBLE
                    Snackbar.make(registerButton, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.validationErrors.observe(viewLifecycleOwner) { errors ->
            errors.forEach { (field, message) ->
                when (field) {
                    "name" -> nameInput.error = message
                    "email" -> emailInput.error = message
                    "phone" -> phoneInput.error = message
                    "password" -> passwordInput.error = message
                }
            }
        }
    }
}

