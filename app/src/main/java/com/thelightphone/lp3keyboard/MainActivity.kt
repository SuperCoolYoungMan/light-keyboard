package com.thelightphone.lp3keyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.layout.LayoutRegistryItem

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KeyboardSettings() }
    }
}

@Composable
fun KeyboardSettings() {
    val ctx = LocalContext.current
    val haptics = remember(ctx) { KeyboardHaptics(ctx) }
    val (text, setValue) = remember { mutableStateOf(TextFieldValue("안녕하세요 Hello")) }

    Column(
        modifier = Modifier
            .systemBarsPadding()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
            .fillMaxWidth(),
    ) {
        Text(text = "Light Keyboard", fontWeight = FontWeight.Bold)
        Text(text = "Korean Dubeolsik + tactile tuning")

        Spacer(Modifier.height(18.dp))
        SectionTitle("Keyboard")

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        ) {
            Text("Open keyboard settings")
        }

        Spacer(Modifier.height(8.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val imm = ctx.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                imm.showInputMethodPicker()
            }
        ) {
            Text("Choose active keyboard")
        }

        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(14.dp))
        SectionTitle("Layout")
        Text("Long-press Space while typing to switch Korean ↔ English.")
        Spacer(Modifier.height(8.dp))
        LayoutPicker()

        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(14.dp))
        SectionTitle("Haptic feel")
        Text("Crisp is the reference tuning: short, precise, and low-rumble.")
        Spacer(Modifier.height(8.dp))
        HapticStrengthPicker(onPreview = {
            haptics.perform(KeyboardHapticEvent.Space)
        })

        Spacer(Modifier.height(12.dp))
        Text("Preview")
        Spacer(Modifier.height(6.dp))
        HapticPreviewButton("Character · soft tick") {
            haptics.perform(KeyboardHapticEvent.Key)
        }
        HapticPreviewButton("Space · crisp click") {
            haptics.perform(KeyboardHapticEvent.Space)
        }
        HapticPreviewButton("Language switch · two-stage") {
            haptics.perform(KeyboardHapticEvent.LanguageSwitch)
        }
        HapticPreviewButton("Enter · confirmation") {
            haptics.perform(KeyboardHapticEvent.Enter)
        }

        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(14.dp))
        SectionTitle("Typing test")
        TextField(
            value = text,
            onValueChange = setValue,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, fontWeight = FontWeight.Bold)
}

@Composable
private fun HapticPreviewButton(label: String, onClick: () -> Unit) {
    Button(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(label)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
fun HapticStrengthPicker(onPreview: () -> Unit) {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf(HapticPreferences.getStrength(ctx)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        HapticStrength.entries.forEach { strength ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = strength == selected,
                        onClick = {
                            selected = strength
                            HapticPreferences.setStrength(ctx, strength)
                            if (strength != HapticStrength.Off) onPreview()
                        }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = strength == selected,
                    onClick = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(strength.label)
                    if (strength == HapticStrength.Crisp) {
                        Text("Recommended")
                    }
                }
            }
        }
    }
}

@Composable
fun LayoutPicker() {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf(LayoutPreferences.getActiveLayout(ctx)) }
    Column(modifier = Modifier.fillMaxWidth()) {
        LayoutRegistryItem.entries.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = item == selected,
                        onClick = {
                            selected = item
                            LayoutPreferences.setActiveLayout(ctx, item)
                        },
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = item == selected,
                    onClick = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = item.label)
            }
        }
    }
}
