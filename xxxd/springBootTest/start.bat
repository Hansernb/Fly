@echo off
chcp 936 >nul
cd /d "%~dp0"
for %%I in ("%~dp0.") do set "PROJ=%%~nxI"
title %PROJ% API

echo ============================================
echo   %PROJ% 后端一键启动（端口动态探测）
echo   前端项目: ../boonie-auth-pro (端口见其 vite 配置)
echo ============================================
echo.

rem 探测端口：只读【激活 profile】对应的 application-{profile}.properties，
rem 避免遍历到 application-prob.properties(8082) 等其它 profile 导致端口错乱
set "ACTIVE_PROFILE=dev"
for /f "tokens=2 delims== " %%p in ('findstr /c:"spring.profiles.active" "%~dp0src\main\resources\application.properties" 2^>nul') do set "ACTIVE_PROFILE=%%p"
set "BE_PORT=8080"
if exist "%~dp0src\main\resources\application-%ACTIVE_PROFILE%.properties" (
  for /f "tokens=2 delims== " %%p in ('findstr /c:"server.port" "%~dp0src\main\resources\application-%ACTIVE_PROFILE%.properties" 2^>nul') do set "BE_PORT=%%p"
)
set "BE_PORT=%BE_PORT:#=%"
echo 后端端口：%BE_PORT%

rem [1/4] 检查 Java（电脑重置可能删掉 JDK）
where java >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 java 命令，电脑重置可能删掉了 JDK，请先安装 JDK 17+
    pause
    exit /b 1
)

rem [2/4] 检查 MySQL（3306 端口），没运行就尝试启动（服务优先，失败则直接拉起 mysqld）
set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.0\bin"
set "MYSQL_INI=C:\ProgramData\MySQL\MySQL Server 8.0\my.ini"

netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul 2>&1
if errorlevel 1 (
    echo [2/4] MySQL 未运行，尝试启动服务 MySQL80 ...
    net start MySQL80 >nul 2>&1
    timeout /t 3 /nobreak >nul
)
netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul 2>&1
if errorlevel 1 (
    echo [2/4] 服务启动失败，尝试直接拉起 mysqld 进程 ...
    if exist "%MYSQL_BIN%\mysqld.exe" (
        start "" "%MYSQL_BIN%\mysqld.exe" --defaults-file="%MYSQL_INI%" --port=3306 --bind-address=0.0.0.0
        timeout /t 8 /nobreak >nul
    )
)
netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul 2>&1
if errorlevel 1 (
    echo [错误] MySQL 未运行且无法自动启动，请手动启动 MySQL 服务后重试
    pause
    exit /b 1
)
echo [2/4] MySQL 正常

rem [3/4] 幂等初始化数据库（重置后库表可能没了，重复执行无副作用）
set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not exist "%MYSQL_BIN%" set "MYSQL_BIN=mysql"
"%MYSQL_BIN%" -uroot -p123456 --default-character-set=utf8mb4 < db-init.sql >nul 2>&1
if errorlevel 1 (
    echo [错误] 数据库初始化失败，请检查 MySQL root 密码是否为 123456
    pause
    exit /b 1
)
echo [3/4] 数据库 boonie_auth 就绪

rem [4/4] 启动 Spring Boot（端口固定为探测到的 %BE_PORT%，防止 SERVER_PORT 环境变量劫持）
set "SERVER_PORT=%BE_PORT%"
echo [4/4] 正在编译并启动后端（端口 %BE_PORT%，首次编译较慢，请耐心等待）...
echo.
call mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=%BE_PORT%

echo.
if errorlevel 1 (
    echo [错误] 后端启动失败，请把上方日志截图发给 AI 排查
) else (
    echo 后端已退出。
)
pause
