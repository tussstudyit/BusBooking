package com.example.busbooking.staff.ui.ticket

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.busbooking.staff.R
import com.example.busbooking.staff.data.api.StaffApiClient
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.utils.StaffViewModelFactory
import com.google.android.material.textfield.TextInputEditText
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.serialization.encodeToString

class TicketScannerFragment : Fragment() {
    private val viewModel: TicketScannerViewModel by viewModels {
        StaffViewModelFactory { TicketScannerViewModel(StaffRepository()) }
    }
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        val content = result.contents.orEmpty()
        if (content.isBlank()) {
            view?.findViewById<TextView>(R.id.cameraStatusText)?.text = "Chưa quét được QR"
            return@registerForActivityResult
        }
        view?.findViewById<TextInputEditText>(R.id.qrInput)?.setText(content)
        viewModel.verify(content)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_ticket_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val qrInput = view.findViewById<TextInputEditText>(R.id.qrInput)
        val scanCameraButton = view.findViewById<Button>(R.id.scanCameraButton)
        val verifyButton = view.findViewById<Button>(R.id.verifyButton)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        val cameraStatusText = view.findViewById<TextView>(R.id.cameraStatusText)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraStatusText.text = "Máy ảnh sẵn sàng"
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        }

        verifyButton.setOnClickListener {
            viewModel.verify(qrInput.text?.toString().orEmpty())
        }
        scanCameraButton.setOnClickListener {
            openQrScanner()
        }
        view.findViewById<View>(R.id.cameraFrame).setOnClickListener {
            openQrScanner()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            verifyButton.isEnabled = state !is TicketVerifyState.Loading
            when (state) {
                is TicketVerifyState.Success -> {
                    errorText.visibility = View.GONE
                    val payload = StaffApiClient.json.encodeToString(state.result)
                    findNavController().navigate(
                        R.id.action_ticketScannerFragment_to_ticketResultFragment,
                        Bundle().apply { putString("ticketResult", payload) }
                    )
                }
                is TicketVerifyState.Error -> {
                    errorText.text = state.message
                    errorText.visibility = View.VISIBLE
                }
                else -> errorText.visibility = View.GONE
            }
        }
    }

    @Deprecated("Deprecated in Android framework")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        view?.findViewById<TextView>(R.id.cameraStatusText)?.text =
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) "Máy ảnh sẵn sàng" else "Chưa cấp quyền máy ảnh"
    }

    private companion object {
        const val CAMERA_PERMISSION_REQUEST = 20
    }

    private fun openQrScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Quét QR vé")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setCaptureActivity(StaffQrCaptureActivity::class.java)
        }
        qrScannerLauncher.launch(options)
    }
}
