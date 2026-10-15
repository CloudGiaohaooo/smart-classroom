import request from '../utils/request'

// ============ 认证 ============
export const login = (data) => request.post('/auth/login', data)
export const getMe = () => request.get('/auth/me')

// ============ 用户 ============
export const listStudents = () => request.get('/users/students')
export const listTeachers = () => request.get('/users/teachers')

// ============ 课程 ============
export const getCourses = (params) => request.get('/courses', { params })
export const getAllCourses = () => request.get('/courses/all')
export const createCourse = (data) => request.post('/courses', data)
export const updateCourse = (id, data) => request.put(`/courses/${id}`, data)
export const deleteCourse = (id) => request.delete(`/courses/${id}`)

// ============ 作业 ============
export const getAssignments = (params) => request.get('/assignments', { params })
export const createAssignment = (data) => request.post('/assignments', data)
export const updateAssignment = (id, data) => request.put(`/assignments/${id}`, data)
export const deleteAssignment = (id) => request.delete(`/assignments/${id}`)

// ============ 提交与批阅 ============
export const submitAssignment = (data) => request.post('/submissions', data)
export const getSubmissions = (params) => request.get('/submissions', { params })
export const getMySubmissions = (params) => request.get('/submissions/my', { params })
export const gradeSubmission = (id, data) => request.put(`/submissions/${id}/grade`, data)

// ============ 课程资源 ============
// FormData 由 axios 自动设置 multipart 边界，勿手动指定 Content-Type
export const uploadResource = (formData) => request.post('/resources/upload', formData)
export const getResources = (params) => request.get('/resources', { params })
export const deleteResource = (id) => request.delete(`/resources/${id}`)

// ============ 统计 ============
export const getStats = () => request.get('/stats/overview')
