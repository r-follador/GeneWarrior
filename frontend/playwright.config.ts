import {defineConfig} from '@playwright/test';

export default defineConfig({
    testDir: './e2e',
    fullyParallel: false,
    use: {
        baseURL: process.env.GENEWARRIOR_FRONTEND_URL ?? 'http://127.0.0.1:5173',
        viewport: {width: 1440, height: 1000},
        launchOptions: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE
            ? {executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE} : {},
        trace: 'retain-on-failure'
    }
});
