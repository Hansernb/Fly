<template>
  <div class="park">
    <!-- 全屏绿叶飘落 -->
    <div class="leaves" aria-hidden="true">
      <span v-for="l in leaves" :key="l.id" class="leaf" :style="l.style">{{ l.char }}</span>
    </div>

    <!-- 顶部导航栏 -->
    <header class="nav">
      <div class="nav-inner">
        <div class="logo">🌲 狗熊岭森林游乐园</div>

        <!-- 导航菜单（宽屏） -->
        <nav class="nav-menu" aria-label="主导航">
          <a
            v-for="m in menus"
            :key="m.id"
            href="javascript:void(0)"
            class="nav-link"
            :class="{ active: activeMenu === m.id }"
            @click="goSection(m.id)"
            >{{ m.icon }} {{ m.label }}</a
          >
        </nav>

        <!-- 汉堡按钮（窄屏） -->
        <button
          class="nav-burger"
          type="button"
          :aria-expanded="menuOpen"
          aria-label="打开菜单"
          @click="menuOpen = !menuOpen"
        >
          <span></span><span></span><span></span>
        </button>

        <div class="nav-user">
          <img class="nav-avatar" :src="DEFAULT_AVATAR" alt="我的头像" />
          <span class="hello">你好，{{ user.name }}</span>
          <!-- 仅管理员可见：进入后台管理系统 -->
          <button
            v-if="isAdmin"
            class="admin-entry"
            type="button"
            title="进入后台管理系统"
            @click="$emit('admin')"
          >
            ⚙️ 后台管理系统
          </button>
          <button class="logout" type="button" @click="$emit('logout')">🚪 退出</button>
        </div>
      </div>

      <!-- 移动端下拉抽屉 -->
      <div v-if="menuOpen" class="nav-mask" @click="menuOpen = false"></div>
      <transition name="drawer">
        <nav v-if="menuOpen" class="nav-drawer" aria-label="移动端导航">
          <a
            v-for="m in menus"
            :key="m.id"
            href="javascript:void(0)"
            class="drawer-link"
            :class="{ active: activeMenu === m.id }"
            @click="goSection(m.id)"
            >{{ m.icon }} {{ m.label }}</a
          >
        </nav>
      </transition>
    </header>

    <!-- 搜索栏 -->
    <section class="search-wrap">
      <div class="search-card">
        <div class="search-row">
          <div class="search-box">
            <span class="search-ico">🔍</span>
            <input
              v-model.trim="query"
              class="search-input"
              type="text"
              placeholder="搜索游乐项目、栖息地、演出、门票…"
              @keyup.enter="runSearch"
            />
            <button
              v-if="query"
              class="search-clear"
              type="button"
              aria-label="清除"
              @click="clearSearch"
            >
              ✕
            </button>
          </div>
          <button class="search-btn" type="button" @click="runSearch">🐾 搜索</button>
        </div>

        <!-- 快捷分类 -->
        <div class="chips">
          <button
            v-for="m in menus"
            :key="m.id"
            class="chip"
            type="button"
            :class="{ on: activeMenu === m.id }"
            @click="goSection(m.id)"
          >
            {{ m.icon }} {{ m.label }}
          </button>
        </div>
      </div>
    </section>

    <!-- 主视觉横幅 -->
    <section class="hero">
      <span class="hero-blob b1" aria-hidden="true"></span>
      <span class="hero-blob b2" aria-hidden="true"></span>
      <span class="hero-sun" aria-hidden="true">☀️</span>
      <!-- 萤火虫 -->
      <span v-for="f in fireflies" :key="f.id" class="firefly" :style="f.style" aria-hidden="true"></span>
      <div class="hero-text">
        <span class="hero-badge">🌿 国家 5A 级森林主题乐园</span>
        <h1>欢迎来到狗熊岭<br /><em>森林游乐园</em></h1>
        <p>和熊大、熊二一起，开启一整天的森林冒险之旅！</p>
        <div class="hero-tags">
          <span>🎢 6 大主题项目</span>
          <span>🎪 3 场森林演出</span>
          <span>🍯 蜂蜜美食街</span>
        </div>
        <div class="hero-cta">
          <button class="hero-btn" type="button" @click="goSection('attractions')">🐾 开始探索</button>
          <button class="hero-btn ghost" type="button" @click="goSection('tickets')">🎫 查看门票</button>
        </div>
      </div>
      <!-- 原 hero 内的 AI 熊头/树/猫头鹰/松鼠 装饰已移除 -->
    </section>

    <!-- 森林生态实时条 -->
    <section class="eco-bar reveal">
      <div v-for="e in ecoStats" :key="e.label" class="eco-item">
        <span class="eco-emoji">{{ e.emoji }}</span>
        <div>
          <div class="eco-value">{{ e.value }}</div>
          <div class="eco-label">{{ e.label }}</div>
        </div>
      </div>
      <div class="eco-live">
        <span class="live-dot"></span>
        {{ loadedFromApi ? '🗄️ 数据库已连接 · 实时数据' : '示例数据模式' }}
        <span v-if="lastUpdated" class="eco-time">· {{ lastUpdated }}</span>
      </div>
    </section>

    <!-- 数据库告警 / 搜索结果提示 -->
    <div v-if="apiError" class="db-warn">⚠️ {{ apiError }}</div>
    <div v-if="query" class="result-tip">
      “<b>{{ query }}</b>” 的搜索结果 · 共 {{ totalResults }} 条
      <button class="result-clear" type="button" @click="clearSearch">清除筛选</button>
    </div>

    <!-- 游乐项目 -->
    <section id="attractions" class="section reveal">
      <div class="section-head">
        <h2 class="section-title">🎡 热门游乐项目</h2>
        <button class="refresh" type="button" :disabled="loading" @click="fetchAttractions">
          ↻ {{ loading ? '加载中…' : '刷新数据' }}
        </button>
      </div>

      <!-- 加载骨架 -->
      <div v-if="loading && !attractions.length" class="grid">
        <div v-for="n in 6" :key="n" class="ride skeleton"></div>
      </div>

      <div v-else-if="filteredAttractions.length" class="grid">
        <article
          v-for="a in filteredAttractions"
          :key="a.id"
          class="ride"
          :style="{ '--accent': a.color }"
          role="button"
          tabindex="0"
          @click="openDetail(a)"
          @keydown.enter="openDetail(a)"
        >
          <div class="ride-cover">
            <img class="ride-img" :src="a.img || FALLBACK_IMG" :alt="a.name" loading="lazy" />
            <span class="ride-tag">{{ a.tag }}</span>
          </div>
          <div class="ride-body">
            <h3>{{ a.name }}</h3>
            <p>{{ a.desc }}</p>
            <div class="ride-meta">
              <span class="wait" :class="'w' + waitLevel(a.wait)">⏱ 排队约 {{ a.wait }} 分钟</span>
              <div class="ride-rate">
                <StarRating :score="a.rating" :size="15" />
                <span class="rate-num">{{ Number(a.rating || 0).toFixed(1) }}</span>
              </div>
            </div>
            <button class="ride-more" type="button" @click.stop="openDetail(a)">查看详情 ›</button>
          </div>
        </article>
      </div>
      <div v-else class="empty">🍃 没有匹配的游乐项目，换个关键词试试？</div>
    </section>

    <!-- 森林栖息地 -->
    <section id="habitats" class="section reveal">
      <h2 class="section-title">🦉 森林栖息地 · 和小动物们打个招呼</h2>
      <div v-if="filteredHabitats.length" class="grid">
        <article
          v-for="h in filteredHabitats"
          :key="h.name"
          class="habitat"
          :style="{ '--accent': h.color }"
        >
          <div class="habitat-cover">
            <img :src="h.img || FALLBACK_IMG" :alt="h.name" loading="lazy" />
            <span class="habitat-badge">{{ h.emoji }}</span>
          </div>
          <h3>{{ h.name }}</h3>
          <p>{{ h.desc }}</p>
          <div class="habitat-meta">
            <span class="resident">🏡 居民：{{ h.residents }}</span>
            <span class="mood">{{ h.mood }}</span>
          </div>
          <div class="ride-rate">
            <StarRating :score="h.rating" :size="15" />
            <span class="rate-num">{{ h.rating.toFixed(1) }}</span>
          </div>
        </article>
      </div>
      <div v-else class="empty">🍃 没有匹配的栖息地，换个关键词试试？</div>
    </section>

    <!-- 演出时间表 -->
    <section id="shows" class="section reveal">
      <h2 class="section-title">🎪 今日森林演出</h2>
      <div v-if="filteredShows.length" class="shows">
        <div v-for="s in filteredShows" :key="s.time" class="show">
          <div class="show-time">{{ s.time }}</div>
          <div class="show-info">
            <h3>{{ s.emoji }} {{ s.name }}</h3>
            <p>{{ s.place }}</p>
          </div>
          <span class="show-tag">{{ s.tag }}</span>
        </div>
      </div>
      <div v-else class="empty">🍃 没有匹配的演出，换个关键词试试？</div>
    </section>

    <!-- 门票 -->
    <section id="tickets" class="section reveal">
      <h2 class="section-title">🎫 门票速览</h2>
      <div v-if="filteredTickets.length" class="tickets">
        <div
          v-for="t in filteredTickets"
          :key="t.name"
          class="ticket"
          :class="{ hot: t.hot }"
        >
          <div v-if="t.hot" class="hot-badge">🔥 热门</div>
          <h3>{{ t.emoji }} {{ t.name }}</h3>
          <div class="price">¥{{ t.price }}<small>/人</small></div>
          <ul>
            <li v-for="(f, i) in t.features" :key="i">✅ {{ f }}</li>
          </ul>
        </div>
      </div>
      <div v-else class="empty">🍃 没有匹配的门票，换个关键词试试？</div>
    </section>

    <!-- 游乐园风景（游客可上传） -->
    <section id="scenery" class="section reveal">
      <div class="scenery-head">
        <h2 class="section-title">📷 游乐园风景</h2>
        <label class="scenery-upload">
          📸 上传风景照
          <input type="file" accept="image/*" multiple hidden @change="onSceneryUpload" />
        </label>
      </div>
      <p class="scenery-tip">把你眼中最美的狗熊岭分享给大家～游客上传的照片会保存在本机浏览器中（单张 ≤ 4MB）。</p>
      <div class="scenery-grid">
        <figure
          v-for="(s, i) in scenery"
          :key="i"
          class="scenery-card"
          :class="{ mine: s.mine }"
          @click="openLightbox(s)"
        >
          <img :src="s.src" :alt="s.caption" loading="lazy" />
          <figcaption>
            <span class="sc-cap">{{ s.caption }}</span>
            <span v-if="s.mine" class="sc-badge">我上传的</span>
          </figcaption>
        </figure>
      </div>
      <div v-if="!scenery.length" class="empty">🍃 还没有风景照，快来上传第一张吧～</div>
    </section>

    <!-- 风景大图预览 -->
    <transition name="fade">
      <div v-if="lightbox" class="lightbox" @click.self="lightbox = null">
        <button class="lb-close" type="button" @click="lightbox = null">✕</button>
        <img :src="lightbox.src" :alt="lightbox.caption" />
        <p>{{ lightbox.caption }}</p>
      </div>
    </transition>

    <!-- 游客评价 -->
    <section id="reviews" class="section reviews reveal">
      <h2 class="section-title">🗣️ 游客评价</h2>

      <!-- 评分概览 -->
      <div class="rate-overview">
        <div class="rate-big">
          <div class="rate-score">{{ avgRating.toFixed(1) }}</div>
          <StarRating :score="avgRating" :size="22" />
          <div class="rate-count">共 {{ allReviews.length }} 条评价</div>
        </div>
        <div class="rate-bars">
          <div v-for="b in dist" :key="b.star" class="rate-bar">
            <span class="rb-label">{{ b.star }}★</span>
            <span class="rb-track"><span class="rb-fill" :style="{ width: b.pct + '%' }"></span></span>
            <span class="rb-num">{{ b.count }}</span>
          </div>
        </div>
      </div>

      <!-- 评价列表 -->
      <div class="review-list">
        <article v-for="r in allReviews" :key="r.id" class="review">
          <div class="review-avatar">
            <img :src="avatarSrc(r)" alt="头像" loading="lazy" />
          </div>
          <div class="review-body">
            <div class="review-head">
              <span class="review-name">{{ r.name }}</span>
              <StarRating :score="r.rating" :size="15" />
              <span class="review-date">{{ r.date }}</span>
            </div>
            <div class="review-target">🎯 评价对象：{{ r.attraction }}</div>
            <p class="review-text">{{ r.text }}</p>
          </div>
        </article>
        <div v-if="!allReviews.length" class="empty">🍃 还没有任何评价，快来当第一个吧～</div>
      </div>

      <!-- 写评价 -->
      <form class="review-form" @submit.prevent="submitReview">
        <h3>✍️ 写下你的评价</h3>
        <div class="rf-row">
          <span class="rf-label">评价项目</span>
          <select v-model="form.attractionId" class="rf-select">
            <option :value="null" disabled>请选择项目</option>
            <option v-for="a in attractions" :key="a.id" :value="a.id">{{ a.emoji }} {{ a.name }}</option>
          </select>
        </div>
        <div class="rf-row">
          <span class="rf-label">你的评分</span>
          <StarRating v-model="form.rating" :editable="true" :size="24" />
          <span class="rf-hint">{{ form.rating ? form.rating + ' 星' : '点击星星评分' }}</span>
        </div>
        <textarea
          v-model.trim="form.text"
          class="rf-text"
          rows="3"
          maxlength="200"
          placeholder="分享你在狗熊岭的游玩体验吧～"
        ></textarea>
        <div class="rf-foot">
          <span class="rf-as">以 <b>{{ user.name }}</b> 的身份发布</span>
          <button class="rf-submit" type="submit" :disabled="submitting || !form.rating || !form.text || !form.attractionId">
            🐾 发布评价
          </button>
        </div>
      </form>
    </section>

    <!-- 林缘树线 -->
    <div class="tree-line" aria-hidden="true">
      <span
        v-for="(t, i) in treeLine"
        :key="i"
        :style="{ animationDelay: i * 0.25 + 's', fontSize: t.size }"
        >{{ t.char }}</span
      >
    </div>

    <footer class="footer">
      🌲 狗熊岭森林游乐园 · 和熊大熊二一起守护森林 · 营业时间 09:00 - 21:00
    </footer>

    <!-- ============ 项目详情弹窗（点进项目看详情） ============ -->
    <Teleport to="body">
      <transition name="modal">
        <div v-if="detail" class="modal-mask" @click.self="closeDetail">
          <div class="modal" :style="{ '--accent': detail.color }">
            <button class="modal-close" type="button" aria-label="关闭" @click="closeDetail">✕</button>

            <div class="modal-hero">
              <div class="modal-emoji">
                <img :src="detail.img || FALLBACK_IMG" :alt="detail.name" />
              </div>
              <div class="modal-info">
                <h2>{{ detail.name }}</h2>
                <div class="modal-tags">
                  <span class="m-tag">{{ detail.tag }}</span>
                  <span class="m-wait">⏱ 排队约 {{ detail.wait }} 分钟</span>
                </div>
                <div class="modal-rate">
                  <StarRating :score="detail.rating" :size="20" />
                  <span class="m-rate-num">{{ Number(detail.rating || 0).toFixed(1) }}</span>
                  <span class="m-rate-count">{{ detail.reviews.length }} 条评价</span>
                </div>
              </div>
            </div>

            <p class="modal-desc">{{ detail.desc }}</p>

            <div class="modal-section-title">🗣️ 项目评价（{{ detail.reviews.length }}）</div>
            <div class="modal-reviews">
              <article v-for="r in detail.reviews" :key="r.id" class="m-review">
                <div class="m-avatar"><img :src="avatarSrc(r)" alt="头像" loading="lazy" /></div>
                <div class="m-body">
                  <div class="m-head">
                    <span class="m-name">{{ r.name }}</span>
                    <StarRating :score="r.rating" :size="14" />
                    <span class="m-date">{{ r.date }}</span>
                  </div>
                  <p class="m-text">{{ r.text }}</p>
                </div>
              </article>
              <div v-if="!detail.reviews.length" class="m-empty">还没有评价，来当第一个吧～</div>
            </div>

            <form class="modal-form" @submit.prevent="submitDetailReview">
              <div class="mf-row">
                <span class="mf-label">你的评分</span>
                <StarRating v-model="detailForm.rating" :editable="true" :size="26" />
                <span class="mf-hint">{{ detailForm.rating ? detailForm.rating + ' 星' : '点击星星' }}</span>
              </div>
              <textarea
                id="detailReviewText"
                v-model.trim="detailForm.text"
                class="mf-text"
                rows="2"
                maxlength="200"
                placeholder="分享你在这个项目的体验…"
              ></textarea>
              <div class="mf-foot">
                <span class="mf-as">以 <b>{{ user.name }}</b> 身份发布</span>
                <button class="mf-submit" type="submit" :disabled="detailSubmitting">
                  {{ detailSubmitting ? '发布中…' : '🐾 发布评价' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </transition>

      <transition name="toast">
        <div v-if="toast" class="toast">{{ toast }}</div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import StarRating from './StarRating.vue'
import { attractionsDB } from '../data/attractions.js'

const props = defineProps({
  user: { type: Object, required: true },
  token: { type: String, required: true }
})
const emit = defineEmits(['logout', 'admin'])

/* 仅管理员（permission === 0）才显示「后台管理系统」入口 */
const isAdmin = computed(() => Number(props.user?.permission) === 0)

/* ===================== 图片资源（public/img/*） =====================
   图片按文件名语义一一对应：
     background.png 背景 / coaster.gif 过山车 / ferris.jpeg 摩天轮
     train.jpeg 火车 / rafting.jpeg 漂流 / maze.jpeg 迷宫
     waterfall.jpg 瀑布 / owl.jpg 猫头鹰 / squirrel.jpg 松鼠
     deer.jpg 鹿 / rabbit.jpg 兔子 / bee.jpeg 蜜蜂 / hedgehog.jpeg 刺猬
     avatar.jpg 头像
*/
const DEFAULT_AVATAR = '/img/avatar.jpg' // 默认用户头像
const FALLBACK_IMG = '/img/background.png' // 项目/栖息地缺省配图

// 评价人 emoji 头像 → 真实照片（命中不到则用默认头像图）
const AVATAR_IMG = {
  '🐰': '/img/rabbit.jpg',
  '🐇': '/img/rabbit.jpg',
  '🐿️': '/img/squirrel.jpg',
  '🦌': '/img/deer.jpg',
  '🦉': '/img/owl.jpg',
  '🐝': '/img/bee.jpeg',
  '🦔': '/img/hedgehog.jpeg'
}

// 评价头像：后端已存图片路径则直接用，否则按 emoji 映射，最后兜底默认头像
function avatarSrc(r) {
  const a = r && (r.avatarImg || r.avatar)
  if (typeof a === 'string' && a.startsWith('/')) return a
  return AVATAR_IMG[a] || DEFAULT_AVATAR
}

/* ===================== 数据：从数据库（Spring Boot + MySQL）加载 ===================== */
const attractions = ref([])
const loading = ref(false)
const apiError = ref('')
const loadedFromApi = ref(false)
const lastUpdated = ref('')

async function fetchAttractions() {
  loading.value = true
  apiError.value = ''
  try {
    const res = await fetch('/api/attractions', {
      headers: { Authorization: 'Bearer ' + props.token }
    })
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const json = await res.json()
    const list = json && Array.isArray(json.data) ? json.data : null
    if (!list) throw new Error('数据格式异常')
    attractions.value = list.map((a) => ({
      id: a.id,
      emoji: a.emoji,
      img: a.img || (attractionsDB.find((x) => x.id === a.id) || {}).img || FALLBACK_IMG,
      name: a.name,
      desc: a.desc || a.description || '',
      tag: a.tag,
      wait: a.wait != null ? a.wait : a.waitMinutes != null ? a.waitMinutes : 0,
      color: a.color || '#2f7d4f',
      rating: a.rating || 0,
      reviews: Array.isArray(a.reviews) ? a.reviews.map((r) => ({ ...r })) : []
    }))
    loadedFromApi.value = true
    lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch (e) {
    // 后端 / 数据库不可达时降级为内置示例数据，保证页面始终有内容可看
    attractions.value = attractionsDB.map((a) => ({ ...a, reviews: [...(a.reviews || [])] }))
    loadedFromApi.value = false
    apiError.value = '数据库暂时连不上，已切换为示例数据（请先启动后端与 MySQL）'
  } finally {
    loading.value = false
  }
}

/* ===================== 聚合评价 ===================== */
const allReviews = computed(() => {
  const out = []
  for (const a of attractions.value) {
    for (const r of a.reviews || []) {
      out.push({ ...r, attraction: a.name, attractionId: a.id })
    }
  }
  return out.sort((x, y) => String(y.date || '').localeCompare(String(x.date || '')))
})

const avgRating = computed(() => {
  if (!allReviews.value.length) return 0
  return (
    allReviews.value.reduce((s, r) => s + Number(r.rating || 0), 0) / allReviews.value.length
  )
})

const dist = computed(() => {
  const arr = []
  for (let star = 5; star >= 1; star--) {
    const count = allReviews.value.filter((r) => Number(r.rating) === star).length
    const pct = allReviews.value.length ? Math.round((count / allReviews.value.length) * 100) : 0
    arr.push({ star, count, pct })
  }
  return arr
})

function recalc(a) {
  if (!a.reviews || !a.reviews.length) {
    a.rating = 0
    return
  }
  const s = a.reviews.reduce((x, r) => x + Number(r.rating || 0), 0)
  a.rating = Math.round((s / a.reviews.length) * 10) / 10
}

// 排队时长分级：1 短 / 2 中 / 3 长（用于颜色提示）
function waitLevel(w) {
  w = Number(w || 0)
  if (w <= 15) return 1
  if (w <= 35) return 2
  return 3
}

/* ===================== 提交评价（写入数据库） ===================== */
async function postReview(payload) {
  const res = await fetch('/api/reviews', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + props.token },
    body: JSON.stringify(payload)
  })
  if (!res.ok) throw new Error('提交失败')
  const json = await res.json()
  return (
    json.review || {
      id: Date.now(),
      name: payload.name,
      avatar: payload.avatar,
      rating: payload.rating,
      date: new Date().toISOString().slice(0, 10),
      text: payload.content
    }
  )
}

// 底部「游客评价」表单
const form = reactive({ rating: 0, text: '', attractionId: null })
const submitting = ref(false)
async function submitReview() {
  if (!form.rating || !form.text || !form.attractionId) return
  submitting.value = true
  try {
    const rev = await postReview({
      attractionId: Number(form.attractionId),
      name: props.user.name,
      avatar: DEFAULT_AVATAR,
      rating: Number(form.rating),
      content: form.text
    })
    const target = attractions.value.find((a) => a.id === Number(form.attractionId))
    if (target) {
      target.reviews.unshift({ ...rev, attraction: target.name, attractionId: target.id })
      recalc(target)
    }
    form.rating = 0
    form.text = ''
    form.attractionId = null
    showToast('🐾 评价发布成功！')
  } catch (e) {
    showToast('😢 评价发布失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}

/* ===================== 项目详情弹窗 ===================== */
const detail = ref(null)
const detailForm = reactive({ rating: 0, text: '' })
const detailSubmitting = ref(false)

function openDetail(a) {
  detail.value = a
  detailForm.rating = 0
  detailForm.text = ''
  document.body.style.overflow = 'hidden'
  nextTick(() => {
    const el = document.getElementById('detailReviewText')
    if (el) el.focus()
  })
}
function closeDetail() {
  detail.value = null
  document.body.style.overflow = ''
}
async function submitDetailReview() {
  if (!detail.value) return
  if (!detailForm.rating || !detailForm.text) {
    showToast('请先选择评分并填写内容')
    return
  }
  detailSubmitting.value = true
  try {
    const rev = await postReview({
      attractionId: detail.value.id,
      name: props.user.name,
      avatar: DEFAULT_AVATAR,
      rating: Number(detailForm.rating),
      content: detailForm.text
    })
    detail.value.reviews.unshift({ ...rev, attraction: detail.value.name, attractionId: detail.value.id })
    recalc(detail.value)
    detailForm.rating = 0
    detailForm.text = ''
    showToast('🐾 评价发布成功！')
  } catch (e) {
    showToast('😢 评价发布失败，请稍后再试')
  } finally {
    detailSubmitting.value = false
  }
}
function onKeydown(e) {
  if (e.key === 'Escape' && detail.value) closeDetail()
}

/* ===================== 轻提示 ===================== */
const toast = ref('')
let toastTimer = null
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 3000)
}

/* ===================== 生命周期 / 导航 / 搜索（保留原逻辑） ===================== */
onMounted(async () => {
  try {
    const res = await fetch('/api/me', {
      headers: { Authorization: 'Bearer ' + props.token }
    })
    if (!res.ok) {
      localStorage.removeItem('boonie_token')
      emit('logout')
      return
    }
  } catch {
    // 后端不可达时保持页面展示
  }
  await fetchAttractions()
  setupScrollSpy()
  setupReveal()
  loadScenery()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  if (spyObserver) spyObserver.disconnect()
  if (revealObserver) revealObserver.disconnect()
  window.removeEventListener('keydown', onKeydown)
  clearTimeout(toastTimer)
  document.body.style.overflow = ''
})

// 滚动联动：导航栏随当前可见区块自动高亮（scroll-spy）
let spyObserver = null
function setupScrollSpy() {
  const ids = menus.map((m) => m.id)
  spyObserver = new IntersectionObserver(
    (entries) => {
      const visible = entries.filter((e) => e.isIntersecting)
      if (!visible.length) return
      const topmost = visible.reduce((a, b) =>
        b.boundingClientRect.top < a.boundingClientRect.top ? b : a
      )
      activeMenu.value = topmost.target.id
    },
    { rootMargin: '-80px 0px -55% 0px', threshold: 0 }
  )
  ids.forEach((id) => {
    const el = document.getElementById(id)
    if (el) spyObserver.observe(el)
  })
}

// 滚动渐显：区块进入视口时淡入上浮（.reveal -> .is-visible）
let revealObserver = null
function setupReveal() {
  const els = document.querySelectorAll('.reveal')
  if (!('IntersectionObserver' in window)) {
    els.forEach((el) => el.classList.add('is-visible'))
    return
  }
  try {
    revealObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            e.target.classList.add('is-visible')
            revealObserver.unobserve(e.target)
          }
        })
      },
      { threshold: 0.12, rootMargin: '0px 0px -8% 0px' }
    )
    els.forEach((el) => revealObserver.observe(el))
  } catch {
    els.forEach((el) => el.classList.add('is-visible'))
  }
}

// 导航菜单（锚点滚动）
const menus = [
  { id: 'attractions', label: '游乐项目', icon: '🎡' },
  { id: 'habitats', label: '栖息地', icon: '🦉' },
  { id: 'shows', label: '演出', icon: '🎪' },
  { id: 'tickets', label: '门票', icon: '🎫' },
  { id: 'scenery', label: '风景', icon: '📷' },
  { id: 'reviews', label: '评价', icon: '🗣️' }
]
const activeMenu = ref('attractions')
const menuOpen = ref(false)

function goSection(id) {
  activeMenu.value = id
  menuOpen.value = false
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// ---- 搜索（前端过滤） ----
const query = ref('')

const ecoStats = [
  { emoji: '🌲', value: '92%', label: '森林覆盖率' },
  { emoji: '🦌', value: '36 种', label: '栖息动物' },
  { emoji: '🍃', value: '5800个/cm³', label: '负氧离子' },
  { emoji: '🌡️', value: '22°C', label: '林间温度' }
]

const habitats = [
  { emoji: '🦉', img: '/img/owl.jpg', name: '猫头鹰树屋', desc: '百年老橡树上的圆圆树洞，夜幕降临后这里最热闹。', residents: '猫头鹰一家 · 夜间巡逻员', mood: '😴 白天打盹中', color: '#7c5cbf', rating: 4.9 },
  { emoji: '🐿️', img: '/img/squirrel.jpg', name: '松鼠松果谷', desc: '满地松果的山谷，小松鼠们正忙着囤积冬天的口粮。', residents: '蹦蹦和它的表兄弟们', mood: '🌰 囤粮大作战', color: '#a9743c', rating: 4.7 },
  { emoji: '🦌', img: '/img/deer.jpg', name: '小鹿溪谷', desc: '清澈溪流穿谷而过，清晨常能遇见来喝水的小鹿。', residents: '鹿妈妈与两只小鹿', mood: '💧 溪边饮水', color: '#2a9d8f', rating: 4.8 },
  { emoji: '🐇', img: '/img/rabbit.jpg', name: '兔子草甸', desc: '开满野花的柔软草甸，是兔子们赛跑的天然运动场。', residents: '萝卜头田径队', mood: '🏃 晨练进行中', color: '#4caf6d', rating: 4.6 },
  { emoji: '🐝', img: '/img/bee.jpeg', name: '蜜蜂花园', desc: '蜂蜜美食街的秘密源头，万花丛中嗡嗡作响。', residents: '熊二的蜂蜜供应商', mood: '🍯 采蜜忙碌中', color: '#ffb703', rating: 4.9 },
  { emoji: '🦔', img: '/img/hedgehog.jpeg', name: '刺猬灌木丛', desc: '浆果灌木深处的小窝，缩成球的小刺猬最可爱。', residents: '团团 & 圆圆', mood: '🍓 浆果品鉴会', color: '#e4572e', rating: 4.7 }
]

const shows = [
  { time: '10:30', emoji: '🎭', name: '熊出没剧场', place: '中央大舞台 · 熊大熊二爆笑登场', tag: '约 40 分钟' },
  { time: '14:00', emoji: '🦜', name: '百鸟森林音乐会', place: '鸟语林剧场 · 涂涂领衔主唱', tag: '约 30 分钟' },
  { time: '19:30', emoji: '🎆', name: '森林星光巡游', place: '主干道 · 花车 + 烟花压轴大秀', tag: '约 50 分钟' }
]

const tickets = [
  { emoji: '🎫', name: '成人票', price: 199, hot: false, features: ['全天畅玩所有项目', '含 1 场剧场演出'] },
  { emoji: '👨‍👩‍👧', name: '亲子套票', price: 399, hot: true, features: ['2 大 1 小全天畅玩', '含全部演出', '赠蜂蜜爆米花一份'] },
  { emoji: '🌙', name: '夜场票', price: 99, hot: false, features: ['17:00 后入园', '含星光巡游观赏位'] }
]

/* ===================== 游乐园风景图廊（游客可上传） ===================== */
const LS_SCENERY = 'boonie_scenery_uploads'
const scenerySeeds = [
  { src: '/scenery/scenery1.png', caption: '森林游乐园大门 🌲' },
  { src: '/scenery/scenery2.png', caption: '林间小径 🍂' },
  { src: '/scenery/scenery3.png', caption: '静谧湖畔的天鹅 🦢' },
  { src: '/scenery/scenery4.png', caption: '树屋与熊大雕像 🐻' },
  { src: '/scenery/scenery5.png', caption: '星空灯光夜 🌟' },
  { src: '/scenery/scenery6.png', caption: '金色秋日蜂蜜街 🍯' }
]
const scenery = ref([])
const lightbox = ref(null)
function openLightbox(s) {
  lightbox.value = s
}
function loadScenery() {
  let uploads = []
  try {
    uploads = JSON.parse(localStorage.getItem(LS_SCENERY) || '[]')
  } catch {
    uploads = []
  }
  scenery.value = [...uploads, ...scenerySeeds]
}
function onSceneryUpload(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  const oversize = files.filter((f) => f.size > 4 * 1024 * 1024)
  if (oversize.length) {
    showToast('⚠️ 单张图片请控制在 4MB 以内')
    e.target.value = ''
    return
  }
  let pending = files.length
  const added = []
  files.forEach((file) => {
    const reader = new FileReader()
    reader.onload = () => {
      added.push({
        src: reader.result,
        caption: '游客上传 · ' + (props.user?.name || '游客'),
        mine: true
      })
      pending -= 1
      if (pending === 0) {
        const prev = JSON.parse(localStorage.getItem(LS_SCENERY) || '[]')
        const next = [...added, ...prev]
        let persisted = true
        try {
          localStorage.setItem(LS_SCENERY, JSON.stringify(next))
        } catch {
          persisted = false
        }
        loadScenery()
        if (!persisted) {
          scenery.value = [...added, ...scenery.value]
          showToast('⚠️ 本地存储空间不足，本次仅当前会话显示')
        } else {
          showToast(`📷 成功上传 ${added.length} 张风景照！`)
        }
      }
    }
    reader.readAsDataURL(file)
  })
  e.target.value = ''
}

function match(text) {
  return text.toLowerCase().includes(query.value.toLowerCase())
}

const filteredAttractions = computed(() =>
  query.value
    ? attractions.value.filter((a) => match(a.name) || match(a.desc) || match(a.tag))
    : attractions.value
)
const filteredHabitats = computed(() =>
  query.value ? habitats.filter((h) => match(h.name) || match(h.desc) || match(h.residents)) : habitats
)
const filteredShows = computed(() =>
  query.value ? shows.filter((s) => match(s.name) || match(s.place) || match(s.tag)) : shows
)
const filteredTickets = computed(() =>
  query.value ? tickets.filter((t) => match(t.name) || t.features.some(match)) : tickets
)

const totalResults = computed(
  () =>
    filteredAttractions.value.length +
    filteredHabitats.value.length +
    filteredShows.value.length +
    filteredTickets.value.length
)

function runSearch() {
  if (query.value) activeMenu.value = 'attractions'
}
function clearSearch() {
  query.value = ''
}

// ---- 飘落的绿叶 ----
const leafChars = ['🍃', '🌿', '🍂', '🍀']
const leaves = Array.from({ length: 18 }, (_, i) => ({
  id: i,
  char: leafChars[i % leafChars.length],
  style: {
    left: Math.random() * 100 + '%',
    fontSize: 14 + Math.random() * 16 + 'px',
    animationDuration: 9 + Math.random() * 9 + 's',
    animationDelay: Math.random() * 12 + 's',
    '--sway': 30 + Math.random() * 70 + 'px'
  }
}))

// ---- 萤火虫 ----
const fireflies = Array.from({ length: 10 }, (_, i) => ({
  id: i,
  style: {
    left: 5 + Math.random() * 90 + '%',
    top: 10 + Math.random() * 80 + '%',
    animationDuration: 4 + Math.random() * 4 + 's',
    animationDelay: Math.random() * 4 + 's'
  }
}))

const treeLine = Array.from({ length: 14 }, (_, i) => ({
  char: i % 3 === 1 ? '🌳' : '🌲',
  size: 26 + Math.random() * 18 + 'px'
}))
</script>

<style scoped>
.park {
  min-height: 100vh;
  max-width: 1280px;
  margin: 0 auto;
}

/* ===== 美化增强：滚动渐显 + 卡片入场 ===== */
.reveal {
  opacity: 0;
  transform: translateY(30px);
  transition: opacity 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: opacity, transform;
}
.reveal.is-visible {
  opacity: 1;
  transform: none;
}
.reveal.is-visible .grid > *,
.reveal.is-visible .shows > *,
.reveal.is-visible .tickets > *,
.reveal.is-visible .review-list > * {
  animation: cardPop 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.reveal.is-visible .grid > *:nth-child(2) { animation-delay: 0.06s; }
.reveal.is-visible .grid > *:nth-child(3) { animation-delay: 0.12s; }
.reveal.is-visible .grid > *:nth-child(4) { animation-delay: 0.18s; }
.reveal.is-visible .grid > *:nth-child(5) { animation-delay: 0.24s; }
.reveal.is-visible .grid > *:nth-child(6) { animation-delay: 0.30s; }
.reveal.is-visible .grid > *:nth-child(7) { animation-delay: 0.36s; }
.reveal.is-visible .grid > *:nth-child(8) { animation-delay: 0.42s; }
@keyframes cardPop {
  from { opacity: 0; transform: translateY(18px) scale(0.98); }
  to { opacity: 1; transform: none; }
}

/* ---- 全屏绿叶飘落 ---- */
.leaves {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 40;
}
.leaf {
  position: absolute;
  top: -40px;
  opacity: 0;
  animation: leafFall linear infinite;
  filter: drop-shadow(0 2px 2px rgba(47, 125, 79, 0.2));
}
@keyframes leafFall {
  0% {
    transform: translate3d(0, -40px, 0) rotate(0deg);
    opacity: 0;
  }
  8% {
    opacity: 0.95;
  }
  25% {
    transform: translate3d(var(--sway, 50px), 25vh, 0) rotate(120deg);
  }
  50% {
    transform: translate3d(calc(var(--sway, 50px) * -0.6), 52vh, 0) rotate(200deg);
  }
  75% {
    transform: translate3d(var(--sway, 50px), 78vh, 0) rotate(300deg);
  }
  100% {
    transform: translate3d(calc(var(--sway, 50px) * -0.4), 108vh, 0) rotate(380deg);
    opacity: 0.5;
  }
}

/* 顶部导航栏 */
.nav {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(255, 248, 236, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 2px solid var(--wood-light);
}
.nav-inner {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 12px 24px;
}
.logo {
  font-size: 18px;
  font-weight: 800;
  color: var(--forest-deep);
  white-space: nowrap;
}
.nav-menu {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
}
.nav-link {
  border: 2px solid transparent;
  background: transparent;
  color: var(--muted);
  font-size: 14px;
  font-weight: 700;
  padding: 7px 14px;
  border-radius: 999px;
  cursor: pointer;
  text-decoration: none;
  white-space: nowrap;
  transition: all 0.15s ease;
}
.nav-link:hover {
  color: var(--forest-deep);
  background: rgba(47, 125, 79, 0.1);
  border-color: var(--leaf);
}
.nav-link.active {
  color: #5a3d00;
  background: linear-gradient(180deg, #ffc83d 0%, var(--honey) 100%);
  border-color: var(--honey);
  box-shadow: 0 3px 0 #e09b00;
}

/* 汉堡按钮（窄屏显示） */
.nav-burger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 42px;
  height: 38px;
  padding: 0 9px;
  border: 2px solid var(--wood-light);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
}
.nav-burger span {
  display: block;
  height: 3px;
  border-radius: 2px;
  background: var(--forest-deep);
  transition: all 0.2s ease;
}
.nav-burger[aria-expanded="true"] span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}
.nav-burger[aria-expanded="true"] span:nth-child(2) {
  opacity: 0;
}
.nav-burger[aria-expanded="true"] span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

/* 移动端遮罩 + 下拉抽屉 */
.nav-mask {
  position: fixed;
  inset: 58px 0 0 0;
  background: rgba(20, 40, 25, 0.28);
  z-index: 40;
}
.nav-drawer {
  display: none;
  position: absolute;
  top: 100%;
  right: 12px;
  left: 12px;
  margin-top: 8px;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
  background: #fffaf0;
  border: 2px solid var(--wood-light);
  border-radius: 18px;
  box-shadow: 0 14px 30px rgba(40, 70, 45, 0.18);
  z-index: 60;
}
.drawer-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 700;
  color: var(--muted);
  text-decoration: none;
  transition: all 0.15s ease;
}
.drawer-link:hover {
  background: rgba(47, 125, 79, 0.1);
  color: var(--forest-deep);
}
.drawer-link.active {
  color: #5a3d00;
  background: linear-gradient(180deg, #ffc83d 0%, var(--honey) 100%);
  box-shadow: 0 3px 0 #e09b00;
}
.drawer-enter-active,
.drawer-leave-active {
  transition: all 0.2s ease;
}
.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}
/* 导航栏用户头像（public/img/avatar.jpg） */
.nav-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--wood-light);
  background: #f2f9ef;
  flex: 0 0 34px;
}
.hello {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}
.logout {
  border: 2px solid var(--wood-light);
  background: #fff;
  color: var(--forest-deep);
  font-size: 13px;
  font-weight: 700;
  padding: 8px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.logout:hover {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
}

/* 后台管理系统入口按钮（仅管理员 permission===0 显示） */
.admin-entry {
  border: 2px solid var(--forest);
  background: var(--forest);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  padding: 8px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow: 0 4px 12px rgba(47, 125, 79, 0.25);
}
.admin-entry:hover {
  background: var(--forest-deep);
  border-color: var(--forest-deep);
  transform: translateY(-1px);
}

/* 搜索栏 */
.search-wrap {
  padding: 20px 24px 0;
}
.search-card {
  background: #fff;
  border: 3px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 18px 20px;
  box-shadow: 0 12px 32px rgba(47, 125, 79, 0.12);
}
.search-row {
  display: flex;
  gap: 12px;
  align-items: stretch;
}
.search-box {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
}
.search-ico {
  position: absolute;
  left: 16px;
  font-size: 18px;
  pointer-events: none;
}
.search-input {
  width: 100%;
  height: 50px;
  padding: 0 44px 0 46px;
  font-size: 15px;
  background: #fffdf8;
  color: var(--text);
  border: 2px solid var(--wood-light);
  border-radius: 14px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.search-input:focus {
  outline: none;
  border-color: var(--forest);
  box-shadow: 0 0 0 3px rgba(47, 125, 79, 0.18);
  transform: translateY(-1px);
}
.search-clear {
  position: absolute;
  right: 12px;
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 50%;
  background: #f0e6d6;
  color: var(--muted);
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}
.search-clear:hover {
  background: var(--danger);
  color: #fff;
}
.search-btn {
  height: 50px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffc83d 0%, var(--honey) 100%);
  color: #5a3d00;
  font-size: 15px;
  font-weight: 800;
  padding: 0 26px;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 5px 0 #e09b00;
  transition: transform 0.08s ease, box-shadow 0.08s ease, filter 0.15s ease;
}
.search-btn:hover {
  filter: brightness(1.03);
}
.search-btn:active {
  transform: translateY(4px);
  box-shadow: 0 1px 0 #e09b00;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
.chip {
  border: 2px solid var(--wood-light);
  background: #fffdf8;
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.chip:hover {
  border-color: var(--forest);
  color: var(--forest-deep);
}
.chip.on {
  background: rgba(47, 125, 79, 0.1);
  border-color: var(--forest);
  color: var(--forest-deep);
}

/* 主视觉 */
.hero {
  margin: 20px 24px 0;
  padding: 64px 44px 96px;
  border-radius: calc(var(--radius) + 8px);
  /* 背景图：public/img/background.png（森林实景主视觉） */
  background:
    radial-gradient(circle at 85% 18%, rgba(255, 214, 102, 0.22) 0, transparent 42%),
    linear-gradient(115deg, rgba(20, 50, 30, 0.78) 0%, rgba(31, 92, 57, 0.5) 48%, rgba(76, 175, 109, 0.3) 100%),
    url("/img/background.png") center center / cover no-repeat,
    linear-gradient(125deg, var(--forest-deep) 0%, var(--forest) 55%, var(--leaf) 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
  box-shadow: 0 24px 60px -20px rgba(31, 92, 57, 0.6);
  animation: cardIn 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 装饰光斑 */
.hero-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.5;
  pointer-events: none;
  animation: blobMove 12s ease-in-out infinite;
}
.hero-blob.b1 {
  width: 220px;
  height: 220px;
  right: -40px;
  top: -60px;
  background: radial-gradient(circle, rgba(255, 214, 102, 0.8), transparent 70%);
}
.hero-blob.b2 {
  width: 260px;
  height: 260px;
  left: -70px;
  bottom: -90px;
  background: radial-gradient(circle, rgba(110, 224, 156, 0.7), transparent 70%);
  animation-delay: -6s;
}
@keyframes blobMove {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, 24px) scale(1.08); }
}
.hero-sun {
  position: absolute;
  right: 40px;
  top: 36px;
  font-size: 60px;
  filter: drop-shadow(0 0 22px rgba(255, 214, 102, 0.85));
  animation: sunPulse 5s ease-in-out infinite;
  pointer-events: none;
}
@keyframes sunPulse {
  0%, 100% { transform: scale(1) rotate(0); opacity: 0.9; }
  50% { transform: scale(1.12) rotate(12deg); opacity: 1; }
}

.hero-text {
  position: relative;
  z-index: 2;
  max-width: 640px;
}
.hero-badge {
  display: inline-block;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(4px);
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 18px;
}
.hero h1 {
  font-size: 42px;
  line-height: 1.18;
  font-weight: 900;
  margin-bottom: 14px;
  text-shadow: 0 4px 18px rgba(0, 0, 0, 0.25);
}
.hero h1 em {
  font-style: normal;
  background: linear-gradient(90deg, #fff3c4, #ffd23f);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.hero p {
  font-size: 17px;
  opacity: 0.94;
  margin-bottom: 22px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}
.hero-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 26px;
}
.hero-tags span {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}
.hero-cta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.hero-btn {
  border: none;
  border-radius: 999px;
  background: linear-gradient(180deg, #ffd23f, var(--honey));
  color: #5a3d00;
  font-size: 15px;
  font-weight: 800;
  padding: 12px 26px;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
  transition: transform 0.12s ease, box-shadow 0.12s ease, filter 0.15s ease;
}
.hero-btn:hover {
  filter: brightness(1.05);
  transform: translateY(-2px);
}
.hero-btn:active {
  transform: translateY(1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
.hero-btn.ghost {
  background: rgba(255, 255, 255, 0.14);
  border: 1.5px solid rgba(255, 255, 255, 0.55);
  color: #fff;
  box-shadow: none;
}
.hero-btn.ghost:hover {
  background: rgba(255, 255, 255, 0.24);
}

/* 萤火虫 */
.firefly {
  position: absolute;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ffe27a;
  box-shadow: 0 0 12px 4px rgba(255, 226, 122, 0.75);
  animation: fly ease-in-out infinite;
  pointer-events: none;
}
@keyframes fly {
  0%, 100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.2;
  }
  25% {
    transform: translate(18px, -22px) scale(1.2);
    opacity: 1;
  }
  50% {
    transform: translate(-14px, -38px) scale(0.9);
    opacity: 0.5;
  }
  75% {
    transform: translate(22px, -12px) scale(1.1);
    opacity: 0.9;
  }
}

/* 森林生态实时条 */
.eco-bar {
  margin: 18px 24px 0;
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 32px;
  flex-wrap: wrap;
}
.eco-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.eco-emoji {
  font-size: 26px;
  animation: bobY 2.6s ease-in-out infinite;
}
@keyframes bobY {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}
.eco-value {
  font-size: 17px;
  font-weight: 900;
  color: var(--forest-deep);
}
.eco-label {
  font-size: 12px;
  color: var(--muted);
}
.eco-live {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--forest);
}
.eco-time {
  color: var(--muted);
  font-weight: 600;
}
.live-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--leaf);
  animation: pulse 1.6s ease-out infinite;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(76, 175, 109, 0.6); }
  70% { box-shadow: 0 0 0 9px rgba(76, 175, 109, 0); }
  100% { box-shadow: 0 0 0 0 rgba(76, 175, 109, 0); }
}

/* 搜索结果提示 / 数据库告警 */
.result-tip {
  margin: 18px 24px 0;
  background: rgba(47, 125, 79, 0.08);
  border: 1px dashed var(--leaf);
  border-radius: 12px;
  padding: 10px 16px;
  font-size: 14px;
  color: var(--forest-deep);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.result-clear {
  margin-left: auto;
  border: none;
  background: var(--forest);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 5px 12px;
  border-radius: 999px;
  cursor: pointer;
}
.db-warn {
  margin: 16px 24px 0;
  background: rgba(228, 87, 46, 0.1);
  border: 1px dashed var(--danger);
  color: var(--danger);
  border-radius: 12px;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
}

/* 通用区块 */
.section {
  padding: 40px 24px 0;
}
.section-title {
  font-size: 23px;
  font-weight: 900;
  color: var(--forest-deep);
  margin-bottom: 22px;
  scroll-margin-top: 80px;
  position: relative;
  padding-left: 18px;
}
.section-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 22px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--leaf), var(--forest-deep));
  box-shadow: 0 2px 8px rgba(47, 125, 79, 0.35);
}
.section-head .section-title {
  margin-bottom: 0;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.section-head .section-title {
  margin-bottom: 0;
}
.refresh {
  border: 2px solid var(--wood-light);
  background: #fff;
  color: var(--forest-deep);
  font-size: 13px;
  font-weight: 700;
  padding: 8px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.refresh:hover:not(:disabled) {
  background: var(--forest);
  border-color: var(--forest);
  color: #fff;
}
.refresh:disabled {
  opacity: 0.6;
  cursor: default;
}

/* 卡片网格 */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

/* 游乐项目卡片 */
.ride {
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  overflow: hidden;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  box-shadow: 0 6px 18px rgba(47, 125, 79, 0.07);
}
.ride:hover {
  transform: translateY(-8px);
  box-shadow: 0 22px 46px -16px var(--accent, var(--forest));
  border-color: var(--accent, var(--forest));
}
.ride:focus-visible {
  outline: 3px solid var(--forest);
  outline-offset: 2px;
}
.ride-cover {
  position: relative;
  height: 132px;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 30% 25%, rgba(255, 255, 255, 0.35), transparent 55%),
    linear-gradient(135deg, var(--accent, var(--forest)), color-mix(in srgb, var(--accent, var(--forest)) 55%, #1f5c39));
  overflow: hidden;
}
.ride-cover::after {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 90%, rgba(255, 255, 255, 0.18), transparent 50%);
}
/* 项目封面实拍图 */
.ride-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 1;
  transition: transform 0.45s ease;
}
.ride:hover .ride-img {
  transform: scale(1.07);
}
.ride-emoji {
  font-size: 64px;
  line-height: 1;
  filter: drop-shadow(0 6px 10px rgba(0, 0, 0, 0.25));
  transition: transform 0.3s ease;
  z-index: 1;
}
.ride:hover .ride-emoji {
  transform: scale(1.16) rotate(-8deg);
}
.ride-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 2;
  background: rgba(255, 255, 255, 0.92);
  color: var(--accent, var(--forest-deep));
  font-size: 12px;
  font-weight: 800;
  padding: 4px 12px;
  border-radius: 999px;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.12);
}
.ride-body {
  padding: 18px 20px 20px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.ride-body h3 {
  font-size: 18px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 8px;
}
.ride-body p {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.6;
  margin-bottom: 14px;
  flex: 1;
}
.ride-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 14px;
}
.tag {
  background: var(--accent, var(--forest));
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 999px;
}
.wait {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(47, 125, 79, 0.1);
  color: var(--forest-deep);
}
.wait.w2 {
  background: rgba(255, 183, 3, 0.16);
  color: #9a6b00;
}
.wait.w3 {
  background: rgba(228, 87, 46, 0.14);
  color: var(--danger);
}

/* ===== 游乐园风景图廊 ===== */
.scenery-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}
.scenery-head .section-title {
  margin-bottom: 0;
}
.scenery-tip {
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 18px;
}
.scenery-upload {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(180deg, #ffd23f, var(--honey));
  color: #5a3d00;
  font-size: 14px;
  font-weight: 800;
  padding: 10px 18px;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(255, 183, 3, 0.35);
  transition: transform 0.12s ease, filter 0.15s ease;
}
.scenery-upload:hover {
  filter: brightness(1.05);
  transform: translateY(-2px);
}
.scenery-upload:active {
  transform: translateY(1px);
}
.scenery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.scenery-card {
  position: relative;
  margin: 0;
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: 0 8px 22px rgba(47, 125, 79, 0.12);
  background: #fff;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  cursor: zoom-in;
}
.scenery-card:hover {
  transform: translateY(-6px) scale(1.01);
  box-shadow: 0 18px 38px rgba(47, 125, 79, 0.22);
}
.scenery-card img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease;
}
.scenery-card:hover img {
  transform: scale(1.08);
}
.scenery-card figcaption {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: linear-gradient(0deg, rgba(31, 92, 57, 0.85), rgba(31, 92, 57, 0));
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}
.scenery-card .sc-badge {
  background: rgba(255, 255, 255, 0.92);
  color: var(--forest-deep);
  font-size: 11px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 999px;
  white-space: nowrap;
}
.scenery-card.mine {
  outline: 3px solid var(--honey);
  outline-offset: -3px;
}

/* 风景大图预览 */
.lightbox {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: rgba(20, 40, 28, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  gap: 12px;
}
.lightbox img {
  max-width: 90vw;
  max-height: 80vh;
  border-radius: 14px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}
.lightbox p {
  color: #fff;
  font-weight: 700;
  margin: 0;
}
.lb-close {
  position: absolute;
  top: 18px;
  right: 22px;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  font-size: 18px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.9);
  color: #1f5c39;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 栖息地卡片 */
.habitat {
  background: linear-gradient(160deg, #ffffff 0%, #f2f9ef 100%);
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 22px;
  position: relative;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.habitat::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5px;
  background: linear-gradient(90deg, var(--accent, var(--forest)), transparent);
}
.habitat:hover {
  transform: translateY(-6px) rotate(-0.5deg);
  box-shadow: 0 16px 36px rgba(47, 125, 79, 0.18);
}
/* 栖息地动物实拍图 + 右上角 emoji 小徽章 */
.habitat-cover {
  position: relative;
  height: 140px;
  margin: -22px -22px 14px;
  border-radius: var(--radius) var(--radius) 0 0;
  overflow: hidden;
  background: #eaf3e6;
}
.habitat-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.45s ease;
}
.habitat:hover .habitat-cover img {
  transform: scale(1.06);
}
.habitat-badge {
  position: absolute;
  right: 10px;
  bottom: 10px;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
}
.habitat:hover .habitat-badge {
  animation: wiggle 0.5s ease-in-out infinite;
}
@keyframes wiggle {
  0%, 100% { transform: rotate(-8deg) scale(1.15); }
  50% { transform: rotate(8deg) scale(1.15); }
}
.habitat h3 {
  font-size: 17px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 8px;
}
.habitat p {
  font-size: 13px;
  color: var(--muted);
  line-height: 1.6;
  margin-bottom: 14px;
}
.habitat-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}
.resident {
  font-size: 12px;
  color: var(--forest-deep);
  font-weight: 600;
}
.mood {
  font-size: 12px;
  font-weight: 700;
  color: var(--accent, var(--forest));
  background: rgba(47, 125, 79, 0.08);
  padding: 4px 10px;
  border-radius: 999px;
  white-space: nowrap;
}

/* 演出 */
.shows {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.show {
  display: flex;
  align-items: center;
  gap: 18px;
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 16px 22px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.show:hover {
  transform: translateX(6px);
  box-shadow: 0 10px 26px rgba(47, 125, 79, 0.14);
}
.show-time {
  font-size: 20px;
  font-weight: 900;
  color: var(--forest);
  min-width: 64px;
}
.show-info {
  flex: 1;
}
.show-info h3 {
  font-size: 15px;
  font-weight: 800;
  margin-bottom: 4px;
}
.show-info p {
  font-size: 13px;
  color: var(--muted);
}
.show-tag {
  font-size: 12px;
  color: var(--wood);
  font-weight: 700;
  white-space: nowrap;
}

/* 门票 */
.tickets {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 18px;
}
.ticket {
  position: relative;
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 26px 22px;
  text-align: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.ticket:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 36px rgba(47, 125, 79, 0.18);
}
.ticket.hot {
  border-color: var(--honey);
  box-shadow: 0 10px 30px rgba(255, 183, 3, 0.22);
}
.hot-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(180deg, #ffc83d, var(--honey));
  color: #5a3d00;
  font-size: 12px;
  font-weight: 800;
  padding: 4px 14px;
  border-radius: 999px;
  box-shadow: 0 4px 10px rgba(255, 183, 3, 0.4);
}
.ticket h3 {
  font-size: 16px;
  font-weight: 800;
  margin-bottom: 10px;
}
.price {
  font-size: 34px;
  font-weight: 900;
  color: var(--forest-deep);
  margin-bottom: 14px;
}
.price small {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
}
.ticket ul {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.ticket li {
  font-size: 13px;
  color: var(--muted);
}

/* 空状态 */
.empty {
  background: #fff;
  border: 2px dashed var(--wood-light);
  border-radius: var(--radius);
  padding: 40px 20px;
  text-align: center;
  font-size: 15px;
  color: var(--muted);
  font-weight: 600;
}

/* 林缘树线 */
.tree-line {
  margin-top: 44px;
  padding: 0 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  pointer-events: none;
}
.tree-line span {
  display: inline-block;
  transform-origin: bottom center;
  animation: sway 4s ease-in-out infinite;
  filter: drop-shadow(0 3px 4px rgba(47, 125, 79, 0.25));
}

/* 页脚 */
.footer {
  margin-top: 8px;
  padding: 20px 24px 34px;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
  border-top: 2px dashed var(--wood-light);
}

/* 卡片评分行 */
.ride-rate {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
}
.rate-num {
  font-size: 13px;
  font-weight: 700;
  color: var(--wood);
}
.ride-more {
  margin-top: 12px;
  border: none;
  background: rgba(47, 125, 79, 0.08);
  color: var(--forest-deep);
  font-size: 12px;
  font-weight: 700;
  padding: 6px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.ride:hover .ride-more {
  background: var(--forest);
  color: #fff;
}

/* 加载骨架 */
.skeleton {
  min-height: 190px;
  border-top: 5px solid var(--wood-light);
  background: linear-gradient(100deg, #f3ecdf 30%, #fff8ec 50%, #f3ecdf 70%);
  background-size: 200% 100%;
  animation: sk 1.2s infinite;
}
@keyframes sk {
  from { background-position: 200% 0; }
  to { background-position: -200% 0; }
}

/* 游客评价 */
.rate-overview {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 22px 26px;
  margin-bottom: 22px;
}
.rate-big {
  text-align: center;
  min-width: 150px;
  border-right: 2px dashed var(--wood-light);
  padding-right: 24px;
}
.rate-score {
  font-size: 46px;
  font-weight: 900;
  color: var(--forest-deep);
  line-height: 1;
}
.rate-count {
  font-size: 12px;
  color: var(--muted);
  margin-top: 6px;
}
.rate-bars {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 7px;
  min-width: 220px;
}
.rate-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--muted);
}
.rb-label {
  width: 28px;
  text-align: right;
}
.rb-track {
  flex: 1;
  height: 8px;
  background: #f0e6d6;
  border-radius: 999px;
  overflow: hidden;
}
.rb-fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #ffd23f, var(--honey));
  border-radius: 999px;
  transition: width 0.4s ease;
}
.rb-num {
  width: 26px;
  text-align: right;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 22px;
}
.review {
  display: flex;
  gap: 14px;
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 16px 20px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.review:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 26px rgba(47, 125, 79, 0.14);
}
.review-avatar {
  font-size: 30px;
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  background: #f2f9ef;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 2px solid var(--wood-light);
}
.review-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.review-body {
  flex: 1;
}
.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}
.review-name {
  font-weight: 800;
  color: var(--forest-deep);
}
.review-date {
  font-size: 12px;
  color: var(--muted);
  margin-left: auto;
}
.review-text {
  font-size: 14px;
  color: var(--text);
  line-height: 1.7;
}
.review-target {
  font-size: 12px;
  font-weight: 700;
  color: var(--muted);
  margin: 4px 0 6px;
}

.review-form {
  background: linear-gradient(160deg, #ffffff 0%, #f2f9ef 100%);
  border: 2px solid var(--wood-light);
  border-radius: var(--radius);
  padding: 22px 24px;
}
.review-form h3 {
  font-size: 17px;
  font-weight: 800;
  color: var(--forest-deep);
  margin-bottom: 14px;
}
.rf-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.rf-label {
  font-size: 14px;
  font-weight: 700;
  color: var(--forest-deep);
}
.rf-hint {
  font-size: 13px;
  color: var(--muted);
}
.rf-select {
  flex: 1;
  min-width: 180px;
  padding: 9px 12px;
  font-size: 14px;
  font-family: inherit;
  color: var(--text);
  background: #fffdf8;
  border: 2px solid var(--wood-light);
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.rf-select:focus {
  outline: none;
  border-color: var(--forest);
  box-shadow: 0 0 0 3px rgba(47, 125, 79, 0.18);
}
.rf-text {
  width: 100%;
  padding: 12px 14px;
  font-size: 14px;
  font-family: inherit;
  background: #fffdf8;
  color: var(--text);
  border: 2px solid var(--wood-light);
  border-radius: 12px;
  resize: vertical;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.rf-text:focus {
  outline: none;
  border-color: var(--forest);
  box-shadow: 0 0 0 3px rgba(47, 125, 79, 0.18);
}
.rf-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  flex-wrap: wrap;
  gap: 10px;
}
.rf-as {
  font-size: 13px;
  color: var(--muted);
}
.rf-submit {
  border: none;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffc83d 0%, var(--honey) 100%);
  color: #5a3d00;
  font-size: 15px;
  font-weight: 800;
  padding: 10px 22px;
  cursor: pointer;
  box-shadow: 0 4px 0 #e09b00;
  transition: transform 0.08s ease, box-shadow 0.08s ease, filter 0.15s ease;
}
.rf-submit:hover {
  filter: brightness(1.03);
}
.rf-submit:active {
  transform: translateY(3px);
  box-shadow: 0 1px 0 #e09b00;
}
.rf-submit:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: 0 4px 0 #e09b00;
}

/* ============ 项目详情弹窗 ============ */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(20, 40, 25, 0.55);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 40px 16px;
  z-index: 100;
  overflow: auto;
}
.modal {
  width: 100%;
  max-width: 620px;
  background: #fffaf0;
  border: 3px solid var(--wood-light);
  border-radius: 22px;
  padding: 26px 26px 22px;
  position: relative;
  box-shadow: 0 30px 70px rgba(20, 40, 25, 0.35);
}
.modal::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 8px;
  background: linear-gradient(90deg, var(--accent, #2f7d4f), transparent);
  border-radius: 22px 22px 0 0;
}
.modal-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 36px;
  height: 36px;
  border: 2px solid var(--wood-light);
  background: #fff;
  border-radius: 50%;
  font-size: 15px;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.15s ease;
  z-index: 2;
}
.modal-close:hover {
  background: var(--danger);
  border-color: var(--danger);
  color: #fff;
}
.modal-hero {
  display: flex;
  gap: 18px;
  align-items: center;
}
.modal-emoji {
  font-size: 64px;
  width: 120px;
  height: 96px;
  flex: 0 0 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #fff, #f2f9ef);
  border: 2px solid var(--wood-light);
  border-radius: 20px;
  overflow: hidden;
}
.modal-emoji img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.modal-info h2 {
  font-size: 24px;
  font-weight: 900;
  color: var(--forest-deep);
  margin-bottom: 8px;
}
.modal-tags {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.m-tag {
  background: var(--accent, #2f7d4f);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 999px;
}
.m-wait {
  font-size: 12px;
  color: var(--muted);
}
.modal-rate {
  display: flex;
  align-items: center;
  gap: 8px;
}
.m-rate-num {
  font-size: 15px;
  font-weight: 800;
  color: var(--wood);
}
.m-rate-count {
  font-size: 12px;
  color: var(--muted);
}
.modal-desc {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text);
  margin: 18px 0 16px;
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: 14px;
  padding: 14px 16px;
}
.modal-section-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--forest-deep);
  margin-bottom: 12px;
}
.modal-reviews {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow: auto;
  padding-right: 4px;
  margin-bottom: 16px;
}
.m-review {
  display: flex;
  gap: 12px;
  background: #fff;
  border: 2px solid var(--wood-light);
  border-radius: 14px;
  padding: 12px 14px;
}
.m-avatar {
  font-size: 24px;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f9ef;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--wood-light);
}
.m-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.m-body {
  flex: 1;
}
.m-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.m-name {
  font-weight: 800;
  color: var(--forest-deep);
  font-size: 14px;
}
.m-date {
  font-size: 12px;
  color: var(--muted);
  margin-left: auto;
}
.m-text {
  font-size: 13px;
  color: var(--text);
  line-height: 1.7;
}
.m-empty {
  text-align: center;
  color: var(--muted);
  font-size: 14px;
  padding: 20px;
}
.modal-form {
  background: linear-gradient(160deg, #fff, #f2f9ef);
  border: 2px solid var(--wood-light);
  border-radius: 16px;
  padding: 16px 18px;
}
.mf-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.mf-label {
  font-size: 14px;
  font-weight: 700;
  color: var(--forest-deep);
}
.mf-hint {
  font-size: 13px;
  color: var(--muted);
}
.mf-text {
  width: 100%;
  padding: 10px 12px;
  font-size: 14px;
  font-family: inherit;
  background: #fffdf8;
  color: var(--text);
  border: 2px solid var(--wood-light);
  border-radius: 12px;
  resize: vertical;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.mf-text:focus {
  outline: none;
  border-color: var(--forest);
  box-shadow: 0 0 0 3px rgba(47, 125, 79, 0.18);
}
.mf-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  gap: 10px;
  flex-wrap: wrap;
}
.mf-as {
  font-size: 13px;
  color: var(--muted);
}
.mf-submit {
  border: none;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffc83d 0%, var(--honey) 100%);
  color: #5a3d00;
  font-size: 15px;
  font-weight: 800;
  padding: 10px 22px;
  cursor: pointer;
  box-shadow: 0 4px 0 #e09b00;
  transition: transform 0.08s ease, box-shadow 0.08s ease, filter 0.15s ease;
}
.mf-submit:hover {
  filter: brightness(1.03);
}
.mf-submit:active {
  transform: translateY(3px);
  box-shadow: 0 1px 0 #e09b00;
}
.mf-submit:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: 0 4px 0 #e09b00;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.25s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-active .modal,
.modal-leave-active .modal {
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}
.modal-enter-from .modal,
.modal-leave-to .modal {
  transform: translateY(20px) scale(0.96);
}

/* 轻提示（全局） */
.toast {
  position: fixed;
  left: 50%;
  bottom: 32px;
  transform: translateX(-50%);
  background: rgba(31, 92, 57, 0.96);
  color: #fff;
  padding: 12px 22px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 12px 34px rgba(0, 0, 0, 0.28);
  z-index: 200;
}
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(12px);
}

/* 窄屏适配 */
@media (max-width: 880px) {
  .nav-menu {
    display: none;
  }
  .nav-burger {
    display: flex;
  }
  .nav-drawer {
    display: flex;
  }
}
@media (max-width: 640px) {
  .hero { padding: 36px 22px 74px; }
  .hero h1 { font-size: 24px; }
  .search-row { flex-wrap: wrap; }
  .search-btn { width: 100%; }
  .show { flex-wrap: wrap; }
  .eco-bar { gap: 18px; }
  .eco-live { margin-left: 0; }
  .nav-user .hello { display: none; }
  .rate-overview { flex-direction: column; }
  .rate-big { border-right: none; border-bottom: 2px dashed var(--wood-light); padding-right: 0; padding-bottom: 16px; }
  .modal-hero { flex-direction: column; text-align: center; }
  .modal-tags, .modal-rate { justify-content: center; }
}
</style>
