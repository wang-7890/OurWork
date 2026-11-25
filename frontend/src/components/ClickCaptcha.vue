<template>
  <div class="click-captcha">
    <div class="captcha-header">
      <span class="captcha-tip">
        请依次点击：
        <strong v-for="(char, index) in targets" :key="`${challengeId}-${index}`">
          <em class="target-char">{{ char }}</em>
          <span v-if="index < targets.length - 1" class="target-sep">、</span>
        </strong>
      </span>

      <el-button text class="refresh-button" @click="regenerate">
        <el-icon><RefreshRight /></el-icon>
        换一张
      </el-button>
    </div>

    <div
      ref="boxRef"
      class="captcha-box"
      :class="{
        'is-success': status === 'success',
        'is-error': status === 'error'
      }"
    >
      <!-- 干扰线 -->
      <svg class="captcha-lines" viewBox="0 0 100 100" preserveAspectRatio="none">
        <line
          v-for="(line, index) in lines"
          :key="`${challengeId}-line-${index}`"
          :x1="line.x1"
          :y1="line.y1"
          :x2="line.x2"
          :y2="line.y2"
          :stroke="line.stroke"
          stroke-width="0.6"
        />
      </svg>

      <!-- 待点击汉字 -->
      <button
        v-for="(item, index) in chars"
        :key="`${challengeId}-char-${index}`"
        type="button"
        class="captcha-char"
        :class="{ 'is-clicked': clickedIndexes.includes(index) }"
        :style="{
          left: `${item.x}%`,
          top: `${item.y}%`,
          color: item.color,
          fontSize: `${item.size}px`,
          transform: `translate(-50%, -50%) rotate(${item.rotate}deg)`
        }"
        @click="handleCharClick(index)"
      >
        {{ item.char }}
      </button>

      <!-- 点击顺序标记 -->
      <span
        v-for="(mark, index) in clickMarks"
        :key="`${challengeId}-mark-${index}`"
        class="click-mark"
        :style="{ left: `${mark.x}%`, top: `${mark.y}%` }"
      >
        {{ index + 1 }}
      </span>

      <!-- 验证通过遮罩 -->
      <div v-if="status === 'success'" class="captcha-result success">
        <el-icon :size="26"><CircleCheckFilled /></el-icon>
        <span>验证通过</span>
      </div>

      <!-- 验证失败提示 -->
      <div v-else-if="status === 'error'" class="captcha-result error">
        <el-icon :size="26"><CircleCloseFilled /></el-icon>
        <span>{{ errorMessage }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  RefreshRight
} from '@element-plus/icons-vue'

// v-model 为 true 表示已通过验证
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

// 字库：一次挑战内不会出现重复汉字
const CHAR_POOL = [
  '微', '承', '诺', '对', '赌', '平', '台', '见', '证',
  '履', '约', '诚', '信', '守', '时', '目', '标', '押',
  '金', '裁', '决', '公', '开'
]

const COLOR_POOL = ['#35405a', '#5b5fef', '#0f8a6a', '#b45309', '#be3455', '#6d3fc9']

const TARGET_COUNT = 3
const TOTAL_COUNT = 6

const boxRef = ref()
const challengeId = ref(0)
const chars = ref([])
const targets = ref([])
const lines = ref([])
const clickedIndexes = ref([])
const clickMarks = ref([])
const status = ref('idle')
const errorMessage = ref('')

function randomBetween(min, max) {
  return min + Math.random() * (max - min)
}

function pickDistinct(pool, count) {
  const copied = [...pool]

  for (let i = copied.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[copied[i], copied[j]] = [copied[j], copied[i]]
  }

  return copied.slice(0, count)
}

// 简单的防重叠摆放：距离过近就重新随机，最多尝试 80 次
function buildPositions(count) {
  const positions = []
  let attempts = 0

  while (positions.length < count && attempts < 80) {
    attempts += 1

    const point = {
      x: randomBetween(10, 90),
      y: randomBetween(22, 80)
    }

    // 容器宽高比约 2.5:1，纵向距离按比例放大后再比较
    const tooClose = positions.some((item) => {
      const dx = point.x - item.x
      const dy = (point.y - item.y) * 2.5
      return Math.sqrt(dx * dx + dy * dy) < 20
    })

    if (!tooClose) {
      positions.push(point)
    }
  }

  // 极端情况下没排满，用网格兜底
  while (positions.length < count) {
    positions.push({
      x: 12 + positions.length * 15,
      y: 50
    })
  }

  return positions
}

function buildLines() {
  return Array.from({ length: 3 }, () => ({
    x1: randomBetween(0, 30),
    y1: randomBetween(0, 100),
    x2: randomBetween(70, 100),
    y2: randomBetween(0, 100),
    stroke: COLOR_POOL[Math.floor(Math.random() * COLOR_POOL.length)]
  }))
}

function regenerate() {
  challengeId.value += 1
  status.value = 'idle'
  errorMessage.value = ''
  clickedIndexes.value = []
  clickMarks.value = []
  emit('update:modelValue', false)

  const picked = pickDistinct(CHAR_POOL, TOTAL_COUNT)
  const positions = buildPositions(TOTAL_COUNT)

  targets.value = picked.slice(0, TARGET_COUNT)

  chars.value = picked.map((char, index) => ({
    char,
    x: positions[index].x,
    y: positions[index].y,
    rotate: Math.round(randomBetween(-30, 30)),
    size: Math.round(randomBetween(22, 29)),
    color: COLOR_POOL[Math.floor(Math.random() * COLOR_POOL.length)]
  }))

  lines.value = buildLines()
}

function handleCharClick(index) {
  if (status.value === 'success' || clickedIndexes.value.includes(index)) {
    return
  }

  const expectedChar = targets.value[clickedIndexes.value.length]
  const clickedChar = chars.value[index].char

  if (clickedChar !== expectedChar) {
    status.value = 'error'
    errorMessage.value = '点选顺序不正确，即将刷新'

    window.setTimeout(() => {
      regenerate()
    }, 900)
    return
  }

  clickedIndexes.value.push(index)
  clickMarks.value.push({
    x: chars.value[index].x,
    y: chars.value[index].y
  })

  if (clickedIndexes.value.length === targets.value.length) {
    status.value = 'success'
    emit('update:modelValue', true)
    emit('success')
  }
}

defineExpose({
  reset: regenerate
})

onMounted(() => {
  regenerate()
})
</script>

<style scoped>
.click-captcha {
  width: 100%;
}

.captcha-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.captcha-tip {
  color: #344054;
  font-size: 13px;
}

.target-char {
  color: #5b5fef;
  font-style: normal;
  font-weight: 700;
  font-size: 15px;
}

.target-sep {
  margin: 0 2px;
}

.refresh-button {
  flex-shrink: 0;
  padding: 0;
  color: #667085;
  font-size: 12px;
}

.captcha-box {
  position: relative;
  width: 100%;
  height: 168px;
  overflow: hidden;
  border: 1px solid #d7dbe8;
  border-radius: 10px;
  background:
    radial-gradient(circle at 20% 30%, rgb(91 95 239 / 6%), transparent 40%),
    radial-gradient(circle at 80% 70%, rgb(22 163 106 / 6%), transparent 40%),
    #f7f8fc;
  transition: border-color 0.2s;
}

.captcha-box.is-success {
  border-color: #16a36a;
}

.captcha-box.is-error {
  border-color: #e05252;
  animation: captcha-shake 0.35s;
}

@keyframes captcha-shake {
  0%,
  100% {
    transform: translateX(0);
  }

  25% {
    transform: translateX(-5px);
  }

  75% {
    transform: translateX(5px);
  }
}

.captcha-lines {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.captcha-char {
  position: absolute;
  padding: 2px 4px;
  border: 0;
  background: transparent;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  user-select: none;
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.captcha-char:hover {
  opacity: 0.75;
}

.captcha-char.is-clicked {
  opacity: 0.45;
  cursor: default;
}

.click-mark {
  position: absolute;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #5b5fef;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  transform: translate(10px, -18px);
  pointer-events: none;
  box-shadow: 0 2px 6px rgb(91 95 239 / 40%);
}

.captcha-result {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  background: rgb(255 255 255 / 82%);
}

.captcha-result.success {
  color: #16a36a;
}

.captcha-result.error {
  color: #e05252;
}
</style>
