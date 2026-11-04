<template>
  <section id="habitats" class="section habitats reveal">
    <h2 class="section-title">🦉 动物栖息地 · 和小动物们打个招呼</h2>

    <div class="grid">
      <article
        v-for="h in habitats"
        :key="h.name"
        class="habitat"
        :style="{ '--accent': h.color }"
      >
        <div class="habitat-cover">
          <img :src="h.img || FALLBACK_IMG" :alt="h.name" loading="lazy" />
          <span class="habitat-avatar" :style="{ background: h.color }">{{ h.emoji }}</span>
        </div>

        <h3>{{ h.name }}</h3>
        <p class="intro">{{ h.desc }}</p>

        <div class="fun-fact">
          <span class="ff-icon">💡</span>
          <span><b>趣味小知识：</b>{{ h.funFact }}</span>
        </div>

        <div class="habitat-meta">
          <span class="resident">🏡 {{ h.residents }}</span>
          <span class="mood">{{ h.mood }}</span>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
const FALLBACK_IMG = '/img/background.png'

const habitats = [
  {
    emoji: '🐻',
    img: '/img/avatar.jpg',
    name: '熊大之家',
    desc: '狗熊岭的"大哥"熊大，聪明稳重、最爱护森林，蜂蜜和玉米是他的心头好。',
    funFact: '熊大能单手举起比自己还重的蜂蜜罐，却总抢不过贪吃的熊二。',
    residents: '熊大 · 森林守护者',
    mood: '😊 正在巡逻',
    color: '#a9743c'
  },
  {
    emoji: '🐻',
    img: '/img/xiong-er.webp',
    name: '熊二小窝',
    desc: '憨厚贪吃的熊二，力气惊人，一闻到蜂蜜味就迈不动腿，心地却特别善良。',
    funFact: '熊二记性差，但能凭气味在整片森林里精准定位最近的蜂巢。',
    residents: '熊二 · 专业吃货',
    mood: '🍯 找蜂蜜中',
    color: '#c89b63'
  },
  {
    emoji: '🐵',
    img: '/img/jiji.webp',
    name: '吉吉王国',
    desc: '自称森林之王的吉吉，头戴皇冠、爱面子，其实热心肠，常组织小伙伴搞活动。',
    funFact: '吉吉的皇冠是用松果和藤条编的，下雨天会先护住皇冠再护自己。',
    residents: '吉吉国王 · 毛毛',
    mood: '👑 巡游中',
    color: '#7c5cbf'
  },
  {
    emoji: '🦉',
    img: '/img/owl.jpg',
    name: '猫头鹰树屋',
    desc: '百年老橡树上的圆圆树洞，夜里最热闹，涂涂总在这里打盹又总撞树。',
    funFact: '涂涂的脖子能转 270°，却还是常常认错方向撞到树干。',
    residents: '涂涂 · 夜间巡逻员',
    mood: '😴 白天打盹中',
    color: '#5b8aa6'
  },
  {
    emoji: '🐰',
    img: '/img/rabbit.jpg',
    name: '跳跳兔草坪',
    desc: '软软的草地是兔子们的乐园，它们最爱在清晨的露珠里蹦蹦跳跳。',
    funFact: '兔子的大耳朵不仅能听远，还能帮身体散热，夏天转起来像小风扇。',
    residents: '兔宝宝一家',
    mood: '🌱 啃三叶草',
    color: '#e58aa8'
  },
  {
    emoji: '🪓',
    img: '/img/background.png',
    name: '光头强木屋',
    desc: '曾经砍树的光头强，如今改行当护林员，木屋里塞满了工具和日记。',
    funFact: '光头强的日记本里，画满了熊大熊二，最后一页写着"他们是朋友"。',
    residents: '光头强 · 护林员',
    mood: '🌲 巡林去咯',
    color: '#2f7d4f'
  }
]
</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
}
.habitat {
  background: #fff;
  border-radius: var(--radius);
  overflow: hidden;
  border: 2px solid var(--wood-light);
  box-shadow: 0 12px 28px rgba(47, 125, 79, 0.1);
  padding-bottom: 16px;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.habitat:hover {
  transform: translateY(-5px);
  box-shadow: 0 18px 38px rgba(47, 125, 79, 0.18);
}
.habitat-cover {
  position: relative;
  height: 150px;
  overflow: hidden;
}
.habitat-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.habitat-avatar {
  position: absolute;
  left: 14px;
  bottom: -22px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 3px solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
.habitat h3 {
  margin: 30px 16px 6px;
  font-size: 18px;
  font-weight: 800;
  color: var(--forest-deep);
}
.intro {
  margin: 0 16px;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--text);
}
.fun-fact {
  margin: 12px 16px 0;
  background: #f3faf0;
  border-left: 4px solid var(--leaf);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.55;
  color: #41614c;
}
.fun-fact b { color: var(--forest-deep); }
.ff-icon { margin-right: 4px; }
.habitat-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin: 12px 16px 0;
  font-size: 12px;
  color: var(--muted);
}
.habitat-meta .mood {
  background: #f0f6ec;
  padding: 3px 10px;
  border-radius: 999px;
}
</style>
