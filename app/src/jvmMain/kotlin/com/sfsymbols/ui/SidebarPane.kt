package com.sfsymbols.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sfsymbols.viewmodel.CatalogViewModel
import dev.nucleusframework.macoscompose.components.Surface
import dev.nucleusframework.macoscompose.components.Text
import dev.nucleusframework.macoscompose.theme.GlassMaterialSize
import dev.nucleusframework.macoscompose.theme.MacosTheme

/**
 * Leading pane: shortcuts + category list, floating as a rounded glass panel.
 * Light greyish rim with ~30% opacity border for a subtle glow.
 */
@Composable
public fun SidebarPane(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
) {
    val cs = MacosTheme.colorScheme
    Surface(
        modifier = modifier
            .width(238.dp),
        materialSize = GlassMaterialSize.Medium,
        tintColor = cs.surface.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, bottom = 12.dp),
        ) {
            SidebarSectionLabel("SF Symbols")
            Spacer(Modifier.height(12.dp))
            SidebarSectionLabel("Browse")
            SidebarRow(
                label = "All Symbols",
                count = viewModel.filteredSymbols.size,
                selected = viewModel.selectedCategory == null && !viewModel.onlyPinned,
                onClick = { viewModel.showAll() },
            )
            SidebarRow(
                label = "Favorites",
                count = viewModel.pinnedSymbols.size,
                selected = viewModel.onlyPinned,
                onClick = { viewModel.showOnlyPinned() },
            )

            Spacer(Modifier.height(12.dp))
            SidebarSectionLabel("Categories")
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(viewModel.categories) { (category, count) ->
                    SidebarRow(
                        label = category,
                        count = count,
                        selected = viewModel.selectedCategory == category,
                        onClick = { viewModel.filterCategory(category) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (viewModel.screen == CatalogViewModel.Screen.Settings) {
                            cs.accent
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable(onClick = { viewModel.navigate(CatalogViewModel.Screen.Settings) })
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Settings",
                    color = if (viewModel.screen == CatalogViewModel.Screen.Settings) cs.onAccent else cs.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "\u2699",
                    color = if (viewModel.screen == CatalogViewModel.Screen.Settings) cs.onAccent else cs.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun SidebarSectionLabel(text: String) {
    val cs = MacosTheme.colorScheme
    Text(
        text = text.uppercase(),
        color = cs.textTertiary,
        modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun SidebarRow(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cs = MacosTheme.colorScheme
    val bg by animateColorAsState(
        when {
            selected -> cs.accent
            else -> Color.Transparent
        },
        label = "sidebar-bg",
    )
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = if (selected) cs.onAccent else cs.textPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (count > 0) {
                Text(
                    text = count.toString(),
                    color = if (selected) cs.onAccent.copy(alpha = 0.8f) else cs.textTertiary,
                )
            }
        }
    }
}