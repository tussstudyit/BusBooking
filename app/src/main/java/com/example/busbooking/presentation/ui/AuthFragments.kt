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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.busbooking.R
import com.example.busbooking.data.db.BusBookingDatabase
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.presentation.ui.state.AuthState
import com.example.busbooking.presentation.viewmodel.AuthViewModel
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



// ─────────────────────────────────────────────────────────────────────────────
// 1. SplashFragment — Kiểm tra session rồi điều hướng
//    Dùng lifecycleScope thay Handler để tránh memory leak khi fragment bị destroy
// ─────────────────────────────────────────────────────────────────────────────

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_splash, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                delay(2000)
                navigateBasedOnSession()
            } catch (e: Exception) {
                Log.e("SplashFragment", "Error: ${e.message}", e)
            }
        }
    }

    private fun navigateBasedOnSession() {
        val nav = findNavController()
        if (SessionManager.isSessionActive()) {
            val dest = if (SessionManager.getCurrentUserRole() == "ADMIN")
                R.id.action_splashFragment_to_adminDashboardFragment
            else
                R.id.action_splashFragment_to_nav_home
            nav.navigate(dest)
        } else {
            nav.navigate(R.id.action_splashFragment_to_loginFragment)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. LoginFragment — Đăng nhập, điều hướng theo role
// ─────────────────────────────────────────────────────────────────────────────

class LoginFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        val repo = AuthRepository(db.userDao())
        AuthViewModel.factory(repo)
    }

    private lateinit var phoneInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        phoneInput    = view.findViewById(R.id.phoneInput)
        passwordInput = view.findViewById(R.id.passwordInput)
        loginButton   = view.findViewById(R.id.loginButton)
        registerButton = view.findViewById(R.id.registerButton)
        progressBar   = view.findViewById(R.id.progressBar)
        errorText     = view.findViewById(R.id.errorText)

        loginButton.setOnClickListener {
            val phone    = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (phone.isBlank() || password.isBlank()) {
                showError("Vui lòng nhập số điện thoại và mật khẩu")
                return@setOnClickListener
            }
            viewModel.login(phone, password)
        }

        registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    errorText.visibility   = View.GONE
                    loginButton.isEnabled  = false
                }
                is AuthState.LoginSuccess -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility   = View.GONE
                    loginButton.isEnabled  = true
                    navigateBasedOnRole()
                }
                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    loginButton.isEnabled  = true
                    showError(state.message)
                }
                else -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility   = View.GONE
                    loginButton.isEnabled  = true
                }
            }
        }
    }

    private fun navigateBasedOnRole() {
        val dest = if (SessionManager.getCurrentUserRole() == "ADMIN")
            R.id.action_loginFragment_to_adminDashboardFragment
        else
            R.id.action_loginFragment_to_homeFragment
        findNavController().navigate(dest)
    }

    private fun showError(message: String) {
        errorText.text       = message
        errorText.visibility = View.VISIBLE
    }
    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return AuthViewModel(authRepository) as T
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. RegisterFragment — Đăng ký tài khoản mới
// ─────────────────────────────────────────────────────────────────────────────

class RegisterFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        val repo = AuthRepository(db.userDao())
        AuthViewModel.factory(repo)
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nameInput            = view.findViewById(R.id.nameInput)
        emailInput           = view.findViewById(R.id.emailInput)
        phoneInput           = view.findViewById(R.id.phoneInput)
        passwordInput        = view.findViewById(R.id.passwordInput)
        confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput)
        registerButton       = view.findViewById(R.id.registerButton)
        backButton           = view.findViewById(R.id.backButton)
        progressBar          = view.findViewById(R.id.progressBar)
        errorText            = view.findViewById(R.id.errorText)

        registerButton.setOnClickListener {
            val name            = nameInput.text.toString().trim()
            val email           = emailInput.text.toString().trim()
            val phone           = phoneInput.text.toString().trim()
            val password        = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                showError("Vui lòng điền đầy đủ thông tin")
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                showError("Mật khẩu xác nhận không khớp")
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
                    progressBar.visibility   = View.VISIBLE
                    errorText.visibility     = View.GONE
                    registerButton.isEnabled = false
                }
                is AuthState.RegisterSuccess -> {
                    progressBar.visibility   = View.GONE
                    errorText.visibility     = View.GONE
                    registerButton.isEnabled = true
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment) // ✅ về nav_home
                }
                is AuthState.Error -> {
                    progressBar.visibility   = View.GONE
                    registerButton.isEnabled = true
                    showError(state.message)
                }
                else -> {
                    progressBar.visibility   = View.GONE
                    errorText.visibility     = View.GONE
                    registerButton.isEnabled = true
                }
            }
        }
    }

    private fun showError(message: String) {
        errorText.text       = message
        errorText.visibility = View.VISIBLE
    }
}