package com.example.ullas.fingerprint

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), AuthenticationListener {

    companion object {
        const val PUBLIC_KEY_PASSWORD = "PUBLIC_KEY_PASSWORD"
    }

    private lateinit var sharedPreferences: SharedPreferences
    private var fingerprintHandler: FingerprintHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        btn_login.setOnClickListener { login() }
        btn_signup.setOnClickListener { signUp() }
    }

    override fun onResume() {
        super.onResume()
        editText.text.clear()
        if (sharedPreferences.contains(PUBLIC_KEY_PASSWORD)) {
            showFingerprintIcon()
        } else {
            hideFingerPrintIcon()
        }
    }

    private fun hideFingerPrintIcon() {
        fingerprint_icon.visibility = GONE
        helper_text.visibility = GONE
        error_text.visibility = GONE
    }

    private fun showFingerprintIcon() {
        fingerprint_icon.setColorFilter(getColor(android.R.color.black), PorterDuff.Mode.SRC_IN)
        fingerprint_icon.visibility = VISIBLE
        helper_text.visibility = VISIBLE
        initSensor()
    }

    override fun onStop() {
        super.onStop()
        fingerprintHandler?.cancel()
    }

    private fun signUp() {
        val password = editText.text.toString()
        if (password.isNotEmpty()) {
            savePassword(password)
            editText.setText("")
            showFingerprintIcon()
            showToast("Password $password registered")
        } else
            showToast(getString(R.string.empty_password))
    }

    private fun login() {
        val password = editText.text.toString()
        if (password.isNotEmpty()) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(MainActivity.MODE_OF_AUTH, "Password")
            startActivity(intent)
        } else {
            showToast(getString(R.string.empty_password))
        }
    }

    private fun savePassword(password: String) {
        if (Utils.checkSensorState(this)) {
            val encoded = Utils.encryptString(password)
            sharedPreferences.edit().putString(PUBLIC_KEY_PASSWORD, encoded).apply()
        }
    }

    private fun initSensor() {
        if (Utils.checkSensorState(this)) {
            val cryptoObject = Utils.cryptoObject
            if (cryptoObject != null) {
                val fingerprintManager = getSystemService(FINGERPRINT_SERVICE) as FingerprintManager
                fingerprintHandler = FingerprintHandler(this, sharedPreferences, this)
                fingerprintHandler?.startAuth(fingerprintManager, cryptoObject)
            }
        }
    }

    override fun onAuthenticationSuccess(decryptPassword: String) {
        fingerprint_icon.setColorFilter(getColor(android.R.color.holo_green_dark), PorterDuff.Mode.SRC_IN)
        editText.setText(decryptPassword)
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.MODE_OF_AUTH, getString(R.string.mode_of_auth_fingerprint))
        startActivity(intent)
    }

    override fun onAuthenticationFailure(error: String?) {
        fingerprint_icon.setColorFilter(getColor(android.R.color.holo_red_dark), PorterDuff.Mode.SRC_IN)
        error?.let {
            error_text.text = it
        }
    }

    fun showToast(message : String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
