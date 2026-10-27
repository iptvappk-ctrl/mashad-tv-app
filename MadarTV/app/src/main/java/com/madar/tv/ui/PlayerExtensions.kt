package com.madar.tv.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun setScreenOrientation(context: Context, landscape: Boolean) {
    val activity = context.findActivity() ?: return
    activity.requestedOrientation = if (landscape) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    val window = activity.window ?: return
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    if (landscape) {
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

internal fun resetScreenOrientation(context: Context) {
    val activity = context.findActivity() ?: return
    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    val window = activity.window ?: return
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.show(WindowInsetsCompat.Type.systemBars())
}
