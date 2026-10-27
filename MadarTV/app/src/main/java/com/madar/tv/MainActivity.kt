package com.madar.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.madar.tv.core.MashadTelemetry
import com.madar.tv.ui.MadarApp
import com.madar.tv.ui.theme.MadarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MashadTelemetry.init(this)
        MashadTelemetry.onAppOpen()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            MadarTheme {
                MadarApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MashadTelemetry.onAppOpen()
    }

    override fun onStop() {
        super.onStop()
        MashadTelemetry.onAppClose()
    }
}
