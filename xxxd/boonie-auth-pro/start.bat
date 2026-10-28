@echo off
chcp 936 >nul
rem 本脚本放在前端项目根目录；自动切到脚本所在目录，全部用相对路径。
rem 动态自适应：项目名 / 端口 / 后端目录全部运行时探测，改 vite 配置、改后端端口、重命名或新增项目都无需改本脚本。
cd /d "%~dp0"

rem 项目名 = 当前目录名（改文件夹名自动跟随）
for %%I in ("%~dp0.") do set "PROJ=%%~nxI"
title %PROJ% 启动器

echo ============================================
echo   %PROJ% 一键启动（连 Java 后端，端口动态探测）
echo ============================================
echo.

rem [0] 探测前端端口：读 vite.config.js 的 server.port，找不到则用 5173
set "FE_PORT=5173"
for /f "tokens=*" %%l in ('findstr /c:"port:" vite.config.js 2^>nul') do (
  for /f "tokens=2 delims=: " %%p in ("%%l") do set "FE_PORT=%%p"
)
set "FE_PORT=%FE_PORT:,=%"
echo 前端端口：%FE_PORT%

rem [1] 探测后端目录：找同级含 mvnw.cmd + pom.xml 的文件夹，找不到回退 springBootTest
set "BE_DIR="
for /d %%D in ("%~dp0..\*") do (
  if exist "%%D\mvnw.cmd" if exist "%%D\pom.xml" set "BE_DIR=%%D"
)
if not defined BE_DIR set "BE_DIR=%~dp0..\springBootTest"
for %%I in ("%BE_DIR%") do set "BE_PROJ=%%~nxI"
echo 后端目录：%BE_PROJ%

rem [2] 探测后端端口：只读【激活 profile】对应的 application-{profile}.properties，找不到则用 8080
set "ACTIVE_PROFILE=dev"
for /f "tokens=2 delims== " %%p in ('findstr /c:"spring.profiles.active" "%BE_DIR%\src\main\resources\application.properties" 2^>nul') do set "ACTIVE_PROFILE=%%p"
set "BE_PORT=8080"
if exist "%BE_DIR%\src\main\resources\application-%ACTIVE_PROFILE%.properties" (
  for /f "tokens=2 delims== " %%p in ('findstr /c:"server.port" "%BE_DIR%\src\main\resources\application-%ACTIVE_PROFILE%.properties" 2^>nul') do set "BE_PORT=%%p"
)
set "BE_PORT=%BE_PORT:#=%"
echo 后端端口：%BE_PORT%
echo.

rem [3] 依赖检查：每次开机 node_modules 可能被删，检测真正的 vite 可执行文件
if not exist "node_modules\.bin\vite" (
    echo [1/3] 未发现可用 vite 依赖，正在安装（国内镜像，跳过 postinstall 以免 esbuild 校验脚本在受限环境 spawnSync 失败）...
    call npm ci --ignore-scripts --registry=https://registry.npmmirror.com
    if errorlevel 1 (
        echo npm ci 失败，改用 npm install 重试...
        call npm install --ignore-scripts --registry=https://registry.npmmirror.com
        if errorlevel 1 (
            echo.
            echo 依赖安装失败，请检查网络后重新运行本脚本
            pause
            exit /b 1
        )
    )
) else (
    echo [1/3] 依赖已就绪，跳过安装
)

rem 清理 vite 异常退出残留
if exist "vite.config.js.timestamp-*.mjs" del /q "vite.config.js.timestamp-*.mjs"

echo [2/3] 启动后端 %BE_PROJ%（端口 %BE_PORT%，窗口标题 "%BE_PROJ% API"）...
start "%BE_PROJ% API" cmd /k call "%BE_DIR%\start.bat"

echo [3/3] 启动前端 Vite（端口 %FE_PORT%，就绪后自动打开浏览器）...
start "%PROJ% Web" cmd /k npx vite --port %FE_PORT%

echo.
echo 启动完成！弹出的两个窗口是前后端进程，请勿手动关闭。
echo 后端首次编译需要几十秒，看到 Tomcat started on port %BE_PORT% 才算就绪。
echo 需要停止时运行根目录下的 stop.bat
pause
