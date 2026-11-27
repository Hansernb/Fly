# AI-CONTEXT.md — 给 AI 助手的快速上手文件（读完即可开工，无需再探索项目）

> 电脑每次开机都会重置：本项目 `node_modules` 会被删除；后端项目 `../springBootTest` 的 `target` 也会被删除。
> **下次会话请先读本文件，不要重新分析项目结构。**

## 项目是什么
「熊出没 · 森林登录注册 Pro」：Vue 3 + Vite 前端，后端已切换为 **Java Spring Boot**（`../springBootTest`）。
- 前端：Vite 开发服务器，端口 **5173**，`vite.config.js` 里 `open: true` 会自动开浏览器
- 后端：`../springBootTest`（Spring Boot 3.4.5 + MyBatis-Plus + MySQL），端口 **8080**，详见 `../springBootTest/AI-CONTEXT.md`
- 前端所有请求走相对路径 `/api/*`，由 Vite 代理转发到 `http://192.168.68.53:${backendPort}`（2026-11-23 全项目 IP 统一；原为 `localhost` / `192.168.68.47`）。`server.host: true` 绑 0.0.0.0，局域网 `http://192.168.68.53:5173` 可访问。**改 IP 前先确认本机持有该地址**，否则后端 `server.address` 绑定会失败
  - **代理端口不是写死的**：`vite.config.js` 启动时扫描同级含 `pom.xml` 的后端目录，读其 `application.properties` 的 `spring.profiles.active`，再读对应 `application-{profile}.properties` 的 `server.port`（回退 8080）。后端改端口，前端代理自动跟随
- `server/index.js` 是旧的 Express 假后端，**已弃用**（不再启动），仅作参考保留

## 一键脚本（根目录，GBK 编码，双击即可）
- `start.bat`：检查 node_modules → 缺失则自动 `npm ci`（国内镜像 npmmirror，失败降级 npm install）→ 清理 `vite.config.js.timestamp-*.mjs` 垃圾文件 → **运行时自动探测**（前端端口读 `vite.config.js` 的 `server.port`；后端目录扫描同级含 `mvnw.cmd+pom.xml` 的文件夹，找不到回退 `springBootTest`；后端端口读其**激活 profile**（`application-{profile}.properties`）的 `server.port`，不再遍历全部 profile 文件）→ 启动 Java 后端 → 启动前端 Vite（`--port` 显式传端口）。项目名/窗口标题由目录名推出，改文件夹名或端口都不用改脚本。
- `stop.bat`：按窗口标题（标题随项目名动态生成，如 `boonie-auth-pro Web` / `springBootTest API`）+ 按端口（`findstr /C:":端口 "` 动态探测，不会误杀其它端口）双重方式杀掉所有相关进程
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
4. 测试账号：**用户名登录** —— 熊大（用户名 `熊大`，邮箱 `admin@boonie.com`）/ 密码 `bear123456`

## 权限与后台（重要）
- 登录/注册返回的 `user.permission`：**1 = 默认权限**（注册用户），**0 = 管理员**
- `ParkHome.vue` 导航栏的「⚙️ 后台管理系统」按钮用 `v-if="isAdmin"`（`user.permission === 0`）控制，普通用户根本看不到
- 登录方式：**用户名 + 密码**（`AuthCard.vue` 密码登录时显示用户名框；验证码登录仍用邮箱，因为验证码发到邮箱）
- 点按钮 → `App.vue` 切到 `AdminLogin.vue`（**只有登录、没有注册**，用户名 + 密码）→ 调 `POST /api/admin/login`，body `{ username, password }`
  - 非管理员账号登录会被后端 403 拒绝，页面提示"权限不足"
- 登录成功 → `AdminPanel.vue`：项目管理 / 评价管理 / 用户与权限 三个 Tab
- 后端密码为明文存储（详见后端 AI-CONTEXT 的「权限模型」）

## 文件地图
- `src/main.js` — 入口：`createApp` → `use(pinia)` → `use(router)` → `sessionStore.restore()` 恢复登录态
- `src/router/index.js` — **vue-router 路由表（2026-11-23 新增）**：`/`、`/admin/login`、`/admin`、`/attractions`、`/map`、`/tickets`、`/games`、`/member`、`/profile`、404；`beforeEach` 全局守卫生效（`/admin` 无令牌→登录页；`/admin/login` 已是管理员→后台、普通权限→首页+提示）
- `src/stores/session.js` — **Pinia 会话 store（2026-11-23 新增）**：`session`（前台）/`adminSession`（后台）/`me`/`toast`；localStorage key 沿用 `boonie_token`/`boonie_admin_token`/`boonie_user`（老登录态无缝兼容）
- `src/utils/request.js` — **统一请求封装（2026-11-23 新增）**：baseURL `/api`、自动注入 Bearer token、JSON/FormData 分流、非 2xx 抛带 `status` 的 Error
- `src/api/content.js` — **内容类接口（2026-11-23 新增）**：attractions/reviews/banners/notices/tickets/habitats/shows/me/avatar
- `src/App.vue` — 应用壳：`view` 来自 `route.name`，模板仍是原来的 v-if 链（**UI 零变化**）；导航统一走 `router.push`
- `src/components/AdminLogin.vue` — 后台登录页（**只有登录、没有注册**），调 `POST /api/admin/login`
- `src/components/AdminPanel.vue` — 后台管理系统：三个 Tab = 游乐项目管理（增删改查）/ 游客评价管理（查看、删除）/ 用户与权限（切换 0|1、重置密码）；请求均带 `Authorization: Bearer <token>`
- `src/components/ParkHome.vue` — **已改为从后端 API 取数**：`onMounted` 调 `GET /api/attractions`（带本地 attractions.js 兜底）；游乐项目卡片可点击打开「项目详情」弹窗（含该项目评价 + 发表评价，POST /api/reviews）；底部游客评价表单同样写库。后端不可达时自动降级为示例数据并显示提示条。
- `server/index.js` — 旧 Express 后端（已弃用，勿启动，会和 Java 后端抢 8080）
- `src/components/MemberCenter.vue` — 会员中心页（路由 `/member`）：手机号查会员、会员卡（等级配色 lv-normal/silver/gold、头像 emoji、昵称编辑、升级进度条）、积分规则卡、积分明细列表；后端 404/断网时降级本地（localStorage `boonie_member_v1`）
- `src/data/member.js` — 积分规则纯函数（POINT_RULES/LEVELS、levelOf/maxDeductPoints/discountOf/earnedOf/decorate）+ 本地兜底（loadLocalMember/localAddPoints）、`MEMBER_PHONE_KEY='boonie_member_phone'` 记住上次手机号
- `src/components/ForestGames.vue` — 小游戏中心（4 款：接蜂蜜/打地鼠/配对/森林跑酷 run）；成绩走 `/api/games/scores`，未登录降级 localStorage
- `data/users.json` — 旧 Express 的用户数据（已弃用，真正数据在 MySQL boonie_auth.boonie_user）
- `vite.config.js` — 端口 5173、自动开浏览器、代理 `/api` → **动态解析的后端端口**（读后端激活 profile 的 `server.port`，默认 8080）
- `start.bat` / `stop.bat` — 启动/停止脚本
- `vite.config.js.timestamp-*.mjs` — vite 崩溃残留垃圾，start.bat 会自动清理，可无视

## 给未来 AI 的提醒
- 不要花时间分析项目结构、读源码细节 —— 本文件已覆盖全部要点
- 用户要求：脚本用 GBK 编码、路径用相对路径、脚本不能依赖 node_modules/target 已存在
- 沙箱执行启动命令时环境变量 SERVER_PORT 会被劫持导致端口错乱，务必显式指定 8080（详见后端 AI-CONTEXT 的踩坑记录）
- 如果 start.bat 报 npm 不存在 → 电脑重置把 Node 也删了，需先装 Node 22+

## 会员积分体系（前端侧，2026-11 新增）
- 路由：本项目用 App.vue 自建 History 路由（view 状态 + ROUTES 映射，非 vue-router），`/member` → MemberCenter.vue；ParkHome 导航和抽屉、TicketBooking 成功页均有「💎 会员中心/我的会员卡」入口（emit 'member'）
- **下单即会员**：TicketBooking 步骤 4 填手机号后（防抖）查 `GET /api/member/preview?phone=&amount=` 显示积分抵扣面板（滑杆/快捷档/实时折后价）；提交时后端 `POST /api/orders` 自动开通会员并计积分，返回 `isNewMember/pointsEarned`，成功页显示会员卡弹卡
- **重要**：submit 已移除「未登录拦截」——游客手机号可直接 POST 后端下单（后端支持游客订单），后端不可达才降级 localStorage（createLocalOrder 里做本地积分记账）
- 积分规则纯函数在 `src/data/member.js`，与后端 MemberService 常量保持一致（1 元=1 分、1 分=0.3 元、单笔最多抵 50%、500 白银/2000 黄金）；改规则要前后端同步
- ⚠️ reveal 渐显陷阱：`.reveal { opacity: 0 }` 依赖 ParkHome 的 IntersectionObserver 加 `.is-visible`，独立路由组件（如 TicketBooking）必须在 onMounted 里自己补，否则卡片永久透明
- `vite.config.js` 已加 `server.host: true`（默认只绑 IPv6 `[::1]`，导致 start.bat 启动后 IPv4 无法访问）

## 后台数据看板 + 轮播/公告/评价审核/用户管理（2026-11-23 新增）
- `AdminPanel.vue` 菜单第一项改为「📊 数据看板」：`GET /api/admin/dashboard`，4 张 KPI 卡（访客数/订单量/门票销量/好评率）+ 近 7 天折线图（访客&订单）+ 门票销量柱状图 + 好评率环形图 + 票种销量分布条；**图表全部是手写 SVG**，无第三方图表依赖，计算函数在 `kpis / trendMax / pointList / linePoints / bars / goodDash / salesWidth`
- 轮播图管理（key `banner`，api `/api/admin/banners`）：图片字段是 `type: 'image'`，可填 `/img/...` 或走 `uploadAvatar()` 上传到 `/api/avatar/upload`（复用头像上传，≤2MB）；上下架开关对 banner/notice **会写库**（其它模块仍是本地 overlay）
- 公告管理（key `notice`，api `/api/admin/notices`）、评价审核（key `reviews`，api `/api/admin/reviews`，无「新增」按钮 `noCreate`，操作列是自写的「通过/驳回/删除」）
- 用户列表管理（key `settings`，api `/api/admin/users`）：新增需填账号/邮箱/密码，编辑时密码留空则 `delete payload.password`（后端 PUT 不修改密码）
- 模块通用配置新增：`noDrag` / `noSwitch` / `noCreate` 控制表格列与按钮显隐；`display()` 里 status / password 做了中文与掩码处理
- 前台 `ParkHome.vue`：`bannerSlides` 改为 ref，新增 `loadBanners()` / `loadNotices()` 从 `/api/banners`、`/api/notices` 读取，**失败降级内置常量** `FALLBACK_BANNERS` / `FALLBACK_NOTICES`；轮播定时器改为取 `bannerSlides.value.length`，<2 张不启动
- ⚠️ 评价改审核制：`POST /api/reviews` 后前台不再立即 unshift 到列表，两处提交（底部表单 + 项目详情弹窗）提示都改为「评价已提交，审核通过后展示」；后台通过后才会出现在 `/api/attractions`

## 架构迁移：引入 vue-router + Pinia（2026-11-23，参考开源仓库）

**背景**：项目此前实际**没有** vue-router / pinia（`App.vue` 用 `ref('view')` + 手写 `ROUTES`/`guard`/`popstate`），与「Vue3 + Vite + Pinia + Vue Router」的技术栈描述不符。

**参考仓库可借鉴的分层**
| 来源 | 借鉴点 | 状态 |
|---|---|---|
| `huzhushan/vue3-element-admin` | `router/` + `permission.js` 全局守卫、`pinia/` 会话模块化 | ✅ 已完成 |
| 同上 | `api/` 按模块拆 + `utils/request` 统一封装 | ⬜ 待做 |
| 同上 | `layout/` 后台布局外壳、`hooks/` 复用逻辑 | ⬜ 待做 |
| `geeeeeeeek/java_travel` | `VisitData` 访问统计表（看板 visitors 目前是拿用户数凑的） | ⬜ 待做 |
| 同上 | `Classification` / `Tag` 分类标签表（首页标签目前写死） | ⬜ 待做 |
| 同上 | `OpLog` 操作日志、`ThingCollect` 收藏 | 可选 |

**已完成（第 1 块，UI 零变化）**
- 新增 `src/router/index.js`：9 条业务路由 + 404，路径与旧手写路由**完全一致**（旧链接不失效）；`beforeEach` 承接原 `guard()` 逻辑；`afterEach` 设置 `document.title`
- 新增 `src/stores/session.js`：`session`/`adminSession`/`me`/`toast` 收进 Pinia；**localStorage key 不变**，老登录态无缝；顺手修复原「刷新 /admin 时 adminSession 为 null 导致 `adminSession.user` 空指针」的缺陷
- `main.js`：`use(pinia)` → `use(router)` → `restore()`；`App.vue`：`view = computed(() => route.name)`，模板 v-if 链与样式原样保留
- ⚠️ 路由组件目前是**同步 import**（因为渲染仍由 App.vue 的 v-if 链负责，组件已被静态引入，用懒加载只会产生分包警告）。等第二步把 App.vue 改成真正的 `<router-view>` 架构后，再换回 `() => import()` 以获得分包收益
- 验证：`vite build` 零警告（73 modules）；`npm run dev` 下 9 条路由全部 200；Chrome headless 实测首页/项目页渲染正常、直接访问 `/admin` 被正确重定向到后台登录页

**已完成（第 2 块：请求层 + API 层，UI 零变化）**
- 新增 `src/utils/request.js`：统一 `baseURL='/api'`、自动注入 `Authorization: Bearer <token>`、自动 JSON/FormData 分流、非 2xx 抛 `Error` 并带 `status`（原实现各处手写 `res.ok` 判断）
- 新增 `src/api/content.js`：`getAttractions / postReview / getBanners / getNotices / getTickets / getHabitats / getShows / getMe / uploadAvatar / updateMyAvatar`
- `ParkHome.vue` 5 处 `fetch` 收敛为 API 调用（attractions / banners / notices / reviews / me）。**顺带修严了 `/api/me` 的失败处理**：原来任何非 2xx（含 500）都会登出，现在只有 401/403 才登出，后端 500 或不可达时保持页面展示
- ⚠️ **头像上传（`/api/avatar/upload`、`/api/me/avatar`）暂保留原 fetch**：这两处有精细的 413（>2MB）分支提示和 localStorage 同步逻辑，迁移动收益低、回归风险高，留待后续统一
- 验证：`vite build` 零警告（75 modules）；curl 实测 `/api/banners|notices|attractions` 均返回 `{ok:true,data:[...]}`、`/api/me` 无 token 返回 401，与封装契约完全一致；**用 CDP 走真实登录流程（熊二/bear123456）实测 ParkHome 完整渲染**（`#app` innerHTML 从 4645 → 110544，公告条显示的是数据库内容，证明 `getNotices()` 生效）

> 踩坑：`chrome --headless` 的 localStorage **无法跨 reload 持久**（实测写入成功、reload 后为 null，iframe 内也是独立空存储），因此验证登录态页面必须走「真实登录表单 + 同页渲染」，不能靠预写 localStorage 再刷新。

**下一步优先级建议**
1. `ParkHome.vue` **5036 行**（模板 878 + 脚本 966 + **样式 3188**）是最大技术债。拆分顺序应为：**先建 `stores/content.js` 承接 attractions/banners/notices 状态**，再把 13 个 `<section>`（搜索/hero/eco-bar/热门/搜索结果/项目/门票/游戏/风景/评价/data-cards/地图/详情弹窗）逐个抽到 `views/home/*`，每抽一块把对应 scoped 样式一起搬走
2. ~~App.vue 换成真正的 `<router-view>` + 懒加载分包~~ ✅ 已完成（2026-11-27，见下方「路由架构升级」）
3. ~~后端补 `boonie_visit` 表 + 埋点接口~~ ✅ 已完成（2026-11-27，见下方「参考仓库借鉴」）
4. ~~把首页写死的分类标签迁到 `boonie_category` 表~~ ✅ 已完成（2026-11-27）

## 参考仓库借鉴改造（2026-11-27，方案见根目录 `MIGRATION_PLAN.md`）
对照 `geeeeeeeek/java_travel` 与 `huzhushan/vue3-element-admin`，只复用结构/表设计/通用组件，熊出没主题配色与页面零变化。分 3 块完成并逐块验证：

- **第 1 块 分类字典**：后端新增 `boonie_category` 表（7 条种子：全部/刺激/亲子/观光/浪漫/清凉/益智）+ `Category` 实体/Mapper + `GET /api/categories`（公开，仅启用按 sort_no）+ `/api/admin/categories` CRUD；前端 `api/content.js` 加 `getCategories`、`stores/content.js` 加 `categories` 状态与 `categoryNames` getter（兜底 `FALLBACK_CATEGORIES`）、`AttractionList.vue` 筛选按钮改读 store（onMounted 加载）、`AdminPanel.vue` 加「🏷️ 项目分类」模块。注：项目编辑的 tag 下拉仍是静态数组（改造风险低收益小，保留）
- **第 2 块 访问统计**：后端新增 `boonie_visit` 表 + `Visit` 实体/Mapper + `VisitController`（`POST /api/visit` 记录 ip/date/page/ua，失败静默）；`AdminController.dashboard()` 的 `visitors` 改为 `max(埋点去重IP, 旧口径注册用户+游客手机号)`，`summary` 新增 `pv`，`trend` 每天新增 `pv`/真实 UV——取大值兼容埋点上线前历史数据。前端 `main.js` 用 sessionStorage(`boonie_visit_reported`) 每会话上报一次 `reportVisit()`，fire-and-forget
- **第 3 块 401 统一拦截**：`utils/request.js` 加 `setUnauthorizedHandler(fn)` 全局处理器 + `skipUnauthorized` 选项；`main.js` 注册 handler（清会话+提示「登录已过期」）；`getMe()` 声明 `skipUnauthorized`（匿名探测不触发），ParkHome 既有 401 处理保留。默认无 handler 时行为完全不变，兼容所有裸 fetch 调用
- 验证：`npm run build` 三块均通过；重启 8080 后 `/api/categories` 返回 7 条、后台接口无 token 401、`POST /api/visit` 写库 PV/UV=1、看板 `pv=1`；无头 Chrome 实测 `/`、`/attractions`、`/tickets` DOM 230KB+ 且零 JS 报错

## 路由架构升级：App.vue → `<router-view>`（2026-11-27，参考 vue3-element-admin 的 router 分层）
- **方案**：原 App.vue 的 10 路 `v-if` 链改为真正的 `<router-view>`；为保 UI 零变化，新增 `src/views/` 薄适配层（9 个 `*View.vue` + `useNav.js`），**原 17 个业务组件零改动**，由适配层承接原 App.vue 的 props 传递与 emit→导航映射
- `src/views/useNav.js`：共享导航 composable，逐字复刻原 App.vue 的 `go`/`goHome`（history.state.back 优先后退）/`enterTickets`（带 ticketId）/`openAttractionDetail`（push('/')+CustomEvent）/`enterAdminLogin`/`onAdminLogin`/`exitAdmin`
- 各 view：`HomeView`（未登录→ForestPanel+AuthCard，已登录→ParkHome，含原 auth-page 样式）、`AdminView`/`AdminLoginView`/`AttractionsView`/`MapView`/`TicketsView`（从 `route.query.ticketId` 读预选票种）/``GamesView`/`MemberView`/`ProfileView`/`NotFoundView`
- `router/index.js`：`component` 全改为 `() => import('../views/XxxView.vue')` 懒加载；守卫（beforeEach/afterEach）与 9 条路径原样保留
- `App.vue` 精简为：背景 `.bg-scene` + toast + `<router-view />` + onMounted 恢复登录态兜底
- **收益**：主包从 309KB → **118KB**（gzip 46KB），10 个 view 各自分包按需加载
- 验证：`npm run build` 零警告；无头 Chrome 逐路由实测 9 条路径全部命中各自特有文案、零 JS 报错；未登录访问 `/admin` 仍被守卫重定向到后台登录页
- ⚠️ 改 view 适配层时注意：`ParkHome` 的 `emit('tickets', id)` 经 HomeView 转为 `query.ticketId`；`PersonalCenter` 的 `emit('attraction', id)` 经 ProfileView 转首页弹窗事件；`PersonalCenter` 的 `emit('member')` 经 ProfileView 转 `/member`（订单/积分真实入口）

## 购票与会员中心完善（2026-11-27，参考 java_travel 订单/用户中心）
- **现状核对结论**：购票 4 步流程（`TicketBooking.vue`：选票种→日期→数量→确认→生成订单）、下单自动开会员、积分规则（1元=1分、1分抵¥0.3、最多抵50%）、数据表（`boonie_ticket_order`/`boonie_member`/`boonie_point_log`）此前**均已完成**，对标 java_travel 的 `b_order`/`b_user` 字段齐全，本轮未改后端与表结构
- **本轮唯一缺口**：真实订单列表没进会员中心（个人中心 `PersonalCenter.vue` 的订单/积分读的是 localStorage 假数据 `boonie_ticket_orders_v1`）
- `api/content.js` 新增 `getOrders(token)` / `cancelOrder(orderNo, token)`（统一 request 层）
- `MemberCenter.vue` 新增「🧾 我的订单」区块（登录态按账号拉 `GET /api/orders`）：订单号/状态徽章/票种/日期/数量/取票人/实付（原价划线）/积分抵扣与获得标签/取消按钮；取消调后端含积分回滚，成功后 `Promise.all([lookup(), loadOrders()])` 刷新会员卡与流水；新增 `.mc-order*` 系列样式
- `PersonalCenter.vue` 的订单/积分 tab 改为引导卡片（`emit('member')` 跳会员中心），消除假数据矛盾；数据概览与徽章仍用本地统计（不影响）
- 验证：`npm run build` 通过；后端实测 798分→申请800→实际用665（50%上限）→抵¥199.5→实付¥199.5得199分→余额332→取消回滚0，积分明细含获取/抵扣/退款/扣回；CDP 注入 token 实测 `/member` 登录态渲染出 3 笔真实订单 + 8 条积分流水，零 JS 报错

## 中后台布局改造（2026-11-27，参考 vue3-element-admin layout + ProTable）
- **新建独立 `src/admin/` 体系，旧 `components/AdminPanel.vue`（1787 行）保留未删但路由已不指向它**；`/admin` → `admin/layout/AdminLayout.vue`（嵌套路由），`/admin/:module` → `admin/views/CrudView.vue`（7 模块共用），`/admin` 默认重定向到 `projects`
- 目录：`admin/config/modules.js`（7 模块的 columns/fields/seed 配置）、`admin/api/mock.js`（Mock CRUD）、`admin/components/{ProTable,FormDialog,Pagination}.vue`、`admin/layout/AdminLayout.vue`、`admin/views/CrudView.vue`
- **7 个菜单 key**：projects 项目管理 / shows 演出管理 / tickets 门票管理 / orders 订单管理 / reviews 评价管理 / users 用户管理 / banners 轮播图管理。新增模块 = 在 `modules.js` 加一段配置 + seed
- `ProTable`：columns 配置（key/label/width/type/format）、type=status/statusBool/color/switch 内置渲染、`#cell-xxx` 槽位覆盖、多选、`#actions` 操作槽位
- `FormDialog`：fields schema（type=text/textarea/number/date/color/select/switch）、required 校验、options/optionLabels
- `Pagination`：v-model:page/pageSize + change 事件，页码省略号
- `mock.js`：`listModule/createRow/updateRow/deleteRow/toggleRow/resetModule`，localStorage key `boonie_admin_mock_<模块>`，220ms 延迟。**接 Springboot 时只改 mock.js 函数体为 request() 即可，页面零改动**
- 守卫不变：`/admin` 无 `boonie_admin_token` → `/admin/login`；退出登录 `store.exitAdmin()` 清 token 跳登录页
- ⚠️ 未引入 Element Plus / i18n / 工作流 / 富文本（沿用项目原生 Vue + 熊出没 CSS 变量）
- 验证：`npm run build` 通过；CDP 注入管理员 token 实测 7 菜单标题/面包屑/高亮/表格数据全对、零 JS 报错；新增（5→6 行 + 成功提示）、搜索命中、退出清 token 均通过

## 熊大快跑（独立 Canvas 跑酷组件，2026-11-27 新增）
- `src/components/BearRunner.vue`：单文件原生 Canvas 跑酷，逻辑抽自 GitHub Topic `endless-runner`（CursedPrograms/endless-runner-html、amoldalwai/Chrome_Dino_Game）——rAF 循环 + 重力跳跃（空格/↑/点击，GRAVITY 0.72 / JUMP_V 12.6）+ 障碍数组 + AABB 碰撞 + 计分加速 + localStorage 最高分
- 玩法：熊大固定 x=118 自动跑，跳过树桩（障碍后 7 成概率放蜂蜜罐），蜂蜜 +10 分，每躲一个障碍 +1 分并加速（5→11），撞树桩结束；最高双写 `boonie_bear_runner_best_v1` 与统一 `boonie_game_best_v1.bearRunner`
- 配色：暖绿 + 暖黄（天空 #fff1c9→#dff0c4、草地 #7cc65b、围巾 #2f7d4f、蜂蜜 #ffb703），沿用主题变量
- `ForestGames.vue` 接入为第 6 款游戏（id `bearRunner`）：大厅卡片 + 舞台 `<BearRunner>` + HUD/最佳分同步 + 登录后成绩上报；原有 5 款游戏逻辑零改动
- ⚠️ 关键坑：障碍生成倒计时必须按 `dt` 时间步进（`spawnIn -= dt`），不能按帧 `--spawnIn`，否则高刷新率下障碍爆发式生成
- 验证：`npm run build` 通过；CDP 智能按障碍距离起跳实测单局 48 分（8 障碍 + 4 蜂蜜）、新纪录提示、双 localStorage 写入、HUD 三处同步，零 JS 报错
