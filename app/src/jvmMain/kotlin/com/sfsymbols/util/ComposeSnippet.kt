package com.sfsymbols.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.sfsymbols.data.SfSymbolMetadata
import com.sfsymbols.data.SymbolMode
import java.util.Locale

/** A complete composable using the public companion library and its extension import. */
public fun composeSnippet(
    symbol: SfSymbolMetadata,
    mode: SymbolMode,
    tint: Color,
    tintEnabled: Boolean,
    background: Color?,
): String {
    val variant = if (mode == SymbolMode.Dualtone) "Dualtone" else "Monochrome"
    val packageName = variant.lowercase(Locale.ROOT)
    fun colorLiteral(color: Color) = "Color(0x${String.format(Locale.ROOT, "%08X", color.toArgb())})"
    val tintCode = if (tintEnabled) colorLiteral(tint) else "Color.Unspecified"
    val imports = mutableListOf(
        "androidx.compose.foundation.layout.size",
        "androidx.compose.material3.Icon",
        "androidx.compose.runtime.Composable",
        "androidx.compose.ui.Modifier",
        "androidx.compose.ui.graphics.Color",
        "androidx.compose.ui.unit.dp",
        "com.composables.sfsymbols.SfSymbols",
        "com.composables.sfsymbols.$packageName.${symbol.pascalName}",
    )
    if (background != null) imports.addAll(listOf(
        "androidx.compose.foundation.background",
        "androidx.compose.foundation.layout.Box",
        "androidx.compose.foundation.shape.RoundedCornerShape",
        "androidx.compose.ui.Alignment",
        "androidx.compose.ui.draw.clip",
    ))
    val icon = """
        Icon(
            imageVector = SfSymbols.$variant.${symbol.pascalName},
            contentDescription = "${symbol.appleName}",
            tint = $tintCode,
            modifier = ${if (background == null) "modifier" else "Modifier"}.size(110.dp),
        )
    """.trimIndent()
    val body = if (background == null) icon else """
        Box(
            modifier = modifier
                .size(240.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(${colorLiteral(background)}),
            contentAlignment = Alignment.Center,
        ) {
    """.trimIndent() + "\n" + icon.prependIndent("    ") + "\n}"
    return """
        // Add the icon library to your module dependencies:
        // implementation("com.github.cosmictaserdev-creator:Jetpack_SF_Symbols:1.0.4")
        // Add maven("https://jitpack.io") to your dependency repositories.
    """.trimIndent() + "\n\n" + imports.sorted().joinToString("\n") { "import $it" } +
        "\n\n@Composable\nfun ${symbol.pascalName}Icon(modifier: Modifier = Modifier) {\n" +
        body.prependIndent("    ") + "\n}\n"
}
