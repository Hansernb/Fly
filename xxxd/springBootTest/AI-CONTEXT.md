# AI-CONTEXT.md — 给 AI 助手的快速上手文件（读完即可开工，无需再探索项目）

> 电脑每次开机都会重置：`target` 文件夹会被删除（前端项目 boonie-auth-pro 的 `node_modules` 也会被删）。
> **下次会话请先读本文件，不要重新分析项目结构。**

## 项目是什么
「森林登录注册后端」：Spring Boot 3.4.5 + MyBatis-Plus 3.5.9 + MySQL 8 + Lombok + Thymeleaf。
- 端口 **8080**（dev profile，见 `src/main/resources/application-dev.properties`）
- 数据库：本地 MySQL `boonie_auth` 库 / `boonie_user` 表（root / 123456，端口 3306）
- 种子用户（表为空时 `DataInitializer` 自动写入）：熊大 `admin@boonie.com`、熊二 `bear2@boonie.com`，密码均为 `bear123456`
- 前端配套项目在 `../boonie-auth-pro`（Vue 3 + Vite，5173 端口，`/api` 代理到 8080）

## 一键脚本（根目录，GBK 编码，双击即可）
- `start.bat`：检查 Java → 检查 MySQL(3306) → 用 `db-init.sql` 幂等建库建表 → `mvnw spring-boot:run`（target 被删会自动重新编译；端口从**激活 profile** 的 `application-{profile}.properties` 的 `server.port` 动态探测（不遍历其它 profile 文件，避免读到 `application-prob.properties` 的 8082），并显式 `--server.port` 防 SERVER_PORT 环境变量劫持）。项目名/窗口标题由目录名推出。
- `stop.bat`：按窗口标题（`springBootTest API`，随项目名动态生成）+ 按端口（`findstr /C:":端口 "` 动态探测）杀掉后端进程
- `db-init.sql`：建库建表脚本，幂等可重复执行

## 历史踩坑（重要，勿再踩）
1. **端口被环境变量劫持**：AI 沙箱执行时会注入 `SERVER_PORT` 环境变量，覆盖 application.properties，
   导致 Tomcat 绑到随机端口（如 49549）并报 "Port already in use"。解决：start.bat 里已 `set SERVER_PORT=8080`
   并传 `-Dspring-boot.run.arguments=--server.port=8080`。AI 直接命令行启动时也要同样处理。
2. **数据库不存在**：`Unknown database 'boonie_auth'` → 跑一遍 `db-init.sql` 即可，表结构见脚本。
3. **MySQL 客户端路径**：`C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`（PATH 里没有 mysql）。
4. **系统环境**：JDK 21（项目要求 17+，兼容）、无系统 mvn（必须用项目自带 mvnw.cmd）、MySQL 是 Windows 服务。

## 快速验证清单
1. 启动后看到 `Tomcat started on port 8080` + `Started SpringBootTestApplication`
2. `curl http://localhost:8080/api/health` → `{"ok":true,...,"users":N}`
3. `curl -X POST http://localhost:8080/api/login -H "Content-Type: application/json" -d '{"username":"熊大","password":"bear123456"}'` → 返回 token（**用户名登录**）

## 接口一览（controller 前缀 /api）
- `GET  /api/health` — 健康检查
- `POST /api/login` — **用户名 + 密码登录**，body：`{ username, password }`（原邮箱登录已改为用户名；验证码登录仍用 `{ email, code }`）
- `POST /api/send-code` — 发验证码
- `GET  /api/me` — 当前用户（带 token）
- `GET  /api/attractions` — 游乐项目列表（含评价列表 + 实时计算的综合评分 `rating`），字段对齐前端 `src/data/attractions.js`（desc/wait/rating/reviews[{name,avatar,rating,date,text}]）
- `POST /api/reviews` — 新增游客评价，body：`{ attractionId, name?, avatar?, rating(1-5), content }`，返回写入后的评价对象

## 权限模型（重要）
- `boonie_user.permission`：**1 = 默认权限**（注册用户），**0 = 管理员权限**
- 注册接口固定写入 `permission = 1`；只有管理员能在首页看到「后台管理系统」按钮并登录后台
- **`/api/login`、`/api/register`、`/api/me` 返回的 `user` 都带 `permission`**，前端据此控制按钮显隐
- **密码按需求明文存储**（字段 `boonie_user.password`，不加密），登录为明文比对；`AppConfig` 已移除 BCrypt Bean。
  若日后改回哈希：恢复 PasswordEncoder Bean，并把 `AuthServiceImpl` 的明文读写改回 `encode`/`matches`
- **登录账号 = 用户名（`boonie_user.name`）**，不再是邮箱；`name` 上有唯一索引 `uk_user_name`（`db-init.sql` 幂等创建），注册也会校验重名
- 迁移脚本 `db-init.sql`（幂等）：新增 `permission` 列、把 `password_hash` 改名为 `password`，种子账号重置为明文 `bear123456`、熊大设为管理员
  - 旧库里已是 BCrypt 哈希的账号无法还原明文，需管理员在后台「重置密码」

## 后端自带页面（跑在 8080，与前端 Vue 5173 相互独立）
- `/` → `src/main/resources/static/index.html`：园区首页，导航栏带后台入口
  - 未登录显示「🔑 管理员登录」；已登录且 `permission === 0` 显示「⚙️ 后台管理系统」；`permission === 1` **隐藏按钮**
  - 项目数据来自 `GET /api/attractions`（该接口字段为 `desc` / `wait`）
- `/admin-login.html` → 后台登录页（**仅用户名 + 密码，无注册**），调 `POST /api/admin/login`，body `{ username, password }`
- `/admin.html` → 后台管理系统：游乐项目 CRUD / 游客评价查看与删除 / 用户权限切换与重置密码
  - 进入时先查 `/api/me`，非管理员（`permission !== 0`）直接退回首页
  - 管理接口字段为 `description` / `waitMinutes`（与 `/api/attractions` 的 `desc` / `wait` 不同，别混用）
- ⚠️ 改 `src/main/resources/static/*` 后需重启后端，或把文件同步到 `target/classes/static/` 才生效

## 后台管理接口（前缀 /api/admin，均需管理员令牌）
调用方式：`Authorization: Bearer <token>`，且该 token 对应用户 `permission == 0`，否则 401/403
- `POST /api/admin/login` — 后台登录，body：`{ username, password }`（先按用户名密码登录再校验管理员；非管理员 403 拒绝）
- `GET|POST /api/admin/attractions` — 项目列表 / 新增
- `PUT|DELETE /api/admin/attractions/{id}` — 修改 / 删除（删项目会连带删其评价）
- `GET /api/admin/reviews` — 全部评价（带所属项目名）
- `DELETE /api/admin/reviews/{id}` — 删除评价
- `GET /api/admin/users` — 用户列表（**不返回密码**）
- `PUT /api/admin/users/{id}` — body `{ permission: 0|1 }` 改权限；`{ password }` 重置明文密码（不能取消自己的管理员）

## 文件地图
- `src/main/java/com/fenglin/springboottest/` — 主类 / AuthController / UserController / AttractionController / service / mapper / entity(User→boonie_user 表；Attraction→boonie_attraction 表；Review→boonie_review 表)
- `src/main/resources/application-dev.properties` — 端口 8080、MySQL 连接（本文件含数据库密码，勿外传）
- `start.bat` / `stop.bat` / `db-init.sql` — 运维脚本（db-init.sql 已含 boonie_attraction / boonie_review 建表；种子数据由 DataInitializer 在表空时写入）
- `log/xx.log` — 运行日志（排错先看这里）

## 给未来 AI 的提醒
- 不要花时间分析项目结构、读源码细节 —— 本文件已覆盖全部要点
- 用户要求：脚本用 GBK 编码、项目内路径用相对路径、脚本不能依赖 target/node_modules 已存在
- 启动整套（前端+后端）：双击 `../boonie-auth-pro/start.bat`（它会连带启动本项目）
- 如果 mvnw 报错说找不到 Java → 电脑重置删了 JDK，需先装 JDK 17+
