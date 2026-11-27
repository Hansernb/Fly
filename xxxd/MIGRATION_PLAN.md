# 狗熊岭森林游乐园 — 参考仓库借鉴与迁移方案

> 生成日期：2026-11-27
> 原则：**只复用结构、数据表设计、通用组件**；不整份复制开源项目；保留熊出没主题、配色、现有页面与全部路由路径。

## 一、现状清单

### 前端 `boonie-auth-pro`（Vue3 + Vite + Pinia + Vue Router，5173）
- 前台：登录/注册、首页（Banner / 公告 / 欢迎区 / 搜索 / 分类标签 / 热门项目 / 栖息地 / 演出 / 门票 / 小游戏 / 风景 / 评价 / 数据卡 / 园区地图）、项目详情弹窗 + 评价、项目列表、园区地图、小游戏中心（4 款 + 排行榜）、门票预订（积分抵扣 + 订单）、会员中心、个人中心
- 后台：登录、数据看板（手写 SVG 图表）、项目 / 评价 / 用户 / 栖息地 / 演出 / 门票 / 成绩 / 轮播 / 公告 CRUD
- 基础设施：`router/index.js`（9 路由 + 守卫）、`stores/{session,content}.js`、`utils/request.js`、`api/content.js`

### 后端 `springBootTest`（Spring Boot 3.4.5 + MyBatis-Plus + MySQL，8080）
- 11 张 `boonie_*` 表：user / attraction / review / habitat / show / ticket / ticket_order / member / point_log / game_score / banner / notice
- 8 个 Controller，约 50 个 `/api` 接口，单点全局 CORS

### 关键文件树（节选）
```
boonie-auth-pro/src/
  main.js  App.vue(297)  router/index.js
  api/content.js  utils/request.js
  stores/{session,content}.js
  components/  ParkHome.vue(4975) ForestGames.vue(2460)
               TicketBooking.vue(2358) AdminPanel.vue(1773)
               AuthCard.vue MemberCenter.vue ParkMap.vue ...
  data/{attractions,tickets,member,fallback}.js
springBootTest/src/main/
  controller/(8) entity/(12) mapper/(12) service/+impl/
  common/ config/  resources/{db-init.sql,application-*.properties}
```

### 已识别的 3 个可改进点
1. 首页分类标签写死在前端，后台无法维护
2. 看板"访客数" = 注册用户数 + 游客订单去重，不是真实 PV/UV
3. 请求层 401 由各组件分别处理，缺统一拦截

## 二、参考仓库结构

### `geeeeeeeek/java_travel`（Spring Boot + MyBatis-Plus + Vue3/TS）
- 后端分层：`common`(统一响应) / `config`(CORS+拦截器) / `controller` / `entity` / `interceptor` / `permission`(注解鉴权) / `service`+`impl` / `mapper` + `resources/mapper/*.xml`
- 表设计（`b_` 前缀）：`thing` 冗余 pv/collect/wish 计数，`classification`(分类) + `tag`(标签) + `thing_tag`(多对多)，`thing_collect`/`thing_wish`(查重)，`op_log`(访问日志，PV/UV 数据源)，`order`/`comment`/`user`(role) 等
- 通用能力：`AccessInterceptor` 记访问日志并做注解鉴权；搜索按标题 like + 分类 eq + 标签内存过滤；UUID 本地文件上传；无缓存层

### `huzhushan/vue3-element-admin`（Vue3 + Vite + Pinia，JS 版）
- 目录：`api/`(按模块) / `components/ProTable` / `directive/v-permission` / `layout/`(Sidebar+Topbar+Tagsbar+Content) / `pinia/modules/`(account/app/menu/tags/layoutSettings/errorLog) / `router/modules/` / `utils/request.js` / `permission.js`
- 权限：白名单 → token → 拉用户信息 → 动态 `addRoute` → `return to.fullPath` 触发重定向；按钮级 `v-permission`
- 请求封装：请求拦截注入 Bearer token，响应取 data，401 用 refresh_token 重放
- 布局：Sidebar + Topbar + Tagsbar + Breadcrumbs + Content，`keep-alive` + `:key=fullPath` 页面缓存

## 三、迁移方案（本轮实施 3 块）

| # | 借鉴来源 | 借鉴点 | 迁移到本项目 | 改动范围 | 主题影响 |
|---|---|---|---|---|---|
| 1 | java_travel | `Classification` 分类表 | 新建 `boonie_category`，首页写死标签改读库，后台可增删改 | db-init.sql + entity/mapper/controller + store/首页/后台 | 无，标签文案与配色保留 |
| 2 | java_travel | `OpLog`/`VisitData` 访问统计 | 新建 `boonie_visit` 埋点表，看板"访客数"改为真实 PV/UV | db-init.sql + 埋点接口 + 看板聚合 + 前端上报 | 无，看板 UI 不变 |
| 3 | vue3-element-admin | `utils/request` 401 拦截 | 请求层统一 401 清会话 + 跳登录 | 仅前端 request.js + main.js | 无 |

### 后续项实施情况
- ✅ **购票/订单/会员中心完善（2026-11-27 已完成，参考 java_travel `b_order`/`b_user`）**：核对发现购票 4 步流程、自动开会员、积分规则（1元=1分、1分抵¥0.3、最多50%）、数据表均已实现且字段齐全，无需改后端/表结构；唯一缺口是真实订单列表未进会员中心——本轮在 `MemberCenter.vue` 新增「我的订单」区块（`GET /api/orders`，含取消订单+积分回滚刷新），`api/content.js` 加 `getOrders/cancelOrder`，个人中心订单/积分 tab 改为引导跳会员中心消除假数据。实测积分上限与回滚规则全部正确，CDP 登录态渲染 3 笔真实订单 + 8 条流水零报错
- ✅ **`App.vue` → `<router-view>` + 懒加载分包（2026-11-27 已完成）**：新增 `src/views/` 薄适配层（9 个 `*View.vue` + `useNav.js`）承接原 v-if 链的 props/emit 映射，原 17 个组件零改动；主包 309KB → 118KB；9 条路由逐条验证渲染正常、零 JS 报错
- ✅ **中后台布局改造（2026-11-27 已完成，参考 vue3-element-admin layout + ProTable）**：新建独立 `src/admin/` 体系（不删旧 `AdminPanel.vue`），`/admin` 路由改指 `AdminLayout.vue` 嵌套路由，`/admin/:module` 共用 `CrudView.vue`。左侧菜单 7 项（项目/演出/门票/订单/评价/用户/轮播图）+ 顶部栏（面包屑 + 退出登录）+ 内容区；配置化组件 `ProTable`（columns/格式化/状态标签/多选/操作槽）、`FormDialog`（fields schema + 必填校验）、`Pagination`；Mock 数据层 `api/mock.js`（分页/搜索/CRUD/切换状态，localStorage 持久化，220ms 延迟）。**未引入 Element Plus/i18n/工作流/富文本**。7 菜单 CDP 实测标题/面包屑/高亮/表格数据全对、零 JS 报错；新增→6行、搜索命中、退出清 token 全通过。后续接 Springboot：替换 `mock.js` 函数体为 `request()` 即可，页面零改动
- ⬜ `v-permission` 按钮级权限：当前仅 0/1 两种角色，够用，暂不需要
- ⬜ `ParkHome.vue` 4975 行拆分：最大技术债，可单独立项（先拆 13 个 section 到 `views/home/*`）

## 四、实施与验证节奏
每完成 1 块：
1. `npm run build` 冒烟（编译级校验）
2. `npm run dev` 启动，确认页面 200、无控制台报错
3. 后端改动需重启 8080 并 curl 校验接口
4. 全部通过后再进入下一块

## 五、熊大快跑（参考 GitHub Topic `endless-runner`，2026-11-27 已完成）
- 参考 `CursedPrograms/endless-runner-html`、`amoldalwai/Chrome_Dino_Game`：抽出 rAF 循环 / 重力跳跃 / 障碍生成 / AABB 碰撞 / 计分 / 最高分存储 6 段逻辑
- 落地为独立组件 `src/components/BearRunner.vue`（原生 Canvas，零依赖），嵌入 `ForestGames.vue` 作为第 6 款游戏；暖绿 + 暖黄画风，保留熊出没主题
- 玩法：自动跑 + 空格/点击跳跃躲树桩 + 蜂蜜罐 +10 分 + 撞障碍结束 + 最高分 localStorage 双写；登录后与其余游戏一样上报 `/api/games/scores`
- 只复用逻辑结构与通用机制，未复制开源项目任何代码/资源；原有 5 款游戏未改
- 验证：`npm run build` 通过；CDP 实测单局 48 分、新纪录、最高分持久化、HUD 同步，零 JS 报错

## 六、不复制清单
- 不引入 Element Plus、不引入 axios、不引入 TS、不引入 Spring Security
- 不复制 java_travel 的 ad/address/wish/fans/follow 等与游乐园无关的业务
- 不复制 vue3-element-admin 的 i18n/lock/errorLog 等与本项目无关的外壳
