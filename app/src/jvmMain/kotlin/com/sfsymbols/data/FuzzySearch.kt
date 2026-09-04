package com.sfsymbols.data

import kotlin.math.min

/**
 * Lightweight fuzzy, typo-tolerant, ranked search over the SF Symbols catalog.
 *
 * Symbols are matched against their apple name (e.g. "0.circle.fill"),
 * its dot/hyphen/space-separated segments, and the camel-cased pascal name.
 * Queries are tokenized the same way; scores combine exact/prefix/substring,
 * segment matches and Levenshtein edit-distance for typos.
 */
public object FuzzySearch {

    private data class Indexed(
        val apple: String,
        val segments: List<String>,
        val pascalWords: List<String>,
        val normalized: String,
    )

    private val index: List<Indexed> by lazy {
        SfSymbolsCatalog.all.map { meta ->
            val apple = meta.appleName.lowercase()
            Indexed(
                apple = apple,
                segments = apple.split('.', '-', '_', ' ').filter { it.isNotEmpty() },
                pascalWords = camelWords(meta.pascalName),
                normalized = apple.replace(".", " ").replace("-", " ").replace("_", " "),
            )
        }
    }

    private fun camelWords(pascal: String): List<String> {
        if (pascal.isEmpty()) return emptyList()
        val sb = StringBuilder(pascal[0].lowercaseChar().toString())
        for (c in pascal.drop(1)) {
            if (c.isUpperCase()) sb.append(' ')
            sb.append(c.lowercaseChar())
        }
        return sb.toString().split(' ').filter { it.isNotEmpty() }
    }

    public fun search(query: String, limit: Int = 200): List<SfSymbolMetadata> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return SfSymbolsCatalog.all.take(limit)

        val tokens = q.split('.', '-', '_', ' ').filter { it.isNotEmpty() }

        val scored = ArrayList<Pair<Int, Int>>(index.size) // (score, symbolIndex)
        val all = SfSymbolsCatalog.all
        for (i in index.indices) {
            val score = score(index[i], tokens, q)
            if (score > 0) scored.add(score to i)
        }
        scored.sortWith { a, b ->
            val c = b.first.compareTo(a.first) // higher score first
            if (c != 0) c else index[a.second].apple.compareTo(index[b.second].apple)
        }
        return scored.take(limit).map { all[it.second] }
    }

    private fun score(ix: Indexed, tokens: List<String>, q: String): Int {
        var total = 0

        // Whole-name matches dominate.
        if (ix.apple == q) total += 1000
        else if (ix.apple.startsWith(q)) total += 700
        else if (ix.apple.contains(q)) total += 350
        else if (ix.normalized.contains(q)) total += 300

        // Per-token matches.
        var anyStrongToken = false
        for (t in tokens) {
            var best = 0
            for (seg in ix.segments) {
                val s = when {
                    seg == t -> 600
                    seg.startsWith(t) -> 450
                    else -> 0
                }
                if (s > best) best = s
            }
            for (w in ix.pascalWords) {
                val s = when {
                    w == t -> 550
                    w.startsWith(t) -> 380
                    else -> 0
                }
                if (s > best) best = s
            }
            if (best == 0 && ix.normalized.contains(t)) best = 250
            if (best == 0 && t.length in 2..10) {
                // Typo fallback via bounded edit distance on segments/words.
                val nearest = editDistanceMatch(ix, t)
                if (nearest > 0) best = nearest
            }
            if (best >= 120) anyStrongToken = true
            total += best
        }

        if (!anyStrongToken) return 0

        // Multi-segment queries (e.g. "arrow.right"): require most segments hit.
        val dotCount = q.count { it == '.' }
        if (dotCount >= 2 && tokens.size == dotCount + 1) {
            val segmentMatches = tokens.count { t ->
                ix.segments.any { it == t || it.startsWith(t) || editDistanceMatch(ix, t) > 0 }
            }
            if (segmentMatches < tokens.size) total /= 2
        }

        val qNoDots = q.replace(".", "")
        if (qNoDots.isNotEmpty() && ix.pascalWords.isNotEmpty() &&
            ix.pascalWords.joinToString("").contains(qNoDots)) {
            total = max(total, 220)
        }
        return total
    }

    private fun editDistanceMatch(ix: Indexed, t: String): Int {
        for (seg in ix.segments) {
            if (seg.length in 2..10 && levenshtein(seg, t) <= 2) return 130
        }
        for (w in ix.pascalWords) {
            if (w.length in 2..10 && levenshtein(w, t) <= 2) return 120
        }
        return 0
    }

    /** Classic Levenshtein (bounded) edit distance. */
    private fun levenshtein(a: String, b: String): Int {
        val la = a.length
        val lb = b.length
        if (la == 0) return lb
        if (lb == 0) return la
        var prev = IntArray(lb + 1) { it }
        var curr = IntArray(lb + 1)
        for (i in 1..la) {
            curr[0] = i
            for (j in 1..lb) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[lb]
    }

    private fun max(a: Int, b: Int): Int = if (a > b) a else b
}
