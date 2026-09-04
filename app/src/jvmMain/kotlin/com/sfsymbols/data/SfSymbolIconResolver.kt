package com.sfsymbols.data

import androidx.compose.ui.graphics.vector.ImageVector
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves an [ImageVector] for a metadata entry in the requested [SymbolMode].
 *
 * Each of the 14,014 icon files declares a top-level extension property on
 * `SfSymbols.Dualtone` / `SfSymbols.Monochrome`, which compiles to a facade
 * class `com.sfsymbols.data.<mode>.<pascalName>Kt` with a static getter
 * `get<pascalName>(scope)`. We look these up once per symbol via JVM reflection
 * and cache the resulting vectors in memory.
 */
public object SfSymbolIconResolver {

    private data class Key(val pascalName: String, val mode: SymbolMode)

    private val cache = ConcurrentHashMap<Key, ImageVector>()

    private fun facadePackage(mode: SymbolMode): String =
        when (mode) {
            SymbolMode.Dualtone -> "com.sfsymbols.data.dualtone"
            SymbolMode.Monochrome -> "com.sfsymbols.data.monochrome"
        }

    private fun scope(mode: SymbolMode): Any =
        when (mode) {
            SymbolMode.Dualtone -> SfSymbols.Dualtone
            SymbolMode.Monochrome -> SfSymbols.Monochrome
        }

    public fun resolve(meta: SfSymbolMetadata, mode: SymbolMode): ImageVector? {
        val key = Key(meta.pascalName, mode)
        cache[key]?.let { return it }
        return runCatching {
            val facade = Class.forName("${facadePackage(mode)}.${meta.pascalName}Kt")
            val getter = facade.methods.firstOrNull {
                it.name.equals("get${meta.pascalName}", ignoreCase = false) && it.parameterCount == 1
            } ?: return null
            (getter.invoke(null, scope(mode)) as? ImageVector)?.also { cache[key] = it }
        }.getOrNull()
    }
}
