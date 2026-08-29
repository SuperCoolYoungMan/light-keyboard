package com.thelightphone.lp3Keyboard.ui.layout

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.FinalRow
import com.thelightphone.lp3Keyboard.ui.FirstRow
import com.thelightphone.lp3Keyboard.ui.ICON_KEY_WIDTH_DP
import com.thelightphone.lp3Keyboard.ui.IconKey
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.lp3Keyboard.ui.LocalAkkuratFamily
import com.thelightphone.lp3Keyboard.ui.Lp3KeyboardCallback
import com.thelightphone.lp3Keyboard.ui.MultiLabelKey
import com.thelightphone.lp3Keyboard.ui.R
import com.thelightphone.lp3Keyboard.ui.SecondRow
import com.thelightphone.lp3Keyboard.ui.SpecialKey
import com.thelightphone.lp3Keyboard.ui.ThirdRow

object KoDubeolsik {
    private const val FIRST_ROW = "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ"
    private const val FIRST_ROW_SHIFT = "ㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖ"
    private const val SECOND_ROW = "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ"
    private const val THIRD_ROW = "ㅋㅌㅊㅍㅠㅜㅡ"

    object LowerCaseLayout : Layout {
        override val isRootLayout: Boolean
            get() = true

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            CompositionLocalProvider(LocalAkkuratFamily provides FontFamily.Default) {
                FirstRow(FIRST_ROW, callback, null, options.enableKeyAnimation)
                SecondRow(SECOND_ROW, callback, null, options.enableKeyAnimation)
                ThirdRow(THIRD_ROW, callback, null, options) {
                    IconKey(
                        R.drawable.up_lp3,
                        SpecialKey.UpCase,
                        callback,
                        options.enableKeyAnimation,
                        width = ICON_KEY_WIDTH_DP.dp,
                        modifier = Modifier.padding(12.dp).padding(bottom = 6.dp, end = 8.dp)
                    )
                }
            }
            FinalRow(options, callback) {
                MultiLabelKey("123", SpecialKey.Numbers, callback, options.enableKeyAnimation)
            }
        }
    }

    object CapsLockedLayout : Layout {
        override val isRootLayout: Boolean
            get() = true

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            CompositionLocalProvider(LocalAkkuratFamily provides FontFamily.Default) {
                FirstRow(FIRST_ROW_SHIFT, callback, null, options.enableKeyAnimation)
                SecondRow(SECOND_ROW, callback, null, options.enableKeyAnimation)
                ThirdRow(THIRD_ROW, callback, null, options) {
                    IconKey(
                        R.drawable.caps_lp3,
                        SpecialKey.DownCase,
                        callback,
                        options.enableKeyAnimation,
                        width = ICON_KEY_WIDTH_DP.dp,
                        modifier = Modifier.padding(9.dp).padding(bottom = 2.dp, end = 4.dp)
                    )
                }
            }
            FinalRow(options, callback) {
                MultiLabelKey("123", SpecialKey.Numbers, callback, options.enableKeyAnimation)
            }
        }
    }

    object UpperCaseLayout : Layout {
        override val isRootLayout: Boolean
            get() = true

        @Composable
        override fun ColumnScope.Render(
            options: KeyboardOptions,
            callback: Lp3KeyboardCallback
        ) {
            CompositionLocalProvider(LocalAkkuratFamily provides FontFamily.Default) {
                FirstRow(FIRST_ROW_SHIFT, callback, null, options.enableKeyAnimation)
                SecondRow(SECOND_ROW, callback, null, options.enableKeyAnimation)
                ThirdRow(THIRD_ROW, callback, null, options) {
                    IconKey(
                        R.drawable.down_lp3,
                        SpecialKey.DownCase,
                        callback,
                        options.enableKeyAnimation,
                        width = ICON_KEY_WIDTH_DP.dp,
                        modifier = Modifier.padding(12.dp).padding(bottom = 6.dp, end = 8.dp)
                    )
                }
            }
            FinalRow(options, callback) {
                MultiLabelKey("123", SpecialKey.Numbers, callback, options.enableKeyAnimation)
            }
        }
    }
}
