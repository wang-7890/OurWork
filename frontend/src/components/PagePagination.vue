<template>
  <div class="pagination-wrapper" v-if="total > 0">
    <el-pagination
      :current-page="page"
      :page-size="size"
      :total="total"
      :page-sizes="pageSizes"
      background
      layout="total, sizes, prev, pager, next"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
const props = defineProps({
  page: {
    type: Number,
    required: true
  },
  size: {
    type: Number,
    required: true
  },
  total: {
    type: Number,
    required: true
  },
  pageSizes: {
    type: Array,
    default: () => [9, 12, 20]
  }
})

const emit = defineEmits(['change'])

function handlePageChange(newPage) {
  emit('change', { page: newPage, size: props.size })
}

function handleSizeChange(newSize) {
  emit('change', { page: 1, size: newSize })
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>