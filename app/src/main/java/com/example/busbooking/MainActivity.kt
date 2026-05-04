package com.example.busbooking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.busbooking.utils.SessionManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SessionManager
        SessionManager.initialize(this)
        SessionManager.restoreSession()
    }
}