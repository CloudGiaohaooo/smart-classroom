<template>
  <div class="page-container">
    <div class="page-header">
      <h2>我的提交</h2>
      <div class="page-desc">查看已提交作业的得分与教师反馈</div>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="assignmentTitle" label="作业标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="courseName" label="课程" min-width="160" />
      <el-table-column prop="submitTime" label="提交时间" width="170">
        <template #default="{ row }">{{ row.submitTime?.replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'GRADED' ? 'success' : 'warning'">
            {{ row.status === 'GRADED' ? '已批阅' : '待批阅' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="得分" width="100">
        <template #default="{ row }">
          {{ row.score != null ? `${row.score} / ${row.totalScore}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="feedback" label="教师反馈" min-width="240" show-overflow-tooltip />
    </el-table>

    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      layout="total, prev, pager, next"
      :total="total"
      :current-page="query.current"
      :page-size="query.size"
      @current-change="onPageChange"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getMySubmissions } from '../../api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })

const load = async () => {
  loading.value = true
  try {
    const data = await getMySubmissions(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const onPageChange = (p) => {
  query.current = p
  load()
}

onMounted(load)
</script>
