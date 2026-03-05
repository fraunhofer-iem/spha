# Metric Catalogue

A lightweight catalogue for SSDLC security metrics. The app turns Markdown-based metric definitions into a browsable, linkable reference so teams can align on what to measure, why it matters, and where each metric fits in the delivery lifecycle.

It is a Vue 3 + Vite frontend with a small build step that validates metrics and produces a static index for fast client-side filtering.

**Requirements**

- Node.js + npm

**Development**

```sh
npm install
npm run dev
```

**Build**

```sh
npm run metrics:build
npm run build
```

**Tests**

```sh
npm test
```

**Formatting**

```sh
npm run format
npm run format:fix
```

**Metrics Data**

- Metrics live under `metrics/` as Markdown with YAML frontmatter.
- `metrics/phases.json` defines available phases.
- `scripts/build-metrics-index.ts` validates and generates `public/metrics/index.json`.

**How It Works**

1. You add or update metric Markdown files in `metrics/`.
2. The build script validates frontmatter and assembles an index.
3. The UI reads the generated index and renders the catalogue.

**Project Structure (high level)**

- `src/`: Vue application and UI components.
- `metrics/`: Source-of-truth metric definitions.
- `public/metrics/index.json`: Generated metrics index.
- `scripts/`: Build and validation utilities.

**Environment Variables**

- `VITE_BASE_PATH`: base path for deployments (used by Vite).
- `VITE_REPO_URL`: repository URL used to link to source files.
- `VITE_REPO_BRANCH`: repository branch for source links (defaults to `main`).

**Why This Exists**

Security metrics often end up scattered across docs and spreadsheets. This catalogue keeps them versioned, reviewable, and easy to navigate, while keeping the content in simple Markdown so subject matter experts can edit without touching the UI.
