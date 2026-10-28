@echo off
chcp 936 >nul
cd /d "%~dp0"
for %%I in ("%~dp0.") do set "PROJ=%%~nxI"

echo 正在停止 %PROJ% 全套服务 ...

rem 探测前端端口
set "FE_PORT=5173"
for /f "tokens=*" %%l in ('findstr /c:"port:" vite.config.js 2^>nul') do (
  for /f "tokens=2 delims=: " %%p in ("%%l") do set "FE_PORT=%%p"
)
set "FE_PORT=%FE_PORT:,=%"

rem 探测后端目录与端口
set "BE_DIR="
for /d %%D in ("%~dp0..\*") do (
  if exist "%%D\mvnw.cmd" if exist "%%D\pom.xml" set "BE_DIR=%%D"
)
if not defined BE_DIR set "BE_DIR=%~dp0..\springBootTest"
for %%I in ("%BE_DIR%") do set "BE_PROJ=%%~nxI"
set "BE_PORT=8080"
for /r "%BE_DIR%\src\main\resources" %%f in (application*.properties) do (
  for /f "tokens=*" %%l in ('findstr /c:"server.port" "%%f" 2^>nul') do (
    for /f "tokens=2 delims== " %%p in ("%%l") do set "BE_PORT=%%p"
  )
)
set "BE_PORT=%BE_PORT:#=%"

rem 1. 按窗口标题关闭（标题随项目名动态生成）
taskkill /f /fi "WINDOWTITLE eq %PROJ% Web*" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq %BE_PROJ% API*" >nul 2>&1

rem 2. 按端口兜底清理（端口动态探测，/C 保证整串作字面量，不会误杀其它端口）
for %%P in (%FE_PORT% %BE_PORT%) do (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr /C:":%%P " ^| findstr "LISTENING"') do (
        taskkill /f /pid %%a >nul 2>&1
    )
)

echo.
echo 已停止全部服务（前端 %FE_PORT% / 后端 %BE_PORT%）。
pause
