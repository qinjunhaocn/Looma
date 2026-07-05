package com.voxyn.looma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.voxyn.looma.ui.screens.RecordScreen
import com.voxyn.looma.ui.theme.LoomaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoomaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RecordScreen()
                }
            }
        }
    }
}
