package com.sfsymbols.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sfsymbols.data.SfSymbolIconResolver
import com.sfsymbols.data.SfSymbolMetadata
import com.sfsymbols.data.SymbolMode
import com.sfsymbols.viewmodel.CatalogViewModel
import dev.nucleusframework.macoscompose.components.ScrollbarState
import dev.nucleusframework.macoscompose.components.Text
import dev.nucleusframework.macoscompose.components.VerticalScrollbar
import dev.nucleusframework.macoscompose.theme.ControlSize
import dev.nucleusframework.macoscompose.theme.MacosTheme

/**
 * Center pane: grid of symbol cards for the current filter/mode. Grid cell size
 * is scaled by [CatalogViewModel.gridScale]; cards show a favorite heart and
 * support multi-select batch favorite/unfavorite.
 */
@Composable
public fun SymbolGrid(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
) {
    if (viewModel.listView) {
        SymbolList(viewModel, modifier)
        return
    }
    val minCell = (110f * viewModel.gridScale).dp
    val gridState = rememberLazyGridState()
    val scrollbarState = remember { GridScrollbarState(gridState) }
    Box(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minCell),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(viewModel.filteredSymbols, key = { it.appleName }) { meta ->
                SymbolCard(
                    meta = meta,
                    mode = viewModel.mode,
                    favorite = viewModel.isFavorite(meta),
                    selected = viewModel.selectedSymbol?.appleName == meta.appleName,
                    multiSelect = viewModel.multiSelectEnabled,
                    multiSelected = meta.appleName in viewModel.selectedIds,
                    onClick = {
                        if (viewModel.multiSelectEnabled) viewModel.toggleMultiSelect(meta)
                        else viewModel.selectSymbol(meta)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(meta) },
                )
            }
        }
        VerticalScrollbar(
            state = scrollbarState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
        )
    }
}

/**
 * List-view alternative to the adaptive grid: one row per symbol with a
 * leading icon and trailing favorite toggle, driven by a native scrollbar.
 */
@Composable
private fun SymbolList(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scrollbarState = remember { LazyListScrollbarState(listState) }
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            lazyItems(viewModel.filteredSymbols, key = { it.appleName }) { meta ->
                SymbolListRow(
                    meta = meta,
                    mode = viewModel.mode,
                    favorite = viewModel.isFavorite(meta),
                    selected = viewModel.selectedSymbol?.appleName == meta.appleName,
                    multiSelect = viewModel.multiSelectEnabled,
                    multiSelected = meta.appleName in viewModel.selectedIds,
                    onClick = {
                        if (viewModel.multiSelectEnabled) viewModel.toggleMultiSelect(meta)
                        else viewModel.selectSymbol(meta)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(meta) },
                )
            }
        }
        VerticalScrollbar(
            state = scrollbarState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
        )
    }
}

/**
 * Adapts a [LazyListState] to the macos-ui [ScrollbarState] interface.
 */
internal class LazyListScrollbarState(
    private val state: LazyListState,
) : ScrollbarState {
    override val scrollOffsetPx: Float
        get() = state.firstVisibleItemScrollOffset.toFloat() +
            state.firstVisibleItemIndex * (firstItemHeight())

    override val maxScrollPx: Float
        get() {
            val li = state.layoutInfo
            val total = li.totalItemsCount
            if (total == 0) return 0f
            val totalPx = li.totalItemsCount * firstItemHeight()
            return (totalPx - li.viewportSize.height).coerceAtLeast(0).toFloat()
        }

    override val isScrollInProgress: Boolean
        get() = state.isScrollInProgress

    private fun firstItemHeight(): Int {
        var max = 0
        for (info in state.layoutInfo.visibleItemsInfo) {
            if (info.size > max) max = info.size
        }
        return max
    }

    override suspend fun scrollTo(px: Float) {
        val h = firstItemHeight()
        if (h == 0) return
        val index = (px / h).toInt().coerceAtLeast(0)
        val offset = (px - index * h).toInt().coerceAtLeast(0)
        state.scrollToItem(index, offset)
    }
}

/**
 * Adapts a [LazyGridState] to the macos-ui [ScrollbarState] interface so the
 * native vertical scrollbar can drive the adaptive symbol grid.
 */
internal class GridScrollbarState(
    private val state: LazyGridState,
) : ScrollbarState {
    override val scrollOffsetPx: Float
        get() = state.layoutInfo.viewportStartOffset.toFloat()

    private fun columns(): Int {
        val visible = state.layoutInfo.visibleItemsInfo
        return visible.maxOfOrNull { it.column + 1 } ?: 1
    }

    private fun rowHeight(): Int {
        val visible = state.layoutInfo.visibleItemsInfo
        return visible.maxOfOrNull { it.size.height } ?: 0
    }

    override val maxScrollPx: Float
        get() {
            val li = state.layoutInfo
            val cols = columns()
            val rh = rowHeight()
            if (rh == 0) return 0f
            val rows = (li.totalItemsCount + cols - 1) / cols
            val total = rows * rh
            return (total - li.viewportSize.height).coerceAtLeast(0).toFloat()
        }

    override val isScrollInProgress: Boolean
        get() = state.isScrollInProgress

    override suspend fun scrollTo(px: Float) {
        val cols = columns()
        val rh = rowHeight()
        if (rh == 0) return
        val row = (px / rh).toInt().coerceAtLeast(0)
        val offsetInRow = (px - row * rh).toInt().coerceAtLeast(0)
        state.scrollToItem(row * cols, scrollOffset = offsetInRow)
    }
}

@Composable
private fun SymbolCard(
    meta: SfSymbolMetadata,
    mode: SymbolMode,
    favorite: Boolean,
    selected: Boolean,
    multiSelect: Boolean,
    multiSelected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val cs = MacosTheme.colorScheme
    val vector: ImageVector? = remember(meta, mode) {
        SfSymbolIconResolver.resolve(meta, mode)
    }
    if (vector == null) return

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val shape = RoundedCornerShape(12.dp)
    val highlight = multiSelect && multiSelected
    val bg by animateColorAsState(
        when {
            highlight -> cs.accent.copy(alpha = 0.22f)
            selected -> cs.accent.copy(alpha = 0.12f)
            else -> cs.surface
        },
        tween(150),
        label = "card-bg",
    )
    val borderColor by animateColorAsState(
        when {
            highlight || selected -> cs.accent
            hovered -> cs.textPrimary.copy(alpha = 0.18f)
            else -> cs.borderSubtle
        },
        tween(150),
        label = "card-border",
    )
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .hoverable(interactionSource)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (hovered && !selected && !highlight) Color.White.copy(alpha = 0.06f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            val ratio = (vector.viewportWidth / vector.viewportHeight).coerceIn(0.5f, 1.8f)
            val iconHeight = 42.dp
            val iconWidth = iconHeight * ratio
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = cs.textPrimary,
                modifier = Modifier.size(iconWidth, iconHeight),
            )

            // Favorite heart (top-end corner, own click area)
            val heartBg by animateColorAsState(
                if (favorite) Color(0x33E0245E) else Color.Transparent,
                tween(150),
                label = "heart-bg",
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .background(heartBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleFavorite,
                    )
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (favorite) "Remove favorite" else "Add favorite",
                    tint = if (favorite) Color(0xFFE0245E) else cs.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Multi-select check badge
            if (multiSelect && multiSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = cs.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Text(
            text = meta.appleName,
            color = cs.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp, start = 2.dp, end = 2.dp),
        )
    }
}

@Composable
private fun SymbolListRow(
    meta: SfSymbolMetadata,
    mode: SymbolMode,
    favorite: Boolean,
    selected: Boolean,
    multiSelect: Boolean,
    multiSelected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val cs = MacosTheme.colorScheme
    val vector: ImageVector? = remember(meta, mode) {
        SfSymbolIconResolver.resolve(meta, mode)
    }
    if (vector == null) return
    val shape = RoundedCornerShape(8.dp)
    val bg by animateColorAsState(
        when {
            multiSelect && multiSelected -> cs.accent.copy(alpha = 0.22f)
            selected -> cs.accent.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        tween(150),
        label = "row-bg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .clip(shape)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ratio = (vector.viewportWidth / vector.viewportHeight).coerceIn(0.5f, 1.8f)
        val h = 28.dp
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = cs.textPrimary,
                modifier = Modifier.size(h * ratio, h),
            )
        }
        Text(
            text = meta.appleName,
            color = cs.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        Icon(
            imageVector = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (favorite) "Remove favorite" else "Add favorite",
            tint = if (favorite) Color(0xFFE0245E) else cs.textTertiary,
            modifier = Modifier
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleFavorite,
                )
                .padding(4.dp)
                .size(18.dp),
        )
    }
}
