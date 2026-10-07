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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import com.sfsymbols.data.SfSymbolIconResolver
import com.sfsymbols.data.SfSymbolsCatalog
import com.sfsymbols.data.SymbolMode
import androidx.compose.ui.unit.dp
import com.sfsymbols.viewmodel.CatalogViewModel
import dev.nucleusframework.macoscompose.components.Surface
import dev.nucleusframework.macoscompose.components.Text
import dev.nucleusframework.macoscompose.theme.GlassMaterialSize
import dev.nucleusframework.macoscompose.theme.MacosTheme

/** Rendering-style categories that the SF Symbols app lists under "Library". */
private val LIBRARY_CATEGORIES = listOf("Multicolor", "Variable", "Draw")

/** Sidebar glyph per row, matching the SF Symbols app's sidebar. */
private val CATEGORY_ICONS = mapOf(
    "All Symbols" to "square.grid.2x2",
    "Favorites" to "heart",
    "Multicolor" to "paintpalette",
    "Variable" to "slider.horizontal.3",
    "Draw" to "pencil.and.scribble",
    "Communication" to "message",
    "Weather" to "cloud.sun",
    "Maps" to "map",
    "Objects & Tools" to "folder",
    "Devices" to "desktopcomputer",
    "Camera & Photos" to "camera",
    "Gaming" to "gamecontroller",
    "Connectivity" to "antenna.radiowaves.left.and.right",
    "Transportation" to "bicycle",
    "Automotive" to "steeringwheel",
    "Accessibility" to "accessibility",
    "Privacy & Security" to "lock.shield",
    "Human" to "person.crop.circle",
    "Home" to "house",
    "Fitness" to "figure.run",
    "Nature" to "leaf",
    "Editing" to "pencil",
    "Text Formatting" to "textformat",
    "Media" to "play.rectangle",
    "Keyboard" to "command",
    "Commerce" to "cart",
    "Time" to "timer",
    "Health" to "heart.text.square",
    "Shapes" to "square.on.circle",
    "Arrows" to "arrow.forward",
    "Indices" to "a.circle",
    "Math" to "sum",
)

/**
 * Leading pane styled after the SF Symbols app: a "Library" section
 * (all, favorites, rendering styles) above the category list, each row
 * led by its SF Symbol glyph.
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
            SidebarSectionLabel("Library")
            SidebarRow(
                label = "All Symbols",
                count = SfSymbolsCatalog.all.size,
                selected = viewModel.selectedCategory == null && !viewModel.onlyPinned,
                onClick = { viewModel.showAll() },
            )
            SidebarRow(
                label = "Favorites",
                count = viewModel.pinnedSymbols.size,
                selected = viewModel.onlyPinned,
                onClick = { viewModel.showOnlyPinned() },
            )
            viewModel.categories.filter { it.first in LIBRARY_CATEGORIES }.forEach { (category, count) ->
                SidebarRow(
                    label = category,
                    count = count,
                    selected = viewModel.selectedCategory == category,
                    onClick = { viewModel.filterCategory(category) },
                )
            }

            Spacer(Modifier.height(12.dp))
            SidebarSectionLabel("Categories")
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(viewModel.categories.filter { it.first !in LIBRARY_CATEGORIES }) { (category, count) ->
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
            val glyph = remember(label) {
                CATEGORY_ICONS[label]?.let { name ->
                    SfSymbolsCatalog.all.firstOrNull { it.appleName == name }
                        ?.let { SfSymbolIconResolver.resolve(it, SymbolMode.Monochrome) }
                }
            }
            if (glyph != null) {
                Icon(
                    imageVector = glyph,
                    contentDescription = null,
                    tint = if (selected) cs.onAccent else cs.accent,
                    modifier = Modifier.padding(end = 8.dp).size(16.dp),
                )
            }
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
