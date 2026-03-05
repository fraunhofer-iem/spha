/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_REPO_URL?: string;
  readonly VITE_REPO_BRANCH?: string;
  readonly VITE_BASE_PATH?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
