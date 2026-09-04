package com.thelightphone.lp3keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

internal data class ImeDiagnosticSnapshot(
    val enabled: Boolean,
    val isDefault: Boolean,
    val enabledImeCount: Int,
    val defaultImeId: String?,
)

internal object ImeDiagnostics {
    internal fun normalizeImeId(raw: String?): String? {
        val value = raw?.substringBefore(';')?.trim().orEmpty()
        if (value.isEmpty()) return null

        val slash = value.indexOf('/')
        if (slash <= 0 || slash == value.lastIndex) return value

        val packageName = value.substring(0, slash)
        val className = value.substring(slash + 1)
        val expandedClassName = if (className.startsWith('.')) packageName + className else className
        return "$packageName/$expandedClassName"
    }

    internal fun parseEnabledImeIds(raw: String?): Set<String> = raw
        .orEmpty()
        .split(':')
        .mapNotNull(::normalizeImeId)
        .toSet()

    internal fun formatReport(snapshot: ImeDiagnosticSnapshot): String = buildString {
        appendLine("Light Keyboard · IME diagnostics")
        appendLine("This keyboard enabled: ${yesNo(snapshot.enabled)}")
        appendLine("This keyboard is default: ${yesNo(snapshot.isDefault)}")
        appendLine("Enabled IME count: ${snapshot.enabledImeCount}")
        append("Default IME: ${snapshot.defaultImeId ?: "none"}")
    }

    fun read(context: Context): ImeDiagnosticSnapshot {
        val resolver = context.contentResolver
        val enabledRaw = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_INPUT_METHODS)
        val defaultRaw = Settings.Secure.getString(resolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val ourImeId = ComponentName(context, IMEService::class.java).let {
            "${it.packageName}/${it.className}"
        }
        val enabledIds = parseEnabledImeIds(enabledRaw)
        val normalizedDefault = normalizeImeId(defaultRaw)

        return ImeDiagnosticSnapshot(
            enabled = ourImeId in enabledIds,
            isDefault = normalizedDefault == ourImeId,
            enabledImeCount = enabledIds.size,
            defaultImeId = defaultRaw?.takeIf { it.isNotBlank() },
        )
    }

    private fun yesNo(value: Boolean): String = if (value) "YES" else "NO"
}

/**
 * Read-only LP3 bring-up screen for verifying whether this APK is actually exposed by LightOS
 * as an Android input method. Launch explicitly over ADB when diagnosing a physical device:
 * `adb shell am start -n com.thelightphone.lp3keyboard/.ImeDiagnosticsActivity`.
 */
class ImeDiagnosticsActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private var latestSnapshot: ImeDiagnosticSnapshot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val padding = (18 * resources.displayMetrics.density).toInt()
        status = TextView(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            addView(TextView(this@ImeDiagnosticsActivity).apply {
                text = "Light Keyboard · IME diagnostics"
                textSize = 20f
            })
            addView(status)
            addView(Button(this@ImeDiagnosticsActivity).apply {
                text = "Refresh status"
                setOnClickListener { refreshStatus() }
            })
            addView(Button(this@ImeDiagnosticsActivity).apply {
                text = "Copy diagnostics"
                setOnClickListener { copyDiagnostics() }
            })
            addView(Button(this@ImeDiagnosticsActivity).apply {
                text = "Open keyboard settings"
                setOnClickListener {
                    startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                }
            })
            addView(Button(this@ImeDiagnosticsActivity).apply {
                text = "Choose active keyboard"
                setOnClickListener {
                    getSystemService(InputMethodManager::class.java).showInputMethodPicker()
                }
            })
        }

        setContentView(root)
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    private fun refreshStatus() {
        val snapshot = ImeDiagnostics.read(this)
        latestSnapshot = snapshot
        status.text = "\n${ImeDiagnostics.formatReport(snapshot).substringAfter('\n')}\n"
    }

    private fun copyDiagnostics() {
        val snapshot = latestSnapshot ?: ImeDiagnostics.read(this).also { latestSnapshot = it }
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Light Keyboard IME diagnostics", ImeDiagnostics.formatReport(snapshot)))
        Toast.makeText(this, "Diagnostics copied", Toast.LENGTH_SHORT).show()
    }
}
