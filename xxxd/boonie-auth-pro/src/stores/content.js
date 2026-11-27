import { defineStore } from 'pinia'
import { attractionsDB } from '../data/attractions'
import { FALLBACK_IMG, FALLBACK_BANNERS, FALLBACK_NOTICES, FALLBACK_CATEGORIES } from '../data/fallback'
import { getAttractions, getBanners, getNotices, getCategories } from '../api/content'

/**
 * 首页内容 store（attractions / banners / notices）
 *
 * 迁移说明：这些数据和它们的加载逻辑原来都在 ParkHome.vue 里（5036 行）。
 * 抽到 store 后，后续拆出去的子组件可以直接读状态、调 action，
 * 不必再通过层层 props 传递 —— 这是拆分 ParkHome 的前置条件。
 *
 * 设计原则：
 *  - 所有接口失败都「降级不报错」，保证页面永远有内容可看（与原逻辑一致）
 *  - 加载状态（loading/apiError/loadedFromApi/lastUpdated）一并收进来，供顶部提示条使用
 */
export const useContentStore = defineStore('content', {
  state: () => ({
    /** 游乐项目列表（每项含 reviews） */
    attractions: [],
    /** 首页轮播 */
    bannerSlides: [...FALLBACK_BANNERS],
    /** 顶部公告文本数组 */
    notices: [...FALLBACK_NOTICES],
    /** 项目分类字典（后台可维护） */
    categories: [...FALLBACK_CATEGORIES],
    /** 项目列表加载中 */
    loading: false,
    /** 降级提示文案（空字符串表示正常） */
    apiError: '',
    /** 本次数据是否来自后端（false = 用了示例数据） */
    loadedFromApi: false,
    /** 最近一次成功加载时间（HH:mm） */
    lastUpdated: ''
  }),

  getters: {
    /** 热门推荐：按评分取前 5 */
    hotAttractions: (s) => [...s.attractions].sort((x, y) => (y.rating || 0) - (x.rating || 0)).slice(0, 5),
    /** 分类名称列表（供筛选/下拉用，始终含「全部」） */
    categoryNames: (s) => {
      const names = s.categories.map((c) => c.name)
      return names.includes('全部') ? names : ['全部', ...names]
    }
  },

  actions: {
    /** 拉取游乐项目（含评价），失败降级为内置示例数据 */
    async loadAttractions(token) {
      this.loading = true
      this.apiError = ''
      try {
        const json = await getAttractions(token)
        const list = json && Array.isArray(json.data) ? json.data : null
        if (!list) throw new Error('数据格式异常')

        this.attractions = list.map((a) => ({
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
        this.loadedFromApi = true
        this.lastUpdated = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      } catch (e) {
        // 后端 / 数据库不可达时降级为内置示例数据，保证页面始终有内容可看
        this.attractions = attractionsDB.map((a) => ({ ...a, reviews: [...(a.reviews || [])] }))
        this.loadedFromApi = false
        this.apiError = '数据库暂时连不上，已切换为示例数据（请先启动后端与 MySQL）'
      } finally {
        this.loading = false
      }
    },

    /** 拉取轮播，失败 / 空列表降级为内置 4 张 */
    async loadBanners() {
      try {
        const json = await getBanners()
        const list = json && Array.isArray(json.data) ? json.data : null
        if (!list || !list.length) throw new Error('空列表')
        this.bannerSlides = list.map((b) => ({
          img: b.img || FALLBACK_BANNERS[0].img,
          tag: b.tag || '🌲 森林乐园',
          title: b.title || '狗熊岭',
          desc: b.desc || ''
        }))
      } catch {
        this.bannerSlides = [...FALLBACK_BANNERS]
      }
    },

    /** 拉取项目分类字典，失败 / 空列表降级为内置分类 */
    async loadCategories() {
      try {
        const json = await getCategories()
        const list = json && Array.isArray(json.data) ? json.data : null
        if (!list || !list.length) throw new Error('空列表')
        this.categories = list
          .map((c) => ({ name: c.name, emoji: c.emoji, color: c.color, sortNo: c.sortNo ?? 0 }))
          .sort((a, b) => a.sortNo - b.sortNo)
      } catch {
        this.categories = [...FALLBACK_CATEGORIES]
      }
    },

    /** 拉取公告，失败 / 空列表降级为内置 3 条 */
    async loadNotices() {
      try {
        const json = await getNotices()
        const list = json && Array.isArray(json.data) ? json.data : null
        if (!list || !list.length) throw new Error('空列表')
        this.notices = list.map((n) => n.content).filter(Boolean)
      } catch {
        this.notices = [...FALLBACK_NOTICES]
      }
    }
  }
})
