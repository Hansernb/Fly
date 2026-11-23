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
5. **局域网 IP 统一为 `192.168.68.33`（2026-11-23 全项目替换，原 `192.168.68.47`）**：目前 IP 只出现在 **2 个必要位置**——`application-dev.properties` 的 `server.address`、前端 `vite.config.js` 的 `/api` 代理目标（另有已弃用的 `server/index.js` 监听地址同步改了）。**8 个 Controller 上写死来源的 `@CrossOrigin(origins=...)` 已全部删除**，CORS 改由 `AppConfig` 全局单点管理（见下方 CORS 条），换 IP 不再需要改任何 Java 文件。**改 IP 前先用 `ipconfig` 确认本机确实持有该地址**——`server.address` 绑定一个本机没有的 IP 会导致 Tomcat 启动直接失败（"Cannot assign requested address"）。本机当前 IPv4 为 `192.168.68.33`（另有 192.168.55.1 / 192.168.137.1 / 172.26.48.1 虚拟网卡）。
6. **AI 沙箱会注入 `SERVER__PORT`（双下划线）等环境变量**：用 `mvnw spring-boot:run` 裸启动会被松散绑定劫持到随机端口（如 59677）。`start.bat` 已用显式 `--server.port=%BE_PORT%` 覆盖；AI 命令行验证时须先 `Remove-Item Env:SERVER__PORT` 并同样传 `--server.port=8080`。

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
- `src/main/java/com/fenglin/springboottest/` — 主类 / AuthController / UserController / AttractionController / TicketOrderController / MemberController / GameScoreController / service(AuthService、AttractionService、MemberService + impl 子包) / mapper(MemberMapper、PointLogMapper 等) / entity(User→boonie_user、Attraction→boonie_attraction、Review→boonie_review、Member→boonie_member、PointLog→boonie_point_log、TicketOrder→boonie_ticket_order) / common(DataInitializer、GlobalExceptionHandler、BizException、AvatarStorage) / config(AppConfig 全局 CORS)。**没有 SchemaInitializer，建表只靠 db-init.sql**
- `src/main/resources/application-dev.properties` — 端口 8080、MySQL 连接（本文件含数据库密码，勿外传）
- `start.bat` / `stop.bat` / `db-init.sql` — 运维脚本（db-init.sql 已含 boonie_attraction / boonie_review 建表；种子数据由 DataInitializer 在表空时写入）
- `log/xx.log` — 运行日志（排错先看这里）

## 给未来 AI 的提醒
- 不要花时间分析项目结构、读源码细节 —— 本文件已覆盖全部要点
- 用户要求：脚本用 GBK 编码、项目内路径用相对路径、脚本不能依赖 target/node_modules 已存在
- 启动整套（前端+后端）：双击 `../boonie-auth-pro/start.bat`（它会连带启动本项目）
- 如果 mvnw 报错说找不到 Java → 电脑重置删了 JDK，需先装 JDK 17+

## 门票预订 / 小游戏成绩接口（2026-11 新增，已落库）
- 表：`boonie_ticket`（扩容 type_code/original_price/discount_label/tag_label/desc_text/color/sort_no/enabled）、`boonie_ticket_order`、`boonie_game_score`，建表与 5 种票种子都在 `db-init.sql`（幂等，可重复执行）
- `GET /api/tickets` — 上架门票，按 sort_no 排序；`id` 返回票种编码（adult/child/senior/family/night），另带 `dbId`、`originalPrice`、`discount`、`tag`、`desc`、`color`、`features[]`
- `POST /api/orders` — 下单 `{ ticketId, quantity, visitDate, buyerName, buyerPhone }`；**价格由后端按票库重算**，前端传的价格不采信；ticketId 支持编码或数据库 id
- `GET /api/orders` — 我的订单（按令牌用户，倒序）
- `POST /api/orders/{orderNo}/cancel` — 取消订单（仅本人 + 待使用）
- `GET /api/games/scores` / `POST /api/games/scores` — 我的成绩 / 上报一局 `{ gameId: honey|mole|match, score }`；honey/mole 取最大，match（步数）取最小，plays 每局 +1
- `GET /api/games/leaderboard` — 三款游戏 Top10（无需登录）
- 前端对应：`src/data/tickets.js` 的 `TICKETS` 是响应式数组 + `loadTickets()`（接口失败自动降级内置票库）；`TicketBooking.vue` 订单走 `/api/orders`，未登录/断网降级 localStorage；`ForestGames.vue` 成绩走 `/api/games/scores`
- ⚠️ 改了后端 Java 需重启 8080 进程（`stop.bat` + `start.bat`）新接口才生效

## 评论头像按名字生成 + 项目风景图（2026-11-11）
- **评论头像**：`POST /api/reviews` 未传/传无效 avatar 时，不再统一回退 🐻，改为按「评价人名字」确定性生成 emoji（`common/NameAvatar`，24 个森林/动物 emoji 池）——同名永远同头像，且避让库里已占用的纯 emoji 头像，做到**异名不同头像**。用户上传头像（/api/avatar/…）与 /img/ 路径不受影响
- 存量回填：`DataInitializer.normalizeReviewAvatars()` 每次启动幂等执行——只重写「纯 emoji 头像」，路径头像不动（`NameAvatar.assignUnique`，按名字排序稳定分配保证互不相同）
- **项目风景图**：`boonie_attraction` 新增 `img` 列（db-init.sql 幂等迁移 + 按项目名回填 6 张图）；`GET /api/attractions` 返回 `img` 字段，库里缺图时接口层按项目 id 从 `FALLBACK_IMGS` 兜底，前端 `img: a.img || FALLBACK_IMG` 逻辑不用改
- 后台新增项目不填 img 也能正常显示（走接口层兜底），想换图直接 UPDATE `boonie_attraction`.img

## 首页搜索改为「只显示搜索结果」（2026-11-11，前端 ParkHome.vue）
- 搜索框有内容即进入搜索态（`searching = !!query.trim()`，输入即时生效，无需点按钮）
- 搜索态下隐藏：轮播 banner / hero / 生态条 / 热门项目 / 栖息地(HabitatCards) / 演出(ShowSchedule) / 门票 / 小游戏 / 风景 / 游客评价 / 林缘树线 / 数据卡片 / 园区地图（共 13 处 `v-if="!searching"`）
- 改为只显示 `#search-results` 区：按类型分组「游乐项目（复用 ride 卡片，可点开详情）/ 栖息地 / 演出 / 门票」，带「共 N 条」计数与空结果提示；点栖息地/演出/门票会先退出搜索再滚到对应板块
- 菜单点击 `goSection` 会先清空 query，避免滚到隐藏板块；`clearSearch` 清空后回到首页全量视图

## 后台管理扩展：全量 CRUD + 小游戏成绩管理（2026-11-11）
- `AdminController` 新增四组接口（全部管理员鉴权）：`/api/admin/habitats`、`/api/admin/shows`、`/api/admin/tickets`、`/api/admin/games`，均支持 GET 列表 / POST 新增 / PUT {id} 修改 / DELETE {id} 删除，直接读写对应表
- 门票表注意事项：`type_code` 需 3–20 位小写字母（订票按编码找票）；`features` 按换行分行存储；删除票种不影响历史订单（订单冗余了 ticket_name）
- 小游戏成绩（boonie_game_score）：GET 支持 `?gameId=honey&q=玩家名` 过滤、按 bestScore 倒序；PUT 可改 bestScore/plays/gameName（清作弊/改榜单）；POST 手工补录（user_id 为 NULL）——表已迁移 user_id 可空（uk_user_game 唯一索引允许多个 NULL）
- 前端 AdminPanel.vue：SECTIONS 里演出改为接库（字段 showTime/tag），新增「栖息地管理 / 门票管理 / 小游戏成绩管理」三个模块；表格 UI 是通用的（新增/编辑/删除/上下架/拖拽全复用）；注意「上下架」开关目前仍是前端本地覆盖，未写库

## 会员积分体系（2026-11 新增，已落库）
- **会员身份 = 登录账号**（2026-11-11 改）：`boonie_member.user_id` 绑定 `boonie_user.id`（唯一），哪个账号买票积分就加到该账号的卡上，**与取票人手机号无关**；`phone` 改为可空，仅作展示与游客兜底（账号会员记录最近一次下单手机号）
- 表：`boonie_member`（user_id/phone/nickname/avatar/total_points/cum_points/level_name，user_id 唯一、phone 唯一且可空）、`boonie_point_log`（member_id/log_type/points/balance/order_no/remark）；`boonie_ticket_order` 含 5 列：member_id/gross_amount/points_used/points_discount/points_earned。建表全靠 `start.bat` 每次启动幂等执行 `db-init.sql`（**项目里没有 SchemaInitializer**，别假设有启动自动迁移）
- 规则：**登录账号下单即自动开通会员**——`getOrCreateForUser(userId, phone, name)`：先按 user_id 找 → 再尝试把同手机号的游客档案归并到该账号 → 都没有才新建；未登录游客仍按手机号建档（`getOrCreate`），但**若该手机号已绑定账号则拒绝下单**（400「该手机号已绑定会员账号，请先登录再下单」，防止冒用别人手机号花积分）。1 元 = 1 积分（FLOOR 取整，按实付金额计）；积分抵扣 1 积分 = 0.3 元，单笔最多抵 50%：`maxDeductible = min(余额, floor(金额×0.5÷0.3))`；实付最低 0.01 元；等级按**累计积分 cum_points**：≥2000 黄金 / ≥500 白银 / 其余普通
- 累计积分口径（2026-11-11 修正）：只有「获取」增加累计，「抵扣/退款」不动累计，「扣回」时累计一并扣回 → 取消订单后等级会回落，避免「下单再取消」刷等级
- 代码结构：`service/MemberService.java` 是接口，实现在 `service/impl/MemberServiceImpl.java`（YUAN_PER_POINT=0.3、MAX_RATIO=0.5、SILVER_AT=500、GOLD_AT=2000）
- 接口（三个都支持 `Authorization: Bearer`；**带令牌一律按账号查，忽略 phone**）：
  - `GET /api/member?phone=` — 会员信息 + 积分明细 `{ member, logs }`（不存在返回 404，前端降级本地；登录态返回 `userId`）
  - `GET /api/member/preview?phone=&amount=` — 抵扣预览 `{ member, maxPoints, maxDiscount }`（member 为 null = 未开通；前端读 `data.member`；登录态手机号可空）
  - `POST /api/member/profile` — 改昵称/头像 `{ phone, nickname, avatar }`（登录态按账号会员改）
- 前端渲染（ParkHome.vue）：评价头像 `avatarIsImg/emoji`——有图片路径或命中 AVATAR_IMG 映射就用 `<img>`，否则**直接渲染 emoji**（旧逻辑未命中的 emoji 全部落到同一张默认图，是「头像都一样」的根因，已修）
- 下单流程（TicketOrderController）：登录态 isNewMember=findByUser 为空 → getOrCreateForUser；游客态按手机号 → maxUse=maxDeductible → usePoints=min(前端传入, maxUse) → paid=gross−discount → earned=floor(paid) → 先插订单后 `memberService.record` 写两条流水（抵扣 −usePoints / 获取 +earned）；返回多带 `member / pointsEarned / isNewMember / message`（前端弹「已自动开通会员，获得 XX 积分」）
- 取消订单：`cancel` 调 `memberService.rollback`（抵扣积分全额退还只回余额；赚取积分扣回，余额与 cum_points 一起回退）
- ⚠️ **会员模块文件曾整体丢失**（2026-11-11 编译报 `找不到符号 Member / MemberService`）：若再次出现，检查 `entity/Member`、`entity/PointLog`、`mapper/MemberMapper`、`mapper/PointLogMapper`、`service/(impl/)MemberService*`、`controller/MemberController` 是否缺失，别去改 TicketOrderController
- 游戏成绩 `GAME_NAMES` 白名单已含 `run`（森林跑酷）
- **CORS（重要，2026-11-23 已收敛为单点）**：浏览器对同源 POST fetch 也会带 Origin 头，旧的 `@CrossOrigin(origins="写死某IP:5173")` 在前端换端口/换主机（localhost、127.0.0.1、局域网 IP）时报 403 "Invalid CORS request"（curl 不带 Origin 测不出）。现由 `config/AppConfig.java` 全局 `addMapping("/api/**").allowedOriginPatterns("*")` **统一单点管理，8 个 Controller 上冗余的 `@CrossOrigin` 注解已全部删除**。实测 localhost:5173 / 127.0.0.1:5173 / 192.168.68.33:5173 / localhost:5174 预检全部 200。**勿再给 Controller 加写死来源的 @CrossOrigin**（会覆盖全局配置、且换 IP 又要改 8 处 Java 文件）

## 后台数据看板 + 轮播/公告/评价审核/用户 CRUD（2026-11-23 新增）
- `db-init.sql` 幂等新增：`boonie_banner`（轮播图）、`boonie_notice`（公告）；`boonie_review` 加 `status` 列（pending/approved/rejected，存量默认 approved）；两张表各有 4 条 / 3 条种子。**必须跑 db-init.sql（start.bat 每次启动会自动跑）新表才存在**，否则 `/api/banners`、`/api/notices`、看板会 500
- 实体/Mapper：`entity/Banner.java`、`entity/Notice.java`、`mapper/BannerMapper.java`、`mapper/NoticeMapper.java`；`Review` 增加 `status` 字段
- 看板 `GET /api/admin/dashboard`：返回 `{ data: { summary, trend, ticketSales, updatedAt } }`
  - 口径：访客数 = 注册用户数 + 游客订单去重 buyer_phone；订单量 = 非取消订单；门票销量 = sum(quantity)；好评率 = 已通过评价里 rating≥4 占比（百分比两位小数）；另有 revenue / pendingReviews
  - `trend` 为近 7 天（含今日）按 created_at / review_date 聚合；`ticketSales` 按票名累计销量倒序
  - 实现在 `AdminController.dashboard()`，用 Java 内存聚合（无 XML SQL），字段缺失按 0 处理
- **评价审核**：`POST /api/reviews` 新评价写 `status=pending`；`AttractionServiceImpl.listWithReviews()` 只查 `status=approved`，故提交后前台不立即显示。`GET /api/admin/reviews?status=pending` 过滤，`PUT /api/admin/reviews/{id}` body `{ status: approved|rejected }` 审核；`DataInitializer` 种子评价显式写 approved
- **轮播/公告后台 CRUD**：`/api/admin/banners`、`/api/admin/notices` 各支持 GET/POST/PUT{id}/DELETE{id}；PUT 支持 `enabled` 上下架（boolVal 兼容 "true"/1）。前台公开接口 `GET /api/banners`、`GET /api/notices`（ContentController，仅 enabled、按 sort_no 排序）
- **用户 CRUD 补全**：`POST /api/admin/users` 新增（name/email/password≥6/permission/avatar，校验 name+email 唯一）、`DELETE /api/admin/users/{id}`（禁止删自己，先解绑 member.user_id）、`PUT /api/admin/users/{id}` 新增支持 name/email（唯一校验）
- 注意 `AdminController` 里订单 Mapper 字段名是 `ticketOrderMapper`（不是 orderMapper）；`Member` 相关解绑用到 `memberMapper`
