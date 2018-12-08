package com.example.ullas.fingerprint

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val MODE_OF_AUTH = "mode_of_auth"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mode_of_auth.text = intent?.extras?.getString(MODE_OF_AUTH) ?: getString(R.string.mode_of_auth_error)
    }
}