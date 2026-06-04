package com.example.busbooking.staff.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.busbooking.staff.R

class ProfileFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val initialText = view.findViewById<TextView>(R.id.initialText)
        val nameText = view.findViewById<TextView>(R.id.nameText)
        val companyText = view.findViewById<TextView>(R.id.companyText)
        val roleText = view.findViewById<TextView>(R.id.roleText)
        val emailText = view.findViewById<TextView>(R.id.emailText)
        val phoneText = view.findViewById<TextView>(R.id.phoneText)
        val statusText = view.findViewById<TextView>(R.id.statusText)

        viewModel.user.observe(viewLifecycleOwner) { user ->
            val name = user?.name.orEmpty().ifBlank { "Nhân viên" }
            initialText.text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
            nameText.text = name
            companyText.text = user?.companyName.orEmpty().ifBlank { "Chưa cập nhật nhà xe" }
            roleText.text = if (user?.role.equals("STAFF", true)) "Nhân viên phụ trách chuyến" else user?.role.orEmpty()
            emailText.text = user?.email.orEmpty().ifBlank { "Chưa cập nhật email" }
            phoneText.text = user?.phone.orEmpty().ifBlank { "Chưa cập nhật SĐT" }
            statusText.text = "Đang đăng nhập"
        }

        view.findViewById<Button>(R.id.logoutButton).setOnClickListener {
            viewModel.logout()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
        viewModel.loadProfile()
    }
}
