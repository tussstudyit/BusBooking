package com.example.busbooking.staff.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.busbooking.staff.R
import com.example.busbooking.staff.data.api.StaffServerConfig
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.utils.StaffViewModelFactory
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {
    private val viewModel: LoginViewModel by viewModels {
        StaffViewModelFactory { LoginViewModel(StaffRepository()) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val loginInput = view.findViewById<TextInputEditText>(R.id.loginInput)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.passwordInput)
        val serverInput = view.findViewById<TextInputEditText>(R.id.serverInput)
        val loginButton = view.findViewById<Button>(R.id.loginButton)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        serverInput.setText(StaffServerConfig.primaryBaseUrl())

        loginButton.setOnClickListener {
            val serverUrl = serverInput.text?.toString().orEmpty()
            if (serverUrl.isBlank()) {
                errorText.text = "Vui lòng nhập địa chỉ web admin"
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }
            StaffServerConfig.setBaseUrl(serverUrl)
            viewModel.login(loginInput.text?.toString().orEmpty(), passwordInput.text?.toString().orEmpty())
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            progressBar.visibility = if (state is LoginState.Loading) View.VISIBLE else View.GONE
            loginButton.isEnabled = state !is LoginState.Loading
            when (state) {
                LoginState.Success -> findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                is LoginState.Error -> {
                    errorText.text = state.message
                    errorText.visibility = View.VISIBLE
                }
                else -> errorText.visibility = View.GONE
            }
        }
    }
}
