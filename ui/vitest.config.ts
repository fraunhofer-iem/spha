import {defineConfig} from "vitest/config";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({mode}) => {
    return {
        plugins: [vue()],
        define: {
            __DEMO_MODE__: JSON.stringify(mode === "demo"),
            __STATIC_FILE_MODE__: JSON.stringify(mode === "staticFile"),
        },
        test:
            {
                globals: true,
                environment:
                    "jsdom",
                include:
                    ["src/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}"],
                exclude:
                    ["node_modules", "dist", ".idea", ".git", ".cache"],
            },
        resolve: {
            alias: {
                "@":
                    "/src",
            },
        },
    }
});
