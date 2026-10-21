@echo off
chcp 936 >nul
title boonie-auth-pro 启动器
rem 本脚本必须放在 boonie-auth-pro 项目根目录，自动切到脚本所在目录，以下全用相对路径
cd /d "%~dp0"

echo ============================================
echo   boonie-auth-pro 一键启动（连 Java 后端）
echo   前端: http://localhost:5173
echo   后端: http://localhost:8080 (springBootTest)
echo ============================================
echo.

rem 每次开机 node_modules 会被系统删除/损坏，所以检查“完整安装”再决定是否重装
rem 注意：只判断 node_modules 文件夹存在会漏掉“残缺安装”（有文件夹却没有 vite），这里改为检测真正的 vite 可执行文件
if not exist "node_modules\.bin\vite" (
    echo [1/3] 未发现可用的 vite 依赖（node_modules 可能不完整），正在安装依赖（走国内镜像，较快）...
    call npm ci --registry=https://registry.npmmirror.com
    if errorlevel 1 (
        echo npm ci 失败，改用 npm install 重试...
        call npm install --registry=https://registry.npmmirror.com
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

rem 清理 vite 异常退出留下的临时文件
if exist "vite.config.js.timestamp-*.mjs" del /q "vite.config.js.timestamp-*.mjs"

echo [2/3] 启动后端 springBootTest（端口 8080，窗口标题 springboot-api）...
start "springboot-api" cmd /k "..\springBootTest\start.bat"

echo [3/3] 启动前端 Vite（端口 5173，就绪后自动打开浏览器）...
start "boonie-auth-web" cmd /k "npx vite"

echo.
echo 启动完成！弹出的两个窗口是前后端进程，请勿手动关闭。
echo 后端首次编译需要几十秒，看到 Tomcat started on port 8080 才算就绪。
echo 需要停止时运行根目录下的 stop.bat
pause
