<template>
  <div class="auth-page">
    <header class="auth-topbar">
      <RouterLink to="/square" class="auth-brand" aria-label="微承诺首页">
        <svg class="auth-brand-mark" viewBox="0 0 36 36" fill="none" aria-hidden="true">
          <path d="M7 9.5 18 4l11 5.5v17L18 32 7 26.5v-17Z" stroke="currentColor" stroke-width="1.6" />
          <path d="m11.5 18 4.2 4.2 8.8-9" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span>
          <strong>微承诺</strong>
          <small>MICRO PACT</small>
        </span>
      </RouterLink>
      <span class="auth-topnote">公开承诺 · 共同见证</span>
    </header>

    <div class="auth-layout">
      <section class="auth-story" aria-labelledby="auth-story-title">
        <div class="auth-story-copy">
          <p class="auth-kicker">一诺既出 · 山海可证</p>
          <h1 id="auth-story-title">{{ isRegister ? '今日立约，' : '言出，' }}<br>{{ isRegister ? '来日相见。' : '必行。' }}</h1>
          <p>{{ isRegister ? '注册即可领取 1000 虚拟币体验金。以一枚印章，为第一份认真盖章。' : '以一枚印章，为认真盖章。用更克制的方式，记录你与自己的约定。' }}</p>
        </div>

        <div class="auth-seal-visual" aria-hidden="true">
          <span class="auth-seal-word">诺</span>
          <div class="auth-seal-statement">
            <p>今日立下小约，<br>来日坦然相见。</p>
            <small>一诺既出 · 山海可证</small>
          </div>
          <div class="auth-seal-stamp">已立约</div>
        </div>

        <div class="auth-feature-row">
          <span>目标公开</span>
          <span>好友见证</span>
          <span>结果可追踪</span>
        </div>
      </section>

      <section class="auth-form-panel">
        <slot />
      </section>
    </div>

    <footer class="auth-footer-line">
      <span>微承诺契约平台</span>
      <span>简约 / 可信赖 / 有辨识度</span>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  mode: {
    type: String,
    default: 'login'
  }
})

const isRegister = computed(() => props.mode === 'register')
</script>

<style>
.auth-page {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  color: var(--ink);
  background: var(--page-bg);
  font-family: var(--font-body);
}
.auth-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 82px;
  padding: 16px 40px;
  border-bottom: 1px solid var(--line);
  background: var(--surface);
}
.auth-brand { display: inline-flex; align-items: center; gap: 12px; min-height: 44px; }
.auth-brand-mark { width: 34px; height: 34px; color: var(--brand); }
.auth-brand strong { display: block; font-size: 17px; letter-spacing: .12em; line-height: 1.3; }
.auth-brand small { display: block; margin-top: 3px; color: var(--ink-3); font-size: 9px; letter-spacing: .18em; }
.auth-topnote { color: var(--ink-3); font-size: 12px; letter-spacing: .12em; }
.auth-layout { flex: 1; display: grid; grid-template-columns: 1fr 1fr; }
.auth-story {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
  padding: 48px 56px 36px;
  overflow: hidden;
  background: #e7eae6;
}
.auth-story-copy { position: relative; z-index: 1; max-width: 600px; }
.auth-kicker { display: flex; align-items: center; gap: 10px; margin: 0 0 24px; color: var(--ink-3); font-size: 12px; letter-spacing: .16em; }
.auth-kicker::before { content: ''; width: 24px; height: 1px; background: var(--brand); }
.auth-story h1 { margin: 0; font-family: var(--font-display); font-size: clamp(42px, 4.6vw, 66px); font-weight: 700; line-height: 1.22; letter-spacing: .04em; }
.auth-story-copy > p:last-child { max-width: 34rem; margin: 22px 0 0; color: var(--ink-3); font-size: 15px; line-height: 1.85; }
.auth-seal-visual { position: relative; width: 100%; max-width: 460px; min-height: 238px; margin-top: 28px; }
.auth-seal-word { position: absolute; bottom: -42px; left: -12px; color: rgb(40 46 42 / 7%); font-family: var(--font-display); font-size: 240px; font-weight: 700; line-height: 1; user-select: none; }
.auth-seal-statement { position: relative; z-index: 1; width: min(82%, 350px); padding: 24px 28px; border: 1px solid var(--ink); background: #e7eae6; }
.auth-seal-statement p { margin: 0; font-family: var(--font-display); font-size: 22px; line-height: 1.7; }
.auth-seal-statement small { display: block; margin-top: 16px; color: var(--ink-3); font-size: 10px; letter-spacing: .15em; }
.auth-seal-stamp { position: absolute; z-index: 2; right: 16px; bottom: 4px; display: grid; place-items: center; width: 84px; height: 84px; border: 3px double var(--brand); border-radius: 50%; color: var(--brand); font-family: var(--font-display); font-size: 18px; font-weight: 700; letter-spacing: .12em; transform: rotate(-12deg); }
.auth-feature-row { display: flex; flex-wrap: wrap; gap: 24px; margin-top: 24px; color: var(--ink-3); font-size: 12px; }
.auth-feature-row span { display: inline-flex; align-items: center; gap: 8px; }
.auth-feature-row span::before { content: ''; width: 5px; height: 5px; border-radius: 50%; background: var(--brand); }
.auth-form-panel { display: flex; align-items: center; justify-content: center; min-width: 0; padding: 40px 48px; background: var(--surface); }
.auth-form-wrap { width: 100%; max-width: 400px; }
.auth-form-heading { margin-bottom: 28px; }
.auth-form-heading > p:first-child { margin: 0 0 8px; color: var(--brand); font-size: 11px; font-weight: 600; letter-spacing: .14em; }
.auth-form-heading h2 { margin: 0; font-family: var(--font-display); font-size: 31px; line-height: 1.4; letter-spacing: .08em; }
.auth-form-heading > p:last-child { margin: 9px 0 0; color: var(--ink-3); font-size: 14px; }
.auth-form .el-form-item { margin-bottom: 18px; }
.auth-form .el-form-item__label { margin-bottom: 6px; color: var(--ink); font-size: 13px; font-weight: 600; }
.auth-form .el-input__wrapper { min-height: 50px; padding: 0 14px; border-radius: 3px; background: transparent; box-shadow: 0 0 0 1px var(--line) inset; transition: box-shadow 160ms ease, background-color 160ms ease; }
.auth-form .el-input__wrapper:hover { box-shadow: 0 0 0 1px var(--ink-3) inset; }
.auth-form .el-input__wrapper.is-focus { box-shadow: 0 0 0 1px var(--brand) inset, 0 0 0 3px var(--brand-bg); }
.auth-form .el-input__inner { color: var(--ink); font-size: 14px; }
.auth-form .el-input__inner::placeholder { color: var(--ink-4); }
.auth-submit { width: 100%; min-height: 50px; margin-top: 2px; border-radius: 3px; font-weight: 600; }
.auth-switch-row { display: flex; align-items: center; justify-content: center; gap: 6px; margin-top: 14px; color: var(--ink-3); font-size: 13px; }
.auth-switch-row .link { display: inline-flex; align-items: center; min-height: 44px; color: var(--brand); font-weight: 600; }
.auth-switch-row .link:hover { text-decoration: underline; text-underline-offset: 4px; }
.auth-demo { display: flex; align-items: flex-start; gap: 12px; margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--line); }
.auth-demo-badge { flex: 0 0 auto; padding: 2px 6px; border: 1px solid var(--line); border-radius: 2px; color: var(--ink-3); font-size: 9px; letter-spacing: .12em; }
.auth-demo p { margin: 0; color: var(--ink-3); font-size: 11px; line-height: 1.7; }
.auth-footer-line { display: flex; align-items: center; justify-content: space-between; gap: 16px; min-height: 46px; padding: 8px 40px; border-top: 1px solid var(--line); color: var(--ink-3); background: var(--surface); font-size: 10px; letter-spacing: .08em; }
@media (max-width: 900px) {
  .auth-story { padding: 40px 32px 32px; }
  .auth-form-panel { padding: 36px 32px; }
}
@media (max-width: 680px) {
  .auth-topbar { min-height: 72px; padding: 12px 20px; }
  .auth-topnote { display: none; }
  .auth-layout { display: block; }
  .auth-story { padding: 28px 20px; }
  .auth-story h1 { font-size: 34px; }
  .auth-story-copy > p:last-child { margin-top: 12px; font-size: 14px; }
  .auth-seal-visual, .auth-feature-row { display: none; }
  .auth-form-panel { padding: 30px 20px 36px; }
  .auth-footer-line { padding: 8px 20px; }
  .auth-footer-line span:last-child { display: none; }
}
</style>




