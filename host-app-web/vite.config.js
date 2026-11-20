import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

const backend = process.env.HOST_APP_BACKEND_URL || 'http://127.0.0.1:8080'
const proxy = Object.fromEntries(
  ['/api', '/auth', '/servers', '/requests', '/actuator', '/health', '/protected-health', '/swagger-ui', '/v3/api-docs']
    .map(path => [path, { target: backend }]),
)

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  server: { proxy },
  preview: { proxy },
})
