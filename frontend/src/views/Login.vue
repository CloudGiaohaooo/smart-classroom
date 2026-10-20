<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-inner">
        <div class="brand-logo">
          <el-icon :size="26"><School /></el-icon>
          <span>优达智慧课堂</span>
        </div>
        <h1>优达智慧课堂教学管理平台</h1>
        <p class="brand-slogan">覆盖「课程 — 作业 — 提交 — 批阅 — 资源」全流程的教学管理平台</p>
        <ul class="brand-points">
          <li>
            <el-icon><MagicStick /></el-icon>
            <span><b>智能辅助批阅</b> · TF-IDF 雷同检测 + 评语一键推荐</span>
          </li>
          <li>
            <el-icon><DataAnalysis /></el-icon>
            <span><b>学习预警看板</b> · AOP 零侵入埋点 + ECharts 可视化</span>
          </li>
          <li>
            <el-icon><Files /></el-icon>
            <span><b>资源 MD5 秒传</b> · 同文件复用去重 + 签名 URL 鉴权下载</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-panel">
      <el-card class="login-card" shadow="always">
        <h2 class="login-welcome">欢迎登录</h2>
        <p class="login-sub">请使用演示账号登录系统</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" placeholder="请输入账号">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="onLogin">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="onLogin">
            登 录
          </el-button>
        </el-form>
        <div class="login-tip">
          <el-icon><InfoFilled /></el-icon>
          演示账号：admin / teacher / student，密码均为 123456
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const onLogin = async () => {
  try {
    await formRef.value.validate()
  } catch (e) {
    return // 校验失败，不继续登录
  }
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    // 按角色直达正确首页：学生 → 课程列表，教师/管理员 → 数据概览
    router.push(userStore.role === 'STUDENT' ? '/courses' : '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
}

/* 左侧品牌区：靛蓝渐变 */
.login-brand {
  flex: 1.15;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: linear-gradient(135deg, #1e3aa8 0%, #2f54eb 55%, #5a77ef 100%);
  color: #fff;
}
.brand-inner {
  max-width: 460px;
}
.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 40px;
}
.brand-inner h1 {
  font-size: 34px;
  line-height: 1.35;
  font-weight: 700;
  letter-spacing: 1px;
}
.brand-slogan {
  margin-top: 16px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.85);
}
.brand-points {
  list-style: none;
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.brand-points li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.92);
}
.brand-points .el-icon {
  font-size: 20px;
  color: #b7c8ff;
  flex-shrink: 0;
}

/* 右侧表单区 */
.login-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: #fff;
}
.login-card {
  width: 400px;
  border-radius: 14px;
}
.login-welcome {
  font-size: 22px;
  font-weight: 600;
  color: #111827;
}
.login-sub {
  margin: 6px 0 24px;
  font-size: 14px;
  color: #6b7280;
}
.login-btn {
  width: 100%;
  margin-top: 4px;
  font-weight: 600;
}
.login-tip {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #9ca3af;
  font-size: 12px;
}

@media (max-width: 820px) {
  .login-brand {
    display: none;
  }
}
</style>
