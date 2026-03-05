import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  base: process.env.VITE_BASE_PATH ?? "/metric-catalogue/",
  plugins: [vue()],
  test: {
    globals: true,
    environment: "happy-dom",
    clearMocks: true,
    setupFiles: "./src/test/setup.ts",
    include: ["src/**/*.test.ts"],
    exclude: ["node_modules/**", "dist/**", "scripts/**"],
  },
});
