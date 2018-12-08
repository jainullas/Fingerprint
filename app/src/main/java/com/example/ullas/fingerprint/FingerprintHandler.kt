package com.example.ullas.fingerprint

import android.content.Context
import android.content.SharedPreferences
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal

internal class FingerprintHandler(private val mContext: Context,
                                  private val mSharedPreferences: SharedPreferences,
                                  private val mListener: AuthenticationListener) : FingerprintManager.AuthenticationCallback() {

    private val cancellationSignal: CancellationSignal?

    init {
        cancellationSignal = CancellationSignal()
    }

    fun startAuth(fingerprintManager: FingerprintManager, cryptoObject: FingerprintManager.CryptoObject) {
        fingerprintManager.authenticate(cryptoObject, cancellationSignal, 0, this, null)
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        mListener.onAuthenticationFailure(mContext.getString(R.string.result_type_error) + errorCode.toString()+ " : " + errString.toString())
    }

    override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
        mListener.onAuthenticationFailure(mContext.getString(R.string.result_type_help) + helpCode.toString()+ " : " + helpString.toString())
    }

    override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
        val cipher = result.cryptoObject.cipher
        val encoded = mSharedPreferences.getString(LoginActivity.PUBLIC_KEY_PASSWORD, null)
        Utils.decryptString(encoded, cipher)?.let {
            mListener.onAuthenticationSuccess(it)
        } ?: run {
            mListener.onAuthenticationFailure()
            (mContext as LoginActivity).showToast("onAuthenticationFailed")
        }
    }

    override fun onAuthenticationFailed() {
        (mContext as LoginActivity).showToast("onAuthenticationFailed")
        mListener.onAuthenticationFailure()
    }

    fun cancel() {
        cancellationSignal?.cancel()
    }
}
