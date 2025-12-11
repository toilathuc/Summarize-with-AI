import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: true, // Listen on all addresses (IPv4/IPv6)
    port: 5173,
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
})
