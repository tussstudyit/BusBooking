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
import com.example.busbooking.presentation.ui.state.AuthState
import com.example.busbooking.presentation.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var backButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (password != confirmPassword) {
                errorText.text = "Passwords do not match"
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            viewModel.register(name, email, password, phone)
        }

        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    errorText.visibility = View.GONE
                    registerButton.isEnabled = false
                }
                is AuthState.RegisterSuccess -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    registerButton.isEnabled = true
                    // Navigate back to login
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                }
                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    errorText.text = state.message
                    errorText.visibility = View.VISIBLE
                    registerButton.isEnabled = true
                }
                else -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    registerButton.isEnabled = true
                }
            }
        }
    }
}
