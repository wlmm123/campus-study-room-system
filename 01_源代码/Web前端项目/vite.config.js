import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api/auth': { target: 'http://127.0.0.1:8081', changeOrigin: true },
      '/api/rooms': { target: 'http://127.0.0.1:8082', changeOrigin: true },
      '/api/admin/seats': { target: 'http://127.0.0.1:8082', changeOrigin: true },
      '/api/orders': { target: 'http://127.0.0.1:8083', changeOrigin: true },
      '/api/config': { target: 'http://127.0.0.1:8083', changeOrigin: true }
    }
  }
})