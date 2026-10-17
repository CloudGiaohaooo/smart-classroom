import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        // 数据概览含全站统计 + 学习预警看板，属教师/管理员能力；学生无 stats:read 权限，落地会导致 403
        meta: { title: '数据概览', icon: 'Odometer', roles: ['TEACHER', 'ADMIN'] }
      },
      {
        path: 'courses',
        name: 'Course',
        component: () => import('../views/course/CourseList.vue'),
        meta: { title: '课程管理', icon: 'Notebook' }
      },
      {
        path: 'assignments',
        name: 'Assignment',
        component: () => import('../views/assignment/AssignmentList.vue'),
        meta: { title: '作业列表', icon: 'Document' }
      },
      {
        path: 'grading',
        name: 'Grading',
        component: () => import('../views/assignment/GradeList.vue'),
        meta: { title: '作业批阅', icon: 'EditPen', roles: ['TEACHER', 'ADMIN'] }
      },
      {
        path: 'my-submissions',
        name: 'MySubmission',
        component: () => import('../views/assignment/MySubmission.vue'),
        meta: { title: '我的提交', icon: 'User', roles: ['STUDENT'] }
      },
      {
        path: 'resources',
        name: 'Resource',
        component: () => import('../views/resource/ResourceList.vue'),
        meta: { title: '课程资源', icon: 'FolderOpened' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

// 安全读取本地用户角色（localStorage 损坏时不抛异常）
function getUserRole() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')?.role
  } catch (e) {
    return null
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳登录页；已登录按角色拦截越权路由（学生→课程列表，教师/管理员→数据概览）
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) {
    next()
  } else if (!token) {
    next('/login')
  } else {
    const role = getUserRole()
    if (to.meta.roles && !to.meta.roles.includes(role)) {
      next(role === 'STUDENT' ? '/courses' : '/dashboard')
    } else {
      next()
    }
  }
})

export default router
