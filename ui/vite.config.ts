import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import {viteSingleFile} from "vite-plugin-singlefile";

// https://vite.dev/config/
export default defineConfig(({mode}) => {
    return {
        plugins: [vue(), viteSingleFile()],
        define: {
            __DEMO_MODE__: JSON.stringify(mode === "demo"),
            __STATIC_FILE_MODE__: JSON.stringify(mode === "staticFile"),
        },
    }
})
