<template>
  <div class="auth-shell">
    <!-- 左边区域 -->
    <div class="auth-left">
      <div class="left-inner">
        <p class="left-subtitle">一诺既出 · 山海可证</p>
        <h1 v-if="mode === 'login'">言出，<br>必行。</h1>
        <h1 v-if="mode === 'register'">MAKE A PROMISE</h1>
        <p class="left-desc">以一枚印章，为认真盖章。用更克制的方式，记录你与自己的约定。</p>
        <div class="left-card">
          <p>今日立下小约，来日坦然相见。</p>
          <p class="small-text">一诺既出 · 山海可证</p>
        </div>
      </div>
    </div>
    <!-- 右边：插槽，放登录/注册表单 -->
    <div class="auth-right">
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  mode: {
    type: String,
    default: 'login'
  }
})
</script>

<style scoped>
.auth-shell {
  display: flex;
  width: 100vw;
  height: 100vh;
  margin: 0;
  padding: 0;
  overflow: hidden;
}
.auth-left {
  width: 50%;
  background-color: #f0f0ed;
  display: flex;
  align-items: center;
  justify-content: center;
}
.left-inner {
  width: 80%;
}
.left-subtitle {
  color: #888;
  letter-spacing: 1px;
}
.auth-left h1 {
  font-size: 64px;
  font-weight: 600;
  color: #222;
  line-height: 1.2;
  margin: 16px 0;
}
.left-desc {
  color: #666;
  margin-bottom: 48px;
}
.left-card {
  border: 1px solid #c9c9c7;
  padding: 28px;
  border-radius: 6px;
}
.small-text {
  margin-top: 12px;
  font-size: 13px;
  color: #777;
}
.auth-right {
  width: 50%;
  background-color: #f7f7f4;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

