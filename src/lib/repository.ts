const DEFAULT_REPO_URL = "https://github.com/janniclas/metric-catalogue";

const normalizeRepositoryUrl = (value: string): string | null => {
  const trimmed = value.trim();
  if (!trimmed) return null;

  let normalized = trimmed.replace(/\/+$/, "");
  if (normalized.endsWith("/issues")) {
    normalized = normalized.slice(0, -"/issues".length);
  }
  normalized = normalized.replace(/\/+$/, "");

  return normalized || null;
};

export function getRepositoryBaseUrl(): string | null {
  const envValue = import.meta.env.VITE_REPO_URL;
  if (typeof envValue === "string") {
    if (!envValue.trim()) return null;
    return normalizeRepositoryUrl(envValue);
  }

  return normalizeRepositoryUrl(DEFAULT_REPO_URL);
}

export { DEFAULT_REPO_URL };
