package com.example.cardwallet

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

interface SecurityHost {
    val lockAvailable: Boolean
    fun authenticate(title: String, onResult: (Boolean) -> Unit)
    fun setSecure(on: Boolean)
}

class MainActivity : FragmentActivity(), SecurityHost {

    override val lockAvailable: Boolean get() = pickAuthenticators() != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = CardStore(applicationContext)
        setSecure(store.loadSettings().blockScreenshots)
        val host: SecurityHost = this
        setContent { AppTheme { WalletApp(store, host) } }
    }

    override fun setSecure(on: Boolean) {
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun pickAuthenticators(): Int? {
        val bm = BiometricManager.from(this)
        val options = listOf(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return options.firstOrNull { bm.canAuthenticate(it) == BiometricManager.BIOMETRIC_SUCCESS }
    }

    override fun authenticate(title: String, onResult: (Boolean) -> Unit) {
        val auth = pickAuthenticators()
        if (auth == null) {
            onResult(false)
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(false)
                }
            }
        )
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("Card Wallet")
            .setAllowedAuthenticators(auth)
        if (auth and BiometricManager.Authenticators.DEVICE_CREDENTIAL == 0) {
            builder.setNegativeButtonText("Cancel")
        }
        prompt.authenticate(builder.build())
    }
}
