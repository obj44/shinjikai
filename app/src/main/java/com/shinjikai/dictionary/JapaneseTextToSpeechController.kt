package com.shinjikai.dictionary

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private enum class JapaneseTextToSpeechStatus {
    NotStarted,
    Initializing,
    Ready,
    Unavailable
}

internal class JapaneseTextToSpeechController(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingCallbacks = mutableListOf<(TextToSpeech?) -> Unit>()
    private var textToSpeech: TextToSpeech? = null
    private var status = JapaneseTextToSpeechStatus.NotStarted

    fun ensure(onReady: (TextToSpeech?) -> Unit) {
        val existing = textToSpeech
        when {
            status == JapaneseTextToSpeechStatus.Ready && existing != null -> {
                onReady(existing)
                return
            }
            status == JapaneseTextToSpeechStatus.Unavailable -> {
                onReady(null)
                return
            }
            status == JapaneseTextToSpeechStatus.Initializing -> {
                pendingCallbacks += onReady
                return
            }
        }

        pendingCallbacks += onReady
        status = JapaneseTextToSpeechStatus.Initializing
        var deferredInitStatus: Int? = null
        fun handleInitStatus(initStatus: Int) {
            val readyInstance = textToSpeech
            if (readyInstance == null) {
                deferredInitStatus = initStatus
                return
            }
            val applyResult = {
                if (initStatus == TextToSpeech.SUCCESS) {
                    val languageResult = readyInstance.setLanguage(Locale.JAPANESE)
                    val canSpeakJapanese = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                        languageResult != TextToSpeech.LANG_NOT_SUPPORTED
                    if (canSpeakJapanese) {
                        readyInstance.setSpeechRate(0.92f)
                        readyInstance.setPitch(1f)
                        status = JapaneseTextToSpeechStatus.Ready
                        complete(readyInstance)
                    } else {
                        readyInstance.shutdown()
                        textToSpeech = null
                        status = JapaneseTextToSpeechStatus.Unavailable
                        complete(null)
                    }
                } else {
                    readyInstance.shutdown()
                    textToSpeech = null
                    status = JapaneseTextToSpeechStatus.Unavailable
                    complete(null)
                }
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                applyResult()
            } else {
                mainHandler.post(applyResult)
            }
        }

        val instance = TextToSpeech(appContext) { initStatus -> handleInitStatus(initStatus) }
        textToSpeech = instance
        deferredInitStatus?.let(::handleInitStatus)
    }

    suspend fun await(): TextToSpeech? = suspendCancellableCoroutine { continuation ->
        val callback: (TextToSpeech?) -> Unit = { instance ->
            if (continuation.isActive) {
                continuation.resume(instance)
            }
        }
        ensure(callback)
        continuation.invokeOnCancellation {
            pendingCallbacks.remove(callback)
        }
    }

    fun speak(text: String, utteranceId: String, onUnavailable: () -> Unit) {
        ensure { instance ->
            if (instance == null) {
                onUnavailable()
            } else {
                instance.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
        }
    }

    fun shutdown() {
        pendingCallbacks.clear()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        status = JapaneseTextToSpeechStatus.NotStarted
    }

    private fun complete(instance: TextToSpeech?) {
        val callbacks = pendingCallbacks.toList()
        pendingCallbacks.clear()
        callbacks.forEach { callback -> callback(instance) }
    }
}

@Composable
internal fun rememberJapaneseTextToSpeechController(): JapaneseTextToSpeechController {
    val context = LocalContext.current
    val controller = remember(context.applicationContext) {
        JapaneseTextToSpeechController(context.applicationContext)
    }
    DisposableEffect(controller) {
        onDispose { controller.shutdown() }
    }
    return controller
}
