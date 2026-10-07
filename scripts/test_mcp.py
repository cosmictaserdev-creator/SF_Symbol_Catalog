"""Exercise the assembled MCP server through real stdin/stdout pipes (no third-party packages)."""
import argparse
import json
from pathlib import Path
import queue
import subprocess
import threading


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--java", default="java", help="Java 21+ executable")
    args = parser.parse_args()
    lib = Path(__file__).resolve().parents[1] / "app/build/mcp/lib"
    if not lib.is_dir():
        parser.error("Run ./gradlew assembleMcp first")
    process = subprocess.Popen(
        [args.java, "-Djava.awt.headless=true", "-cp", str(lib / "*"), "com.sfsymbols.MainKt", "--mcp"],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        text=True, encoding="utf-8", bufsize=1,
    )
    replies = queue.Queue()

    def read_replies():
        for line in process.stdout:
            replies.put(line)

    threading.Thread(target=read_replies, daemon=True).start()

    def send(message):
        process.stdin.write(json.dumps(message) + "\n")
        process.stdin.flush()

    def receive(expected_id):
        response = json.loads(replies.get(timeout=20))
        assert response["jsonrpc"] == "2.0", response
        assert response["id"] == expected_id, response
        return response

    try:
        send({"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {
            "protocolVersion": "2025-11-25", "capabilities": {},
            "clientInfo": {"name": "catalog-smoke-test", "version": "1"}}})
        assert receive(1)["result"]["protocolVersion"] == "2025-11-25"
        send({"jsonrpc": "2.0", "method": "notifications/initialized"})
        # One write deliberately pipelines requests to detect lost buffered input.
        requests = [
            {"id": 2, "method": "tools/list"},
            {"id": 3, "method": "tools/call", "params": {"name": "search_symbols", "arguments": {"query": "ai", "limit": 50}}},
            {"id": 4, "method": "tools/call", "params": {"name": "get_symbol", "arguments": {"code": "SFHeartFill"}}},
            {"id": 5, "method": "tools/call", "params": {"name": "search_symbols", "arguments": {"query": "a", "category": "weather", "limit": 1}}},
            {"id": 6, "method": "tools/call", "params": {"name": "get_symbol", "arguments": {"code": "missing-symbol"}}},
            {"id": 7, "method": "not_a_method"},
            {"id": 8, "method": "tools/call", "params": {"name": "search_symbols", "arguments": {"query": "heart", "limit": "wrong"}}},
            {"id": 9, "method": "ping"},
        ]
        process.stdin.write("".join(json.dumps({"jsonrpc": "2.0", **r}) + "\n" for r in requests))
        process.stdin.flush()
        responses = {i: receive(i) for i in range(2, 10)}
        assert {t["name"] for t in responses[2]["result"]["tools"]} == {"search_symbols", "get_symbol"}

        def content(request_id):
            return json.loads(responses[request_id]["result"]["content"][0]["text"])

        assert "sparkles" in {s["appleName"] for s in content(3)["symbols"]}
        assert content(4)["appleName"] == "heart.fill"
        assert content(5)["count"] == 1
        assert "weather" in [c.lower() for c in content(5)["symbols"][0]["categories"]]
        assert responses[6]["result"]["isError"] is True
        assert responses[7]["error"]["code"] == -32601
        assert responses[8]["error"]["code"] == -32602
        assert responses[9]["result"] == {}
        process.stdin.close()
        assert process.wait(timeout=20) == 0, process.stderr.read()
        assert replies.empty(), "Unexpected stdout or notification response"
        print("MCP smoke test passed: handshake, tools, pipelined requests, categories, errors, ping, and clean EOF.")
    finally:
        if process.poll() is None:
            process.kill()
            process.wait()


if __name__ == "__main__":
    main()
