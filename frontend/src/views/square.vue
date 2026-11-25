<template>
  <div class="square-page">
     <header class="hero-banner">
       <div class="hero-blob blob-a"></div>
       <div class="hero-blob blob-b"></div>
       <div class="hero-grid-overlay"></div>

       <div class="hero-content">
         <div class="hero-badge">公开承诺 · 共同见证</div>
         <h1>发现值得见证的目标</h1>
         <p>立下目标、押上押金、邀请朋友见证——按时兑现取回押金</p>

         <el-button
           v-if="authStore.isLoggedIn"
           class="hero-cta"
           size="large"
           @click="router.push('/contract/create')"
         >
           <el-icon><Promotion /></el-icon>
           发起对赌
         </el-button>
         <el-button
           v-else
           class="hero-cta"
           size="large"
           @click="router.push('/login')"
         >
           登录后发起对赌
         </el-button>
       </div>

       <div class="hero-deco">
         <div class="deco-card deco-card-1">
           <span class="deco-check">✓</span>
           <div>
             <strong>30 天背完 1000 个单词</strong>
             <span>见证人 3/3 已投 · 履约成功</span>
           </div>
         </div>
         <div class="deco-card deco-card-2">
           <span class="deco-coin">押</span>
           <div>
             <strong>押金 200 币已冻结</strong>
             <span>履约后全额退回</span>
           </div>
         </div>
       </div>
     </header>

     <el-card class="filter-card" shadow="never">
       <div class="filter-row">
         <el-input
           v-model.trim="filters.keyword"
           class="keyword-input"
           placeholder="搜索合约标题或验收标准"
           clearable
           @keyup.enter="handleSearch"
           @clear="handleSearch"
         >
           <template #prefix>
             <el-icon><Search /></el-icon>
           </template>
         </el-input>

         <el-select
           v-model="filters.category"
           class="filter-select"
           placeholder="全部分类"
           clearable
           @change="handleSearch"
         >
           <el-option
             v-for="(text, value) in CONTRACT_CATEGORY_TEXT"
             :key="value"
             :label="text"
             :value="value"
           />
         </el-select>

         <el-select
           v-model="filters.status"
           class="filter-select"
           placeholder="全部状态"
           clearable
           @change="handleSearch"
         >
           <el-option
             v-for="(text, value) in CONTRACT_STATUS_TEXT"
             :key="value"
             :label="text"
             :value="value"
           />
         </el-select>

         <el-button type="primary" @click="handleSearch">查询</el-button>
         <el-button @click="handleReset">重置</el-button>
       </div>
     </el-card>

     <div v-loading="loading" class="contract-grid-wrapper">
       <div v-if="contracts.length" class="contract-grid">
         <ContractCard
           v-for="contract in contracts"
           :key="contract.contractId"
           :contract="contract"
         />
       </div>

       <el-empty
         v-else-if="!loading"
         class="empty-state"
         description="没有符合条件的合约，换个关键词试试"
       />
     </div>

     <PagePagination
       :page="pagination.page"
       :size="pagination.size"
       :total="pagination.total"
       @change="handlePageChange"
     />
   </div>
 </template>

 <script setup>
 import { onMounted, reactive, ref } from 'vue'
 import { useRouter } from 'vue-router'
 import { Promotion, Search } from '@element-plus/icons-vue'
 import ContractCard from '../components/ContractCard.vue'
 import PagePagination from '../components/PagePagination.vue'
 import { getSquareContractsApi } from '../api/square'
 import { useAuthStore } from '../stores/auth'
 import {
   CONTRACT_CATEGORY_TEXT,
   CONTRACT_STATUS_TEXT
 } from '../utils/contract'

 const router = useRouter()
 const authStore = useAuthStore()

 const loading = ref(false)
 const contracts = ref([])

 const filters = reactive({
   keyword: '',
   category: '',
   status: ''
 })

 const pagination = reactive({
   page: 1,
   size: 9,
   total: 0
 })

 async function loadContracts() {
   loading.value = true

   try {
     const data = await getSquareContractsApi({
       page: pagination.page,
       size: pagination.size,
       keyword: filters.keyword || undefined,
       category: filters.category || undefined,
       status: filters.status || undefined
     })

     contracts.value = data.records
     pagination.total = data.total
   } catch {
     // request.js 已统一提示错误
     contracts.value = []
     pagination.total = 0
   } finally {
     loading.value = false
   }
 }

 function handleSearch() {
   pagination.page = 1
   loadContracts()
 }

 function handleReset() {
   filters.keyword = ''
   filters.category = ''
   filters.status = ''
   pagination.page = 1
   loadContracts()
 }

 function handlePageChange({ page, size }) {
   pagination.page = page
   pagination.size = size
   loadContracts()
 }

 onMounted(() => {
   loadContracts()
 })
 </script>

 <style scoped>
 .square-page {
   max-width: 1200px;
   margin: 0 auto;
   padding: 32px 24px 48px;
 }

 .page-header {
   display: flex;
   justify-content: space-between;
   align-items: flex-end;
   gap: 16px;
   margin-bottom: 24px;
 }

 .page-header h1 {
   margin: 0 0 8px;
   color: #182230;
   font-size: 28px;
 }

 .page-header p {
   margin: 0;
   color: #667085;
 }

 /* ===== Hero 横幅 ===== */
 .hero-banner {
   position: relative;
   display: flex;
   justify-content: space-between;
   align-items: center;
   gap: 32px;
   margin-bottom: 24px;
   padding: 44px 48px;
   border-radius: 20px;
   overflow: hidden;
   color: #fff;
   background: linear-gradient(120deg, #2d3278 0%, #4b4ee8 52%, #6f73ff 100%);
   box-shadow: 0 20px 48px rgb(45 50 120 / 26%);
   animation: rise-in 0.5s ease both;
 }

 .hero-grid-overlay {
   position: absolute;
   inset: 0;
   background-image:
     linear-gradient(rgb(255 255 255 / 5%) 1px, transparent 1px),
     linear-gradient(90deg, rgb(255 255 255 / 5%) 1px, transparent 1px);
   background-size: 42px 42px;
   mask-image: radial-gradient(600px 320px at 20% 40%, #000 30%, transparent 100%);
   pointer-events: none;
 }

 .hero-blob {
   position: absolute;
   border-radius: 50%;
   filter: blur(2px);
   pointer-events: none;
 }

 .blob-a {
   width: 260px;
   height: 260px;
   right: -60px;
   top: -100px;
   background: radial-gradient(circle, rgb(255 255 255 / 16%), transparent 68%);
 }

 .blob-b {
   width: 180px;
   height: 180px;
   left: 38%;
   bottom: -90px;
   background: radial-gradient(circle, rgb(143 148 255 / 40%), transparent 70%);
 }

 .hero-content {
   position: relative;
   z-index: 1;
   max-width: 620px;
 }

 .hero-badge {
   display: inline-flex;
   padding: 6px 14px;
   border: 1px solid rgb(255 255 255 / 26%);
   border-radius: 999px;
   background: rgb(255 255 255 / 12%);
   font-size: 13px;
   letter-spacing: 1px;
   backdrop-filter: blur(4px);
 }

 .hero-content h1 {
   margin: 18px 0 12px;
   font-size: clamp(28px, 3.6vw, 40px);
   line-height: 1.2;
   letter-spacing: 0.5px;
 }

 .hero-content p {
   margin: 0 0 26px;
   color: #d6d9ff;
   font-size: 15px;
   line-height: 1.8;
 }

 .hero-cta {
   --el-button-text-color: #4b4ee8;
   height: 46px;
   padding: 0 28px;
   border: 0;
   border-radius: 12px;
   background: #fff;
   color: #4b4ee8;
   font-weight: 700;
   box-shadow: 0 10px 24px rgb(24 34 48 / 22%);
   transition: transform 0.18s ease, box-shadow 0.18s ease;
 }

 .hero-cta:hover,
 .hero-cta:focus {
   background: #f4f5ff;
   color: #2d3278;
   transform: translateY(-2px);
   box-shadow: 0 14px 30px rgb(24 34 48 / 28%);
 }

 /* 右侧漂浮装饰卡片 */
 .hero-deco {
   position: relative;
   z-index: 1;
   display: flex;
   flex-direction: column;
   gap: 16px;
   flex-shrink: 0;
 }

 .deco-card {
   display: flex;
   align-items: center;
   gap: 12px;
   width: 300px;
   padding: 14px 18px;
   border: 1px solid rgb(255 255 255 / 22%);
   border-radius: 14px;
   background: rgb(255 255 255 / 12%);
   backdrop-filter: blur(8px);
   box-shadow: 0 12px 28px rgb(24 34 48 / 18%);
 }

 .deco-card-1 {
   animation: float-slow 5s ease-in-out infinite;
 }

 .deco-card-2 {
   margin-left: 36px;
   animation: float-slow 5s ease-in-out 1.2s infinite;
 }

 .deco-card strong,
 .deco-card span {
   display: block;
 }

 .deco-card strong {
   font-size: 14px;
   font-weight: 600;
 }

 .deco-card div > span {
   margin-top: 3px;
   color: #cdd1ff;
   font-size: 12px;
 }

 .deco-check,
 .deco-coin {
   display: flex;
   align-items: center;
   justify-content: center;
   width: 36px;
   height: 36px;
   flex-shrink: 0;
   border-radius: 10px;
   font-size: 15px;
   font-weight: 700;
 }

 .deco-check {
   background: rgb(22 163 106 / 90%);
   color: #fff;
 }

 .deco-coin {
   background: rgb(255 255 255 / 92%);
   color: #b45309;
 }

 .filter-card {
   margin-bottom: 24px;
   border: 1px solid #e7eaf0;
   border-radius: 14px;
 }

 .filter-row {
   display: flex;
   gap: 12px;
   flex-wrap: wrap;
 }

 .keyword-input {
   flex: 1;
   min-width: 240px;
 }

 .filter-select {
   width: 150px;
 }

 .contract-grid-wrapper {
   min-height: 200px;
 }

 .contract-grid {
   display: grid;
   grid-template-columns: repeat(3, 1fr);
   gap: 20px;
 }

 .empty-state {
   padding: 60px 0;
 }

 @media (max-width: 1024px) {
   .contract-grid {
     grid-template-columns: repeat(2, 1fr);
   }

   .hero-deco {
     display: none;
   }
 }

 @media (max-width: 640px) {
   .contract-grid {
     grid-template-columns: 1fr;
   }

   .hero-banner {
     padding: 32px 24px;
   }
 }
 </style>