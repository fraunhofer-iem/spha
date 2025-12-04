import { vi, beforeEach, afterEach, beforeAll, afterAll } from "vitest";
import { config } from "@vue/test-utils";

// =============================================================================
// GLOBAL TEST SETUP FOR SPHA VISUALIZATION
// =============================================================================
//
// This file provides global test configuration and utilities for the entire test suite.
// It is automatically loaded by Vitest before running any tests.
//
// KEY FEATURES:
// 1. Global mocks for Chart.js, Bootstrap, CSS imports, and DOM APIs
// 2. Mock utilities: createMockResult, createMockKpi, createMockRepoInfo
// 3. File utilities: createMockFile, createMockFileList
// 4. Performance monitoring: startPerformanceMonitoring, endPerformanceMonitoring
// 5. Console utilities: suppressConsoleErrors, suppressConsoleWarnings
// 6. Test utilities: waitForNextTick, waitFor, withErrorBoundary
// 7. Vue Test Utils configuration with router and i18n mocks
//
// USAGE:
// - Global mocks are applied automatically
// - Import utilities in your test files:
//   import { createMockResult, waitForNextTick } from '../__test__/setup'
// - Or use them directly since they're globally available in some cases
//
// =============================================================================

// =============================================================================
// Global Mocks
// =============================================================================

// Mock Chart.js completely to avoid canvas rendering issues in tests
vi.mock("chart.js", () => {
  const mockChart = {
    destroy: vi.fn(),
    update: vi.fn(),
    resize: vi.fn(),
    render: vi.fn(),
    clear: vi.fn(),
    stop: vi.fn(),
    reset: vi.fn(),
    toBase64Image: vi.fn(() => "data:image/png;base64,mock"),
    generateLegend: vi.fn(() => "<div>Mock Legend</div>"),
    canvas: {
      width: 400,
      height: 300,
      getContext: vi.fn(() => ({
        fillRect: vi.fn(),
        clearRect: vi.fn(),
        beginPath: vi.fn(),
        moveTo: vi.fn(),
        lineTo: vi.fn(),
        stroke: vi.fn(),
        fill: vi.fn(),
      })),
    },
  };

  return {
    Chart: vi.fn(() => mockChart),
    DoughnutController: vi.fn(),
    BarController: vi.fn(),
    BarElement: vi.fn(),
    ArcElement: vi.fn(),
    CategoryScale: vi.fn(),
    LinearScale: vi.fn(),
    Tooltip: vi.fn(),
    Legend: vi.fn(),
    Title: vi.fn(),
    register: vi.fn(),
    defaults: {
      font: {
        family: "Arial",
        size: 12,
      },
    },
  };
});

// Mock Chart.js annotation plugin
vi.mock("chartjs-plugin-annotation", () => ({
  default: {
    id: "annotation",
    afterDraw: vi.fn(),
    beforeDraw: vi.fn(),
  },
}));

// Mock Bootstrap JavaScript
vi.mock("bootstrap/dist/js/bootstrap.bundle.min.js", () => ({}));

// Mock CSS imports
vi.mock("bootstrap/dist/css/bootstrap.min.css", () => ({}));
vi.mock("bootstrap-icons/font/bootstrap-icons.css", () => ({}));

// Mock SCSS imports
vi.mock("../assets/styles/dashboard-card.scss", () => ({}));

// Mock image imports
vi.mock(
  "../assets/img/supportedTools/trufflehog.svg",
  () => "/mock/trufflehog.svg",
);
vi.mock("../assets/img/supportedTools/osv.svg", () => "/mock/osv.svg");
vi.mock(
  "../assets/img/supportedTools/github-mark.svg",
  () => "/mock/github.svg",
);

// Mock colors
vi.mock("../assets/styles/Colors.ts", () => ({
  background_grey: "#e6e6e6",
  blue_chart: "#007bff",
  primary_color: "#0d6efd",
  success_color: "#198754",
  warning_color: "#ffc107",
  danger_color: "#dc3545",
}));

// =============================================================================
// Global DOM Setup
// =============================================================================

// Mock FileReader for file upload tests
global.FileReader = vi.fn(() => ({
  onload: null,
  onerror: null,
  onabort: null,
  onprogress: null,
  readAsText: vi.fn(function (this: any) {
    // Simulate async file reading
    setTimeout(() => {
      if (this.onload) {
        this.onload({ target: { result: '{"mock": "data"}' } });
      }
    }, 0);
  }),
  readAsDataURL: vi.fn(),
  readAsArrayBuffer: vi.fn(),
  readAsBinaryString: vi.fn(),
  abort: vi.fn(),
  result: null,
  error: null,
  readyState: 0,
})) as any;

// Mock URL constructor for validation tests
global.URL =
  global.URL ||
  class URL {
    href: string;

    constructor(href: string, _base?: string) {
      if (!href || typeof href !== "string") {
        throw new TypeError("Invalid URL");
      }
      this.href = href;
    }
    toString() {
      return this.href;
    }
  };

// Mock performance API if not available
if (typeof global.performance === "undefined") {
  global.performance = {
    now: vi.fn(() => Date.now()),
    mark: vi.fn(),
    measure: vi.fn(),
    getEntriesByName: vi.fn(() => []),
    getEntriesByType: vi.fn(() => []),
    clearMarks: vi.fn(),
    clearMeasures: vi.fn(),
  } as any;

  // Add memory property separately to avoid TypeScript issues
  (global.performance as any).memory = {
    usedJSHeapSize: 1000000,
    totalJSHeapSize: 2000000,
    jsHeapSizeLimit: 4000000,
  };
}

// =============================================================================
// Vue Test Utils Configuration
// =============================================================================

config.global.mocks = {
  $t: (key: string) => key, // Mock i18n if used in future
  $route: {
    params: {},
    query: {},
    path: "/",
    name: "test",
  },
  $router: {
    push: vi.fn(),
    replace: vi.fn(),
    go: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
  },
};

config.global.stubs = {
  // Stub transition components to avoid animation issues in tests
  transition: false,
  "transition-group": false,

  // Stub router components if needed
  "router-link": {
    template: "<a><slot /></a>",
    props: ["to"],
  },
  "router-view": {
    template: "<div><slot /></div>",
  },
};

// =============================================================================
// Test Data Factories
// =============================================================================

export const createMockResult = (overrides = {}) => ({
  healthScore: 80,
  repoInfo: {
    projectName: "Test Project",
    repoLanguages: [{ name: "TypeScript", percentage: 100 }],
    stars: 100,
    lastCommitDate: "2023-01-01",
    contributors: 5,
    projectUrl: "https://github.com/test/test",
  },
  root: {
    displayName: "Root",
    score: 80,
    id: "root",
    children: [],
  },
  tools: [],
  ...overrides,
});

export const createMockKpi = (overrides = {}) => ({
  displayName: "Test KPI",
  score: 80,
  id: "test-kpi",
  children: [],
  thresholds: [],
  ...overrides,
});

export const createMockRepoInfo = (overrides = {}) => ({
  projectName: "Test Project",
  stars: 127,
  contributors: 8,
  lastCommitDate: "2024-01-15T10:30:00Z",
  projectUrl: "https://github.com/example/awesome-project",
  repoLanguages: [
    { name: "TypeScript", size: 65.2 },
    { name: "JavaScript", size: 20.8 },
    { name: "CSS", size: 10.5 },
    { name: "HTML", size: 3.5 },
  ],
  ...overrides,
});

// =============================================================================
// Test Utilities
// =============================================================================

export const waitForNextTick = () =>
  new Promise((resolve) => setTimeout(resolve, 0));

export const waitFor = (condition: () => boolean, timeout = 1000) => {
  return new Promise((resolve, reject) => {
    const startTime = Date.now();
    const check = () => {
      if (condition()) {
        resolve(true);
      } else if (Date.now() - startTime > timeout) {
        reject(new Error(`Condition not met within ${timeout}ms`));
      } else {
        setTimeout(check, 10);
      }
    };
    check();
  });
};

export const createMockFile = (
  content: string,
  filename = "test.json",
  type = "application/json",
) => {
  return new File([content], filename, { type });
};

export const createMockFileList = (files: File[]): FileList => {
  const fileList = {
    length: files.length,
    item: (index: number) => files[index] || null,
    [Symbol.iterator]: function* () {
      for (let i = 0; i < files.length; i++) {
        yield files[i];
      }
    },
  };

  // Add files as indexed properties
  files.forEach((file, index) => {
    (fileList as any)[index] = file;
  });

  return fileList as FileList;
};

// =============================================================================
// Performance Monitoring
// =============================================================================

interface TestMetrics {
  startTime: number;
  memoryStart: number;
  testName: string;
}

const testMetrics = new Map<string, TestMetrics>();

export const startPerformanceMonitoring = (testName: string) => {
  const startTime = performance.now();
  const memoryStart = (performance as any).memory?.usedJSHeapSize || 0;

  testMetrics.set(testName, {
    startTime,
    memoryStart,
    testName,
  });
};

export const endPerformanceMonitoring = (testName: string) => {
  const metrics = testMetrics.get(testName);
  if (!metrics) return;

  const duration = performance.now() - metrics.startTime;
  const memoryEnd = (performance as any).memory?.usedJSHeapSize || 0;
  const memoryDelta = memoryEnd - metrics.memoryStart;

  // Log performance warnings
  if (duration > 1000) {
    console.warn(
      `Slow test detected: ${testName} took ${duration.toFixed(2)}ms`,
    );
  }

  if (memoryDelta > 10 * 1024 * 1024) {
    // 10MB
    console.warn(
      `Memory leak potential: ${testName} used ${(memoryDelta / 1024 / 1024).toFixed(2)}MB`,
    );
  }

  testMetrics.delete(testName);
  return { duration, memoryDelta };
};

// =============================================================================
// Console Utilities
// =============================================================================

export const suppressConsoleErrors = () => {
  const originalError = console.error;
  console.error = vi.fn();
  return () => {
    console.error = originalError;
  };
};

export const suppressConsoleWarnings = () => {
  const originalWarn = console.warn;
  console.warn = vi.fn();
  return () => {
    console.warn = originalWarn;
  };
};

// =============================================================================
// Global Test Hooks
// =============================================================================

beforeAll(() => {
  // Global setup before all tests
  vi.clearAllTimers();

  // Set up global error handler
  const originalError = console.error;
  console.error = (...args: any[]) => {
    // Suppress known test-only warnings
    const message = args[0];
    if (typeof message === "string") {
      const suppressedMessages = [
        "Warning: ReactDOM.render is no longer supported",
        "Failed to resolve component",
        "Canvas is not supported",
        "ResizeObserver loop limit exceeded",
      ];

      if (suppressedMessages.some((msg) => message.includes(msg))) {
        return;
      }
    }
    originalError(...args);
  };
});

beforeEach(() => {
  // Clear all mocks before each test
  vi.clearAllMocks();
  vi.clearAllTimers();

  // Reset DOM
  document.body.innerHTML = "";

  // Clear any global state
  if (typeof window !== "undefined") {
    window.localStorage?.clear();
    window.sessionStorage?.clear();
  }
});

afterEach(() => {
  // Cleanup after each test
  vi.restoreAllMocks();

  // Check for memory leaks in test environment
  if (testMetrics.size > 0) {
    console.warn(
      "Unfinished performance monitoring:",
      Array.from(testMetrics.keys()),
    );
    testMetrics.clear();
  }
});

afterAll(() => {
  // Global cleanup after all tests
  vi.clearAllMocks();
  vi.clearAllTimers();

  // Final memory check
  if ((performance as any).memory) {
    const memoryUsage =
      (performance as any).memory.usedJSHeapSize / 1024 / 1024;
    if (memoryUsage > 100) {
      // 100MB
      console.warn(
        `High memory usage after tests: ${memoryUsage.toFixed(2)}MB`,
      );
    }
  }
});

// =============================================================================
// Error Boundary for Tests
// =============================================================================

export const withErrorBoundary = (testFn: () => void | Promise<void>) => {
  return async () => {
    try {
      await testFn();
    } catch (error) {
      // Enhanced error reporting for test failures
      console.error("Test failed with error:", error);
      if (error instanceof Error && error.stack) {
        console.error("Stack trace:", error.stack);
      }
      throw error;
    }
  };
};

// =============================================================================
// Export for External Use
// =============================================================================

export {
  vi,
  beforeEach,
  afterEach,
  beforeAll,
  afterAll,
  config as vueTestConfig,
};

// Type augmentations for better TypeScript support
declare global {
  interface Window {
    __TEST_ENV__: boolean;
  }

  namespace Vi {
    interface MockContext {
      // Add custom mock utilities here if needed
    }
  }
}

// Mark as test environment
if (typeof window !== "undefined") {
  window.__TEST_ENV__ = true;
}
