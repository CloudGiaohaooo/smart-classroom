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
        meta: { title: '数据概览', icon: 'Odometer' }
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

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳转到登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) {
    next()
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
