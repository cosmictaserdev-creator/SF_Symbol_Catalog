package com.sfsymbols.util

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Copies text to the system clipboard.
 */
public fun copyToClipboard(text: String) {
    val selection = StringSelection(text)
    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
}
