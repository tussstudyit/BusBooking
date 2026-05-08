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
import com.example.busbooking.utils.SessionManager

class LoginFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            viewModel.login(email, password)
        }

        registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    errorText.visibility = View.GONE
                    loginButton.isEnabled = false
                }
                is AuthState.LoginSuccess -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    loginButton.isEnabled = true
                    navigateBasedOnRole()
                }
                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    errorText.text = state.message
                    errorText.visibility = View.VISIBLE
                    loginButton.isEnabled = true
                }
                else -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    loginButton.isEnabled = true
                }
            }
        }
    }

    private fun navigateBasedOnRole() {
        val role = SessionManager.getCurrentUserRole()
        if (role == "ADMIN") {
            findNavController().navigate(R.id.action_loginFragment_to_adminDashboardFragment)
        } else {
            findNavController().navigate(R.id.action_loginFragment_to_userDashboardFragment)
        }
    }
}
