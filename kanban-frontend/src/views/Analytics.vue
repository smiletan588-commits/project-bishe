<template>
  <div class="analytics-page">
    <header class="topbar">
      <div class="topbar-left">
        <button class="back-btn" @click="$router.push('/dashboard')">
          <el-icon><ArrowLeft /></el-icon> 返回主页
        </button>
        <span class="brand">SmartPM</span>
        <span class="divider">|</span>
        <span class="page-title">数据大屏</span>
      </div>
    </header>

    <main class="main" v-loading="loading">
      <!-- 四个概览卡片 -->
      <el-row :gutter="20" class="stat-row">
        <el-col :span="6">
          <div class="stat-card" style="--card-color: #D58A22;">
            <div class="stat-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ overview.totalProjects }}</span>
              <span class="stat-label">项目总数</span>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card" style="--card-color: #3B82F6;">
            <div class="stat-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="3"/><line x1="9" y1="9" x2="15" y2="9"/><line x1="9" y1="13" x2="15" y2="13"/><line x1="9" y1="17" x2="12" y2="17"/></svg>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ overview.totalTasks }}</span>
              <span class="stat-label">任务总数</span>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card" style="--card-color: #10B981;">
            <div class="stat-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="16 10 11 15 8 12"/></svg>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ todayCompleted }}</span>
              <span class="stat-label">今日完成</span>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card" style="--card-color: #F59E0B;">
            <div class="stat-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ todoCount }}</span>
              <span class="stat-label">待办任务</span>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 三个图表 -->
      <el-row :gutter="20" class="chart-row">
        <el-col :span="8">
          <div class="chart-card">
            <h4>任务状态占比</h4>
            <div ref="pieChartRef" class="chart-box"></div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="chart-card">
            <h4>项目任务量排行</h4>
            <div ref="barChartRef" class="chart-box"></div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="chart-card">
            <h4>近7天任务完工趋势</h4>
            <div ref="lineChartRef" class="chart-box"></div>
          </div>
        </el-col>
      </el-row>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const loading = ref(false)
const overview = ref({
  totalProjects: 0,
  totalTasks: 0,
  completedTasks: 0,
  inProgressTasks: 0,
  statusDistribution: [],
  projectTaskRanking: [],
  dailyCompletedTrend: []
})

const pieChartRef = ref(null)
const barChartRef = ref(null)
const lineChartRef = ref(null)
let pieChart = null
let barChart = null
let lineChart = null

const todayCompleted = computed(() => {
  const trend = overview.value.dailyCompletedTrend
  return trend.length ? trend[trend.length - 1].count : 0
})

const todoCount = computed(() => {
  const item = overview.value.statusDistribution.find(s => s.status === 'TODO')
  return item ? item.count : 0
})

async function fetchOverview() {
  loading.value = true
  try {
    const res = await request.get('/analytics/overview')
    overview.value = res.data?.data || overview.value
  } finally {
    loading.value = false
  }
}

function initCharts() {
  if (!pieChartRef.value || !barChartRef.value || !lineChartRef.value) return

  // ---- 饼图：任务状态占比 ----
  pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: '#94A3B8', fontSize: 12 } },
    color: ['#F59E0B', '#3B82F6', '#10B981'],
    series: [{
      type: 'pie', radius: ['55%', '78%'], center: ['50%', '48%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { scale: true, scaleSize: 6 },
      data: (overview.value.statusDistribution || []).map(item => ({
        name: statusLabel(item.status), value: item.count
      }))
    }]
  })

  // ---- 柱状图：项目任务量排行 ----
  barChart = echarts.init(barChartRef.value)
  const ranking = overview.value.projectTaskRanking || []
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 20, top: 10, bottom: 0, containLabel: true },
    xAxis: {
      type: 'value',
      axisLine: { show: false }, axisTick: { show: false },
      splitLine: { lineStyle: { color: '#E2E8F0', type: 'dashed' } }
    },
    yAxis: {
      type: 'category', inverse: true,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#64748B', fontSize: 11,
        formatter: v => v.length > 8 ? v.slice(0, 8) + '...' : v },
      data: ranking.map(r => r.projectName)
    },
    series: [{
      type: 'bar', barWidth: 16,
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#E2A43A' }, { offset: 1, color: '#A95D12' }
        ])
      },
      data: ranking.map(r => r.taskCount)
    }]
  })

  // ---- 折线图：近7天任务完工趋势 ----
  lineChart = echarts.init(lineChartRef.value)
  const trend = overview.value.dailyCompletedTrend || []
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 10, right: 20, top: 16, bottom: 0, containLabel: true },
    xAxis: {
      type: 'category', boundaryGap: false,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#94A3B8', fontSize: 10,
        formatter: v => v.slice(5) },
      data: trend.map(d => d.date)
    },
    yAxis: {
      type: 'value', minInterval: 1,
      axisLine: { show: false }, axisTick: { show: false },
      splitLine: { lineStyle: { color: '#E2E8F0', type: 'dashed' } }
    },
    series: [{
      type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
      lineStyle: { color: '#D58A22', width: 2.5 },
      itemStyle: { color: '#D58A22' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(99,102,241,0.25)' },
          { offset: 1, color: 'rgba(99,102,241,0.02)' }
        ])
      },
      data: trend.map(d => d.count)
    }]
  })
}

function statusLabel(s) {
  return { TODO: '待办', IN_PROGRESS: '进行中', DONE: '已完成' }[s] || s
}

function handleResize() {
  pieChart?.resize()
  barChart?.resize()
  lineChart?.resize()
}

onMounted(async () => {
  await fetchOverview()
  await nextTick()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  barChart?.dispose()
  lineChart?.dispose()
})
</script>

<style scoped>
.analytics-page { min-height: 100vh; background: var(--bg-base); }

.topbar {
  display: flex; align-items: center; height: 56px; padding: 0 24px;
  background: #242321; border-bottom: 1px solid #3A3732;
}
.topbar-left { display: flex; align-items: center; gap: 12px; }
.back-btn {
  display: inline-flex; align-items: center; gap: 4px;
  background: none; border: 1px solid #E2E8F0; color: #64748B;
  font-size: 13px; cursor: pointer; padding: 5px 12px; border-radius: 6px;
  transition: all 0.15s;
}
.back-btn:hover { border-color: #D58A22; color: #D58A22; }
.brand { font-size: 16px; font-weight: 700; color: #F7F1E7; }
.divider { color: #CBD5E1; }
.page-title { font-size: 14px; color: #64748B; font-weight: 500; }

.main { max-width: 1280px; margin: 0 auto; padding: 28px 24px; }

/* 统计卡片 */
.stat-row { margin-bottom: 20px; }
.stat-card {
  display: flex; align-items: center; gap: 16px;
  background: #242321; border-radius: var(--radius); padding: 22px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F1F5F9; transition: transform 0.15s, box-shadow 0.15s;
}
.stat-card:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.stat-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--card-color) 10%, transparent);
  color: var(--card-color); flex-shrink: 0;
}
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 28px; font-weight: 700; color: #F7F1E7; line-height: 1.1; }
.stat-label { font-size: 13px; color: #94A3B8; margin-top: 3px; }

/* 图表卡片 */
.chart-row { margin-bottom: 20px; }
.chart-card {
  background: #242321; border-radius: var(--radius); padding: 20px 20px 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #F1F5F9; height: 100%;
}
.chart-card h4 { margin: 0 0 12px; font-size: 15px; font-weight: 600; color: #F7F1E7; }
.chart-box { width: 100%; height: 300px; }

@media (max-width: 992px) {
  .stat-card { padding: 16px; }
  .stat-value { font-size: 22px; }
  .stat-icon { width: 40px; height: 40px; }
}
</style>
