package com.skdev.ytlivevideo.util

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Starting with targetSdk 35 the window is always drawn edge-to-edge, so the content goes under
 * the status and navigation bars. Pads the activity content to keep it clear of the system bars
 * and the display cutout. Call it after `setContentView`.
 *
 * @param lightSystemBars draw dark system bar icons, for activities with a light background
 */
fun Activity.fitContentToSystemBars(lightSystemBars: Boolean = true) {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = lightSystemBars
        isAppearanceLightNavigationBars = lightSystemBars
    }
    val content = findViewById<View>(android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
        WindowInsetsCompat.CONSUMED
    }
}
