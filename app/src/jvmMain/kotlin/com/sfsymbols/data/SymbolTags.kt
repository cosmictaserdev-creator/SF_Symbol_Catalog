package com.sfsymbols.data

/**
 * Far-guess search tags loaded from `symbol_tags.txt`. Keys are either one
 * name segment ("trash") or a full apple name ("wand.and.stars"); a symbol's
 * tags are the union of its segments' tags and its full-name tags.
 */
public object SymbolTags {

    private val table: Map<String, Set<String>> by lazy {
        val text = SymbolTags::class.java.getResourceAsStream("/symbol_tags.txt")
            ?.bufferedReader()?.use { it.readText() }
            ?: return@lazy emptyMap()
        val out = HashMap<String, MutableSet<String>>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val colon = line.indexOf(':')
            if (colon <= 0) continue
            val key = line.substring(0, colon).trim().lowercase()
            val tags = line.substring(colon + 1).split(' ').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            out.getOrPut(key) { LinkedHashSet() }.addAll(tags)
        }
        out
    }

    public fun tagsFor(appleName: String): List<String> {
        val name = appleName.lowercase()
        val result = LinkedHashSet<String>()
        table[name]?.let(result::addAll)
        for (seg in name.split('.')) table[seg]?.let(result::addAll)
        result.removeAll(name.split('.').toSet())
        return result.toList()
    }
}
