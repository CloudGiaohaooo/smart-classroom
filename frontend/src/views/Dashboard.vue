<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
            <el-icon :size="20"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-meta">
            <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>各课程作业数量分布</template>
          <div v-if="hasCourseData" ref="chartRef" style="height: 320px"></div>
          <div v-else class="chart-empty" style="height: 320px">暂无课程作业数据</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>各课程资源访问热度（AOP 埋点）</template>
          <div v-if="hasHeatData" ref="heatRef" style="height: 320px"></div>
          <div v-else class="chart-empty" style="height: 320px">暂无资源访问数据</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top: 16px" v-if="isTeacher">
      <template #header>近 {{ trendDays }} 天提交量趋势（AOP 埋点）</template>
      <div v-if="hasTrendData" ref="trendRef" style="height: 280px"></div>
      <div v-else class="chart-empty" style="height: 280px">暂无提交趋势数据</div>
    </el-card>

    <el-card shadow="hover" style="margin-top: 16px" v-if="isTeacher">
      <template #header>学习预警列表（长期未活跃学生）</template>
      <el-table :data="warnings" border stripe v-loading="behaviorLoading">
        <el-table-column prop="studentName" label="学生" width="140" />
        <el-table-column prop="reason" label="预警原因" min-width="220" />
        <el-table-column label="最近活跃时间" width="180">
          <template #default="{ row }">{{ row.lastActiveTime ? row.lastActiveTime.replace('T', ' ') : '从未活跃' }}</template>
        </el-table-column>
        <el-table-column label="已静默" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.inactiveDays == null" type="danger">从未活跃</el-tag>
            <el-tag v-else type="warning">{{ row.inactiveDays }} 天</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!behaviorLoading && !warnings.length" style="color: #909399; padding: 16px; text-align: center">
        暂无预警学生
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
// 按需引入 ECharts，避免全量打包（Dashboard chunk 从 ~1MB 降到 ~400KB）
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getStats, getBehaviorDashboard } from '../api'
import { useUserStore } from '../store/user'

echarts.use([BarChart, LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const userStore = useUserStore()
const isTeacher = computed(() => userStore.isTeacher)

const stats = ref(null)
const behavior = ref(null)
const behaviorLoading = ref(false)
const chartRef = ref()
const heatRef = ref()
const trendRef = ref()
let chart = null
let heatChart = null
let trendChart = null

const cards = computed(() => [
  { label: '课程总数', value: stats.value?.courseCount ?? 0, color: '#2f54eb', bg: '#eef1ff', icon: 'Notebook' },
  { label: '教师人数', value: stats.value?.teacherCount ?? 0, color: '#389e0d', bg: '#f0fbea', icon: 'User' },
  { label: '学生人数', value: stats.value?.studentCount ?? 0, color: '#722ed1', bg: '#f6ecfd', icon: 'UserFilled' },
  { label: '作业总数', value: stats.value?.assignmentCount ?? 0, color: '#d46b08', bg: '#fff5e8', icon: 'Document' },
  { label: '待批阅', value: stats.value?.pendingGradeCount ?? 0, color: '#cf1322', bg: '#fff0f0', icon: 'EditPen' },
  { label: '课程资源', value: stats.value?.resourceCount ?? 0, color: '#08979c', bg: '#e6fffb', icon: 'FolderOpened' }
])

const warnings = computed(() => behavior.value?.warningList || [])
const trendDays = computed(() => behavior.value?.submitTrend?.length || 7)
const hasCourseData = computed(() => (stats.value?.courseAssignmentStats || []).length > 0)
const hasHeatData = computed(() => (behavior.value?.resourceHeat || []).length > 0)
const hasTrendData = computed(() => (behavior.value?.submitTrend || []).length > 0)

const renderCourseChart = () => {
  if (!chartRef.value) return
  const list = stats.value?.courseAssignmentStats || []
  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: list.map((i) => i.courseName) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '作业数量',
        type: 'bar',
        barWidth: 40,
        data: list.map((i) => i.assignmentCount),
        itemStyle: { color: '#1677ff', borderRadius: [4, 4, 0, 0] }
      }
    ]
  })
}

const renderHeatChart = () => {
  if (!heatRef.value) return
  const list = behavior.value?.resourceHeat || []
  heatChart = echarts.init(heatRef.value)
  heatChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: list.map((i) => i.name) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '浏览次数',
        type: 'bar',
        barWidth: 36,
        data: list.map((i) => i.count),
        itemStyle: { color: '#13c2c2', borderRadius: [4, 4, 0, 0] }
      }
    ]
  })
}

const renderTrendChart = () => {
  if (!trendRef.value) return
  const list = behavior.value?.submitTrend || []
  trendChart = echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: list.map((i) => i.date) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '提交量',
        type: 'line',
        smooth: true,
        data: list.map((i) => i.count),
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#1677ff' }
      }
    ]
  })
}

const handleResize = () => {
  chart?.resize()
  heatChart?.resize()
  trendChart?.resize()
}

onMounted(async () => {
  try {
    stats.value = await getStats()
    if (hasCourseData.value) renderCourseChart()
  } catch (e) {
    // 统计加载失败不阻断页面，仅留空卡片
  }
  // 学习行为看板为教师端能力，学生不请求（避免无权限报错）
  if (isTeacher.value) {
    behaviorLoading.value = true
    try {
      behavior.value = await getBehaviorDashboard()
      if (hasHeatData.value) renderHeatChart()
      if (hasTrendData.value) renderTrendChart()
    } catch (e) {
      // 行为看板加载失败不阻断页面
    } finally {
      behaviorLoading.value = false
    }
  }
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  heatChart?.dispose()
  trendChart?.dispose()
  chart = null
  heatChart = null
  trendChart = null
})
</script>

<style scoped>
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 16px;
}
.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-meta {
  text-align: left;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-label {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}
.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 13px;
}
</style>
