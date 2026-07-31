package io.github.shinhyo.signinwithapple

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.shinhyo.signinwithapple.model.AppleSignInResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Apple Sign-In Service (Chrome Custom Tabs variant)
 * 
 * Provides an alternative to the WebView-based authentication by utilizing Chrome Custom Tabs (CCT).
 * CCT provides a more secure, isolated, and user-trusted environment that shares session state with Chrome,
 * allowing password managers and active Apple sessions to autofill.
 * 
 * Important: To use this variant, the consumer application MUST register an intent filter
 * in their AndroidManifest.xml to catch the redirect URI:
 * 
 * <activity android:name=".YourAuthRedirectActivity" android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.VIEW" />
 *         <category android:name="android.intent.category.DEFAULT" />
 *         <category android:name="android.intent.category.BROWSABLE" />
 *         <data android:scheme="https" android:host="your.redirect.host" android:path="/callback" />
 *     </intent-filter>
 * </activity>
 */
object SignInWithAppleCCT {

    internal var serviceId: String? = null
    internal var redirectUri: String? = null

    fun init(serviceId: String, redirectUri: String) {
        require(serviceId.isNotEmpty()) { "Service ID cannot be empty" }
        require(redirectUri.isNotEmpty()) { "Redirect URI cannot be empty" }
        this.serviceId = serviceId
        this.redirectUri = redirectUri
    }

    /**
     * Call this from your Deep Link Activity when the redirect intent is received.
     */
    fun handleRedirectIntent(intent: Intent, nonce: String): Boolean {
        val data = intent.data ?: return false
        val idToken = data.getQueryParameter("id_token")
        val error = data.getQueryParameter("error")

        return when {
            !idToken.isNullOrEmpty() -> {
                AppleSignInCCTRegistry.resumeSuccess(nonce, AppleSignInResult(idToken))
                true
            }
            !error.isNullOrEmpty() -> {
                AppleSignInCCTRegistry.resumeError(nonce, Exception("Apple login error: $error"))
                true
            }
            else -> false
        }
    }
}

/**
 * Extension function that enables Apple Sign-In to be used as a Flow utilizing CCT.
 */
fun SignInWithAppleCCT.flow(context: Context, nonce: String): Flow<AppleSignInResult> = flow {
    val result = suspendCancellableCoroutine { cont ->
        AppleSignInCCTRegistry.register(nonce, cont)

        // Ideally, you would use androidx.browser.customtabs.CustomTabsIntent here.
        // For simplicity, we launch a standard ACTION_VIEW which CCT or the default browser will handle.
        val authUrl = "https://appleid.apple.com/auth/authorize?client_id=\${serviceId}&redirect_uri=\${redirectUri}&response_type=code%20id_token&scope=name%20email&response_mode=form_post&nonce=\$nonce"
        
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    }
    emit(result)
}
