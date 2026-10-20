<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22"><School /></el-icon>
        <div class="logo-text">
          <div class="logo-title">优达智慧课堂</div>
          <div class="logo-sub">教学管理平台</div>
        </div>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#001529"
        text-color="#c0c4cc"
        active-text-color="#ffffff"
      >
        <el-menu-item
          v-for="item in menus"
          :key="item.path"
          :index="'/' + item.path"
        >
          <el-icon><component :is="item.meta.icon" /></el-icon>
          <span>{{ item.meta.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <el-dropdown @command="onCommand">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ userStore.user?.realName }}（{{ roleLabel }}）
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 根据当前角色过滤菜单
const menus = computed(() => {
  const allMenus = router.options.routes.find((r) => r.path === '/').children
  return allMenus.filter((m) => {
    if (!m.meta?.roles) return true
    return m.meta.roles.includes(userStore.role)
  })
})

const currentTitle = computed(() => route.meta.title || '')

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
  return map[userStore.role] || '用户'
})

const onCommand = async (command) => {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background: linear-gradient(180deg, #15203c 0%, #001529 100%);
}
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo .el-icon {
  color: #6f8bff;
}
.logo-text {
  text-align: left;
  line-height: 1.2;
}
.logo-title {
  font-size: 16px;
  font-weight: 700;
}
.logo-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}
.aside :deep(.el-menu) {
  border-right: none;
}
.aside :deep(.el-menu-item.is-active) {
  background: #2f54eb;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eef0f4;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}
.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #333;
}
.user-info .el-icon:first-child {
  color: #2f54eb;
}
.main {
  background: var(--bg);
}
</style>
