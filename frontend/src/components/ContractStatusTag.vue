<template>
  <el-tag
    :type="tagType"
    effect="light"
    round
    class="contract-status-tag"
    disable-transitions
  >
    {{ statusText }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'
import {
  CONTRACT_STATUS_TAG_TYPE,
  getContractStatusText
} from '../utils/contract'

const props = defineProps({
  status: {
    type: String,
    required: true
  }
})

const statusText = computed(() => getContractStatusText(props.status))

const tagType = computed(
  () => CONTRACT_STATUS_TAG_TYPE[props.status] || 'info'
)
</script>

<style scoped>
.contract-status-tag {
  font-weight: 600;
}
</style>
