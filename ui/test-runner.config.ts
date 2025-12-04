import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";
import { resolve } from "path";

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: "jsdom",

    // Test file patterns
    include: [
      "src/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}",
      "src/**/__tests__/**/*.{js,mjs,cjs,ts,mts,cts,jsx,tsx}",
    ],
    exclude: [
      "node_modules",
      "dist",
      ".idea",
      ".git",
      ".cache",
      "coverage",
      "src/**/*.large.test.{js,ts}", // Exclude large/slow tests by default
    ],

    // Test execution settings
    testTimeout: 10000, // 10 second timeout for individual tests
    hookTimeout: 10000, // 10 second timeout for hooks

    // Coverage configuration
    coverage: {
      provider: "v8",
      reporter: ["text", "json", "html", "lcov"],
      reportsDirectory: "./coverage",
      exclude: [
        "node_modules/**",
        "dist/**",
        "coverage/**",
        "**/*.d.ts",
        "**/*.config.{js,ts}",
        "**/vite.config.{js,ts}",
        "**/vitest.config.{js,ts}",
        "src/main.ts",
        "src/vite-env.d.ts",
        "src/assets/**",
        "**/*.large.test.{js,ts}",
        "src/example/**",
      ],
      include: [
        "src/**/*.{js,ts,vue}",
        "!src/**/*.{test,spec}.{js,ts}",
        "!src/**/__tests__/**",
      ],
      // Coverage thresholds
      thresholds: {
        global: {
          branches: 80,
          functions: 85,
          lines: 85,
          statements: 85,
        },
        // Specific thresholds for critical files
        "src/util/Parser.ts": {
          branches: 90,
          functions: 95,
          lines: 95,
          statements: 95,
        },
        "src/util/KpiService.ts": {
          branches: 90,
          functions: 95,
          lines: 95,
          statements: 95,
        },
      },
      all: true, // Include all files in coverage report
      skipFull: false, // Don't skip files with 100% coverage
    },

    // Reporting
    reporters: ["default", "json", "html", "junit"],
    outputFile: {
      json: "./test-results/results.json",
      html: "./test-results/index.html",
      junit: "./test-results/junit.xml",
    },

    // Setup files
    setupFiles: ["./src/__test__/setup.ts"],

    // Test environment configuration
    env: {
      NODE_ENV: "test",
    },

    // Browser-like environment settings
    deps: {
      inline: ["vue"],
    },

    // Mock configuration
    clearMocks: true,
    restoreMocks: true,
    mockReset: true,

    // Performance monitoring
    logHeapUsage: true,

    // Test categorization using custom logic
    workspace: [
      {
        extends: true,
        test: {
          name: "unit",
          include: [
            "src/util/**/*.test.ts",
            "src/model/**/*.test.ts",
            "src/components/**/*.test.ts",
          ],
          testTimeout: 5000,
        },
      },
      {
        extends: true,
        test: {
          name: "integration",
          include: [
            "src/**/*integration*.test.ts",
            "src/__tests__/integration.test.ts",
          ],
          testTimeout: 15000,
        },
      },
      {
        extends: true,
        test: {
          name: "large",
          include: ["src/**/*.large.test.ts"],
          testTimeout: 30000,
        },
      },
    ],
  },

  resolve: {
    alias: {
      "@": resolve(__dirname, "src"),
      "~": resolve(__dirname),
    },
  },

  // Build configuration for tests
  esbuild: {
    target: "es2020",
  },

  // Define custom test commands and configurations
  define: {
    __TEST__: true,
    __VERSION__: JSON.stringify(process.env.npm_package_version || "1.0.0"),
    __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
  },
});

// Custom test runner commands that can be added to package.json:
/*
{
  "scripts": {
    "test": "vitest",
    "test:unit": "vitest --workspace=unit",
    "test:integration": "vitest --workspace=integration",
    "test:large": "vitest --workspace=large",
    "test:ui": "vitest --ui",
    "test:coverage": "vitest run --coverage",
    "test:watch": "vitest --watch",
    "test:run": "vitest run",
    "test:ci": "vitest run --coverage --reporter=junit --reporter=json",
    "test:debug": "vitest --inspect-brk --no-coverage --reporter=verbose",
    "test:performance": "vitest run --reporter=verbose --logHeapUsage",
    "test:all": "npm run test:unit && npm run test:integration && npm run test:large"
  }
}
*/

// Performance monitoring utilities
export const performanceConfig = {
  // Memory leak detection
  memoryLeakThreshold: 50 * 1024 * 1024, // 50MB

  // Execution time monitoring
  slowTestThreshold: 2000, // 2 seconds

  // Test result analysis
  enablePerformanceMetrics: true,

  // Custom reporters for performance data
  customReporters: {
    performance: "./src/test/reporters/performance-reporter.ts",
    memory: "./src/test/reporters/memory-reporter.ts",
  },
};

// Test categories and tagging system
export const testCategories = {
  unit: {
    pattern: "src/{util,model,components}/**/*.test.ts",
    timeout: 5000,
    coverage: { threshold: 90 },
  },

  integration: {
    pattern: "src/**/*integration*.test.ts",
    timeout: 15000,
    coverage: { threshold: 80 },
  },

  component: {
    pattern: "src/components/**/*.test.ts",
    timeout: 10000,
    coverage: { threshold: 85 },
  },

  e2e: {
    pattern: "src/**/*e2e*.test.ts",
    timeout: 30000,
    coverage: { threshold: 70 },
  },

  performance: {
    pattern: "src/**/*performance*.test.ts",
    timeout: 60000,
    coverage: { threshold: 60 },
  },
};

// Quality gates configuration
export const qualityGates = {
  coverage: {
    minimum: 85,
    target: 95,
  },

  performance: {
    maxTestDuration: 30000, // 30 seconds
    maxMemoryUsage: 100 * 1024 * 1024, // 100MB
    maxSlowTests: 5, // Maximum number of slow tests allowed
  },

  reliability: {
    maxFlakeRate: 0.1, // 10% flake rate maximum
    requiredPassRate: 0.99, // 99% pass rate required
  },
};

// Test data management
export const testDataConfig = {
  fixtures: {
    baseDir: "./src/test/fixtures",
    autoLoad: true,
  },

  mocks: {
    baseDir: "./src/test/mocks",
    autoMock: ["chart.js", "chartjs-plugin-annotation"],
  },

  snapshots: {
    updateOnFail: false,
    threshold: 0.1, // 10% pixel difference threshold
  },
};
