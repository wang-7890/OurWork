<template>
  <div class="auth-shell">
    <!-- 顶部通栏 -->
    <header class="auth-header">
      <div class="header-left">
        <span class="logo-mark"></span>
        <span class="logo-text">微承诺 <small>MICRO PACT</small></span>
      </div>
      <div class="header-right">
        <span class="header-link">公开承诺</span>
        <span class="header-dot">·</span>
        <span class="header-link">共同见证</span>
      </div>
    </header>

    <!-- 主体区域 -->
    <div class="auth-body">
      <!-- 左侧宣传区 -->
      <div class="auth-left">
        <div class="left-watermark">诺</div>

        <div class="left-inner">
          <p class="line-title">—— 一诺既出 · 山海可证</p>

          <h1 class="main-title">
            <template v-if="mode === 'login'">
              言出，<br>必行。
            </template>
            <template v-else>
              MAKE A PROMISE
            </template>
          </h1>

          <p class="desc">
            以一枚印章，为认真盖章。用更克制的方式，记录你与自己的约定。
          </p>

          <div class="text-card">
            <p>今日立下小约，来日坦然相见。</p>
            <p class="card-sub">一诺既出 · 山海可证</p>
          </div>
        </div>

        <div class="seal">已立约</div>
        <p class="left-bottom-tip">目标公开 · 好友见证 · 结果可追踪</p>
      </div>

      <!-- 右侧表单区 -->
      <div class="auth-right">
        <div class="auth-form-card">
          <slot></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  mode: {
    type: String,
    default: 'login'
  }
})
</script>

<style scoped>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}
/* 最外层容器 */
.auth-shell {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: #f6f6f1;
  color: #222;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC",
  "Hiragino Sans GB", "Microsoft YaHei", Arial, sans-serif;
}

/* 顶部导航 */
.auth-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 40px;
  height: 64px;
  background-color: #f6f6f1;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-mark {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #222;
  position: relative;
}

.logo-mark::before {
  content: "";
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  border: 2px solid #222;
}

.logo-text {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.logo-text small {
  font-size: 12px;
  font-weight: 400;
  color: #777;
  margin-left: 4px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #555;
}

.header-link {
  cursor: pointer;
}

.header-link:hover {
  color: #c95549;
}

.header-dot {
  color: #999;
}

/* 主体 */
.auth-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* 左侧 */
.auth-left {
  width: 50%;
  background-color: #ecece8;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  overflow: hidden;
}

.left-watermark {
  position: absolute;
  left: 8%;
  bottom: 18%;
  font-size: 260px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.045);
  letter-spacing: 4px;
  user-select: none;
  pointer-events: none;
  z-index: 0;
}

.left-inner {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
}

.line-title {
  font-size: 14px;
  color: #7a7a77;
  letter-spacing: 2px;
  margin-bottom: 24px;
}

.main-title {
  font-size: 76px;
  font-weight: 500;
  line-height: 1.1;
  color: #111;
  margin-bottom: 24px;
  letter-spacing: 2px;
}

.desc {
  font-size: 15px;
  color: #555;
  line-height: 1.7;
  margin-bottom: 48px;
}

.text-card {
  border: 1px solid #c5c5c1;
  border-radius: 8px;
  padding: 32px;
  background-color: rgba(255, 255, 255, 0.35);
}

.text-card p {
  font-size: 15px;
  color: #333;
}

.card-sub {
  margin-top: 14px;
  font-size: 13px;
  color: #777;
}

/* 印章 */
.seal {
  position: absolute;
  right: 48px;
  bottom: 48px;
  width: 76px;
  height: 76px;
  border-radius: 50%;
  border: 2px solid #c95549;
  color: #c95549;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  letter-spacing: 2px;
  font-weight: 500;
  z-index: 2;
  transform: rotate(-6deg);
}

.left-bottom-tip {
  position: absolute;
  left: 48px;
  bottom: 48px;
  font-size: 13px;
  color: #777;
  z-index: 2;
}

/* 右侧 */
.auth-right {
  width: 50%;
  background-color: #f6f6f1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

/* 表单卡片：让右侧表单区域更贴近原图尺寸 */
.auth-form-card {
  width: 100%;
  max-width: 420px;
}
</style>
<style>
html, body, #app {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
}
</style>


