package io.github.shinhyo.signinwithapple

import io.github.shinhyo.signinwithapple.model.AppleSignInResult
import kotlinx.coroutines.CancellableContinuation
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object AppleSignInCCTRegistry {
    private val pending = ConcurrentHashMap<String, CancellableContinuation<AppleSignInResult>>()

    fun register(nonce: String, continuation: CancellableContinuation<AppleSignInResult>) {
        pending[nonce] = continuation
        continuation.invokeOnCancellation { pending.remove(nonce) }
    }

    fun resumeSuccess(nonce: String, result: AppleSignInResult) {
        pending.remove(nonce)?.resume(result)
    }

    fun resumeError(nonce: String, error: Exception) {
        pending.remove(nonce)?.resumeWithException(error)
    }
}
