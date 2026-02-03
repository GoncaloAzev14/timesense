import { coverageConfigDefaults, defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    test: {
        environment: 'jsdom',
        alias: {
            '@/': new URL('./src/', import.meta.url).pathname,
        },
        coverage: {
            exclude: [
                '**/next.config.mjs',
                '**/tailwind.config.ts',
                '**/postcss.config.mjs',
                ...coverageConfigDefaults.exclude,
            ],
            reporter: ['text', 'json', 'html'],
        },
    },
});
