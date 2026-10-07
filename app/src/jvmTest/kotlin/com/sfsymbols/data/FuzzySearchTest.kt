package com.sfsymbols.data

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class FuzzySearchTest {

    @Test
    fun `home finds house`() {
        val names = FuzzySearch.search("home", limit = 200).map { it.appleName }
        assertTrue(
            names.contains("house"),
            "expected \"house\" for query \"home\", got ${names.take(10)}",
        )
    }

    @Test
    fun `direct query outranks alias match`() {
        val names = FuzzySearch.search("house", limit = 200).map { it.appleName }
        assertEquals("house", names.first(), "exact apple name should rank first")
    }

    @Test
    fun `phone finds telephone`() {
        val names = FuzzySearch.search("phone", limit = 200).map { it.appleName }
        assertTrue(
            names.any { it.contains("phone") },
            "expected a phone symbol for \"phone\", got ${names.take(10)}",
        )
    }

    @Test
    fun `far-guess tags reveal symbols`() {
        val expected = mapOf(
            "garbage" to "trash",
            "logout" to "rectangle.portrait.and.arrow.right",
            "ai" to "sparkles",
            "cog" to "gearshape",
            "money" to "dollarsign",
        )
        for ((query, symbol) in expected) {
            val names = FuzzySearch.search(query, limit = 50).map { it.appleName }
            assertTrue(symbol in names, "expected \"$symbol\" for \"$query\", got ${names.take(10)}")
        }
    }

    @Test
    fun `typos still match`() {
        val names = FuzzySearch.search("chekmark", limit = 200).map { it.appleName }
        assertTrue(
            names.any { it.contains("checkmark") },
            "expected \"checkmark\" for typo \"chekmark\", got ${names.take(10)}",
        )
    }

    @Test
    fun `categories are searchable`() {
        val names = FuzzySearch.search("weather", limit = 200).map { it.appleName }
        assertTrue(
            names.isNotEmpty(),
            "expected the Weather category to be searchable, got no results",
        )
    }

    @Test
    fun `subsequence matching works`() {
        val names = FuzzySearch.search("hsf", limit = 200).map { it.appleName }
        assertTrue(
            names.any { it.contains("house") },
            "expected house symbols for \"hsf\", got ${names.take(10)}",
        )
    }

    @Test
    fun `empty query returns catalog head`() {
        assertEquals(
            SfSymbolsCatalog.all.take(200).map { it.appleName },
            FuzzySearch.search("   ", limit = 200).map { it.appleName },
        )
    }

    @Test
    fun `repeated queries are consistent`() {
        val first = FuzzySearch.search("home", limit = 200).map { it.appleName }
        val second = FuzzySearch.search("home", limit = 200).map { it.appleName }
        assertEquals(first, second, "cached result must match a fresh evaluation")
    }
}
