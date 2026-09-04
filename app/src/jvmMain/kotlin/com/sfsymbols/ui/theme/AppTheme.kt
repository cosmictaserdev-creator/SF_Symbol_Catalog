package com.sfsymbols.ui.theme

import androidx.compose.runtime.Composable
import dev.nucleusframework.macoscompose.theme.AccentColor
import dev.nucleusframework.macoscompose.theme.GlassType
import dev.nucleusframework.macoscompose.theme.MacosTheme

/**
 * App theme wrapper. Uses the macOS 26 design language from compose-macos-ui,
 * with Liquid Glass enabled so native [Surface] materials render frosted glass.
 */
@Composable
public fun AppTheme(
    darkTheme: Boolean = true,
    accent: AccentColor = AccentColor.Blue,
    content: @Composable () -> Unit
) {
    MacosTheme(
        darkTheme = darkTheme,
        accentColor = accent,
        liquidGlass = true,
        glassType = GlassType.Tinted,
    ) {
        content()
    }
}
