<template>
  <div class="page-container">
    <div class="page-header">
      <h2>作业列表</h2>
      <div class="page-desc">按课程筛选作业，支持布置、编辑与在线提交</div>
    </div>
    <div class="page-toolbar">
      <el-select v-model="query.courseId" placeholder="全部课程" clearable style="width: 220px" @change="load">
        <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="作业标题" clearable style="width: 200px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <div class="toolbar-spacer"></div>
      <el-button v-if="userStore.isTeacher" type="primary" @click="openDialog()">布置作业</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="作业标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="courseName" label="所属课程" min-width="180" />
      <el-table-column prop="deadline" label="截止时间" width="170">
        <template #default="{ row }">{{ row.deadline ? row.deadline.replace('T', ' ') : '不限' }}</template>
      </el-table-column>
      <el-table-column prop="totalScore" label="满分" width="80" />
      <el-table-column label="提交/批阅" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.gradedCount }}/{{ row.submissionCount }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="userStore.isStudent" label="提交状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="isSubmitted(row)" type="success">已提交</el-tag>
          <el-tag v-else-if="isDeadlinePassed(row)" type="danger">已截止</el-tag>
          <el-tag v-else type="info">未提交</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="userStore.isStudent" link type="primary" :disabled="!canSubmit(row)" @click="openSubmit(row)">
            {{ isSubmitted(row) ? '已提交' : isDeadlinePassed(row) ? '已截止' : '提交作业' }}
          </el-button>
          <el-button v-if="userStore.isTeacher" link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button v-if="userStore.isTeacher" link type="danger" :loading="deleting === row.id" @click="onDelete(row)">删除</el-button>
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

    <!-- 布置/编辑作业 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑作业' : '布置作业'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="form.courseId" style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="作业要求">
          <el-input v-model="form.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="满分">
          <el-input-number v-model="form.totalScore" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 提交作业 -->
    <el-dialog v-model="submitVisible" title="提交作业" width="520px">
      <div style="margin-bottom: 8px; color: #666">作业：{{ submitForm.title }}</div>
      <el-input v-model="submitForm.content" type="textarea" :rows="5" placeholder="请输入作业内容" />
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAssignments, createAssignment, updateAssignment, deleteAssignment, getAllCourses, getMyCourses, getMySubmissions, submitAssignment } from '../../api'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const courses = ref([])
const mySubmittedIds = ref(new Set())
const dialogVisible = ref(false)
const submitVisible = ref(false)
const formRef = ref()
const saving = ref(false)
const deleting = ref(null)

const query = reactive({ current: 1, size: 10, courseId: null, keyword: '' })
const form = reactive({ id: null, courseId: null, title: '', content: '', deadline: null, totalScore: 100 })
const submitForm = reactive({ assignmentId: null, title: '', content: '' })

const rules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  title: [{ required: true, message: '请输入作业标题', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const data = await getAssignments(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const loadCourses = async () => {
  // 选课模型：学生只显示已选修课程的作业，教师/管理员显示全部
  courses.value = userStore.isStudent ? await getMyCourses() : await getAllCourses()
}

const loadMySubmissions = async () => {
  if (!userStore.isStudent) return
  try {
    const data = await getMySubmissions({ current: 1, size: 100 })
    mySubmittedIds.value = new Set((data.records || []).map((s) => s.assignmentId))
  } catch (e) {
    mySubmittedIds.value = new Set()
  }
}

const isSubmitted = (row) => mySubmittedIds.value.has(row.id)
const isDeadlinePassed = (row) => !!row.deadline && new Date(row.deadline).getTime() < Date.now()
const canSubmit = (row) => !isSubmitted(row) && !isDeadlinePassed(row)

const openDialog = (row) => {
  Object.assign(form, { id: null, courseId: null, title: '', content: '', deadline: null, totalScore: 100 })
  if (row) {
    Object.assign(form, {
      id: row.id, courseId: row.courseId, title: row.title,
      content: row.content, deadline: row.deadline, totalScore: Number(row.totalScore)
    })
  }
  dialogVisible.value = true
}

const onSave = async () => {
  if (saving.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateAssignment(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await createAssignment(form)
      ElMessage.success('布置成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const onDelete = async (row) => {
  if (deleting.value) return
  deleting.value = row.id
  try {
    await ElMessageBox.confirm(`确定删除作业「${row.title}」吗？`, '提示', { type: 'warning' })
    await deleteAssignment(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    // 取消删除：忽略
  } finally {
    deleting.value = null
  }
}

const openSubmit = (row) => {
  submitForm.assignmentId = row.id
  submitForm.title = row.title
  submitForm.content = ''
  submitVisible.value = true
}

const onSubmit = async () => {
  await submitAssignment({ assignmentId: submitForm.assignmentId, content: submitForm.content })
  ElMessage.success('提交成功')
  submitVisible.value = false
  load()
  loadMySubmissions() // 提交后刷新「已提交」状态，避免再次点击触发重复提交
}

const onPageChange = (p) => {
  query.current = p
  load()
}

onMounted(() => {
  load()
  loadCourses()
  loadMySubmissions()
})
</script>
