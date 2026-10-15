<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top: 16px">
      <template #header>各课程作业数量分布</template>
      <div ref="chartRef" style="height: 360px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import * as echarts from 'echarts'
import { getStats } from '../api'

const stats = ref(null)
const chartRef = ref()
let chart = null

const cards = computed(() => [
  { label: '课程总数', value: stats.value?.courseCount ?? 0, color: '#1677ff' },
  { label: '教师人数', value: stats.value?.teacherCount ?? 0, color: '#52c41a' },
  { label: '学生人数', value: stats.value?.studentCount ?? 0, color: '#722ed1' },
  { label: '作业总数', value: stats.value?.assignmentCount ?? 0, color: '#fa8c16' },
  { label: '待批阅', value: stats.value?.pendingGradeCount ?? 0, color: '#f5222d' },
  { label: '课程资源', value: stats.value?.resourceCount ?? 0, color: '#13c2c2' }
])

const renderChart = () => {
  const list = stats.value?.courseAssignmentStats || []
  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 40 },
    xAxis: {
      type: 'category',
      data: list.map((i) => i.courseName)
    },
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

onMounted(async () => {
  stats.value = await getStats()
  renderChart()
  window.addEventListener('resize', () => chart?.resize())
})

onBeforeUnmount(() => {
  chart?.dispose()
})
</script>

<style scoped>
.stat-card {
  text-align: center;
}
.stat-value {
  font-size: 30px;
  font-weight: bold;
}
.stat-label {
  margin-top: 8px;
  color: #666;
  font-size: 13px;
}
</style>
