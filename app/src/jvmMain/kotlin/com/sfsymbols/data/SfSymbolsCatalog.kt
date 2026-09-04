package com.sfsymbols.data

/**
 * Metadata for an SF Symbol.
 */
public data class SfSymbolMetadata(
    val appleName: String,
    val pascalName: String,
    val categories: List<String>,
    val isRestricted: Boolean
)

/**
 * Complete catalog of all 7,007 SF Symbols with search and category indexing.
 */
public object SfSymbolsCatalog {
    public val all: List<SfSymbolMetadata> by lazy {
        catalogPart0() + catalogPart1() + catalogPart2() + catalogPart3() + catalogPart4() + catalogPart5() + catalogPart6() + catalogPart7() + catalogPart8() + catalogPart9() + catalogPart10() + catalogPart11() + catalogPart12() + catalogPart13() + catalogPart14()
    }
}