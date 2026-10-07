package com.sfsymbols

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import com.sfsymbols.mcp.McpServer
import com.sfsymbols.ui.SfSymbolsCatalogApp
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.macoscompose.window.MacosDecoratedWindow
import org.jetbrains.skia.Image

private fun loadAppIcon(): Painter {
    val bytes = checkNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream("app_icon.png")).use { it.readBytes() }
    return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
}

fun main(args: Array<String>) {
    if ("--mcp" in args) {
        McpServer().runBlockingUntilExit()
        return
    }
    launchCatalog()
}

private fun launchCatalog() = nucleusApplication {
    MacosDecoratedWindow(
        onCloseRequest = { exitApplication() },
        title = "SF Symbols Catalog",
        icon = loadAppIcon(),
        state = WindowState(size = DpSize(1280.dp, 820.dp)),
        minimumSize = DpSize(1000.dp, 680.dp),
    ) {
        SfSymbolsCatalogApp()
    }
}
