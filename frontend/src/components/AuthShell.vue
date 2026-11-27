<template>
  <div class="auth-shell">
    <!-- 左侧文案区域 -->
    <div class="auth-left">
      <div class="auth-left-inner">
        <p class="sub-title">一诺既出 · 山海可证</p>
        <h1>
          <span v-if="mode === 'login'">归来赴约</span>
          <span v-if="mode === 'register'">立下第一份约定</span>
        </h1>
        <p class="desc">
          <span v-if="mode === 'login'">你的承诺，正在这里等你。</span>
          <span v-if="mode === 'register'">注册即领取 1000 虚拟币体验金。</span>
        </p>
        <div class="tip-box">
          <p>今日立下小约，来日坦然相见。</p>
          <p class="small">一诺既出 · 山海可证</p>
        </div>
      </div>
    </div>
    <!-- 右侧表单区域，插槽放登录/注册表单 -->
    <div class="auth-right">
      <slot />
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
}
.auth-left {
  width: 50%;
  background-color: #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
.auth-left-inner {
  max-width: 420px;
}
.sub-title {
  color: #999;
  letter-spacing: 2px;
}
.auth-left h1 {
  font-size: 48px;
  font-weight: bold;
  margin: 16px 0;
}
.desc {
  color: #666;
  margin-bottom: 40px;
}
.tip-box {
  border: 1px solid #ccc;
  padding: 24px;
  border-radius: 8px;
}
.auth-right {
  width: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
</style>
