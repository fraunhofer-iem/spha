import { afterEach, describe, expect, it, vi } from "vitest";
import { getRepositoryBaseUrl } from "../repository";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("getRepositoryBaseUrl", () => {
  it("returns a normalized env URL without trailing slash", () => {
    vi.stubEnv("VITE_REPO_URL", "https://github.com/example/metric-catalogue/");
    expect(getRepositoryBaseUrl()).toBe("https://github.com/example/metric-catalogue");
  });

  it("strips a trailing /issues suffix", () => {
    vi.stubEnv("VITE_REPO_URL", "https://github.com/example/metric-catalogue/issues");
    expect(getRepositoryBaseUrl()).toBe("https://github.com/example/metric-catalogue");
  });

  it("returns the env URL as-is when already normalized", () => {
    vi.stubEnv("VITE_REPO_URL", "https://github.com/example/metric-catalogue");
    expect(getRepositoryBaseUrl()).toBe("https://github.com/example/metric-catalogue");
  });

  it("returns the fallback when env is missing", () => {
    vi.stubEnv("VITE_REPO_URL", undefined as unknown as string);
    expect(getRepositoryBaseUrl()).toBe("https://github.com/fraunhofer-iem/spha");
  });

  it("returns null when env is an empty string", () => {
    vi.stubEnv("VITE_REPO_URL", "");
    expect(getRepositoryBaseUrl()).toBeNull();
  });
});
