<template>
  <AppShell>
    <main class="analytics-main" v-loading="loading">
      <PageHeader eyebrow="数据分析" title="数据大屏">
        <template #actions><el-button :loading="loading" @click="fetchOverview">刷新数据</el-button></template>
      </PageHeader>

      <section class="metrics-band" aria-label="项目核心指标">
        <div v-for="metric in metrics" :key="metric.label" class="metric-item">
          <span>{{ metric.label }}</span><strong>{{ metric.value }}</strong><small>{{ metric.note }}</small>
        </div>
      </section>

      <section class="chart-grid">
        <article class="chart-panel chart-panel-main">
          <header><div><span>交付趋势</span><h2>近 7 天任务完工趋势</h2></div><small>按完成日期统计</small></header>
          <div ref="lineChartRef" class="chart-box chart-box-main" />
        </article>
        <article class="chart-panel">
          <header><div><span>任务结构</span><h2>任务状态占比</h2></div></header>
          <div ref="pieChartRef" class="chart-box" />
        </article>
        <article class="chart-panel">
          <header><div><span>项目对比</span><h2>项目任务量排行</h2></div></header>
          <div ref="barChartRef" class="chart-box" />
        </article>
      </section>
    </main>
  </AppShell>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const overview = ref({ totalProjects: 0, totalTasks: 0, completedTasks: 0, inProgressTasks: 0, statusDistribution: [], projectTaskRanking: [], dailyCompletedTrend: [] })
const pieChartRef = ref(null)
const barChartRef = ref(null)
const lineChartRef = ref(null)
let pieChart
let barChart
let lineChart

const todayCompleted = computed(() => {
  const trend = overview.value.dailyCompletedTrend
  return trend.length ? trend[trend.length - 1].count : 0
})
const todoCount = computed(() => overview.value.statusDistribution.find(item => item.status === 'TODO')?.count || 0)
const metrics = computed(() => [
  { label: '项目总数', value: overview.value.totalProjects, note: '当前参与项目' },
  { label: '任务总数', value: overview.value.totalTasks, note: '全部项目任务' },
  { label: '今日完成', value: todayCompleted.value, note: '本日交付' },
  { label: '待办任务', value: todoCount.value, note: '等待处理' }
])

async function fetchOverview() {
  loading.value = true
  try {
    const res = await request.get('/analytics/overview')
    overview.value = res.data?.data || overview.value
    await nextTick()
    updateCharts()
  } finally { loading.value = false }
}

const axisText = { color: '#778296', fontSize: 11, fontFamily: 'system-ui, sans-serif' }
function updateCharts() {
  if (!pieChartRef.value || !barChartRef.value || !lineChartRef.value) return
  pieChart ||= echarts.init(pieChartRef.value)
  barChart ||= echarts.init(barChartRef.value)
  lineChart ||= echarts.init(lineChartRef.value)

  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: axisText },
    color: ['#b76e14', '#315ee7', '#287a58'],
    series: [{ type: 'pie', radius: ['56%', '76%'], center: ['50%', '45%'], itemStyle: { borderRadius: 3, borderColor: '#fbfcfe', borderWidth: 3 }, label: { show: false }, emphasis: { scaleSize: 4 }, data: (overview.value.statusDistribution || []).map(item => ({ name: statusLabel(item.status), value: item.count })) }]
  })

  const ranking = overview.value.projectTaskRanking || []
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 16, top: 8, bottom: 4, containLabel: true },
    xAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false }, axisLabel: axisText, splitLine: { lineStyle: { color: '#e4e8ef' } } },
    yAxis: { type: 'category', inverse: true, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { ...axisText, formatter: value => value.length > 8 ? `${value.slice(0, 8)}...` : value }, data: ranking.map(item => item.projectName) },
    series: [{ type: 'bar', barWidth: 13, itemStyle: { borderRadius: [0, 4, 4, 0], color: '#315ee7' }, data: ranking.map(item => item.taskCount) }]
  })

  const trend = overview.value.dailyCompletedTrend || []
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 10, right: 18, top: 20, bottom: 4, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, axisLine: { lineStyle: { color: '#dbe1ea' } }, axisTick: { show: false }, axisLabel: { ...axisText, formatter: value => value.slice(5) }, data: trend.map(item => item.date) },
    yAxis: { type: 'value', minInterval: 1, axisLine: { show: false }, axisTick: { show: false }, axisLabel: axisText, splitLine: { lineStyle: { color: '#e4e8ef' } } },
    series: [{ type: 'line', smooth: true, symbol: 'circle', symbolSize: 7, lineStyle: { color: '#315ee7', width: 2 }, itemStyle: { color: '#315ee7', borderColor: '#fff', borderWidth: 2 }, areaStyle: { color: 'rgba(49,94,231,.08)' }, data: trend.map(item => item.count) }]
  })
}

function statusLabel(status) { return { TODO: '待办', IN_PROGRESS: '进行中', DONE: '已完成' }[status] || status }
function handleResize() { pieChart?.resize(); barChart?.resize(); lineChart?.resize() }
onMounted(async () => { await fetchOverview(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); pieChart?.dispose(); barChart?.dispose(); lineChart?.dispose() })
</script>

<style scoped>
.analytics-main { max-width: 1320px; margin: 0 auto; padding: 34px 36px 52px; }
.metrics-band { display: grid; grid-template-columns: repeat(4, 1fr); margin: 24px 0; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); box-shadow: var(--shadow-xs); }
.metric-item { min-width: 0; padding: 20px 22px; }
.metric-item + .metric-item { border-left: 1px solid var(--border-light); }
.metric-item span,.metric-item small { display: block; color: var(--text-tertiary); }
.metric-item span { font-size: 12px; font-weight: 650; }
.metric-item strong { display: block; margin: 7px 0 3px; color: var(--text-primary); font-size: 30px; line-height: 1; font-variant-numeric: tabular-nums; }
.metric-item small { font-size: 11px; }
.chart-grid { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(300px, .65fr); gap: 18px; }
.chart-panel { min-width: 0; padding: 20px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface); box-shadow: var(--shadow-xs); }
.chart-panel-main { grid-row: span 2; }
.chart-panel header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.chart-panel header span,.chart-panel header small { color: var(--text-tertiary); font-size: 11px; font-weight: 650; }
.chart-panel h2 { margin: 4px 0 0; color: var(--text-primary); font-size: 16px; }
.chart-box { width: 100%; height: 240px; }
.chart-box-main { height: 534px; }
@media (max-width: 1024px) { .analytics-main { padding: 28px 24px 44px; }.chart-grid { grid-template-columns: 1fr 1fr; }.chart-panel-main { grid-column: 1 / -1; grid-row: auto; }.chart-box-main { height: 340px; } }
@media (max-width: 767px) { .analytics-main { padding: 22px 16px 36px; }.metrics-band { grid-template-columns: 1fr 1fr; }.metric-item:nth-child(3) { border-left: 0; border-top: 1px solid var(--border-light); }.metric-item:nth-child(4) { border-top: 1px solid var(--border-light); }.metric-item { padding: 16px; }.metric-item strong { font-size: 25px; }.chart-grid { grid-template-columns: 1fr; }.chart-panel-main { grid-column: auto; }.chart-box,.chart-box-main { height: 280px; } }
</style>
