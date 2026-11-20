<template>
  <div class="nf">
    <div class="nf-card">
      <div class="nf-bear">🐻</div>
      <h1 class="nf-code">404</h1>
      <h2 class="nf-title">哎呀，这片森林迷路了…</h2>
      <p class="nf-desc">你访问的页面不存在或已被移走，熊二找了一圈也没找到它。</p>
      <div class="nf-acts">
        <button class="nf-btn primary" type="button" @click="$emit('home')">🌲 回到首页</button>
        <button class="nf-btn ghost" type="button" @click="$emit('attractions')">🎡 浏览项目大全</button>
      </div>
      <div class="nf-leaves">
        <span v-for="n in 6" :key="n" class="nf-leaf" :style="{ '--d': (n - 1) * 0.6 + 's', '--x': (n - 1) * 16 + '%' }">🍃</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineEmits(['home', 'attractions'])
</script>

<style scoped>
.nf {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(circle at 20% 15%, rgba(124, 196, 107, 0.22), transparent 45%),
    radial-gradient(circle at 82% 80%, rgba(255, 200, 61, 0.2), transparent 45%),
    linear-gradient(180deg, #f3faee, #e6f3df);
  overflow: hidden;
}
.nf-card {
  position: relative;
  max-width: 460px;
  width: 100%;
  text-align: center;
  background: rgba(255, 253, 248, 0.94);
  border: 2px solid #d8ecd0;
  border-radius: 22px;
  padding: 42px 32px 34px;
  box-shadow: 0 14px 40px rgba(47, 125, 79, 0.16);
  animation: nf-in 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes nf-in {
  from { opacity: 0; transform: translateY(24px) scale(0.96); }
  to { opacity: 1; transform: none; }
}
.nf-bear {
  font-size: 56px;
  animation: nf-bob 2.4s ease-in-out infinite;
}
@keyframes nf-bob {
  0%, 100% { transform: translateY(0) rotate(-3deg); }
  50% { transform: translateY(-8px) rotate(3deg); }
}
.nf-code {
  margin: 6px 0 0;
  font-size: 64px;
  font-weight: 900;
  letter-spacing: 4px;
  color: #2f7d4f;
  line-height: 1;
  text-shadow: 3px 3px 0 rgba(124, 196, 107, 0.35);
}
.nf-title {
  margin: 12px 0 6px;
  font-size: 20px;
  color: #274d35;
}
.nf-desc {
  margin: 0 0 22px;
  font-size: 14px;
  color: #7c8a80;
}
.nf-acts {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}
.nf-btn {
  border: 2px solid transparent;
  border-radius: 999px;
  padding: 11px 22px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.18s ease;
}
.nf-btn:active { transform: scale(0.92); }
.nf-btn.primary {
  background: linear-gradient(180deg, #58a86f, #2f7d4f);
  color: #fff;
  box-shadow: 0 5px 0 #24633e;
}
.nf-btn.primary:hover { transform: translateY(-2px); }
.nf-btn.ghost {
  background: #fff;
  border-color: #9fcaa8;
  color: #2f7d4f;
}
.nf-btn.ghost:hover { background: #eef7e9; transform: translateY(-2px); }
/* 飘落的叶子装饰 */
.nf-leaves { position: absolute; inset: 0; pointer-events: none; }
.nf-leaf {
  position: absolute;
  top: -28px;
  left: var(--x);
  font-size: 18px;
  opacity: 0.75;
  animation: nf-fall 5s linear infinite;
  animation-delay: var(--d);
}
@keyframes nf-fall {
  0% { transform: translateY(0) rotate(0deg); opacity: 0; }
  10% { opacity: 0.75; }
  100% { transform: translateY(480px) rotate(320deg); opacity: 0; }
}
</style>
