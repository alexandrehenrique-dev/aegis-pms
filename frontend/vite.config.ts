import { defineConfig } from 'vite'
import path from 'path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
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

  build: {
    /**
     * Spring Boot serve arquivos estáticos de `src/main/resources/static`.
     * `npm run build` dentro de `frontend/` deposita os artefatos lá
     * diretamente — sem copiar arquivos manualmente.
     */
    outDir: '../backend/src/main/resources/static',
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
})
