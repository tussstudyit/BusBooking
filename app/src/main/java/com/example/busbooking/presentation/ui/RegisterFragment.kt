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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

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

    private val authRepository by lazy {
        AuthRepository(BusBookingDatabase.getInstance(requireContext()).userDao())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nameInput = view.findViewById(R.id.nameInput)
        emailInput = view.findViewById(R.id.emailInput)
        phoneInput = view.findViewById(R.id.phoneInput)
        passwordInput = view.findViewById(R.id.passwordInput)
        confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput)
        registerButton = view.findViewById(R.id.registerButton)
        backButton = view.findViewById(R.id.backButton)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                showError("Please fill all fields")
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                showError("Passwords do not match")
                return@setOnClickListener
            }
            doRegister(name, email, password, phone)
        }

        backButton.setOnClickListener { findNavController().popBackStack() }
    }

    private fun doRegister(name: String, email: String, password: String, phone: String) {
        progressBar.visibility = View.VISIBLE
        registerButton.isEnabled = false
        errorText.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = authRepository.registerUser(name, email, password, phone)) {
                is Result.Success -> {
                    progressBar.visibility = View.GONE
                    registerButton.isEnabled = true
                    Snackbar.make(requireView(), "Registration successful", Snackbar.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                }
                is Result.Error -> {
                    progressBar.visibility = View.GONE
                    registerButton.isEnabled = true
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

