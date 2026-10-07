package com.sfsymbols.data

import kotlin.math.abs
import kotlin.math.min

/**
 * Lightweight fuzzy, typo-tolerant, ranked search over the SF Symbols catalog.
 *
 * Symbols are matched against their apple name (e.g. "0.circle.fill"),
 * its dot/hyphen/space-separated segments, the camel-cased pascal name and
 * their categories and far-guess [SymbolTags]. Queries are tokenized the same
 * way; scores combine exact/prefix/substring, subsequence, tag and category
 * matches, with a bounded Levenshtein edit-distance fallback for typos.
 */
public object FuzzySearch {

    private data class Indexed(
        val apple: String,
        val segments: List<String>,
        val pascalWords: List<String>,
        val normalized: String,
        val categories: List<String>,
        val tags: List<String>,
    )

    private val index: List<Indexed> by lazy {
        SfSymbolsCatalog.all.map { meta ->
            val apple = meta.appleName.lowercase()
            Indexed(
                apple = apple,
                segments = apple.split('.', '-', '_', ' ').filter { it.isNotEmpty() },
                pascalWords = camelWords(meta.pascalName),
                normalized = apple.replace(".", " ").replace("-", " ").replace("_", " "),
                categories = meta.categories.map { it.lowercase() },
                tags = SymbolTags.tagsFor(meta.appleName),
            )
        }
    }

    private const val STRONG_TOKEN = 120
    private const val CACHE_LIMIT = 256

    /** Access-ordered so the least recently used query is evicted first. */
    private val cache = LinkedHashMap<String, List<SfSymbolMetadata>>(64, 0.75f, true)

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

        synchronized(cache) { cache[q] }?.let { return it.take(limit) }

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
        val result = scored.map { all[it.second] }
        synchronized(cache) {
            if (cache.size >= CACHE_LIMIT) cache.clear()
            cache[q] = result
        }
        return result.take(limit)
    }

    private fun score(ix: Indexed, tokens: List<String>, q: String): Int {
        var total = 0

        // Exact names rank first; exact tags outrank incidental name prefixes.
        if (ix.apple == q) total += 1000
        else if (q in ix.tags) total += 700
        else if (ix.apple.startsWith(q)) total += 700
        else if (ix.apple.contains(q)) total += 350
        else if (ix.normalized.contains(q)) total += 300

        var anyStrongToken = false
        for (t in tokens) {
            val best = tokenScore(ix, t)
            if (best >= STRONG_TOKEN) anyStrongToken = true
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
            ix.pascalWords.joinToString("").contains(qNoDots)
        ) {
            total = max(total, 220)
        }
        return total
    }

    private fun tokenScore(ix: Indexed, t: String): Int {
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
        if (best == 0 && t.length >= 3 && isSubsequence(t, ix.normalized)) best = 150
        if (best < STRONG_TOKEN && t.length in 3..12) {
            val near = editDistanceMatch(ix, t)
            if (near > best) best = near
        }
        for (cat in ix.categories) {
            val s = when {
                cat == t -> 500
                cat.startsWith(t) -> 320
                t.length >= 3 && cat.contains(t) -> 200
                else -> 0
            }
            if (s > best) best = s
        }
        // Tags sit just below a direct name-word hit and above category hits.
        for (tag in ix.tags) {
            val s = when {
                tag == t -> 560
                t.length >= 3 && tag.startsWith(t) -> 300
                else -> 0
            }
            if (s > best) best = s
        }
        return best
    }

    private fun editDistanceMatch(ix: Indexed, t: String): Int {
        val budget = editBudget(t.length)
        for (seg in ix.segments) {
            if (seg.length in 2..14 && abs(seg.length - t.length) <= budget &&
                boundedLevenshtein(seg, t, budget) <= budget
            ) return 130
        }
        for (w in ix.pascalWords) {
            if (w.length in 2..14 && abs(w.length - t.length) <= budget &&
                boundedLevenshtein(w, t, budget) <= budget
            ) return 120
        }
        return 0
    }

    /** Short words tolerate few edits; longer words tolerate proportionally more. */
    private fun editBudget(len: Int): Int = when {
        len <= 3 -> 1
        len <= 6 -> 2
        len <= 10 -> 3
        else -> 4
    }

    private fun isSubsequence(needle: String, haystack: String): Boolean {
        var i = 0
        for (c in haystack) {
            if (i < needle.length && c == needle[i]) i++
        }
        return i == needle.length
    }

    /**
     * Levenshtein distance that gives up as soon as [maxDist] is exceeded,
     * so a hopeless candidate costs O(maxDist) instead of O(la * lb).
     * Returns [maxDist] + 1 when the distance exceeds [maxDist].
     */
    private fun boundedLevenshtein(a: String, b: String, maxDist: Int): Int {
        val la = a.length
        val lb = b.length
        if (abs(la - lb) > maxDist) return maxDist + 1
        if (la == 0) return lb
        if (lb == 0) return la
        var prev = IntArray(lb + 1) { it }
        var curr = IntArray(lb + 1)
        for (i in 1..la) {
            curr[0] = i
            var rowMin = i
            for (j in 1..lb) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
                if (curr[j] < rowMin) rowMin = curr[j]
            }
            if (rowMin > maxDist) return maxDist + 1
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[lb]
    }

    private fun max(a: Int, b: Int): Int = if (a > b) a else b
}
