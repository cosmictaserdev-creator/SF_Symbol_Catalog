@echo off
setlocal
set "MCP_JAVA=java"
if defined JAVA_HOME set "MCP_JAVA=%JAVA_HOME%\bin\java.exe"
"%MCP_JAVA%" -Djava.awt.headless=true -cp "%~dp0lib\*" com.sfsymbols.MainKt --mcp
exit /b %errorlevel%
