<template>
  <div class="admin">
    <!-- ============ 左侧深色侧边栏 ============ -->
    <aside class="side">
      <div class="side-logo">
        <svg class="logo-leaf" viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="currentColor"
            d="M20 3C10 4 4 9 4 15c0 2 .7 3.7 1.8 5C7 15 11 11 17 9c-4 2.6-7.4 6.2-9.2 10.6C10 21 13.6 22 17.5 22 20.7 22 22 19.6 22 16.5 22 10 21.5 5.5 20 3Z"
          />
        </svg>
        <div class="logo-text">
          <strong>狗熊岭后台</strong>
          <span>森林游乐园管理系统</span>
        </div>
      </div>

      <nav class="side-menu" aria-label="后台菜单">
        <button
          v-for="s in SECTIONS"
          :key="s.key"
          type="button"
          class="side-item"
          :class="{ active: activeKey === s.key }"
          @click="selectSection(s.key)"
        >
          <span class="side-icon">{{ s.icon }}</span>
          <span class="side-name">{{ s.name }}</span>
        </button>
      </nav>

      <div class="side-foot">
        <svg class="foot-leaf" viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="currentColor"
            d="M20 3C10 4 4 9 4 15c0 2 .7 3.7 1.8 5C7 15 11 11 17 9c-4 2.6-7.4 6.2-9.2 10.6C10 21 13.6 22 17.5 22 20.7 22 22 19.6 22 16.5 22 10 21.5 5.5 20 3Z"
          />
        </svg>
        <span>数据实时同步前台</span>
      </div>
    </aside>

    <!-- ============ 右侧浅色内容区 ============ -->
    <main class="main">
      <!-- 顶部：面包屑 + 管理员头像 + 退出 -->
      <header class="topbar">
        <nav class="crumb" aria-label="面包屑">
          <span class="crumb-item">后台管理</span>
          <span class="crumb-sep">/</span>
          <span class="crumb-item current">{{ current.name }}</span>
        </nav>

        <div class="top-right">
          <img class="admin-avatar" src="/img/avatar.jpg" alt="管理员头像" />
          <div class="admin-who">
            <strong>{{ user?.name || '管理员' }}</strong>
            <span>超级管理员</span>
          </div>
          <button class="btn-exit" type="button" @click="onExit">退出</button>
        </div>
      </header>

      <div class="content">
        <div class="content-head">
          <div>
            <h1 class="page-title">{{ current.icon }} {{ current.name }}</h1>
            <p class="page-desc">{{ current.desc }}</p>
          </div>
          <div class="head-actions">
            <span v-if="msg" class="msg" :class="{ err: msgType === 'err' }">{{ msg }}</span>
            <button class="btn-ghost" type="button" @click="load">🔄 刷新</button>
            <button
              v-if="activeKey !== 'dashboard' && !current.noCreate"
              class="btn-primary"
              type="button"
              @click="openCreate"
            >＋ 新增</button>
          </div>
        </div>

        <!-- ============ 数据看板 ============ -->
        <div v-if="activeKey === 'dashboard'" class="dash">
          <div class="kpi-grid">
            <div v-for="k in kpis" :key="k.key" class="kpi-card">
              <div class="kpi-top">
                <span class="kpi-ico">{{ k.icon }}</span>
                <span class="kpi-label">{{ k.label }}</span>
              </div>
              <div class="kpi-value">{{ k.value }}<small>{{ k.unit }}</small></div>
              <div class="kpi-foot">{{ k.note }}</div>
            </div>
          </div>

          <div class="chart-grid">
            <section class="chart-card wide">
              <h2 class="chart-title">访客数与订单量趋势 <small>近 7 天</small></h2>
              <div class="legend">
                <span><i class="lg green"></i>访客数</span>
                <span><i class="lg gold"></i>订单量</span>
              </div>
              <svg viewBox="0 0 620 190" class="chart-svg" role="img" aria-label="访客数与订单量趋势折线图">
                <g v-for="i in 5" :key="'g' + i">
                  <line x1="46" :y1="16 + (i - 1) * 36" x2="606" :y2="16 + (i - 1) * 36" stroke="#eef2ef" stroke-width="1" />
                  <text x="38" :y="20 + (i - 1) * 36" class="axis" text-anchor="end">
                    {{ Math.round(trendMax * (5 - i) / 4) }}
                  </text>
                </g>
                <polyline :points="linePoints('visitors')" fill="none" stroke="#3f9c68" stroke-width="3" stroke-linejoin="round" />
                <polyline :points="linePoints('orders')" fill="none" stroke="#e0a52a" stroke-width="3" stroke-linejoin="round" />
                <circle v-for="(p, i) in pointList('visitors')" :key="'v' + i" :cx="p.x" :cy="p.y" r="3.5" fill="#3f9c68" />
                <circle v-for="(p, i) in pointList('orders')" :key="'o' + i" :cx="p.x" :cy="p.y" r="3.5" fill="#e0a52a" />
                <text v-for="(t, i) in dash.trend" :key="'x' + i" :x="xAt(i)" y="186" class="axis" text-anchor="middle">{{ t.date }}</text>
              </svg>
            </section>

            <section class="chart-card">
              <h2 class="chart-title">好评率 <small>4 星及以上</small></h2>
              <div class="donut-wrap">
                <svg viewBox="0 0 140 140" class="donut" role="img" aria-label="好评率环形图">
                  <circle cx="70" cy="70" r="52" fill="none" stroke="#eef2ef" stroke-width="14" />
                  <circle cx="70" cy="70" r="52" fill="none" stroke="#3f9c68" stroke-width="14"
                    stroke-linecap="round" :stroke-dasharray="goodDash" transform="rotate(-90 70 70)" />
                  <text x="70" y="68" text-anchor="middle" class="donut-val">{{ sum.goodRate }}%</text>
                  <text x="70" y="88" text-anchor="middle" class="donut-sub">{{ sum.goodReviews }}/{{ sum.totalReviews }} 条好评</text>
                </svg>
              </div>
              <p class="chart-note">待审核评价 {{ sum.pendingReviews }} 条，审核通过后才计入统计与前台展示。</p>
            </section>

            <section class="chart-card wide">
              <h2 class="chart-title">门票销量 <small>近 7 天 · 张</small></h2>
              <svg viewBox="0 0 620 190" class="chart-svg" role="img" aria-label="门票销量柱状图">
                <g v-for="i in 4" :key="'bg' + i">
                  <line x1="46" :y1="20 + (i - 1) * 38" x2="606" :y2="20 + (i - 1) * 38" stroke="#eef2ef" stroke-width="1" />
                  <text x="38" :y="24 + (i - 1) * 38" class="axis" text-anchor="end">
                    {{ Math.round(ticketMax * (4 - i) / 3) }}
                  </text>
                </g>
                <g v-for="(b, i) in bars" :key="'bar' + i">
                  <rect :x="b.x" :y="b.y" width="42" :height="b.h" rx="6" fill="#8fc3a5" />
                  <text :x="b.x + 21" :y="b.y - 6" class="axis" text-anchor="middle">{{ b.v }}</text>
                  <text :x="b.x + 21" y="186" class="axis" text-anchor="middle">{{ dash.trend[i].date }}</text>
                </g>
              </svg>
            </section>

            <section class="chart-card">
              <h2 class="chart-title">票种销量分布 <small>累计</small></h2>
              <div v-if="dash.ticketSales.length" class="sales-list">
                <div v-for="t in dash.ticketSales" :key="t.name" class="sales-row">
                  <span class="sales-name">{{ t.name }}</span>
                  <div class="sales-track"><div class="sales-fill" :style="{ width: salesWidth(t.qty) }"></div></div>
                  <b class="sales-qty">{{ t.qty }}</b>
                </div>
              </div>
              <p v-else class="chart-note">暂无订单数据</p>
            </section>
          </div>
          <p class="tip">📈 数据来自订单、用户与评价表实时聚合；访客数 = 注册用户 + 游客订单去重手机号，点击「刷新」可更新。</p>
        </div>

        <!-- 数据表格 -->
        <div v-if="activeKey !== 'dashboard'" class="table-card">
          <table class="tbl">
            <thead>
              <tr>
                <th v-if="!current.noDrag" class="col-drag" title="拖拽排序"></th>
                <th class="col-idx">#</th>
                <th v-for="c in current.columns" :key="c.key">{{ c.label }}</th>
                <th v-if="!current.noSwitch" class="col-switch">上下架</th>
                <th class="col-ops">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(row, i) in rows"
                :key="row.id ?? row._k"
                class="tbl-row"
                :class="{ dragging: dragIndex === i, off: !row.enabled }"
                draggable="true"
                @dragstart="onDragStart(i)"
                @dragover.prevent="onDragOver(i)"
                @drop.prevent="onDrop(i)"
                @dragend="onDragEnd"
              >
                <td v-if="!current.noDrag" class="col-drag"><span class="drag-handle" title="按住拖拽排序">⋮⋮</span></td>
                <td class="col-idx">{{ i + 1 }}</td>
                <td v-for="c in current.columns" :key="c.key">
                  <span v-if="c.type === 'color'" class="dot" :style="{ background: row[c.key] }"></span>
                  <img v-else-if="c.type === 'avatar'" class="tbl-avatar" :src="row[c.key] || defaultAvatar" :alt="c.label" />
                  <span v-else>{{ display(row, c) }}</span>
                </td>
                <td v-if="!current.noSwitch" class="col-switch">
                  <button
                    type="button"
                    class="sw"
                    :class="{ on: row.enabled !== false }"
                    :aria-pressed="row.enabled !== false"
                    @click="toggleEnabled(row)"
                  >
                    <span class="sw-dot"></span>
                  </button>
                </td>
                <td class="col-ops">
                  <template v-if="current.key === 'reviews'">
                    <button
                      v-if="row.status !== 'approved'"
                      class="op ok"
                      type="button"
                      @click="reviewRow(row, 'approved')"
                    >通过</button>
                    <button
                      v-if="row.status !== 'rejected'"
                      class="op"
                      type="button"
                      @click="reviewRow(row, 'rejected')"
                    >驳回</button>
                    <button class="op danger" type="button" @click="removeRow(row)">删除</button>
                  </template>
                  <template v-else>
                    <button class="op" type="button" @click="openEdit(row)">编辑</button>
                    <button class="op danger" type="button" @click="removeRow(row)">删除</button>
                  </template>
                </td>
              </tr>
              <tr v-if="!rows.length">
                <td class="empty" :colspan="current.columns.length + 4">暂无数据，点击右上角「＋ 新增」添加</td>
              </tr>
            </tbody>
          </table>
        </div>

        <p v-if="activeKey !== 'dashboard'" class="tip">
          <template v-if="current.key === 'reviews'">
            💡 游客提交的评价默认「待审核」，通过后才会展示在前台；也可直接删除违规评价。
          </template>
          <template v-else-if="current.key === 'settings'">
            💡 新增用户需填账号、邮箱和初始密码（≥6 位）；编辑时密码留空表示不修改。不能删除当前登录的管理员。
          </template>
          <template v-else-if="current.key === 'banner' || current.key === 'notice'">
            💡 排序值越小越靠前；「上下架」开关直接写库并同步前台。轮播图可上传图片（≤2MB）或填 /img/... 路径。
          </template>
          <template v-else>
            💡 拖拽行首的 ⋮⋮ 可调整顺序；「上下架」控制前台是否展示。
            <template v-if="current.api">本模块已对接后端接口，保存后前台首页实时生效。</template>
            <template v-else>本模块为本地配置（后端暂未开放接口），数据保存在浏览器本地。</template>
          </template>
        </p>
      </div>
    </main>

    <!-- ============ 新增 / 编辑弹窗 ============ -->
    <div v-if="editing" class="mask" @click.self="closeForm">
      <div class="modal">
        <h2 class="modal-title">{{ editing.id ? '编辑' : '新增' }} · {{ current.name }}</h2>
        <div class="modal-body">
          <label v-for="c in current.columns" :key="c.key" class="fld">
            <span class="fld-label">{{ c.label }}</span>

            <template v-if="c.type === 'avatar'">
              <div class="avatar-edit">
                <img class="avatar-edit-prev" :src="form[c.key] || defaultAvatar" alt="头像预览" />
                <div class="avatar-edit-opts">
                  <button
                    v-for="(a, i) in defaultAvatars"
                    :key="i"
                    type="button"
                    class="avatar-edit-opt"
                    :class="{ active: form[c.key] === a }"
                    @click="form[c.key] = a"
                  >
                    <img :src="a" alt="默认头像" />
                  </button>
                  <label class="avatar-edit-opt upload" :class="{ active: isUploadedAvatar(form[c.key]) }">
                    <span>＋</span>
                    <input
                      type="file"
                      accept="image/*"
                      class="avatar-edit-file"
                      :disabled="uploadingAvatar"
                      @change="onAdminAvatarUpload($event, c.key)"
                    />
                  </label>
                </div>
                <span v-if="uploadingAvatar" class="avatar-edit-hint">上传中…</span>
              </div>
            </template>

            <select v-else-if="c.type === 'select'" v-model="form[c.key]" class="fld-input">
              <option v-for="o in c.options" :key="o.value ?? o" :value="o.value ?? o">
                {{ o.label ?? o }}
              </option>
            </select>

            <textarea
              v-else-if="c.type === 'textarea'"
              v-model="form[c.key]"
              class="fld-input"
              rows="3"
            ></textarea>

            <div v-else-if="c.type === 'image'" class="image-edit">
              <img v-if="form[c.key]" class="image-prev" :src="form[c.key]" alt="图片预览" />
              <input
                v-model="form[c.key]"
                class="fld-input"
                type="text"
                placeholder="填 /img/... 路径，或点右侧上传"
              />
              <label class="btn-upload">
                ＋ 上传图片
                <input
                  type="file"
                  accept="image/*"
                  class="avatar-edit-file"
                  :disabled="uploadingAvatar"
                  @change="onImageUpload($event, c.key)"
                />
              </label>
            </div>

            <input
              v-else-if="c.type === 'password'"
              v-model="form[c.key]"
              class="fld-input"
              type="password"
              :placeholder="editing && editing.id ? '留空表示不修改密码' : '请输入密码'"
            />

            <input v-else-if="c.type === 'color'" v-model="form[c.key]" class="fld-color" type="color" />

            <input
              v-else
              v-model="form[c.key]"
              class="fld-input"
              :type="c.type === 'number' ? 'number' : 'text'"
              :placeholder="'请输入' + c.label"
            />
          </label>

          <div v-if="!current.noSwitch" class="fld fld-inline">
            <span class="fld-label">上架状态</span>
            <button type="button" class="sw" :class="{ on: form.enabled !== false }" @click="toggleFormEnabled">
              <span class="sw-dot"></span>
            </button>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-ghost" type="button" @click="closeForm">取消</button>
          <button class="btn-primary" type="button" :disabled="saving" @click="saveForm">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'

const props = defineProps({
  user: { type: Object, default: null },
  token: { type: String, default: '' }
})
const emit = defineEmits(['exit'])

/* ===================== 头像（默认头像 + 上传） ===================== */
const defaultAvatar = '/img/avatar.jpg'
const defaultAvatars = [
  '/img/defaults/avatar-1.jpg',
  '/img/defaults/avatar-2.jpg',
  '/img/defaults/avatar-3.jpg'
]
const uploadingAvatar = ref(false)
function isUploadedAvatar(v) {
  return typeof v === 'string' && v.startsWith('/api/avatar/')
}
async function uploadAvatar(file) {
  // 前端预校验：与后端保持一致，避免撞 413 时拿到空响应体导致报错信息不明确
  if (!/^image\/(jpeg|png|gif|webp)$/i.test(file.type)) throw new Error('仅支持 JPG / PNG / GIF / WEBP 图片')
  if (file.size > 2 * 1024 * 1024) throw new Error('图片不能超过 2MB，请压缩后再上传')

  const fd = new FormData()
  fd.append('file', file)
  const res = await fetch('/api/avatar/upload', { method: 'POST', body: fd })
  const data = await res.json().catch(() => ({}))
  if (!res.ok || !data.ok) {
    throw new Error(
      data.message || (res.status === 413 ? '图片不能超过 2MB，请压缩后再上传' : '头像上传失败')
    )
  }
  return data.url
}

/* ===================== 模块配置（9 个菜单） ===================== */
const SECTIONS = [
  {
    key: 'dashboard',
    name: '数据看板',
    icon: '📊',
    desc: '访客数、订单量、门票销量、好评率实时聚合',
    dashboard: true
  },
  {
    key: 'banner',
    name: '轮播图管理',
    icon: '🖼️',
    desc: '配置首页轮播图（数据库 boonie_banner，前台实时生效）',
    api: '/api/admin/banners',
    columns: [
      { key: 'title', label: '标题', type: 'text' },
      { key: 'subtitle', label: '副标题', type: 'textarea' },
      { key: 'tag', label: '角标', type: 'text' },
      { key: 'img', label: '图片', type: 'image' },
      { key: 'link', label: '跳转板块', type: 'select', options: ['游乐项目', '栖息地', '演出', '门票', '风景'] },
      { key: 'color', label: '主题色', type: 'color' },
      { key: 'sortNo', label: '排序', type: 'number' }
    ],
    seed: []
  },
  {
    key: 'notice',
    name: '公告管理',
    icon: '📢',
    desc: '维护首页顶部滚动公告（数据库 boonie_notice）',
    api: '/api/admin/notices',
    columns: [
      { key: 'content', label: '公告内容', type: 'textarea' },
      { key: 'sortNo', label: '排序', type: 'number' }
    ],
    seed: []
  },
  {
    key: 'reviews',
    name: '评价审核',
    icon: '✅',
    desc: '审核游客评价，通过后才会展示在前台（数据库 boonie_review）',
    api: '/api/admin/reviews',
    noDrag: true,
    noSwitch: true,
    noCreate: true,
    columns: [
      { key: 'attraction', label: '所属项目', type: 'text' },
      { key: 'name', label: '评价人', type: 'text' },
      { key: 'rating', label: '评分', type: 'text' },
      { key: 'date', label: '日期', type: 'text' },
      { key: 'text', label: '评价内容', type: 'textarea' },
      {
        key: 'status',
        label: '状态',
        type: 'select',
        options: [
          { value: 'pending', label: '待审核' },
          { value: 'approved', label: '已通过' },
          { value: 'rejected', label: '已驳回' }
        ]
      }
    ],
    seed: []
  },
  {
    key: 'projects',
    name: '热门项目管理',
    icon: '🎡',
    desc: '管理前台「游乐项目」卡片，保存后前台首页实时生效',
    api: '/api/admin/attractions',
    columns: [
      { key: 'name', label: '项目名称', type: 'text' },
      { key: 'emoji', label: '图标', type: 'text' },
      { key: 'tag', label: '标签', type: 'select', options: ['刺激', '浪漫', '亲子', '清凉', '益智'] },
      { key: 'waitMinutes', label: '排队(分钟)', type: 'number' },
      { key: 'color', label: '主题色', type: 'color' },
      { key: 'description', label: '玩法说明', type: 'textarea' }
    ],
    seed: []
  },
  {
    key: 'shows',
    name: '演出时刻表',
    icon: '🎪',
    desc: '维护各剧场的演出时间与地点（数据库 boonie_show）',
    api: '/api/admin/shows',
    columns: [
      { key: 'emoji', label: '图标', type: 'text' },
      { key: 'name', label: '演出名称', type: 'text' },
      { key: 'showTime', label: '时间', type: 'text' },
      { key: 'place', label: '地点', type: 'text' },
      { key: 'tag', label: '时长标签', type: 'text' }
    ],
    seed: []
  },
  {
    key: 'habitats',
    name: '栖息地管理',
    icon: '🏡',
    desc: '管理首页「森林栖息地」卡片（数据库 boonie_habitat）',
    api: '/api/admin/habitats',
    columns: [
      { key: 'emoji', label: '图标', type: 'text' },
      { key: 'name', label: '名称', type: 'text' },
      { key: 'description', label: '简介', type: 'textarea' },
      { key: 'residents', label: '居民', type: 'text' },
      { key: 'mood', label: '当前状态', type: 'text' },
      { key: 'color', label: '主题色', type: 'color' }
    ],
    seed: []
  },
  {
    key: 'tickets',
    name: '门票管理',
    icon: '🎫',
    desc: '维护票种价格与权益（数据库 boonie_ticket，订票页同步生效）',
    api: '/api/admin/tickets',
    columns: [
      { key: 'typeCode', label: '票种编码', type: 'text' },
      { key: 'emoji', label: '图标', type: 'text' },
      { key: 'name', label: '票种名称', type: 'text' },
      { key: 'price', label: '售价(元)', type: 'number' },
      { key: 'originalPrice', label: '原价(元)', type: 'number' },
      { key: 'discountLabel', label: '优惠标签', type: 'text' },
      { key: 'tagLabel', label: '角标', type: 'select', options: ['标准', '儿童优惠', '长者优惠', '热门', '夜场'] },
      { key: 'descText', label: '一句话说明', type: 'text' },
      { key: 'features', label: '权益说明(每行一条)', type: 'textarea' }
    ],
    seed: []
  },
  {
    key: 'games',
    name: '小游戏成绩管理',
    icon: '🎮',
    desc: '查询 / 修改 / 删除玩家成绩（数据库 boonie_game_score，可清作弊改榜单）',
    api: '/api/admin/games',
    columns: [
      { key: 'gameId', label: '游戏', type: 'select', options: ['honey', 'mole', 'match'] },
      { key: 'gameName', label: '游戏名称', type: 'text' },
      { key: 'userName', label: '玩家', type: 'text' },
      { key: 'bestScore', label: '最佳成绩', type: 'number' },
      { key: 'plays', label: '游玩局数', type: 'number' },
      { key: 'updatedAt', label: '最近更新', type: 'text' }
    ],
    seed: []
  },
  {
    key: 'routes',
    name: '游园路线',
    icon: '🗺️',
    desc: '推荐游玩路线与强度分级',
    columns: [
      { key: 'name', label: '路线名称', type: 'text' },
      { key: 'stops', label: '途经点数', type: 'number' },
      { key: 'duration', label: '建议时长(小时)', type: 'number' },
      { key: 'level', label: '强度', type: 'select', options: ['轻松', '适中', '挑战'] }
    ],
    seed: [
      { name: '亲子半日游', stops: 6, duration: 4, level: '轻松' },
      { name: '刺激一整天', stops: 10, duration: 8, level: '挑战' },
      { name: '浪漫黄昏线', stops: 5, duration: 3, level: '适中' }
    ]
  },
  {
    key: 'spots',
    name: '打卡圣地',
    icon: '📸',
    desc: '前台热门拍照点推荐',
    columns: [
      { key: 'name', label: '打卡点', type: 'text' },
      { key: 'tag', label: '风格', type: 'select', options: ['花海', '林间', '湖畔', '木屋', '云端'] },
      { key: 'hot', label: '热度', type: 'number' }
    ],
    seed: [
      { name: '樱花大道', tag: '花海', hot: 98 },
      { name: '古树树洞', tag: '林间', hot: 91 },
      { name: '萤火湖畔', tag: '湖畔', hot: 87 }
    ]
  },
  {
    key: 'realtime',
    name: '实时数据配置',
    icon: '📊',
    desc: '配置前台实时数据看板的指标项',
    columns: [
      { key: 'label', label: '指标名称', type: 'text' },
      { key: 'value', label: '当前值', type: 'text' },
      { key: 'unit', label: '单位', type: 'text' },
      { key: 'refresh', label: '刷新间隔(秒)', type: 'number' }
    ],
    seed: [
      { label: '在园游客', value: '3,280', unit: '人', refresh: 30 },
      { label: '平均排队', value: '18', unit: '分钟', refresh: 60 },
      { label: '今日演出', value: '6', unit: '场', refresh: 120 }
    ]
  },
  {
    key: 'weather',
    name: '天气 / 客流',
    icon: '🌤️',
    desc: '维护天气与客流预测，供前台展示',
    columns: [
      { key: 'date', label: '日期', type: 'text' },
      { key: 'weather', label: '天气', type: 'select', options: ['晴', '多云', '阴', '小雨', '阵雨'] },
      { key: 'temp', label: '气温(℃)', type: 'number' },
      { key: 'flow', label: '预计客流', type: 'number' }
    ],
    seed: [
      { date: '今天', weather: '晴', temp: 24, flow: 3200 },
      { date: '明天', weather: '多云', temp: 22, flow: 2800 },
      { date: '后天', weather: '小雨', temp: 19, flow: 1500 }
    ]
  },
  {
    key: 'coupon',
    name: '优惠券 / 积分',
    icon: '🎟️',
    desc: '配置可发放的优惠券与积分规则',
    columns: [
      { key: 'name', label: '名称', type: 'text' },
      { key: 'type', label: '类型', type: 'select', options: ['优惠券', '积分'] },
      { key: 'amount', label: '面额(元)', type: 'number' },
      { key: 'points', label: '所需积分', type: 'number' }
    ],
    seed: [
      { name: '春日踏青券', type: '优惠券', amount: 20, points: 0 },
      { name: '森林积分兑换', type: '积分', amount: 10, points: 200 },
      { name: '亲子套票立减', type: '优惠券', amount: 50, points: 0 }
    ]
  },
  {
    key: 'settings',
    name: '用户列表管理',
    icon: '👥',
    desc: '管理注册用户：新增 / 改资料 / 重置密码 / 改权限 / 删除',
    api: '/api/admin/users',
    noDrag: true,
    noSwitch: true,
    columns: [
      { key: 'avatar', label: '头像', type: 'avatar' },
      { key: 'name', label: '账号', type: 'text' },
      { key: 'email', label: '邮箱', type: 'text' },
      { key: 'password', label: '密码', type: 'password' },
      {
        key: 'permission',
        label: '权限',
        type: 'select',
        options: [
          { value: 0, label: '管理员' },
          { value: 1, label: '普通用户' }
        ]
      },
      { key: 'createdAt', label: '注册时间', type: 'text' }
    ],
    seed: []
  }
]

const activeKey = ref(SECTIONS[0].key)
const current = computed(() => SECTIONS.find((s) => s.key === activeKey.value))
const rows = ref([])

/* ===================== 数据看板 ===================== */
const dash = ref({ summary: {}, trend: [], ticketSales: [] })
const sum = computed(() => dash.value.summary || {})
const kpis = computed(() => [
  { key: 'visitors', icon: '👥', label: '访客数', value: sum.value.visitors ?? 0, unit: '人', note: '注册用户 + 游客去重' },
  { key: 'orders', icon: '📦', label: '订单量', value: sum.value.orders ?? 0, unit: '单', note: '不含已取消订单' },
  { key: 'tickets', icon: '🎫', label: '门票销量', value: sum.value.ticketsSold ?? 0, unit: '张', note: '累计已售门票' },
  {
    key: 'rate',
    icon: '⭐',
    label: '好评率',
    value: sum.value.goodRate ?? 0,
    unit: '%',
    note: `${sum.value.goodReviews ?? 0} 条好评 / ${sum.value.totalReviews ?? 0} 条评价`
  }
])
const trendMax = computed(() =>
  Math.max(1, ...dash.value.trend.flatMap((t) => [Number(t.visitors) || 0, Number(t.orders) || 0]))
)
const ticketMax = computed(() => Math.max(1, ...dash.value.trend.map((t) => Number(t.tickets) || 0)))
function xAt(i) {
  return 46 + i * (560 / Math.max(1, dash.value.trend.length - 1))
}
function pointList(key) {
  return dash.value.trend.map((t, i) => ({
    x: xAt(i),
    y: 160 - (Number(t[key]) || 0) / trendMax.value * 136
  }))
}
function linePoints(key) {
  return pointList(key).map((p) => `${p.x},${p.y}`).join(' ')
}
const bars = computed(() =>
  dash.value.trend.map((t, i) => {
    const v = Number(t.tickets) || 0
    const h = Math.max(2, v / ticketMax.value * 120)
    return { x: 52 + i * 82, y: 152 - h, h, v }
  })
)
const goodDash = computed(() => `${((Number(sum.value.goodRate) || 0) / 100 * 326.7).toFixed(1)} 326.7`)
const maxSales = computed(() => Math.max(1, ...dash.value.ticketSales.map((t) => Number(t.qty) || 0)))
function salesWidth(q) {
  return Math.max(3, (Number(q) || 0) / maxSales.value * 100) + '%'
}
async function loadDashboard() {
  try {
    const res = await fetch('/api/admin/dashboard', { headers: authHeaders() })
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const json = await res.json()
    if (json && json.data) dash.value = json.data
  } catch {
    flash('看板数据加载失败，请确认后端已启动', 'err')
  }
}
const msg = ref('')
const msgType = ref('ok')
const editing = ref(null)
const form = reactive({})
const saving = ref(false)
const dragIndex = ref(-1)

/* ===================== 本地持久化（后端暂无接口的模块） ===================== */
const LS = (k) => 'boonie_admin_rows_' + k
const LS_ORDER = (k) => 'boonie_admin_order_' + k
const LS_ENABLED = (k) => 'boonie_admin_enabled_' + k

function readJSON(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}
function writeJSON(key, val) {
  localStorage.setItem(key, JSON.stringify(val))
}

function persistLocal() {
  const key = activeKey.value
  writeJSON(LS(key), rows.value)
}

/** 上下架 / 排序覆盖（后端暂无 sort、enabled 字段，落本地保证刷新后仍生效） */
function persistOverlay() {
  const key = activeKey.value
  const en = {}
  rows.value.forEach((r) => (en[r.id ?? r._k] = r.enabled !== false))
  writeJSON(LS_ENABLED(key), en)
  writeJSON(LS_ORDER(key), rows.value.map((r) => r.id ?? r._k))
}

/* ===================== 数据加载 ===================== */
async function load() {
  const sec = current.value
  if (sec.dashboard) {
    await loadDashboard()
    return
  }
  rows.value = []
  if (sec.api) {
    try {
      const res = await fetch(sec.api, { headers: authHeaders() })
      const data = await res.json()
      let list = data.data ?? data.attractions ?? data.users ?? data.list ?? []
      const en = readJSON(LS_ENABLED(sec.key), {})
      list = list.map((r) => ({ ...r, enabled: en[r.id] !== undefined ? en[r.id] : true }))
      const order = readJSON(LS_ORDER(sec.key), null)
      if (order && order.length) {
        list.sort((a, b) => order.indexOf(a.id) - order.indexOf(b.id))
      }
      rows.value = list
      return
    } catch {
      flash('加载失败，请确认后端已启动', 'err')
      return
    }
  }
  const local = readJSON(LS(sec.key), null)
  rows.value = local && local.length ? local : sec.seed.map((s, i) => ({ ...s, _k: i + 1, enabled: true }))
}

function authHeaders() {
  return props.token
    ? { Authorization: 'Bearer ' + props.token, 'Content-Type': 'application/json' }
    : { 'Content-Type': 'application/json' }
}

function selectSection(key) {
  activeKey.value = key
  msg.value = ''
  load()
}

/* ===================== 增删改 ===================== */
function blankForm() {
  const f = { enabled: true }
  current.value.columns.forEach((c) => {
    if (c.type === 'number') f[c.key] = 0
    else if (c.type === 'color') f[c.key] = '#4caf50'
    else if (c.type === 'select') f[c.key] = c.options[0].value ?? c.options[0]
    else f[c.key] = ''
  })
  return f
}

function openCreate() {
  editing.value = { id: null }
  Object.assign(form, blankForm())
}
function openEdit(row) {
  editing.value = { id: row.id ?? row._k }
  Object.assign(form, blankForm(), JSON.parse(JSON.stringify(row)))
}
function closeForm() {
  editing.value = null
}

async function onAdminAvatarUpload(e, key) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  uploadingAvatar.value = true
  try {
    const url = await uploadAvatar(file)
    form[key] = url
    flash('头像已选择，点「保存」生效')
  } catch (err) {
    flash(err.message || '头像上传失败', 'err')
  } finally {
    uploadingAvatar.value = false
    e.target.value = ''
  }
}
function toggleFormEnabled() {
  form.enabled = form.enabled === false
}

async function saveForm() {
  const sec = current.value
  saving.value = true
  try {
    if (sec.api) {
      const payload = {}
      sec.columns.forEach((c) => (payload[c.key] = form[c.key]))
      const isEdit = !!(editing.value && editing.value.id)
      if (sec.key === 'settings') {
        const pwd = String(payload.password || '').trim()
        if (!isEdit && !pwd) {
          flash('请填写初始密码', 'err')
          return
        }
        if (isEdit && !pwd) delete payload.password   // 留空 = 不修改密码
      }
      const res = await fetch(sec.api + (isEdit ? '/' + editing.value.id : ''), {
        method: isEdit ? 'PUT' : 'POST',
        headers: authHeaders(),
        body: JSON.stringify(payload)
      })
      if (!res.ok) {
        const d = await res.json().catch(() => ({}))
        flash(d.message || '保存失败', 'err')
        return
      }
      flash('已保存，前台首页已同步 ✓')
    } else {
      if (editing.value && editing.value.id) {
        const row = rows.value.find((r) => (r.id ?? r._k) === editing.value.id)
        if (row) Object.assign(row, JSON.parse(JSON.stringify(form)))
      } else {
        rows.value.push({ ...JSON.parse(JSON.stringify(form)), _k: Date.now() })
      }
      persistLocal()
      flash('已保存（本地配置）')
    }
    closeForm()
    await load()
  } catch {
    flash('网络异常，请确认后端已启动', 'err')
  } finally {
    saving.value = false
  }
}

async function removeRow(row) {
  if (!confirm('确定删除这条记录吗？')) return
  const sec = current.value
  const id = row.id ?? row._k
  if (sec.api && row.id) {
    try {
      const res = await fetch(sec.api + '/' + row.id, { method: 'DELETE', headers: authHeaders() })
      if (!res.ok) {
        flash('删除失败', 'err')
        return
      }
      flash('已删除，前台首页已同步 ✓')
    } catch {
      flash('网络异常，请确认后端已启动', 'err')
      return
    }
  } else {
    rows.value = rows.value.filter((r) => (r.id ?? r._k) !== id)
    persistLocal()
    flash('已删除')
  }
  await load()
}

async function toggleEnabled(row) {
  row.enabled = row.enabled === false
  if (current.value.key === 'banner' || current.value.key === 'notice') {
    try {
      const res = await fetch(current.value.api + '/' + (row.id ?? row._k), {
        method: 'PUT',
        headers: authHeaders(),
        body: JSON.stringify({ enabled: row.enabled })
      })
      if (!res.ok) throw new Error('HTTP ' + res.status)
      flash(row.enabled ? '已上架，前台已同步 ✓' : '已下架，前台已同步 ✓')
    } catch {
      row.enabled = !row.enabled
      flash('上架状态保存失败', 'err')
    }
    return
  }
  if (current.value.api) persistOverlay()
  else persistLocal()
  flash(row.enabled ? '已上架' : '已下架')
}

/** 评价审核：通过 / 驳回 */
async function reviewRow(row, status) {
  try {
    const res = await fetch('/api/admin/reviews/' + row.id, {
      method: 'PUT',
      headers: authHeaders(),
      body: JSON.stringify({ status })
    })
    if (!res.ok) {
      const d = await res.json().catch(() => ({}))
      flash(d.message || '操作失败', 'err')
      return
    }
    flash(status === 'approved' ? '已通过，评价现已在前台展示 ✓' : '已驳回，评价不会在前台展示')
    await load()
  } catch {
    flash('网络异常，请确认后端已启动', 'err')
  }
}

/** 图片字段上传（轮播图等） */
async function onImageUpload(e, key) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  uploadingAvatar.value = true
  try {
    form[key] = await uploadAvatar(file)
    flash('图片已上传，点「保存」生效')
  } catch (err) {
    flash(err.message || '图片上传失败', 'err')
  } finally {
    uploadingAvatar.value = false
    e.target.value = ''
  }
}

/* ===================== 拖拽排序 ===================== */
function onDragStart(i) {
  dragIndex.value = i
}
function onDragOver() {
  /* 允许放置 */
}
function onDrop(i) {
  const from = dragIndex.value
  if (from < 0 || from === i) return
  const list = rows.value.slice()
  const moved = list.splice(from, 1)[0]
  list.splice(i, 0, moved)
  rows.value = list
  if (current.value.api) persistOverlay()
  else persistLocal()
  flash('顺序已更新')
}
function onDragEnd() {
  dragIndex.value = -1
}

/* ===================== 其它 ===================== */
let msgTimer = null
function flash(text, type = 'ok') {
  msg.value = text
  msgType.value = type
  clearTimeout(msgTimer)
  msgTimer = setTimeout(() => (msg.value = ''), 3000)
}

function display(row, c) {
  const v = row[c.key]
  if (c.key === 'permission') return Number(v) === 0 ? '管理员' : '普通用户'
  if (c.key === 'status') return v === 'approved' ? '已通过' : v === 'rejected' ? '已驳回' : '待审核'
  if (c.key === 'password') return '••••••'
  if (v === undefined || v === null || v === '') return '—'
  const s = String(v)
  return s.length > 22 ? s.slice(0, 22) + '…' : s
}

function onExit() {
  emit('exit')
}

onMounted(load)
</script>

<style scoped>
.admin {
  display: flex;
  min-height: 100vh;
  background: #f5f7f4;
}

/* ================= 侧边栏（深色 + 金色点缀） ================= */
.side {
  width: 232px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #16291f 0%, #101f17 100%);
  color: #cfded2;
}
.side-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px;
  border-bottom: 1px solid rgba(232, 196, 106, 0.14);
}
.logo-leaf {
  width: 26px;
  height: 26px;
  color: #e8c46a;
  flex-shrink: 0;
}
.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.logo-text strong {
  font-size: 15px;
  color: #f3ecd8;
  font-weight: 800;
}
.logo-text span {
  font-size: 11px;
  color: #8ea596;
}

.side-menu {
  flex: 1;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow-y: auto;
}
.side-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  border: none;
  background: transparent;
  color: #b3c6b9;
  font-size: 13.5px;
  font-weight: 600;
  text-align: left;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.side-item:hover {
  background: rgba(232, 196, 106, 0.1);
  color: #f3ecd8;
}
.side-item.active {
  background: linear-gradient(90deg, rgba(232, 196, 106, 0.2), rgba(232, 196, 106, 0.04));
  color: #f7dd9a;
  box-shadow: inset 3px 0 0 #e8c46a;
}
.side-icon {
  font-size: 15px;
  width: 20px;
  text-align: center;
}

.side-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px;
  font-size: 11.5px;
  color: #7d9086;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}
.foot-leaf {
  width: 16px;
  height: 16px;
  color: rgba(232, 196, 106, 0.55);
}

/* ================= 内容区 ================= */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 26px;
  background: #fff;
  border-bottom: 1px solid #e6ebe6;
}
.crumb {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
}
.crumb-item {
  color: #8b9a90;
}
.crumb-sep {
  color: #c3cec5;
}
.crumb-item.current {
  color: #2f4a38;
  font-weight: 700;
}
.top-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.admin-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e8c46a;
}
.admin-who {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}
.admin-who strong {
  font-size: 13px;
  color: #2f4a38;
}
.admin-who span {
  font-size: 11px;
  color: #8b9a90;
}
.btn-exit {
  border: 1px solid #dfe5df;
  background: #fff;
  color: #6b7c71;
  font-size: 12.5px;
  font-weight: 700;
  padding: 7px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-exit:hover {
  border-color: #e0a52a;
  color: #b5811a;
  background: #fffaf0;
}

.content {
  flex: 1;
  padding: 22px 26px 30px;
  overflow-y: auto;
}
.content-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.page-title {
  margin: 0 0 4px;
  font-size: 19px;
  font-weight: 800;
  color: #24382c;
}
.page-desc {
  margin: 0;
  font-size: 12.5px;
  color: #7d8c82;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.msg {
  font-size: 12.5px;
  font-weight: 700;
  color: #2f7d4f;
  background: #eaf6ee;
  border: 1px solid #bfe3c8;
  padding: 6px 12px;
  border-radius: 8px;
}
.msg.err {
  color: #c0392b;
  background: #fdecea;
  border-color: #f5c6c2;
}
.btn-primary {
  border: none;
  background: linear-gradient(135deg, #f7dd9a, #e0b455);
  color: #3a2a08;
  font-size: 13px;
  font-weight: 800;
  padding: 9px 16px;
  border-radius: 9px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(224, 180, 85, 0.3);
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-ghost {
  border: 1px solid #dfe5df;
  background: #fff;
  color: #5f7066;
  font-size: 13px;
  font-weight: 700;
  padding: 9px 14px;
  border-radius: 9px;
  cursor: pointer;
}
.btn-ghost:hover {
  border-color: #c3cec5;
}

/* ================= 表格 ================= */
.table-card {
  background: #fff;
  border: 1px solid #e6ebe6;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(30, 50, 38, 0.05);
}
.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.tbl thead th {
  background: #f6f9f7;
  color: #6b7c71;
  font-weight: 700;
  text-align: left;
  padding: 11px 12px;
  border-bottom: 1px solid #e6ebe6;
  white-space: nowrap;
}
.tbl tbody td {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f4f1;
  color: #3d4f44;
  vertical-align: middle;
}
.tbl-row:hover {
  background: #fafcfb;
}
.tbl-row.dragging {
  opacity: 0.45;
  background: #fff8e8;
}
.tbl-row.off td {
  color: #a9b5ac;
}
.col-drag {
  width: 40px;
  text-align: center;
}
.drag-handle {
  cursor: grab;
  color: #b6c2b9;
  font-size: 14px;
  user-select: none;
}
.drag-handle:active {
  cursor: grabbing;
}
.col-idx {
  width: 42px;
  color: #a9b5ac;
}
.col-switch {
  width: 92px;
  text-align: center;
}
.col-ops {
  width: 130px;
}
.op {
  border: 1px solid #dfe5df;
  background: #fff;
  color: #5f7066;
  font-size: 12px;
  font-weight: 700;
  padding: 5px 11px;
  border-radius: 7px;
  cursor: pointer;
  margin-right: 6px;
}
.op:hover {
  border-color: #e8c46a;
  color: #b5811a;
}
.op.danger:hover {
  border-color: #e08a7a;
  color: #c0392b;
}
.op.ok {
  border-color: #bfe3c8;
  background: #eaf6ee;
  color: #2f7d4f;
}
.op.ok:hover {
  border-color: #3f9c68;
  background: #dff0e5;
}
.dot {
  display: inline-block;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.1);
  vertical-align: middle;
}
.empty {
  text-align: center;
  padding: 34px 0;
  color: #a1aea5;
}

/* 上下架开关 */
.sw {
  position: relative;
  width: 42px;
  height: 22px;
  border: none;
  border-radius: 999px;
  background: #d5ddd7;
  cursor: pointer;
  transition: background 0.18s ease;
  padding: 0;
}
.sw.on {
  background: linear-gradient(135deg, #6fcf97, #3f9c68);
}
.sw-dot {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: transform 0.18s ease;
}
.sw.on .sw-dot {
  transform: translateX(20px);
}

.tip {
  margin: 12px 0 0;
  font-size: 12px;
  color: #8b9a90;
  line-height: 1.7;
}

/* ================= 弹窗 ================= */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(16, 31, 23, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 60;
}
.modal {
  width: 100%;
  max-width: 460px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
  overflow: hidden;
}
.modal-title {
  margin: 0;
  padding: 16px 20px;
  font-size: 15px;
  font-weight: 800;
  color: #24382c;
  border-bottom: 1px solid #eef2ef;
}
.modal-body {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 13px;
  max-height: 56vh;
  overflow-y: auto;
}
.fld {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.fld-inline {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}
.fld-label {
  font-size: 12.5px;
  font-weight: 700;
  color: #5f7066;
}
.fld-input {
  border: 1px solid #dfe5df;
  border-radius: 9px;
  padding: 9px 12px;
  font-size: 13.5px;
  color: #3d4f44;
  outline: none;
  font-family: inherit;
  resize: vertical;
}
.fld-input:focus {
  border-color: #e8c46a;
  box-shadow: 0 0 0 3px rgba(232, 196, 106, 0.15);
}
.fld-color {
  width: 52px;
  height: 34px;
  border: 1px solid #dfe5df;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  padding: 2px;
}
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  background: #fafcfb;
  border-top: 1px solid #eef2ef;
}

@media (max-width: 900px) {
  .side {
    width: 74px;
  }
  .side-name,
  .logo-text,
  .side-foot span {
    display: none;
  }
  .side-logo {
    justify-content: center;
  }
  .side-item {
    justify-content: center;
  }
}

/* ================= 头像（表格 + 编辑弹窗） ================= */
.tbl-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e6ebe6;
  vertical-align: middle;
}
.avatar-edit {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.avatar-edit-prev {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e8c46a;
  flex-shrink: 0;
}
.avatar-edit-opts {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.avatar-edit-opt {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  padding: 0;
  border: 2px solid #dfe5df;
  background: #fffdf8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: #8b9a90;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.avatar-edit-opt img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.avatar-edit-opt.active {
  border-color: #e8c46a;
  box-shadow: 0 0 0 3px rgba(232, 196, 106, 0.25);
}
.avatar-edit-opt.upload {
  position: relative;
}
.avatar-edit-file {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
.avatar-edit-hint {
  width: 100%;
  font-size: 11px;
  color: #8b9a90;
}

/* ================= 图片字段（轮播图） ================= */
.image-edit {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.image-prev {
  width: 100%;
  max-height: 150px;
  object-fit: cover;
  border-radius: 9px;
  border: 1px solid #e6ebe6;
  background: #f6f9f7;
}
.btn-upload {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: flex-start;
  border: 1px dashed #c3cec5;
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 12.5px;
  font-weight: 700;
  color: #5f7066;
  cursor: pointer;
  overflow: hidden;
}
.btn-upload:hover {
  border-color: #e8c46a;
  color: #b5811a;
}

/* ================= 数据看板 ================= */
.dash {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}
.kpi-card {
  background: #fff;
  border: 1px solid #e6ebe6;
  border-radius: 12px;
  padding: 16px 18px;
  box-shadow: 0 2px 10px rgba(30, 50, 38, 0.05);
}
.kpi-top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.kpi-ico {
  font-size: 18px;
}
.kpi-label {
  font-size: 12.5px;
  font-weight: 700;
  color: #7d8c82;
}
.kpi-value {
  margin-top: 10px;
  font-size: 29px;
  font-weight: 800;
  color: #24382c;
  line-height: 1.1;
}
.kpi-value small {
  font-size: 13px;
  font-weight: 700;
  color: #8b9a90;
  margin-left: 4px;
}
.kpi-foot {
  margin-top: 8px;
  font-size: 11.5px;
  color: #9aa79e;
}
.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.chart-card {
  background: #fff;
  border: 1px solid #e6ebe6;
  border-radius: 12px;
  padding: 16px 18px;
  box-shadow: 0 2px 10px rgba(30, 50, 38, 0.05);
  min-width: 0;
}
.chart-card.wide {
  grid-column: span 2;
}
.chart-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 800;
  color: #24382c;
}
.chart-title small {
  font-size: 11.5px;
  font-weight: 600;
  color: #9aa79e;
  margin-left: 6px;
}
.chart-svg {
  width: 100%;
  height: auto;
  display: block;
}
.axis {
  fill: #9aa79e;
  font-size: 11px;
}
.legend {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #7d8c82;
  margin-bottom: 4px;
}
.lg {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 5px;
}
.lg.green { background: #3f9c68; }
.lg.gold { background: #e0a52a; }
.donut-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 0;
}
.donut {
  width: 150px;
  height: 150px;
}
.donut-val {
  fill: #24382c;
  font-size: 25px;
  font-weight: 800;
}
.donut-sub {
  fill: #9aa79e;
  font-size: 10.5px;
}
.chart-note {
  margin: 8px 0 0;
  font-size: 11.5px;
  color: #9aa79e;
  line-height: 1.6;
}
.sales-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
  padding: 6px 0;
}
.sales-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}
.sales-name {
  width: 74px;
  flex-shrink: 0;
  color: #5f7066;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sales-track {
  flex: 1;
  height: 10px;
  border-radius: 999px;
  background: #f0f4f1;
  overflow: hidden;
}
.sales-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #8fc3a5, #3f9c68);
}
.sales-qty {
  width: 32px;
  text-align: right;
  color: #24382c;
}

@media (max-width: 1100px) {
  .kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .chart-grid { grid-template-columns: 1fr; }
  .chart-card.wide { grid-column: span 1; }
}
</style>
