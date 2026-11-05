import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// FE-STYLE §2 — AntD la component core; Tailwind chi utility layout (khong preflight
// de khong reset style AntD, khong dung Tailwind cho mau/font/radius).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 5173 },
});
