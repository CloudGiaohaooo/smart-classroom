import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端代理目标：可用环境变量 VITE_PROXY_TARGET 覆盖（默认 8080），避免硬编码端口
const proxyTarget = process.env.VITE_PROXY_TARGET || 'http://localhost:8080'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5173,
    // 开发环境代理：前端请求 /api、/files 转发到后端，避免跨域
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true
      },
      '/files': {
        target: proxyTarget,
        changeOrigin: true
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 800,
    rollupOptions: {
      output: {
        // 把 element-plus 及其图标拆成独立 chunk，减小主包并利于缓存
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
            if (id.includes('echarts')) return 'echarts'
          }
        }
      }
    }
  }
})
