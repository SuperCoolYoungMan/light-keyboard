package com.thelightphone.lp3keyboard

import android.content.SharedPreferences
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardSwipeCallback
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardView
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.layout.LayoutRegistryItem
import com.thelightphone.lp3Keyboard.ui.layout.buildRootViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback

class IMEService : LifecycleInputMethodService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner,
    Lp3RepeatableKeyboardCallback {

    private var renderedLayout: LayoutRegistryItem? = null
    private var viewModel: Lp3KeyboardViewModel<*>? = null
    private val hangulComposer = HangulComposer()
    private val haptics by lazy { KeyboardHaptics(this) }

    private var layoutPrefs: SharedPreferences? = null
    private val layoutChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == LayoutPreferences.KEY_ACTIVE_LAYOUT) {
                refreshLayoutIfNeeded()
            }
        }

    private fun isKoreanLayout() = renderedLayout == LayoutRegistryItem.KoDubeolsik

    private fun finishHangulComposition() {
        if (hangulComposer.isEmpty) return
        currentInputConnection?.finishComposingText()
        hangulComposer.clear()
    }

    private fun refreshLayoutIfNeeded() {
        if (LayoutPreferences.getActiveLayout(this) != renderedLayout) {
            finishHangulComposition()
            setInputView(onCreateInputView())
        }
    }

    private fun buildViewModel(layout: LayoutRegistryItem): Lp3KeyboardViewModel<*> {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val dummySwipeCallback = object : Lp3KeyboardSwipeCallback<Unit> {}
                val hapticCallback: () -> Unit =
                    if (layout == LayoutRegistryItem.KoDubeolsik) ({}) else ::tick
                return layout.buildRootViewModel(
                    this@IMEService,
                    dummySwipeCallback,
                    haptic = hapticCallback
                ) as T
            }
        }
        // Key by the layout's uniqueId so each layout gets its own retained ViewModel instance.
        return ViewModelProvider(store, factory)[layout.uniqueId, ViewModel::class.java]
                as Lp3KeyboardViewModel<*>
    }

    override fun onCreateInputView(): View {
        val layout = LayoutPreferences.getActiveLayout(this)
        val vm = buildViewModel(layout)
        renderedLayout = layout
        viewModel = vm

        val view = Lp3KeyboardView(
            context = this,
            viewModel = vm,
            // don't need to remap since no external keyboard
            remapKeyCode = null
        ).apply {
            // don't need the keyboard view itself ot handle external keys, Android inputs will do it
            handleHardwareKeyboardInput = false
        }
        setCandidatesViewShown(false)
        window?.window?.let {
            it.decorView.apply {
                setViewTreeLifecycleOwner(this@IMEService)
                setViewTreeViewModelStoreOwner(this@IMEService)
                setViewTreeSavedStateRegistryOwner(this@IMEService)
            }
        }
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        refreshLayoutIfNeeded()
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        layoutPrefs = LayoutPreferences.registerOnChange(this, layoutChangeListener)
    }

    override fun onDestroy() {
        layoutPrefs?.unregisterOnSharedPreferenceChangeListener(layoutChangeListener)
        store.clear()
        super.onDestroy()
    }

    override val viewModelStore: ViewModelStore
        get() = store
    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    private val store = ViewModelStore()

    private fun tick() {
        haptics.perform(KeyboardHapticEvent.Key)
    }

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onWindowHidden() {
        finishHangulComposition()
        super.onWindowHidden()
        viewModel?.cancelHeldKeys()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        finishHangulComposition()
        updateCapsMode()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )
        if (!hangulComposer.isEmpty &&
            (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)
        ) {
            finishHangulComposition()
        }
    }

    private fun updateCapsMode() {
        if (isKoreanLayout()) return
        val ic = currentInputConnection ?: return
        val ei = currentInputEditorInfo ?: return
        // might be set if the TextField is set to capitalize sentence starts, for example
        val caps = ic.getCursorCapsMode(ei.inputType)
        viewModel?.setCapsMode(caps != 0)
    }

    override fun onKeyPressed(code: Int) {
        if (isKoreanLayout()) haptics.perform(KeyboardHapticEvent.Key)
    }

    override fun onSubmitWord(word: CharSequence) {
        finishHangulComposition()
        currentInputConnection?.commitText("$word ", 1)
    }

    override fun onSpecialKeyPressed(key: SpecialKey) {
        if (isKoreanLayout()) {
            val event = when (key) {
                SpecialKey.Space -> KeyboardHapticEvent.Space
                SpecialKey.UpCase, SpecialKey.DownCase -> KeyboardHapticEvent.Shift
                SpecialKey.Backspace -> KeyboardHapticEvent.Backspace
                SpecialKey.Return, SpecialKey.Submit -> KeyboardHapticEvent.Enter
                else -> KeyboardHapticEvent.Key
            }
            haptics.perform(event)
        }

        when (key) {
            SpecialKey.Space -> {
                finishHangulComposition()
                currentInputConnection?.commitText(" ", 1)
                updateCapsMode()
            }

            else -> {}
        }
    }

    override fun onKeyReleased(code: Int) {
        val text = buildString { appendCodePoint(code) }
        val char = text.singleOrNull()
        if (isKoreanLayout() && char != null && HangulComposer.isHangulKey(char)) {
            currentInputConnection?.setComposingText(hangulComposer.input(char), 1)
            return
        }

        finishHangulComposition()
        currentInputConnection?.commitText(text, 1)
        updateCapsMode()
    }

    override fun onSpecialKeyReleased(key: SpecialKey) {
        when (key) {
            SpecialKey.Backspace -> {
                val ic = currentInputConnection ?: return
                if (isKoreanLayout() && !hangulComposer.isEmpty) {
                    val text = hangulComposer.backspace()
                    ic.setComposingText(text, 1)
                    if (text.isEmpty()) ic.finishComposingText()
                    return
                }

                val before = ic.getTextBeforeCursor(1, 0)
                val charsToDelete =
                    if (!before.isNullOrEmpty() && Character.isLowSurrogate(before[0])) 2 else 1
                ic.deleteSurroundingText(charsToDelete, 0)
                updateCapsMode()
            }

            SpecialKey.Return -> {
                finishHangulComposition()
                currentInputConnection?.commitText("\n", 1)
            }

            SpecialKey.Close -> {
                finishHangulComposition()
                requestHideSelf(0)
            }

            else -> {}
        }
    }

    override fun onKeyLongPressed(code: Int) {
        if (isKoreanLayout()) haptics.perform(KeyboardHapticEvent.LongPress)
    }

    private fun deletePrecedingWord() {
        finishHangulComposition()
        val ic = currentInputConnection ?: return
        // Get text before cursor to find the word boundary (max 100 chars long)
        val before = ic.getTextBeforeCursor(100, 0) ?: return
        val trimmed = before.trimEnd()
        val lastSpace = trimmed.indexOfLast { it.isWhitespace() }
        // Delete from cursor back to start of word (including trailing spaces)
        val charsToDelete = before.length - (if (lastSpace >= 0) lastSpace + 1 else 0)
        ic.deleteSurroundingText(charsToDelete, 0)
        updateCapsMode()
    }

    override fun onSpecialKeyLongPressed(key: SpecialKey) {
        if (isKoreanLayout()) haptics.perform(KeyboardHapticEvent.LongPress)

        when (key) {
            SpecialKey.Backspace -> {
                deletePrecedingWord()
            }

            else -> {}
        }
    }

    override fun onKeyRepeated(code: Int) {
        onKeyReleased(code)
    }

    override fun onSpecialKeyRepeated(specialKey: SpecialKey) {
        if (isKoreanLayout()) {
            when (specialKey) {
                SpecialKey.Backspace -> haptics.perform(KeyboardHapticEvent.BackspaceRepeat)
                SpecialKey.Space -> haptics.perform(KeyboardHapticEvent.Space)
                else -> {}
            }
        }

        when (specialKey) {
            SpecialKey.Space -> {
                finishHangulComposition()
                currentInputConnection?.commitText(" ", 1)
                updateCapsMode()
            }

            SpecialKey.Backspace -> {
                deletePrecedingWord()
            }

            else -> {}
        }
    }
}
