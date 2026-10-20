<template>
  <div class="page-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="选课中心"
      description="浏览全部课程并自主选修 / 退课；选修后即可在「课程管理」「作业列表」「课程资源」中看到对应内容。"
      style="margin-bottom: 16px"
    />

    <div class="page-toolbar">
      <el-input v-model="keyword" placeholder="课程名称 / 编号" clearable style="width: 240px" />
      <div class="toolbar-spacer"></div>
      <el-tag type="success">已选 {{ enrolledIds.size }} 门</el-tag>
    </div>

    <el-table :data="filtered" v-loading="loading" border stripe>
      <el-table-column prop="courseName" label="课程名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="courseCode" label="课程编号" width="140" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="semester" label="学期" min-width="180" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '进行中' : '已结课' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="选修状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="enrolledIds.has(row.id)" type="success">已选</el-tag>
          <el-tag v-else type="info">未选</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="!enrolledIds.has(row.id)" link type="primary" :loading="busyId === row.id" @click="onEnroll(row)">
            选课
          </el-button>
          <el-button v-else link type="danger" :loading="busyId === row.id" @click="onUnenroll(row)">
            退课
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && !filtered.length" class="empty-tip">暂无课程</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllCourses, getMyCourses, enrollCourse, unenrollCourse } from '../../api'

const allCourses = ref([])
const enrolledIds = ref(new Set())
const loading = ref(false)
const keyword = ref('')
const busyId = ref(null)

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return allCourses.value
  return allCourses.value.filter(
    (c) =>
      (c.courseName || '').toLowerCase().includes(kw) ||
      (c.courseCode || '').toLowerCase().includes(kw)
  )
})

const load = async () => {
  loading.value = true
  try {
    const [all, mine] = await Promise.all([getAllCourses(), getMyCourses()])
    allCourses.value = all || []
    enrolledIds.value = new Set((mine || []).map((c) => c.id))
  } finally {
    loading.value = false
  }
}

const onEnroll = async (row) => {
  if (busyId.value !== null) return
  busyId.value = row.id
  try {
    await enrollCourse(row.id)
    ElMessage.success(`已选修「${row.courseName}」`)
    enrolledIds.value = new Set(enrolledIds.value).add(row.id)
  } finally {
    busyId.value = null
  }
}

const onUnenroll = async (row) => {
  if (busyId.value !== null) return
  busyId.value = row.id
  try {
    await ElMessageBox.confirm(`确定退选「${row.courseName}」吗？退选后将无法查看该课程的作业与资源。`, '提示', {
      type: 'warning'
    })
    await unenrollCourse(row.id)
    ElMessage.success(`已退选「${row.courseName}」`)
    const next = new Set(enrolledIds.value)
    next.delete(row.id)
    enrolledIds.value = next
  } catch (e) {
    // 取消退选：忽略
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.empty-tip {
  color: #909399;
  padding: 24px;
  text-align: center;
}
</style>
