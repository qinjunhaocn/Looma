package com.voxyn.looma.ui.theme

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun LoomaTheme(
    content: @Composable () -> Unit,
) {
    val controller = androidx.compose.runtime.remember {
        ThemeController(colorSchemeMode = ColorSchemeMode.Dark)
    }
    MiuixTheme(controller = controller, content = content)
}
