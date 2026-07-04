import { defineConfig } from 'vite'
import path from 'path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => ({
  plugins: [
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      // Alias @ to the src directory
      '@': path.resolve(__dirname, './src'),
    },
  },

  // File types to support raw imports. Never add .css, .tsx, or .ts files to this.
  assetsInclude: ['**/*.svg', '**/*.csv'],

  // Remove toda chamada console.* e debugger do bundle de producao (Sprint
  // de Integracao 02, Secao J) — esbuild faz isso em tempo de transformacao,
  // antes do bundling, sem afetar tree-shaking nem source maps de dev.
  esbuild: {
    drop: mode === 'production' ? ['console', 'debugger'] : [],
  },

  build: {
    /**
     * `npm run build` gera o build Vite localmente em `frontend/dist`.
     * `npm run build:backend` copia esse resultado para o Spring Boot.
     */
    outDir: 'dist',
    emptyOutDir: true,
  },

  server: {
    /**
     * Proxy de desenvolvimento: redireciona chamadas /api/* e /auth/* para
     * o Spring Boot local, evitando CORS durante o desenvolvimento com
     * `VITE_API_MODE=api`.
     *
     * Em modo mock (VITE_API_MODE=mock), o proxy nunca é atingido — nenhuma
     * chamada HTTP real é feita.
     */
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
    port: 5173,
  },
}))
