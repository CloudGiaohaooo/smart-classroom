<template>
  <div class="page-container">
    <div class="page-header">
      <h2>课程管理</h2>
      <div class="page-desc">课程信息分页管理与授课教师联表展示</div>
    </div>
    <div class="page-toolbar">
      <el-input v-model="query.keyword" placeholder="课程名称/编号" clearable style="width: 220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <div class="toolbar-spacer"></div>
      <el-button v-if="userStore.isTeacher" type="primary" @click="openDialog()">新增课程</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="courseName" label="课程名称" min-width="180" />
      <el-table-column prop="courseCode" label="课程编号" width="120" />
      <el-table-column prop="teacherName" label="授课教师" width="120" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="semester" label="学期" min-width="160" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '进行中' : '已结课' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="userStore.isTeacher" label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑课程' : '新增课程'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" />
        </el-form-item>
        <el-form-item label="课程编号">
          <el-input v-model="form.courseCode" />
        </el-form-item>
        <el-form-item label="授课教师" prop="teacherId">
          <el-select v-model="form.teacherId" placeholder="选择教师" style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.realName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学分">
          <el-input-number v-model="form.credit" :min="0" :max="10" :step="0.5" />
        </el-form-item>
        <el-form-item label="学期">
          <el-input v-model="form.semester" placeholder="如：2026-2027 第一学期" />
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="进行中" inactive-text="已结课" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCourses, createCourse, updateCourse, deleteCourse, listTeachers } from '../../api'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const teachers = ref([])
const saving = ref(false)
const deleting = ref(false)

const query = reactive({ current: 1, size: 10, keyword: '' })
const form = reactive({
  id: null, courseName: '', courseCode: '', teacherId: null,
  credit: 3, semester: '', description: '', status: 1
})

const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请选择授课教师', trigger: 'change' }]
}

const load = async () => {
  loading.value = true
  try {
    const data = await getCourses(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const loadTeachers = async () => {
  teachers.value = await listTeachers()
}

const openDialog = (row) => {
  Object.assign(form, { id: null, courseName: '', courseCode: '', teacherId: null, credit: 3, semester: '', description: '', status: 1 })
  if (row) {
    Object.assign(form, {
      id: row.id, courseName: row.courseName, courseCode: row.courseCode,
      teacherId: row.teacherId, credit: Number(row.credit), semester: row.semester,
      description: row.description, status: row.status
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
      await updateCourse(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await createCourse(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const onDelete = async (row) => {
  if (deleting.value) return
  deleting.value = true
  try {
    await ElMessageBox.confirm(`确定删除课程「${row.courseName}」吗？`, '提示', { type: 'warning' })
    await deleteCourse(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    // 取消删除：忽略
  } finally {
    deleting.value = false
  }
}

const onPageChange = (p) => {
  query.current = p
  load()
}

onMounted(() => {
  load()
  loadTeachers()
})
</script>
