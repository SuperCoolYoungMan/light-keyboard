package com.thelightphone.lp3keyboard

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.thelightphone.lp3Keyboard.ui.AdaptiveMotionMode
import com.thelightphone.lp3Keyboard.ui.AdaptiveMotionPreferences
import com.thelightphone.lp3Keyboard.ui.KeyMotionStyle
import com.thelightphone.lp3Keyboard.ui.MotionCadenceSample
import com.thelightphone.lp3Keyboard.ui.TypingCadenceTracker
import com.thelightphone.lp3Keyboard.ui.layout.LayoutRegistryItem
import com.thelightphone.lp3Keyboard.ui.premiumKeyMotion
import kotlin.math.roundToInt

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
    val capabilities = remember(haptics) { haptics.capabilities() }
    var text by remember { mutableStateOf(TextFieldValue("안녕하세요 Hello")) }
    var motionMode by remember { mutableStateOf(AdaptiveMotionPreferences.getMode(ctx)) }
    val labTracker = remember { TypingCadenceTracker() }
    var labSample by remember { mutableStateOf(MotionCadenceSample(0L, null, 1f)) }

    fun recordLabPress(): MotionCadenceSample {
        val sample = labTracker.recordPress(SystemClock.uptimeMillis(), motionMode)
        labSample = sample
        return sample
    }

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
        Text("Hardware profile")
        Text(
            if (capabilities.fullCrispProfile) {
                "Full primitive composition supported"
            } else {
                "Partial primitive support · automatic fallback enabled"
            }
        )
        Text(
            "LowTick ${mark(capabilities.lowTick)}  Tick ${mark(capabilities.tick)}  " +
                "Click ${mark(capabilities.click)}  Thud ${mark(capabilities.thud)}"
        )
        Text("Rise ${mark(capabilities.quickRise)}  Fall ${mark(capabilities.quickFall)}")

        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(14.dp))
        SectionTitle("Device haptic calibration")
        Text("Blind A/B comparison on the actual phone. Five choices narrow the preferred strength for each action.")
        Spacer(Modifier.height(8.dp))
        HapticCalibrationLab(haptics)

        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(14.dp))
        SectionTitle("Adaptive motion")
        Text("Only visual travel changes with typing speed. Haptic strength stays untouched.")
        Spacer(Modifier.height(8.dp))
        AdaptiveMotionPicker(
            selected = motionMode,
            onSelected = { mode ->
                motionMode = mode
                AdaptiveMotionPreferences.setMode(ctx, mode)
                labTracker.reset()
                labSample = MotionCadenceSample(0L, null, 1f)
            }
        )

        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(14.dp))
        SectionTitle("Haptic / Motion Lab")
        Text("Press the samples or type below. Saved device calibration is applied automatically.")
        Spacer(Modifier.height(8.dp))
        MotionMeter(labSample, motionMode)
        Spacer(Modifier.height(10.dp))

        MotionLabKey(
            label = "Character · lift + soft tick",
            style = KeyMotionStyle.Character,
            onPress = {
                haptics.perform(KeyboardHapticEvent.Key)
                recordLabPress()
            }
        )
        MotionLabKey(
            label = "Space · quiet compression",
            style = KeyMotionStyle.Space,
            onPress = {
                haptics.perform(KeyboardHapticEvent.Space)
                recordLabPress()
            }
        )
        MotionLabKey(
            label = "Language switch · two-stage",
            style = KeyMotionStyle.ModeSwitch,
            onPress = {
                haptics.perform(KeyboardHapticEvent.LanguageSwitch)
                recordLabPress()
            }
        )
        MotionLabKey(
            label = "Enter · press + confirmation",
            style = KeyMotionStyle.Enter,
            onPress = {
                haptics.perform(KeyboardHapticEvent.Enter)
                recordLabPress()
            }
        )

        Spacer(Modifier.height(8.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                labTracker.reset()
                labSample = MotionCadenceSample(0L, null, 1f)
            }
        ) {
            Text("Reset motion lab")
        }

        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(14.dp))
        SectionTitle("Typing test")
        TextField(
            value = text,
            onValueChange = { next ->
                if (next.text != text.text) recordLabPress()
                text = next
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(24.dp))
    }
}

private fun mark(value: Boolean): String = if (value) "✓" else "–"

private fun scaleLabel(scale: Float): String = "${(scale * 100f).roundToInt()}%"

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, fontWeight = FontWeight.Bold)
}

@Composable
private fun HapticCalibrationLab(haptics: KeyboardHaptics) {
    val ctx = LocalContext.current
    var selectedTarget by remember { mutableStateOf(HapticCalibrationTarget.Character) }
    var generation by remember { mutableIntStateOf(0) }
    val session = remember(selectedTarget, generation) { HapticCalibrationSession() }
    var pair by remember(selectedTarget, generation) { mutableStateOf(session.currentPair()) }
    var status by remember(selectedTarget, generation) { mutableStateOf<String?>(null) }
    var profileRevision by remember { mutableIntStateOf(0) }

    fun resetSession() {
        generation += 1
    }

    fun preferA() {
        val chosen = session.chooseA()
        if (session.isComplete) {
            HapticCalibrationPreferences.setScale(ctx, selectedTarget, chosen)
            profileRevision += 1
            status = "Saved ${selectedTarget.label} at ${scaleLabel(chosen)}"
        } else {
            pair = session.currentPair()
            status = null
        }
    }

    fun preferB() {
        val chosen = session.chooseB()
        if (session.isComplete) {
            HapticCalibrationPreferences.setScale(ctx, selectedTarget, chosen)
            profileRevision += 1
            status = "Saved ${selectedTarget.label} at ${scaleLabel(chosen)}"
        } else {
            pair = session.currentPair()
            status = null
        }
    }

    Text("Target")
    HapticCalibrationTarget.entries.forEach { target ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = target == selectedTarget,
                    onClick = {
                        selectedTarget = target
                        status = null
                    }
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = target == selectedTarget,
                onClick = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("${target.label} · saved ${scaleLabel(HapticCalibrationPreferences.getScale(ctx, target))}")
        }
    }

    Spacer(Modifier.height(8.dp))
    Text(
        if (session.isComplete) {
            status ?: "Calibration complete"
        } else {
            "Round ${pair.round}/${pair.totalRounds} · blind comparison"
        },
        fontWeight = FontWeight.Bold,
    )
    Text("A/B ordering changes between rounds. Choose only by feel; candidate strength stays hidden until saved.")
    Text("Use Crisp while calibrating so A/B only measures the device-specific adjustment.")

    Spacer(Modifier.height(8.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !session.isComplete,
        onClick = { haptics.previewCalibration(selectedTarget, pair.aScale) },
    ) {
        Text("Play A")
    }
    Spacer(Modifier.height(6.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !session.isComplete,
        onClick = { haptics.previewCalibration(selectedTarget, pair.bScale) },
    ) {
        Text("Play B")
    }
    Spacer(Modifier.height(6.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !session.isComplete,
        onClick = ::preferA,
    ) {
        Text("Prefer A")
    }
    Spacer(Modifier.height(6.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !session.isComplete,
        onClick = ::preferB,
    ) {
        Text("Prefer B")
    }

    Spacer(Modifier.height(8.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { resetSession() },
    ) {
        Text("Restart this target")
    }
    Spacer(Modifier.height(6.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            HapticCalibrationPreferences.reset(ctx)
            profileRevision += 1
            status = "Device haptic profile reset to 100%"
            resetSession()
        },
    ) {
        Text("Reset all device calibration")
    }

    if (profileRevision < 0) Text("")
}

@Composable
private fun MotionMeter(sample: MotionCadenceSample, mode: AdaptiveMotionMode) {
    val speed = sample.keysPerSecond
    val speedLabel = speed?.let { "${((it * 10f).roundToInt() / 10f)} keys/s" } ?: "—"
    val factorLabel = "${(sample.factor * 100f).roundToInt()}%"
    Text("Mode ${mode.label} · cadence $speedLabel · motion $factorLabel")
}

@Composable
private fun MotionLabKey(
    label: String,
    style: KeyMotionStyle,
    onPress: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(pressed) {
        if (pressed) onPress()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .premiumKeyMotion(pressed = pressed, enabled = true, style = style)
            .background(Color(0xFFF1F1F1))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label)
    }
    Spacer(Modifier.height(7.dp))
}

@Composable
fun AdaptiveMotionPicker(
    selected: AdaptiveMotionMode,
    onSelected: (AdaptiveMotionMode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AdaptiveMotionMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = mode == selected, onClick = { onSelected(mode) })
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == selected, onClick = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(mode.label)
                    Text(mode.description)
                    if (mode == AdaptiveMotionMode.Balanced) Text("Recommended")
                }
            }
        }
    }
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
                RadioButton(selected = strength == selected, onClick = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(strength.label)
                    if (strength == HapticStrength.Crisp) Text("Recommended")
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
                RadioButton(selected = item == selected, onClick = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = item.label)
            }
        }
    }
}
