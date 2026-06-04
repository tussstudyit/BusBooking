package com.example.busbooking.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.busbooking.R
import com.example.busbooking.domain.repository.ApiAuthRepository
import com.example.busbooking.domain.repository.ServerConfig
import com.example.busbooking.presentation.ui.state.AuthState
import com.example.busbooking.presentation.viewmodel.AuthViewModel
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_splash, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                delay(1200)
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
            } catch (e: Exception) {
                Log.e("SplashFragment", "Error: ${e.message}", e)
            }
        }
    }
}

class LoginFragment : Fragment() {
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModel.factory(ApiAuthRepository())
    }

    private lateinit var phoneInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var serverInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        phoneInput = view.findViewById(R.id.phoneInput)
        passwordInput = view.findViewById(R.id.passwordInput)
        serverInput = view.findViewById(R.id.serverInput)
        loginButton = view.findViewById(R.id.loginButton)
        registerButton = view.findViewById(R.id.registerButton)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)

        serverInput.setText(ServerConfig.primaryBaseUrl())

        loginButton.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val serverUrl = serverInput.text.toString().trim()
            if (phone.isBlank() || password.isBlank()) {
                showError("Vui l\u00f2ng nh\u1eadp s\u1ed1 \u0111i\u1ec7n tho\u1ea1i v\u00e0 m\u1eadt kh\u1ea9u")
                return@setOnClickListener
            }
            if (serverUrl.isBlank()) {
                showError("Vui lòng nhập địa chỉ web admin")
                return@setOnClickListener
            }
            ServerConfig.setBaseUrl(serverUrl)
            viewModel.login(phone, password)
        }

        registerButton.setOnClickListener {
            ServerConfig.setBaseUrl(serverInput.text.toString())
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> setLoading(true)
                is AuthState.LoginSuccess -> {
                    setLoading(false)
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is AuthState.Error -> {
                    setLoading(false)
                    showError(state.message)
                }
                else -> setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        errorText.visibility = View.GONE
        loginButton.isEnabled = !loading
        registerButton.isEnabled = !loading
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }
}

class RegisterFragment : Fragment() {
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModel.factory(ApiAuthRepository())
    }

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_register, container, false)

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

            when {
                name.isBlank() || phone.isBlank() || password.isBlank() -> {
                    showError("Vui l\u00f2ng \u0111i\u1ec1n \u0111\u1ea7y \u0111\u1ee7 th\u00f4ng tin")
                }
                password != confirmPassword -> {
                    showError("M\u1eadt kh\u1ea9u x\u00e1c nh\u1eadn kh\u00f4ng kh\u1edbp")
                }
                else -> viewModel.register(name, email, password, phone)
            }
        }

        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> setLoading(true)
                is AuthState.RegisterSuccess -> {
                    setLoading(false)
                    if (SessionManager.isSessionActive()) {
                        findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                    } else {
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    }
                }
                is AuthState.Error -> {
                    setLoading(false)
                    showError(state.message)
                }
                else -> setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        errorText.visibility = View.GONE
        registerButton.isEnabled = !loading
        backButton.isEnabled = !loading
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }
}

