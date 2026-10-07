package com.sfsymbols.mcp

import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import kotlinx.serialization.json.*
import kotlin.test.*

class McpServerTest {
    private fun exchange(vararg lines: String): List<JsonObject> {
        val output = StringWriter()
        McpServer(StringReader(lines.joinToString("\n")).buffered(), PrintWriter(output))
            .runBlockingUntilExit()
        return output.toString().lineSequence().filter { it.isNotBlank() }
            .map { Json.parseToJsonElement(it).jsonObject }.toList()
    }

    @Test fun `pipelined handshake and notifications preserve all requests`() {
        val replies = exchange(
            """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}""",
            """{"jsonrpc":"2.0","method":"notifications/initialized"}""",
            """{"jsonrpc":"2.0","id":2,"method":"tools/list"}""",
            """{"jsonrpc":"2.0","method":"ping"}""",
            """{"jsonrpc":"2.0","id":3,"method":"ping"}""",
        )
        assertEquals(listOf(1, 2, 3), replies.map { it["id"]!!.jsonPrimitive.int })
        assertEquals("2025-11-25", replies[0]["result"]!!.jsonObject["protocolVersion"]!!.jsonPrimitive.content)
        assertEquals(setOf("search_symbols", "get_symbol"), replies[1]["result"]!!.jsonObject["tools"]!!.jsonArray
            .map { it.jsonObject["name"]!!.jsonPrimitive.content }.toSet())
    }

    @Test fun `bad messages return errors and do not kill the connection`() {
        val replies = exchange(
            "{broken",
            "[]",
            """{"jsonrpc":"2.0","id":1,"method":"unknown"}""",
            """{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"search_symbols","arguments":{"query":5}}}""",
            """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"search_symbols","arguments":{"query":"heart","limit":0}}}""",
            """{"jsonrpc":"2.0","id":4,"method":"ping"}""",
        )
        assertEquals(listOf(-32700, -32600, -32601, -32602, -32602), replies.take(5)
            .map { it["error"]!!.jsonObject["code"]!!.jsonPrimitive.int })
        assertNotNull(replies.last()["result"])
    }

    @Test fun `symbol lookup and missing symbol use tool results`() {
        val replies = exchange(
            """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"get_symbol","arguments":{"code":"SFHeartFill"}}}""",
            """{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"get_symbol","arguments":{"code":"heart.fill"}}}""",
            """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"get_symbol","arguments":{"code":"does-not-exist"}}}""",
        )
        assertEquals(replies[0]["result"], replies[1]["result"])
        val text = replies[0]["result"]!!.jsonObject["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content
        assertEquals("heart.fill", Json.parseToJsonElement(text).jsonObject["appleName"]!!.jsonPrimitive.content)
        assertTrue(replies[2]["result"]!!.jsonObject["isError"]!!.jsonPrimitive.boolean)
    }

    @Test fun `category filtering happens before the requested limit`() {
        val reply = exchange(
            """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"search_symbols","arguments":{"query":"a","category":"weather","limit":1}}}""",
        ).single()
        val text = reply["result"]!!.jsonObject["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content
        val symbols = Json.parseToJsonElement(text).jsonObject["symbols"]!!.jsonArray
        assertEquals(1, symbols.size)
        assertTrue(symbols.single().jsonObject["categories"]!!.jsonArray.any { it.jsonPrimitive.content.equals("weather", true) })
    }
}
