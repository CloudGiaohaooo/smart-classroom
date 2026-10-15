<template>
  <div class="page-container">
    <div class="page-toolbar">
      <el-select v-model="query.courseId" placeholder="全部课程" clearable style="width: 220px" @change="load">
        <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <div class="toolbar-spacer"></div>
      <el-button v-if="userStore.isTeacher" type="primary" @click="openUpload">上传资源</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="resourceName" label="资源名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="courseName" label="所属课程" min-width="180" />
      <el-table-column prop="resourceType" label="类型" width="90">
        <template #default="{ row }">
          <el-tag>{{ row.resourceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="110">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="uploaderName" label="上传人" width="110" />
      <el-table-column prop="uploadTime" label="上传时间" width="170">
        <template #default="{ row }">{{ row.uploadTime?.replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="download(row)">下载</el-button>
          <el-button v-if="userStore.isTeacher" link type="danger" @click="onDelete(row)">删除</el-button>
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

    <el-dialog v-model="uploadVisible" title="上传课程资源" width="500px">
      <el-form label-width="90px">
        <el-form-item label="所属课程" required>
          <el-select v-model="uploadForm.courseId" style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="uploadForm.resourceType" style="width: 100%">
            <el-option label="视频" value="VIDEO" />
            <el-option label="PDF" value="PDF" />
            <el-option label="文档" value="DOC" />
            <el-option label="PPT" value="PPT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件" required>
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="onFileChange"
            :on-remove="() => (uploadForm.file = null)"
          >
            <el-icon style="font-size: 40px; color: #999"><UploadFilled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="onUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getResources, uploadResource, deleteResource, getAllCourses } from '../../api'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const courses = ref([])
const uploadVisible = ref(false)
const uploading = ref(false)

const query = reactive({ current: 1, size: 10, courseId: null })
const uploadForm = reactive({ courseId: null, resourceType: 'OTHER', file: null })

const load = async () => {
  loading.value = true
  try {
    const data = await getResources(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const loadCourses = async () => {
  courses.value = await getAllCourses()
}

const formatSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

const openUpload = () => {
  Object.assign(uploadForm, { courseId: null, resourceType: 'OTHER', file: null })
  uploadVisible.value = true
}

const onFileChange = (file) => {
  uploadForm.file = file.raw
}

const onUpload = async () => {
  if (!uploadForm.courseId) {
    ElMessage.warning('请选择所属课程')
    return
  }
  if (!uploadForm.file) {
    ElMessage.warning('请选择要上传的文件')
    return
  }
  const fd = new FormData()
  fd.append('courseId', uploadForm.courseId)
  fd.append('resourceType', uploadForm.resourceType)
  fd.append('file', uploadForm.file)
  uploading.value = true
  try {
    await uploadResource(fd)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    load()
  } finally {
    uploading.value = false
  }
}

const download = (row) => {
  if (row.fileUrl) {
    window.open(row.fileUrl, '_blank')
  }
}

const onDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除资源「${row.resourceName}」吗？`, '提示', { type: 'warning' })
  await deleteResource(row.id)
  ElMessage.success('删除成功')
  load()
}

const onPageChange = (p) => {
  query.current = p
  load()
}

onMounted(() => {
  load()
  loadCourses()
})
</script>
