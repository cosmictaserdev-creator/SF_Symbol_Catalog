package com.sfsymbols.mcp

import com.sfsymbols.data.FuzzySearch
import com.sfsymbols.data.SfSymbolMetadata
import com.sfsymbols.data.SfSymbolsCatalog
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.io.PrintWriter

/**
 * Minimal MCP (Model Context Protocol) server over stdio — newline-delimited
 * JSON-RPC 2.0. Exposes two tools so AI agents can browse the catalog:
 *  - search_symbols(query, category?, limit?) : fuzzy, ranked search
 *  - get_symbol(code)                         : resolve a pascalName to metadata
 *
 * Launch with --mcp. A single UTF-8 reader preserves pipelined messages;
 * stdout contains only protocol responses and EOF stops the process.
 */
public class McpServer(
    private val input: BufferedReader = System.`in`.bufferedReader(Charsets.UTF_8),
    private val output: PrintWriter = PrintWriter(OutputStreamWriter(System.out, Charsets.UTF_8), true),
) {
    private val json = Json
    private val protocolVersions = setOf("2025-11-25", "2025-06-18", "2025-03-26", "2024-11-05")
    private class RpcError(val code: Int, message: String) : IllegalArgumentException(message)

    /** Run synchronously to completion (used by tests / non-window launches). */
    public fun runBlockingUntilExit() {
        serve()
    }

    private fun serve() {
        while (true) {
            val line = input.readLine() ?: return
            if (line.isBlank()) continue
            val element = try {
                json.parseToJsonElement(line)
            } catch (_: IllegalArgumentException) {
                respondError(null, -32700, "Parse error")
                continue
            }
            val message = element as? JsonObject
            if (message == null) respondError(null, -32600, "Invalid request")
            else handleMessage(message)
        }
    }

    private fun handleMessage(msg: JsonObject) {
        val id = msg["id"]
        val method = (msg["method"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        val validId = id is JsonPrimitive && id != JsonNull && (id.isString || id.longOrNull != null)
        if (msg["jsonrpc"] != JsonPrimitive("2.0") || method == null || ("id" in msg && !validId)) {
            respondError(if (validId) id else null, -32600, "Invalid request")
            return
        }
        // Notifications have no response, including unrecognized notifications.
        if (id == null) return
        try {
            val params = msg["params"]?.let {
                it as? JsonObject ?: throw RpcError(-32602, "'params' must be an object")
            } ?: buildJsonObject { }
            when (method) {
            "initialize" -> respond(id) {
                val requested = requiredString(params, "protocolVersion")
                buildJsonObject {
                    put("protocolVersion", requested.takeIf { it in protocolVersions } ?: protocolVersions.first())
                    putJsonObject("capabilities") { putJsonObject("tools") {} }
                    putJsonObject("serverInfo") {
                        put("name", "sf-symbols-catalog")
                        put("version", "1.0.0")
                    }
                }
            }
            "ping" -> respond(id) { buildJsonObject { } }
            "tools/list" -> respond(id) { toolsList() }
            "tools/call" -> {
                val toolName = requiredString(params, "name")
                val args = params["arguments"]?.let {
                    it as? JsonObject ?: throw RpcError(-32602, "'arguments' must be an object")
                } ?: buildJsonObject { }
                respond(id) { callTool(toolName, args) }
            }
            else -> respondError(id, -32601, "Method not found: $method")
            }
        } catch (e: RpcError) {
            respondError(id, e.code, e.message ?: "Invalid params")
        } catch (_: Exception) {
            respondError(id, -32603, "Internal error")
        }
    }

    private fun requiredString(args: JsonObject, key: String): String =
        optionalString(args, key)?.takeIf { it.isNotBlank() }
            ?: throw RpcError(-32602, "'$key' is required and must be a nonblank string")

    private fun optionalString(args: JsonObject, key: String): String? {
        val value = args[key] ?: return null
        return (value as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: throw RpcError(-32602, "'$key' must be a string")
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
                                put("minimum", 1)
                                put("maximum", 300)
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
                val query = requiredString(args, "query")
                val category = optionalString(args, "category")
                val limit = args["limit"]?.let {
                    (it as? JsonPrimitive)?.takeUnless { value -> value.isString }?.intOrNull
                        ?.takeIf { value -> value in 1..300 }
                        ?: throw RpcError(-32602, "'limit' must be an integer from 1 to 300")
                } ?: 25
                // Filter the full ranked result before limiting so a category
                // cannot lose matches to higher-ranked symbols outside it.
                val results = FuzzySearch.search(query, limit = SfSymbolsCatalog.all.size)
                    .filter { meta -> category == null || meta.categories.any { it.equals(category, true) } }
                    .take(limit)
                buildJsonObject {
                    putJsonArray("content") { add(textContent(symbolsJson(results))) }
                }
            }
            "get_symbol" -> {
                val code = requiredString(args, "code")
                val meta = SfSymbolsCatalog.all.firstOrNull {
                    it.pascalName.equals(code, ignoreCase = true) ||
                        it.appleName.equals(code, ignoreCase = true)
                } ?: return buildJsonObject {
                    put("isError", true)
                    putJsonArray("content") {
                        add(buildJsonObject { put("type", "text"); put("text", "No symbol found for '$code'") })
                    }
                }
                buildJsonObject {
                    put("content", buildJsonArray { add(textContent(symbolJson(meta))) })
                }
            }
            else -> throw RpcError(-32602, "Unknown tool: $name")
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
            put("id", id ?: JsonNull)
            put("result", result())
        }
        write(obj)
    }

    private fun respondError(id: JsonElement?, code: Int, message: String) {
        val obj = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id ?: JsonNull)
            putJsonObject("error") {
                put("code", code)
                put("message", message)
            }
        }
        write(obj)
    }

    private fun write(obj: JsonObject) {
        output.println(json.encodeToString(JsonObject.serializer(), obj))
        output.flush()
    }
}
