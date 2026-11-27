/**
 * 中后台模块配置（参考 vue3-element-admin 的「配置驱动 CRUD」写法）
 *
 * 每个模块声明：
 *   - name/icon/desc      菜单与页面元信息
 *   - columns[]           表格列：{ key, label, width?, type?, options?, format? }
 *   - fields[]            表单字段：{ key, label, type, required?, options?, placeholder? }
 *   - seed[]              Mock 种子数据
 *
 * 通用 CRUD 页 CrudView.vue 只认这套配置，新增模块 = 加一段配置 + 一份种子。
 * 字段 type：text / textarea / number / select / date / image / color / switch
 */
export const MODULES = [
  {
    key: 'projects',
    name: '项目管理',
    icon: '🎢',
    desc: '维护游乐项目的名称、分类、票价与展示状态（Mock 数据）',
    columns: [
      { key: 'name', label: '项目名称', width: '22%' },
      { key: 'emoji', label: '图标', width: '8%' },
      { key: 'category', label: '分类', width: '12%' },
      { key: 'price', label: '票价', width: '10%', format: (v) => `¥${Number(v || 0).toFixed(2)}` },
      { key: 'rating', label: '评分', width: '10%' },
      { key: 'status', label: '状态', width: '12%', type: 'status' },
      { key: 'createdAt', label: '创建时间', width: '16%' }
    ],
    fields: [
      { key: 'name', label: '项目名称', type: 'text', required: true, placeholder: '如：过山车' },
      { key: 'emoji', label: '图标', type: 'text', placeholder: '一个 emoji，如 🎢' },
      { key: 'category', label: '分类', type: 'select', options: ['刺激', '亲子', '观光', '浪漫', '清凉', '益智'] },
      { key: 'price', label: '票价', type: 'number', required: true },
      { key: 'rating', label: '评分', type: 'number' },
      { key: 'status', label: '状态', type: 'select', options: ['上架', '下架'] }
    ],
    seed: [
      { id: 1, name: '森林过山车', emoji: '🎢', category: '刺激', price: 80, rating: 4.8, status: '上架', createdAt: '2026-09-12' },
      { id: 2, name: '熊大旋转木马', emoji: '🎠', category: '亲子', price: 40, rating: 4.6, status: '上架', createdAt: '2026-09-18' },
      { id: 3, name: '玻璃栈道', emoji: '🌉', category: '观光', price: 60, rating: 4.5, status: '上架', createdAt: '2026-10-02' },
      { id: 4, name: '激流勇进', emoji: '🚣', category: '清凉', price: 70, rating: 4.7, status: '下架', createdAt: '2026-10-21' },
      { id: 5, name: '森林迷宫', emoji: '🌲', category: '益智', price: 30, rating: 4.3, status: '上架', createdAt: '2026-11-05' }
    ]
  },
  {
    key: 'shows',
    name: '演出管理',
    icon: '🎭',
    desc: '维护园区演出场次、时间与票价（Mock 数据）',
    columns: [
      { key: 'title', label: '演出名称', width: '24%' },
      { key: 'venue', label: '场地', width: '16%' },
      { key: 'date', label: '演出日期', width: '16%' },
      { key: 'time', label: '时段', width: '12%' },
      { key: 'price', label: '票价', width: '12%', format: (v) => `¥${Number(v || 0).toFixed(2)}` },
      { key: 'status', label: '状态', width: '12%', type: 'status' },
      { key: 'createdAt', label: '创建时间', width: '8%' }
    ],
    fields: [
      { key: 'title', label: '演出名称', type: 'text', required: true },
      { key: 'venue', label: '场地', type: 'text', placeholder: '如：森林剧场' },
      { key: 'date', label: '演出日期', type: 'date', required: true },
      { key: 'time', label: '时段', type: 'text', placeholder: '如：10:00-11:00' },
      { key: 'price', label: '票价', type: 'number' },
      { key: 'status', label: '状态', type: 'select', options: ['上架', '下架'] }
    ],
    seed: [
      { id: 1, title: '熊出没舞台剧', venue: '森林剧场', date: '2026-12-01', time: '10:00-11:00', price: 120, status: '上架', createdAt: '2026-10-11' },
      { id: 2, title: '森林音乐会', venue: '中央草坪', date: '2026-12-02', time: '15:00-16:00', price: 80, status: '上架', createdAt: '2026-10-15' },
      { id: 3, title: '光影烟花秀', venue: '湖畔广场', date: '2026-12-03', time: '19:30-20:00', price: 150, status: '上架', createdAt: '2026-10-28' }
    ]
  },
  {
    key: 'tickets',
    name: '门票管理',
    icon: '🎫',
    desc: '维护票种、价格与库存（Mock 数据）',
    columns: [
      { key: 'name', label: '票种名称', width: '20%' },
      { key: 'typeCode', label: '编码', width: '14%' },
      { key: 'price', label: '现价', width: '12%', format: (v) => `¥${Number(v || 0).toFixed(2)}` },
      { key: 'originalPrice', label: '原价', width: '12%', format: (v) => `¥${Number(v || 0).toFixed(2)}` },
      { key: 'stock', label: '库存', width: '10%' },
      { key: 'enabled', label: '状态', width: '12%', type: 'statusBool' },
      { key: 'createdAt', label: '创建时间', width: '20%' }
    ],
    fields: [
      { key: 'name', label: '票种名称', type: 'text', required: true },
      { key: 'typeCode', label: '编码', type: 'text', required: true, placeholder: '如：adult' },
      { key: 'price', label: '现价', type: 'number', required: true },
      { key: 'originalPrice', label: '原价', type: 'number' },
      { key: 'stock', label: '库存', type: 'number' },
      { key: 'enabled', label: '启用', type: 'switch' }
    ],
    seed: [
      { id: 1, name: '成人票', typeCode: 'adult', price: 199, originalPrice: 259, stock: 500, enabled: true, createdAt: '2026-09-01' },
      { id: 2, name: '儿童票', typeCode: 'child', price: 99, originalPrice: 129, stock: 300, enabled: true, createdAt: '2026-09-01' },
      { id: 3, name: '亲子套票', typeCode: 'family', price: 399, originalPrice: 497, stock: 120, enabled: true, createdAt: '2026-09-08' },
      { id: 4, name: '夜场票', typeCode: 'night', price: 99, originalPrice: 129, stock: 80, enabled: false, createdAt: '2026-10-20' }
    ]
  },
  {
    key: 'orders',
    name: '订单管理',
    icon: '🧾',
    desc: '查询购票订单、支付状态与积分明细（Mock 数据）',
    columns: [
      { key: 'orderNo', label: '订单号', width: '20%' },
      { key: 'ticketName', label: '票种', width: '14%' },
      { key: 'buyerName', label: '取票人', width: '10%' },
      { key: 'quantity', label: '数量', width: '8%' },
      { key: 'total', label: '金额', width: '12%', format: (v) => `¥${Number(v || 0).toFixed(2)}` },
      { key: 'status', label: '状态', width: '12%', type: 'status' },
      { key: 'createdAt', label: '下单时间', width: '24%' }
    ],
    fields: [
      { key: 'ticketName', label: '票种', type: 'text', required: true },
      { key: 'buyerName', label: '取票人', type: 'text', required: true },
      { key: 'quantity', label: '数量', type: 'number', required: true },
      { key: 'total', label: '金额', type: 'number', required: true },
      { key: 'status', label: '状态', type: 'select', options: ['待使用', '已使用', '已取消'] }
    ],
    seed: [
      { id: 1, orderNo: 'BK202611200001', ticketName: '亲子套票', buyerName: '熊大', quantity: 2, total: 798, status: '待使用', createdAt: '2026-11-20 10:32' },
      { id: 2, orderNo: 'BK202611210002', ticketName: '成人票', buyerName: '熊二', quantity: 3, total: 597, status: '已使用', createdAt: '2026-11-21 14:08' },
      { id: 3, orderNo: 'BK202611230003', ticketName: '儿童票', buyerName: '光头强', quantity: 1, total: 99, status: '已取消', createdAt: '2026-11-23 09:15' },
      { id: 4, orderNo: 'BK202611250004', ticketName: '成人票', buyerName: '蹦蹦', quantity: 2, total: 398, status: '待使用', createdAt: '2026-11-25 16:47' }
    ]
  },
  {
    key: 'reviews',
    name: '评价管理',
    icon: '⭐',
    desc: '审核游客评价，通过后展示在前台（Mock 数据）',
    columns: [
      { key: 'attraction', label: '所属项目', width: '18%' },
      { key: 'name', label: '评价人', width: '12%' },
      { key: 'rating', label: '评分', width: '10%' },
      { key: 'text', label: '评价内容', width: '32%' },
      { key: 'status', label: '状态', width: '14%', type: 'status' },
      { key: 'date', label: '日期', width: '14%' }
    ],
    fields: [
      { key: 'attraction', label: '所属项目', type: 'text', required: true },
      { key: 'name', label: '评价人', type: 'text', required: true },
      { key: 'rating', label: '评分', type: 'number' },
      { key: 'text', label: '评价内容', type: 'textarea' },
      { key: 'status', label: '状态', type: 'select', options: ['待审核', '已通过', '已驳回'] }
    ],
    seed: [
      { id: 1, attraction: '森林过山车', name: '熊大', rating: 5, text: '太刺激啦！下次还要来玩～', status: '已通过', date: '2026-11-18' },
      { id: 2, attraction: '熊大旋转木马', name: '熊二', rating: 4, text: '适合带小朋友，很温馨。', status: '已通过', date: '2026-11-20' },
      { id: 3, attraction: '玻璃栈道', name: '光头强', rating: 4, text: '风景不错，就是有点恐高。', status: '待审核', date: '2026-11-24' },
      { id: 4, attraction: '激流勇进', name: '蹦蹦', rating: 3, text: '衣服湿了，建议带雨衣。', status: '待审核', date: '2026-11-26' }
    ]
  },
  {
    key: 'users',
    name: '用户管理',
    icon: '👥',
    desc: '管理注册用户与权限（Mock 数据）',
    columns: [
      { key: 'name', label: '用户名', width: '18%' },
      { key: 'email', label: '邮箱', width: '26%' },
      { key: 'phone', label: '手机号', width: '16%' },
      { key: 'permission', label: '权限', width: '14%', format: (v) => (Number(v) === 0 ? '管理员' : '普通用户') },
      { key: 'points', label: '积分', width: '10%' },
      { key: 'createdAt', label: '注册时间', width: '16%' }
    ],
    fields: [
      { key: 'name', label: '用户名', type: 'text', required: true },
      { key: 'email', label: '邮箱', type: 'text', required: true },
      { key: 'phone', label: '手机号', type: 'text' },
      { key: 'permission', label: '权限', type: 'select', options: [0, 1], optionLabels: { 0: '管理员', 1: '普通用户' } },
      { key: 'points', label: '积分', type: 'number' }
    ],
    seed: [
      { id: 1, name: '熊大', email: 'bear1@boonie.com', phone: '13800138001', permission: 0, points: 860, createdAt: '2026-09-01' },
      { id: 2, name: '熊二', email: 'bear2@boonie.com', phone: '13800138002', permission: 1, points: 520, createdAt: '2026-09-02' },
      { id: 3, name: '光头强', email: 'logger@boonie.com', phone: '13800138003', permission: 1, points: 130, createdAt: '2026-10-11' },
      { id: 4, name: '蹦蹦', email: 'squirrel@boonie.com', phone: '13800138004', permission: 1, points: 45, createdAt: '2026-11-03' }
    ]
  },
  {
    key: 'banners',
    name: '轮播图管理',
    icon: '🖼️',
    desc: '配置首页轮播图文案、跳转与排序（Mock 数据）',
    columns: [
      { key: 'title', label: '标题', width: '20%' },
      { key: 'subtitle', label: '副标题', width: '24%' },
      { key: 'tag', label: '角标', width: '10%' },
      { key: 'link', label: '跳转板块', width: '14%' },
      { key: 'sortNo', label: '排序', width: '8%' },
      { key: 'enabled', label: '状态', width: '12%', type: 'statusBool' },
      { key: 'color', label: '主题色', width: '12%', type: 'color' }
    ],
    fields: [
      { key: 'title', label: '标题', type: 'text', required: true },
      { key: 'subtitle', label: '副标题', type: 'text' },
      { key: 'tag', label: '角标', type: 'text' },
      { key: 'link', label: '跳转板块', type: 'select', options: ['游乐项目', '栖息地', '演出', '门票', '风景'] },
      { key: 'sortNo', label: '排序', type: 'number' },
      { key: 'enabled', label: '启用', type: 'switch' },
      { key: 'color', label: '主题色', type: 'color' }
    ],
    seed: [
      { id: 1, title: '熊出没·森林狂欢节', subtitle: '暑假限定 · 全家总动员', tag: 'HOT', link: '门票', sortNo: 1, enabled: true, color: '#2f7d4f' },
      { id: 2, title: '探索神秘栖息地', subtitle: '和熊大熊二一起走进森林', tag: 'NEW', link: '栖息地', sortNo: 2, enabled: true, color: '#a9743c' },
      { id: 3, title: '精彩演出不间断', subtitle: '每日多场舞台剧与烟花秀', tag: '', link: '演出', sortNo: 3, enabled: true, color: '#ffb703' }
    ]
  }
]

/** 按 key 查模块配置 */
export function findModule(key) {
  return MODULES.find((m) => m.key === key) || null
}
