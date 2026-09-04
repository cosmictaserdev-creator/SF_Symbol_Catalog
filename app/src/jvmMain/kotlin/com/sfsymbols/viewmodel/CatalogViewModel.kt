package com.sfsymbols.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.sfsymbols.data.FuzzySearch
import com.sfsymbols.data.PinnedSymbols
import com.sfsymbols.data.SfSymbolMetadata
import com.sfsymbols.data.SfSymbolsCatalog
import com.sfsymbols.data.SymbolMode
import com.sfsymbols.util.copyToClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Central state holder for the SF Symbols catalog.
 * Plain Compose state + coroutines (no Android ViewModel dependency).
 */
public class CatalogViewModel {

    private val scope = CoroutineScope(Dispatchers.Default)

    public enum class Screen { Catalog, Settings }

    public var screen: Screen by mutableStateOf(Screen.Catalog)
        private set

    public var darkTheme: Boolean by mutableStateOf(true)
        private set

    public var query: String by mutableStateOf("")
        private set

    public var selectedCategory: String? by mutableStateOf(null)
        private set

    public var onlyPinned: Boolean by mutableStateOf(false)
        private set

    public var mode: SymbolMode by mutableStateOf(SymbolMode.Dualtone)
        private set

    public var pinnedIds: Set<String> by mutableStateOf(PinnedSymbols.load())
        private set

    public var selectedSymbol: SfSymbolMetadata? by mutableStateOf(
        SfSymbolsCatalog.all.firstOrNull()
    )
        private set

    public var iconColor: Color by mutableStateOf(Color.White)
        private set

    public var backgroundColor: Color? by mutableStateOf(null)
        private set

    public var badgeText: String? by mutableStateOf(null)
        private set

    public var tintIcon: Boolean by mutableStateOf(true)
        private set

    public var gridScale: Float by mutableStateOf(1f)
        private set

    public var listView: Boolean by mutableStateOf(false)
        private set

    public fun toggleListView() {
        listView = !listView
    }

    public var multiSelectEnabled: Boolean by mutableStateOf(false)
        private set

    /** Currently selected apple-names when multi-select is on. */
    public var selectedIds: Set<String> by mutableStateOf(emptySet())
        private set

    private var badgeJob: Job? = null

    private val allSymbols by lazy { SfSymbolsCatalog.all }

    public val categories: List<Pair<String, Int>> by lazy {
        allSymbols
            .flatMap { it.categories }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    public val filteredSymbols: List<SfSymbolMetadata>
        get() = computeFiltered()

    public val pinnedSymbols: List<SfSymbolMetadata>
        get() = allSymbols.filter { it.appleName in pinnedIds }

    private fun computeFiltered(): List<SfSymbolMetadata> {
        val q = query.trim()
        val cat = selectedCategory
        val base = when {
            onlyPinned -> allSymbols.filter { it.appleName in pinnedIds }
            else -> allSymbols
        }
        val scoped = if (cat == null) base else base.filter { it.categories.contains(cat) }
        if (q.isEmpty()) return scoped
        val fuzzy = FuzzySearch.search(q, limit = 300)
        return if (cat == null && !onlyPinned) fuzzy
        else fuzzy.filter { m -> scoped.any { it.appleName == m.appleName } }
    }

    public fun search(text: String) {
        query = text
        if (selectedSymbol == null && filteredSymbols.isNotEmpty()) {
            selectedSymbol = filteredSymbols.first()
        }
    }

    public fun filterCategory(category: String?) {
        selectedCategory = category
        onlyPinned = false
    }

    public fun showOnlyPinned() {
        onlyPinned = true
        selectedCategory = null
    }

    public fun showAll() {
        onlyPinned = false
        selectedCategory = null
    }

    public fun togglePin(meta: SfSymbolMetadata) {
        val next = if (meta.appleName in pinnedIds) {
            pinnedIds - meta.appleName
        } else {
            pinnedIds + meta.appleName
        }
        pinnedIds = next
        PinnedSymbols.save(next)
    }

    public fun selectSymbol(meta: SfSymbolMetadata) {
        selectedSymbol = meta
    }

    public fun updateMode(newMode: SymbolMode) {
        mode = newMode
    }

    public fun updateIconColor(color: Color) {
        iconColor = color
    }

    public fun updateBackground(color: Color?) {
        backgroundColor = color
    }

    public fun updateTintIcon(enabled: Boolean) {
        tintIcon = enabled
    }

    public fun navigate(screen: Screen) {
        this.screen = screen
    }

    public fun updateDarkTheme(dark: Boolean) {
        darkTheme = dark
    }

    public fun updateGridScale(scale: Float) {
        gridScale = scale.coerceIn(0.6f, 1.8f)
    }

    public fun updateMultiSelectEnabled(enabled: Boolean) {
        multiSelectEnabled = enabled
        if (!enabled) selectedIds = emptySet()
    }

    public fun toggleMultiSelect(meta: SfSymbolMetadata) {
        selectedIds = if (meta.appleName in selectedIds)
            selectedIds - meta.appleName
        else
            selectedIds + meta.appleName
    }

    public fun clearMultiSelection() {
        selectedIds = emptySet()
    }

    public fun setFavoriteSelected(value: Boolean) {
        val current = selectedIds
        if (current.isEmpty()) return
        val next = if (value) {
            (pinnedIds + current).toSet()
        } else {
            (pinnedIds - current).toSet()
        }
        pinnedIds = next
        PinnedSymbols.save(next)
        selectedIds = emptySet()
        badgeText = if (value) "Added ${current.size} favorites" else "Removed ${current.size} favorites"
    }

    public fun toggleFavorite(meta: SfSymbolMetadata) {
        val next = if (meta.appleName in pinnedIds) pinnedIds - meta.appleName
        else pinnedIds + meta.appleName
        pinnedIds = next
        PinnedSymbols.save(next)
    }

    public fun isFavorite(meta: SfSymbolMetadata): Boolean = meta.appleName in pinnedIds

    public fun isPinned(meta: SfSymbolMetadata): Boolean = meta.appleName in pinnedIds

    public fun copyAppleName(meta: SfSymbolMetadata) {
        copyToClipboard(meta.appleName)
        showBadge("Copied \"${meta.appleName}\"")
    }

    public fun copyPascalName(meta: SfSymbolMetadata) {
        copyToClipboard(meta.pascalName)
        showBadge("Copied \"${meta.pascalName}\"")
    }

    public fun copyCodeSnippet(meta: SfSymbolMetadata) {
        val prefix = if (mode == SymbolMode.Dualtone) "SfSymbols.Dualtone." else "SfSymbols.Monochrome."
        copyToClipboard("$prefix${meta.pascalName}")
        showBadge("Copied code snippet")
    }

    private fun showBadge(text: String) {
        badgeText = text
        badgeJob?.cancel()
        badgeJob = scope.launch {
            delay(1800)
            badgeText = null
        }
    }
}
