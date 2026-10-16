// 狗熊岭森林游乐园 —— 游乐项目数据库（前端数据源）
//
// 字段说明：
//   id          项目唯一 ID
//   emoji       项目图标
//   name        项目名称
//   desc        项目具体内容（简介 / 玩法说明）
//   tag         标签（刺激 / 浪漫 / 亲子 / 清凉 / 益智）
//   wait        预计排队时间（分钟）
//   color       主题色
//   rating      项目综合评分（0–5，可含小数，由评价实时计算得出）
//   reviews     游客评价列表：{ id, name, avatar, rating, date, text }
//
// 对接后端时：
//   1) 用  GET /api/attractions  的返回替换本文件的 attractionsDB；
//   2) 用  POST /api/reviews  { attractionId, rating, text }  替换 addReview 的本地写入。

export const attractionsDB = [
  {
    id: 1,
    emoji: '🎢',
    img: '/img/coaster.gif',
    name: '树洞过山车',
    desc: '穿梭在千年古树之间，俯冲、旋转，尖叫声响彻狗熊岭！全程 880 米，最高时速 78km/h，适合追求刺激的勇敢者。',
    tag: '刺激',
    wait: 25,
    color: '#e4572e',
    rating: 4.8,
    reviews: [
      { id: 101, name: '熊大粉丝', avatar: '🐻', rating: 5, date: '2026-10-08', text: '太刺激了！熊大熊二就在旁边加油，孩子们玩得不想走。' },
      { id: 102, name: '森林旅人', avatar: '🦊', rating: 4, date: '2026-10-05', text: '排队略久但值回票价，建议上午来避开高峰。' }
    ]
  },
  {
    id: 2,
    emoji: '🎡',
    img: '/img/ferris.jpeg',
    name: '星空摩天轮',
    desc: '升至森林之巅，俯瞰整片狗熊岭的日落与星空。每舱可坐 6 人，缓缓转动 12 分钟一圈，浪漫满分。',
    tag: '浪漫',
    wait: 15,
    color: '#7c5cbf',
    rating: 4.6,
    reviews: [
      { id: 103, name: '仰望星空的猫', avatar: '🐱', rating: 4, date: '2026-10-07', text: '傍晚坐上去看日落特别美，就是风有点大。' },
      { id: 104, name: '情侣小白', avatar: '💑', rating: 5, date: '2026-10-02', text: '求婚成功！摩天轮顶端的那一刻太浪漫了。' }
    ]
  },
  {
    id: 3,
    emoji: '🚂',
    img: '/img/train.jpeg',
    name: '森林小火车',
    desc: '呜——哐当哐当，跟着吉吉国王环游五大主题区。全程 1.6 公里，沿途有动物演员挥手互动，亲子首选。',
    tag: '亲子',
    wait: 10,
    color: '#2f7d4f',
    rating: 4.9,
    reviews: [
      { id: 105, name: '亲子妈妈', avatar: '🐰', rating: 5, date: '2026-10-03', text: '超适合带娃，吉吉国王还会挥手，女儿玩得超开心！' },
      { id: 106, name: '吉吉粉丝', avatar: '🐵', rating: 4, date: '2026-09-28', text: '环游一圈能看遍全园，性价比很高。' }
    ]
  },
  {
    id: 4,
    emoji: '🍯',
    img: '/img/rafting.jpeg',
    name: '蜂蜜漂流',
    desc: '乘上蜂蜜罐小船，顺着金色溪流来一场甜蜜漂流。全长 420 米，中途有喷泉惊喜，清凉解暑。',
    tag: '清凉',
    wait: 30,
    color: '#ffb703',
    rating: 4.7,
    reviews: [
      { id: 107, name: '甜筒熊', avatar: '🐻', rating: 4, date: '2026-10-06', text: '夏天玩太爽了，就是排队有点久。' },
      { id: 108, name: '漂流达人', avatar: '🌊', rating: 5, date: '2026-09-30', text: '蜂蜜罐小船超可爱，拍照出片率很高！' }
    ]
  },
  {
    id: 5,
    emoji: '🏰',
    img: '/img/maze.jpeg',
    name: '光头强迷宫',
    desc: '在光头强的小木屋迷宫里寻宝，小心别被发现哦！共 3 层木质迷宫，藏着 12 个惊喜彩蛋。',
    tag: '益智',
    wait: 8,
    color: '#a9743c',
    rating: 4.5,
    reviews: [
      { id: 109, name: '迷宫王', avatar: '🧩', rating: 3, date: '2026-10-01', text: '有点绕，小朋友偶尔会急，建议大人陪同。' },
      { id: 110, name: '寻宝少年', avatar: '🗺️', rating: 4, date: '2026-09-26', text: '找到彩蛋那一刻超有成就感！' }
    ]
  },
  {
    id: 6,
    emoji: '💦',
    img: '/img/waterfall.jpg',
    name: '激流勇士',
    desc: '从瀑布顶端一跃而下，夏日限定的透心凉体验。落差 18 米，尖叫与水花齐飞，记得带雨衣。',
    tag: '刺激',
    wait: 40,
    color: '#2a9d8f',
    rating: 4.8,
    reviews: [
      { id: 111, name: '浪里白条', avatar: '🐬', rating: 5, date: '2026-10-04', text: '落差超带感，下来全身湿透但超爽！' },
      { id: 112, name: '夏日控', avatar: '☀️', rating: 4, date: '2026-09-29', text: '夏天必玩项目，排队久但值得。' }
    ]
  }
]

// 聚合所有项目的评价，附带项目名称，按日期倒序，供首页「游客评价」区块展示
export function getAllReviews(list = attractionsDB) {
  const out = []
  for (const a of list) {
    for (const r of a.reviews) {
      out.push({ ...r, attraction: a.name, attractionId: a.id })
    }
  }
  return out.sort((x, y) => y.date.localeCompare(x.date))
}

// 向指定项目追加一条评价（本地写入；接后端时替换为 POST /api/reviews）
export function addReview(list, attractionId, review) {
  const a = list.find((x) => x.id === attractionId)
  if (!a) return null
  a.reviews.unshift(review)
  // 重新计算该项目综合评分（保留一位小数）
  const sum = a.reviews.reduce((s, r) => s + r.rating, 0)
  a.rating = Math.round((sum / a.reviews.length) * 10) / 10
  return review
}
