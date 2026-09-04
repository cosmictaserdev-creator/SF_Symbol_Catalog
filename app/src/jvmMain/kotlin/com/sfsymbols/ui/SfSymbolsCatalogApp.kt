package com.sfsymbols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sfsymbols.ui.theme.AppTheme
import com.sfsymbols.viewmodel.CatalogViewModel

/**
 * Root composable: theme + app chrome. Layout is assembled inside [AppChrome]
 * with macos-ui components.
 */
@Composable
public fun SfSymbolsCatalogApp() {
    val viewModel = remember { CatalogViewModel() }
    AppTheme(darkTheme = viewModel.darkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (viewModel.darkTheme) Color(0xFF171719) else Color.White)
        ) {
            when (viewModel.screen) {
                CatalogViewModel.Screen.Catalog -> AppChrome(viewModel, Modifier.fillMaxSize())
                CatalogViewModel.Screen.Settings -> SettingsPane(viewModel, Modifier.fillMaxSize())
            }
        }
    }
}
