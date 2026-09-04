package com.sfsymbols.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Internal helper to construct an SF Symbol ImageVector efficiently.
 */
internal inline fun sfIcon(
    name: String,
    viewportWidth: Float,
    viewportHeight: Float,
    defaultWidth: Float = 24f,
    defaultHeight: Float = 24f,
    block: ImageVector.Builder.() -> Unit
): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = defaultWidth.dp,
        defaultHeight = defaultHeight.dp,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight
    ).apply(block).build()
}

/**
 * Internal helper to add an SVG path with opacity to an ImageVector builder.
 */
internal inline fun ImageVector.Builder.addSfPath(
    pathData: String,
    fillAlpha: Float = 1.0f,
    fillColor: Color = Color.Black
): ImageVector.Builder {
    return addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(fillColor),
        fillAlpha = fillAlpha
    )
}
