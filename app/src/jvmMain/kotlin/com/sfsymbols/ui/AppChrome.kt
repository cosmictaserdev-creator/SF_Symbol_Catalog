package com.sfsymbols.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sfsymbols.data.SymbolMode
import com.sfsymbols.viewmodel.CatalogViewModel
import dev.nucleusframework.macoscompose.components.NavigationButtons
import dev.nucleusframework.macoscompose.components.PopupButton
import dev.nucleusframework.macoscompose.components.Surface
import dev.nucleusframework.macoscompose.components.Text
import dev.nucleusframework.macoscompose.components.TitleBar
import dev.nucleusframework.macoscompose.components.TitleBarButtonGroup
import dev.nucleusframework.macoscompose.components.TitleBarGroupButton
import dev.nucleusframework.macoscompose.components.SearchField
import dev.nucleusframework.macoscompose.components.ToolbarSeparator
import dev.nucleusframework.macoscompose.icons.Icon
import dev.nucleusframework.macoscompose.icons.LucideLayoutGrid
import dev.nucleusframework.macoscompose.icons.LucideList
import dev.nucleusframework.macoscompose.theme.GlassMaterialSize
import dev.nucleusframework.macoscompose.theme.MacosTheme

private val GRID_SIZE_OPTIONS = listOf("50%", "70%", "100%", "130%", "160%")
private val GRID_SIZE_SCALES = listOf(0.5f, 0.7f, 1f, 1.3f, 1.6f)

/**
 * App chrome: a Finder-style [TitleBar] at the top (natively reserving the
 * window traffic-light space) above a three-region body. The toolbar controls
 * (mode, layout size, search, count) live in a browser toolbar that sits
 * side-by-side with the sidebar.
 */
@Composable
public fun AppChrome(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FinderTitleBar(viewModel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            SidebarPane(
                viewModel,
                Modifier.fillMaxHeight(),
            )
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                BrowserToolbar(viewModel)
                SymbolGrid(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
                )
            }
            DetailPane(
                viewModel,
                Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .padding(vertical = 6.dp, horizontal = 6.dp),
            )
        }
    }
}

/**
 * Finder-style top bar. The native [TitleBar] reserves the window-control
 * (close/minimize/zoom) space on the left; it carries just the title and
 * navigation here — the actual controls live in [BrowserToolbar] below.
 */
@Composable
private fun FinderTitleBar(viewModel: CatalogViewModel) {
    TitleBar(
        title = {
            Text(browserTitle(viewModel))
        },
    )
}

private fun browserTitle(viewModel: CatalogViewModel): String =
    viewModel.selectedCategory
        ?: if (viewModel.onlyPinned) "Favorites" else "All Symbols"

/**
 * Browser toolbar below the title bar, laid out beside the sidebar. Holds the
 * symbol mode pop-up, layout-size pop-up, symbol count, and search field.
 */
@Composable
private fun BrowserToolbar(viewModel: CatalogViewModel) {
    val cs = MacosTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        materialSize = GlassMaterialSize.Medium,
        tintColor = cs.background.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back / forward navigation
            NavigationButtons(
                onBack = { viewModel.search("") },
                onForward = {},
                backEnabled = viewModel.query.isNotEmpty(),
                forwardEnabled = false,
            )
            Spacer(Modifier.width(12.dp))
            ToolbarSeparator()
            Spacer(Modifier.width(12.dp))

            // Symbol mode
            PopupButton(
                items = listOf("Dualtone", "Monochrome"),
                selectedIndex = if (viewModel.mode == SymbolMode.Dualtone) 0 else 1,
                onSelectedChange = { idx ->
                    viewModel.updateMode(if (idx == 0) SymbolMode.Dualtone else SymbolMode.Monochrome)
                },
                modifier = Modifier.width(150.dp),
                itemText = { it },
            )

            Spacer(Modifier.width(12.dp))
            ToolbarSeparator()
            Spacer(Modifier.width(12.dp))

            // Layout size (grid zoom)
            PopupButton(
                items = GRID_SIZE_OPTIONS,
                selectedIndex = nearestGridIndex(viewModel.gridScale),
                onSelectedChange = { idx ->
                    viewModel.updateGridScale(GRID_SIZE_SCALES[idx])
                },
                modifier = Modifier.width(110.dp),
                itemText = { it },
            )

            TitleBarButtonGroup {
                TitleBarGroupButton(
                    circularHighlight = !viewModel.listView,
                    onClick = { if (viewModel.listView) viewModel.toggleListView() },
                ) {
                    Icon(LucideLayoutGrid, modifier = Modifier.size(14.dp))
                }
                TitleBarGroupButton(
                    circularHighlight = viewModel.listView,
                    onClick = { if (!viewModel.listView) viewModel.toggleListView() },
                ) {
                    Icon(LucideList, modifier = Modifier.size(14.dp))
                }
            }

            ToolbarSeparator()

            Text(
                text = "${viewModel.filteredSymbols.size} symbols",
                color = cs.textSecondary,
            )

            Spacer(Modifier.weight(1f))

            SearchField(
                value = viewModel.query,
                onValueChange = { viewModel.search(it) },
                placeholder = "Search",
                modifier = Modifier.width(200.dp),
            )
        }
    }
}

private fun nearestGridIndex(scale: Float): Int {
    var best = 2
    var bestDist = Float.MAX_VALUE
    GRID_SIZE_SCALES.forEachIndexed { i, s ->
        val d = kotlin.math.abs(s - scale)
        if (d < bestDist) {
            bestDist = d
            best = i
        }
    }
    return best
}
