import { afterEach, vi } from "vitest";
import { cleanup } from "@testing-library/vue";

afterEach(() => {
  vi.unstubAllGlobals();
  cleanup();
});
