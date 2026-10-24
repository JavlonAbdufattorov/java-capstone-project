import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The local proxy avoids CORS and keeps the legacy Spring pages separate.
const proxy = { '/api': { target: process.env.BACKEND_URL || 'http://localhost:8080', changeOrigin: true, rewrite: (path: string) => path.replace(/^\/api/, '') } }
export default defineConfig({ plugins: [react()], server: { proxy }, preview: { proxy } })
