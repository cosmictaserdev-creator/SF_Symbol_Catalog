package com.sfsymbols.mcp

import com.sfsymbols.data.FuzzySearch
import com.sfsymbols.data.SfSymbolMetadata
import com.sfsymbols.data.SfSymbolsCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*

/**
 * Minimal MCP (Model Context Protocol) server over stdio — newline-delimited
 * JSON-RPC 2.0. Exposes two tools so AI agents can browse the catalog:
 *  - search_symbols(query, category?, limit?) : fuzzy, ranked search
 *  - get_symbol(code)                         : resolve a pascalName to metadata
 *
 * Transported exactly like an LSP server (one JSON object per line on stdin,
 * responses on stdout), so it can be launched by any MCP client
 * (Claude, etc.) via `command` + `args`.
 */
@OptIn(ExperimentalSerializationApi::class)
public class McpServer(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val readLine: () -> String? = { System.`in`.bufferedReader().readLine() },
    private val writeLine: (String) -> Unit = { println(it); System.out.flush() },
) {
    private val json = Json

    private val protocolVersion = "2024-11-05"

    /** Start the server: launch a background coroutine that reads stdin. */
    public fun start() {
        scope.launch(Dispatchers.IO) {
            runCatching { serve() }
        }
    }

    /** Run synchronously to completion (used by tests / non-window launches). */
    public fun runBlockingUntilExit() {
        serve()
    }

    private fun serve() {
        while (true) {
            val line = readLine() ?: return
            if (line.isBlank()) continue
            val message = runCatching { json.parseToJsonElement(line).jsonObject }.getOrNull()
                ?: continue
            handleMessage(message)
        }
    }

    private fun handleMessage(msg: JsonObject) {
        val method = msg["method"]?.jsonPrimitive?.contentOrNull ?: return
        val id = msg["id"]

        when (method) {
            "initialize" -> respond(id) {
                buildJsonObject {
                    put("protocolVersion", protocolVersion)
                    putJsonObject("capabilities") { putJsonObject("tools") {} }
                    putJsonObject("serverInfo") {
                        put("name", "sf-symbols-catalog")
                        put("version", "1.0.0")
                    }
                }
            }
            "notifications/initialized" -> { /* fire-and-forget */ }
            "ping" -> respond(id) { buildJsonObject { } }
            "tools/list" -> respond(id) { toolsList() }
            "tools/call" -> {
                val params = msg["params"]?.jsonObject ?: JsonObject(emptyMap())
                val toolName = params["name"]?.jsonPrimitive?.contentOrNull ?: ""
                val args = params["arguments"]?.jsonObject ?: JsonObject(emptyMap())
                try {
                    val out = callTool(toolName, args)
                    respond(id) { out }
                } catch (e: Exception) {
                    respondError(id, -32602, e.message ?: "tool error")
                }
            }
            "shutdown" -> respond(id) { buildJsonObject { } }
            "exit" -> {
                respond(id) { buildJsonObject { } }
                kotlin.system.exitProcess(0)
            }
        }
    }

    private fun toolsList(): JsonObject = buildJsonObject {
        putJsonArray("tools") {
            add(
                buildJsonObject {
                    put("name", "search_symbols")
                    put("description", "Fuzzy, typo-tolerant, ranked search of the SF Symbols catalog. Returns symbol metadata matching the query.")
                    putJsonObject("inputSchema") {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("query") {
                                put("type", "string")
                                put("description", "Search text, e.g. 'heart' or 'arrow.right'")
                            }
                            putJsonObject("category") {
                                put("type", "string")
                                put("description", "Optional category to filter within, e.g. 'objects'")
                            }
                            putJsonObject("limit") {
                                put("type", "integer")
                                put("description", "Max results to return (default 25)")
                            }
                        }
                        putJsonArray("required") { add(JsonPrimitive("query")) }
                    }
                }
            )
            add(
                buildJsonObject {
                    put("name", "get_symbol")
                    put("description", "Resolve a symbol's pascal code (e.g. 'SFHeartFill') or apple name (e.g. 'heart.fill') to its metadata including categories and mode prefix.")
                    putJsonObject("inputSchema") {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("code") {
                                put("type", "string")
                                put("description", "Pascal name like 'SFHeartFill' or apple name like 'heart.fill'")
                            }
                        }
                        putJsonArray("required") { add(JsonPrimitive("code")) }
                    }
                }
            )
        }
    }

    private fun callTool(name: String, args: JsonObject): JsonObject {
        return when (name) {
            "search_symbols" -> {
                val query = (args["query"] as? JsonPrimitive)?.contentOrNull ?: ""
                val category = (args["category"] as? JsonPrimitive)?.contentOrNull
                val limit = ((args["limit"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
                    ?: 25).coerceIn(1, 300)
                if (query.isBlank()) throw IllegalArgumentException("'query' is required")
                val results = FuzzySearch.search(query, limit = limit).filter { meta ->
                    category == null || meta.categories.contains(category)
                }
                buildJsonObject {
                    putJsonArray("content") { add(textContent(symbolsJson(results))) }
                }
            }
            "get_symbol" -> {
                val code = (args["code"] as? JsonPrimitive)?.contentOrNull ?: ""
                if (code.isBlank()) throw IllegalArgumentException("'code' is required")
                val meta = SfSymbolsCatalog.all.firstOrNull {
                    it.pascalName.equals(code, ignoreCase = true) ||
                        it.appleName.equals(code, ignoreCase = true)
                } ?: throw IllegalArgumentException("No symbol found for '$code'")
                buildJsonObject {
                    put("content", buildJsonArray { add(textContent(symbolJson(meta))) })
                }
            }
            else -> throw IllegalArgumentException("Unknown tool: $name")
        }
    }

    private fun symbolsJson(symbols: List<SfSymbolMetadata>): JsonObject = buildJsonObject {
        put("count", symbols.size)
        putJsonArray("symbols") { symbols.forEach { add(symbolJson(it)) } }
    }

    private fun symbolJson(meta: SfSymbolMetadata): JsonObject = buildJsonObject {
        put("appleName", meta.appleName)
        put("pascalName", meta.pascalName)
        put("code", "SfSymbols.Dualtone.${meta.pascalName}")
        put("monochromeCode", "SfSymbols.Monochrome.${meta.pascalName}")
        putJsonArray("categories") { meta.categories.forEach { add(JsonPrimitive(it)) } }
        put("isRestricted", meta.isRestricted)
    }

    private fun textContent(element: JsonElement): JsonObject = buildJsonObject {
        put("type", "text")
        put("text", element.toString())
    }

    private fun respond(id: JsonElement?, result: () -> JsonObject) {
        val obj = buildJsonObject {
            put("jsonrpc", "2.0")
            if (id != null) {
                put("id", id)
            } else {
                put("id", JsonPrimitive(null))
            }
            put("result", result())
        }
        write(obj)
    }

    private fun respondError(id: JsonElement?, code: Int, message: String) {
        val obj = buildJsonObject {
            put("jsonrpc", "2.0")
            if (id != null) put("id", id) else put("id", JsonPrimitive(null))
            putJsonObject("error") {
                put("code", code)
                put("message", message)
            }
        }
        write(obj)
    }

    private fun write(obj: JsonObject) {
        writeLine(json.encodeToString(JsonObject.serializer(), obj))
    }
}
