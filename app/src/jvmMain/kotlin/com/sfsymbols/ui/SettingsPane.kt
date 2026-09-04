package com.sfsymbols.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sfsymbols.util.openUrl
import com.sfsymbols.viewmodel.CatalogViewModel
import dev.nucleusframework.macoscompose.components.GroupBox
import dev.nucleusframework.macoscompose.components.NavigationButtons
import dev.nucleusframework.macoscompose.components.Surface
import dev.nucleusframework.macoscompose.components.Switch
import dev.nucleusframework.macoscompose.components.Text
import dev.nucleusframework.macoscompose.components.TitleBar
import dev.nucleusframework.macoscompose.components.TitleBarButtonGroup
import dev.nucleusframework.macoscompose.components.TitleBarGroupButton
import dev.nucleusframework.macoscompose.icons.Icon
import dev.nucleusframework.macoscompose.icons.LucideChevronLeft
import dev.nucleusframework.macoscompose.theme.GlassMaterialSize
import dev.nucleusframework.macoscompose.theme.MacosTheme

// Personal / project links. Edit these freely.
private const val NAME = "cosmicTaser"
private const val PROJECT_URL = "https://cosmictaser.de5.net"
private const val GITHUB_URL = "https://github.com/cosmictaserdev-creator"
private const val KOFI_URL = "https://ko-fi.com/cosmictaser"
private const val UPI_ID = "cosmictaser@okicici"

/**
 * Dedicated Settings screen: appearance toggle, MCP usage instructions and
 * creator links (project, GitHub, Ko-fi, UPI).
 */
@Composable
public fun SettingsPane(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
) {
    val cs = MacosTheme.colorScheme
    Column(modifier = modifier.fillMaxSize()) {
        TitleBar(
            navigationActions = {
                NavigationButtons(
                    onBack = { viewModel.navigate(CatalogViewModel.Screen.Catalog) },
                    onForward = {},
                    backEnabled = true,
                    forwardEnabled = false,
                )
            },
            title = {
                Text("Settings")
            },
            actions = {
                TitleBarButtonGroup {
                    TitleBarGroupButton(onClick = { viewModel.navigate(CatalogViewModel.Screen.Catalog) }) {
                        Icon(LucideChevronLeft, modifier = Modifier.size(12.dp))
                    }
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            GroupBox(label = "Appearance", modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Dark mode", color = cs.textPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = viewModel.darkTheme,
                        onCheckedChange = viewModel::updateDarkTheme,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            GroupBox(label = "MCP (AI agent access)", modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This app exposes a Model Context Protocol (MCP) server over stdio. " +
                        "AI agents (Claude, etc.) can browse/search the full SF Symbols catalog.",
                    color = cs.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Text(text = "Available tools", color = cs.textPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "• search_symbols(query, category?, limit?) — fuzzy, ranked search\n" +
                        "• get_symbol(code) — resolve a pascal or apple name to metadata",
                    color = cs.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Text(text = "MCP client config", color = cs.textPrimary, fontWeight = FontWeight.SemiBold)
                SelectionContainer {
                    Text(
                        text = "\"sf-symbols-catalog\": {\n" +
                            "  \"command\": \"java\",\n" +
                            "  \"args\": [\"-jar\", \"/path/to/app-jvm.jar\"]\n" +
                            "}",
                        color = cs.textTertiary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            GroupBox(label = "Creator", modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinkRow(label = "Name", value = NAME)
                    LinkRow(label = "Project", value = PROJECT_URL, url = PROJECT_URL)
                    LinkRow(label = "GitHub", value = GITHUB_URL, url = GITHUB_URL)
                    LinkRow(label = "Ko-fi", value = KOFI_URL, url = KOFI_URL)
                    LinkRow(label = "UPI", value = UPI_ID)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(text = "Made with love by $NAME", color = cs.textTertiary)
        }
    }
}

@Composable
private fun LinkRow(label: String, value: String, url: String? = null) {
    val cs = MacosTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = url != null) {
                if (url != null) openUrl(url)
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = cs.textTertiary, modifier = Modifier.width(88.dp))
        Text(
            text = value,
            color = cs.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (url != null) {
            Text(text = "↗", color = cs.accent)
        }
    }
}
