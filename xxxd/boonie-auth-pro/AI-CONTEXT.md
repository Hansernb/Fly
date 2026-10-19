# AI-CONTEXT.md — 给 AI 助手的快速上手文件（读完即可开工，无需再探索项目）

> 电脑每次开机都会重置：本项目 `node_modules` 会被删除；后端项目 `../springBootTest` 的 `target` 也会被删除。
> **下次会话请先读本文件，不要重新分析项目结构。**

## 项目是什么
「熊出没 · 森林登录注册 Pro」：Vue 3 + Vite 前端，后端已切换为 **Java Spring Boot**（`../springBootTest`）。
- 前端：Vite 开发服务器，端口 **5173**，`vite.config.js` 里 `open: true` 会自动开浏览器
- 后端：`../springBootTest`（Spring Boot 3.4.5 + MyBatis-Plus + MySQL），端口 **8080**，详见 `../springBootTest/AI-CONTEXT.md`
- 前端所有请求走相对路径 `/api/*`，由 Vite 代理转发到 `http://localhost:8080`
- `server/index.js` 是旧的 Express 假后端，**已弃用**（不再启动），仅作参考保留

## 一键脚本（根目录，GBK 编码，双击即可）
- `start.bat`：检查 node_modules → 缺失则自动 `npm ci`（国内镜像 npmmirror，失败降级 npm install）
  → 清理 `vite.config.js.timestamp-*.mjs` 垃圾文件 → 用相对路径 `..\springBootTest\start.bat` 启动 Java 后端(8080) → 启动前端(5173)
- `stop.bat`：按窗口标题 + 按端口(8080/5173) 双重方式杀掉所有相关进程
- 只想启动后端：双击 `..\springBootTest\start.bat`

## 手动启动（脚本不可用时的后备方案）
```bash
# 后端（MySQL 需在运行，库表用 ../db-init.sql 初始化；该 SQL 已含建库建表 + 示例数据种子，可重复执行）
cd ../springBootTest
set SERVER_PORT=8080 && mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
# 前端
cd ../boonie-auth-pro
npm ci --registry=https://registry.npmmirror.com
npx vite
```

## 快速验证清单
1. 后端日志出现 `Tomcat started on port 8080` → Java 后端就绪（首次编译要几十秒）
2. `curl http://localhost:8080/api/health` → `{"ok":true,...}`
3. 浏览器开 5173 → 能收到验证码、能注册登录 → 说明代理和端口都正常
4. 测试账号：熊大 `admin@boonie.com` / 密码 `bear123456`

## 文件地图
- `src/` — 前端（main.js / App.vue / components/AuthCard.vue / style.css）
- `src/components/ParkHome.vue` — **已改为从后端 API 取数**：`onMounted` 调 `GET /api/attractions`（带本地 attractions.js 兜底）；游乐项目卡片可点击打开「项目详情」弹窗（含该项目评价 + 发表评价，POST /api/reviews）；底部游客评价表单同样写库。后端不可达时自动降级为示例数据并显示提示条。
- `server/index.js` — 旧 Express 后端（已弃用，勿启动，会和 Java 后端抢 8080）
- `data/users.json` — 旧 Express 的用户数据（已弃用，真正数据在 MySQL boonie_auth.boonie_user）
- `vite.config.js` — 端口 5173、代理 /api → 8080、自动开浏览器
- `start.bat` / `stop.bat` — 启动/停止脚本
- `vite.config.js.timestamp-*.mjs` — vite 崩溃残留垃圾，start.bat 会自动清理，可无视

## 给未来 AI 的提醒
- 不要花时间分析项目结构、读源码细节 —— 本文件已覆盖全部要点
- 用户要求：脚本用 GBK 编码、路径用相对路径、脚本不能依赖 node_modules/target 已存在
- 沙箱执行启动命令时环境变量 SERVER_PORT 会被劫持导致端口错乱，务必显式指定 8080（详见后端 AI-CONTEXT 的踩坑记录）
- 如果 start.bat 报 npm 不存在 → 电脑重置把 Node 也删了，需先装 Node 22+
