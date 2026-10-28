@echo off
chcp 936 >nul
cd /d "%~dp0"
for %%I in ("%~dp0.") do set "PROJ=%%~nxI"

echo 正在停止 %PROJ% 后端 ...

rem 探测端口：读 application*.properties 的 server.port，找不到则用 8080
set "BE_PORT=8080"
for /r "%~dp0src\main\resources" %%f in (application*.properties) do (
  for /f "tokens=*" %%l in ('findstr /c:"server.port" "%%f" 2^>nul') do (
    for /f "tokens=2 delims== " %%p in ("%%l") do set "BE_PORT=%%p"
  )
)
set "BE_PORT=%BE_PORT:#=%"

rem 1. 按窗口标题关闭（标题随项目名动态生成）
taskkill /f /fi "WINDOWTITLE eq %PROJ% API*" >nul 2>&1

rem 2. 按端口兜底清理残留的 java 进程（端口动态探测，/C 保证整串作字面量）
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /C:":%BE_PORT% " ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a >nul 2>&1
)

echo 已停止后端（%BE_PORT%）。
pause
