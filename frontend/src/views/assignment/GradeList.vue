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

    <el-dialog v-model="dialogVisible" title="作业批阅（智能辅助）" width="680px">
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
          <el-input v-model="gradeForm.feedback" type="textarea" :rows="3" placeholder="请输入批阅反馈" />
        </el-form-item>
      </el-form>

      <!-- 创新点 A：相似提交提示（雷同检测） -->
      <div class="assist-block" v-if="assistLoaded">
        <div class="assist-title">⚠ 相似提交提示（TF-IDF 雷同检测）</div>
        <template v-if="assist.similar.length">
          <el-table :data="assist.similar" size="small" max-height="180">
            <el-table-column prop="studentName" label="学生" width="110" />
            <el-table-column label="相似度" width="100" align="right">
              <template #default="{ row }">
                <span :style="{ color: row.flagged ? '#f56c6c' : '#333', fontWeight: row.flagged ? 700 : 400 }">
                  {{ row.similarity }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column label="疑似雷同" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.flagged" type="danger" size="small">疑似雷同</el-tag>
                <span v-else style="color: #999">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="excerpt" label="内容摘要" min-width="180" show-overflow-tooltip />
          </el-table>
        </template>
        <div v-else class="assist-empty">未发现明显相似提交</div>
      </div>

      <!-- 创新点 A：推荐评语（历史高频） -->
      <div class="assist-block" v-if="assistLoaded">
        <div class="assist-title">💡 推荐评语（历史高频，点击填入）</div>
        <template v-if="assist.suggestions.length">
          <div v-for="(s, i) in assist.suggestions" :key="i" class="suggestion-row">
            <span class="suggestion-text">{{ s.text }}</span>
            <span class="suggestion-meta">用 {{ s.count }} 次 · 均分 {{ s.avgScore }}</span>
            <el-button link type="primary" @click="fillFeedback(s.text)">填入</el-button>
          </div>
        </template>
        <div v-else class="assist-empty">暂无历史评语可推荐</div>
      </div>

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
import { getSubmissions, gradeSubmission, getAssignments, getGradingAssist } from '../../api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const assignments = ref([])
const dialogVisible = ref(false)

const query = reactive({ current: 1, size: 10, assignmentId: null, status: '' })
const gradeForm = reactive({
  id: null, studentName: '', assignmentTitle: '', content: '', score: 0, totalScore: 100, feedback: ''
})
// 批阅辅助：相似提交 + 推荐评语
const assist = reactive({ similar: [], suggestions: [] })
const assistLoaded = ref(false)

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
  loadAssist(row.id)
}

const loadAssist = async (id) => {
  assistLoaded.value = false
  try {
    const data = await getGradingAssist(id)
    assist.similar = data.similar || []
    assist.suggestions = data.suggestions || []
  } catch (e) {
    assist.similar = []
    assist.suggestions = []
  } finally {
    assistLoaded.value = true
  }
}

const fillFeedback = (text) => {
  gradeForm.feedback = text
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

<style scoped>
.assist-block {
  margin-top: 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
}
.assist-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}
.assist-empty {
  color: #909399;
  font-size: 13px;
}
.suggestion-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed #ebeef5;
}
.suggestion-row:last-child {
  border-bottom: none;
}
.suggestion-text {
  flex: 1;
  font-size: 13px;
  color: #303133;
}
.suggestion-meta {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
</style>
