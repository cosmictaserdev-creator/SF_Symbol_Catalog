package com.sfsymbols.util

import java.awt.Desktop
import java.net.URI

/** Opens a URL in the default browser. No-op when the Desktop API is unavailable. */
public fun openUrl(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}
