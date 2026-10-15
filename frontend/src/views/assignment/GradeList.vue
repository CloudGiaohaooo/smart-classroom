<template>
  <div class="page-container">
    <div class="page-toolbar">
      <el-select v-model="query.assignmentId" placeholder="全部作业" clearable style="width: 260px" @change="load">
        <el-option v-for="a in assignments" :key="a.id" :label="a.title" :value="a.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px" @change="load">
        <el-option label="待批阅" value="SUBMITTED" />
        <el-option label="已批阅" value="GRADED" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="assignmentTitle" label="作业标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="courseName" label="课程" min-width="160" />
      <el-table-column prop="studentName" label="学生" width="100" />
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
      <el-table-column prop="score" label="得分" width="80" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openGrade(row)">{{ row.status === 'GRADED' ? '修改批阅' : '批阅' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      layout="total, prev, pager, next"
      :total="total"
      :current-page="query.current"
      :page-size="query.size"
      @current-change="onPageChange"
    />

    <el-dialog v-model="dialogVisible" title="作业批阅" width="520px">
      <el-descriptions :column="2" border style="margin-bottom: 16px">
        <el-descriptions-item label="学生">{{ gradeForm.studentName }}</el-descriptions-item>
        <el-descriptions-item label="作业">{{ gradeForm.assignmentTitle }}</el-descriptions-item>
        <el-descriptions-item label="提交内容" :span="2">{{ gradeForm.content || '（无）' }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="得分" required>
          <el-input-number v-model="gradeForm.score" :min="0" :max="Number(gradeForm.totalScore) || 100" />
          <span style="margin-left: 8px; color: #999">满分 {{ gradeForm.totalScore }}</span>
        </el-form-item>
        <el-form-item label="反馈">
          <el-input v-model="gradeForm.feedback" type="textarea" :rows="4" placeholder="请输入批阅反馈" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onGrade">提交批阅</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSubmissions, gradeSubmission, getAssignments } from '../../api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const assignments = ref([])
const dialogVisible = ref(false)

const query = reactive({ current: 1, size: 10, assignmentId: null, status: '' })
const gradeForm = reactive({
  id: null, studentName: '', assignmentTitle: '', content: '', score: 0, totalScore: 100, feedback: ''
})

const load = async () => {
  loading.value = true
  try {
    const data = await getSubmissions(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const loadAssignments = async () => {
  const data = await getAssignments({ current: 1, size: 100 })
  assignments.value = data.records
}

const openGrade = (row) => {
  Object.assign(gradeForm, {
    id: row.id, studentName: row.studentName, assignmentTitle: row.assignmentTitle,
    content: row.content, score: Number(row.score) || 0, totalScore: Number(row.totalScore) || 100,
    feedback: row.feedback || ''
  })
  dialogVisible.value = true
}

const onGrade = async () => {
  await gradeSubmission(gradeForm.id, { score: gradeForm.score, feedback: gradeForm.feedback })
  ElMessage.success('批阅成功')
  dialogVisible.value = false
  load()
}

const onPageChange = (p) => {
  query.current = p
  load()
}

onMounted(() => {
  load()
  loadAssignments()
})
</script>
