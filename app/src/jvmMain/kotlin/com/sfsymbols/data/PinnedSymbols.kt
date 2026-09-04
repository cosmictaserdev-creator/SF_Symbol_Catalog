package com.sfsymbols.data

import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists the set of pinned symbol apple-names to a JSON file
 * in the user home directory so pins survive app restarts.
 */
public object PinnedSymbols {

    private val json = Json { prettyPrint = true }

    private fun stateDir(): File =
        File(System.getProperty("user.home"), ".sf-symbols-catalog").apply { mkdirs() }

    private fun pinsFile(): File = File(stateDir(), "pins.json")

    public fun load(): Set<String> {
        return runCatching {
            val file = pinsFile()
            if (!file.exists()) return emptySet()
            val text = file.readText()
            json.decodeFromString(SetSerializer(String.serializer()), text)
        }.getOrDefault(emptySet())
    }

    public fun save(pinned: Set<String>) {
        runCatching {
            pinsFile().writeText(json.encodeToString(SetSerializer(String.serializer()), pinned))
        }
    }
}
