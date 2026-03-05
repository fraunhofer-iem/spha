# Metric Catalogue Implementation To-Do (Phase 1)

## 1) Confirm Core Tech Decisions

- Data format: Markdown files with YAML frontmatter in `metrics/`.
- Frontmatter parser: use `front-matter`.
- Markdown renderer: use `marked` for client-side rendering and sanitize HTML with `DOMPurify`.
- Schema validation: use `zod` for frontmatter validation in a CI script.
- Index generation: add a Node script to create `metrics/index.json` from frontmatter.
- Hosting: GitHub Pages with Vite (confirm `base` path in `vite.config.ts`).

## 2) Define Minimal Metric Schema (v1)

- Required fields: `id`, `title`, `phase`.
- Optional fields: `thresholds`, `tags`, `related_tools`, `depends_on`, `references`.
- SSDLC phase taxonomy: Plan, Code, Build, Test, Release, Deploy, Operate, Monitor.
- Phase metadata: include `description` and `icon` fields.
- Dependency rules: confirm how hierarchical metrics are represented (`depends_on` list of ids).

## 3) Establish Repository Structure

- Create `metrics/` directory for Markdown files.
- Add `metrics/README.md` describing the schema briefly.
- Add `metrics/templates/metric.md` template.
- Add `.github/PULL_REQUEST_TEMPLATE.md` aligned with schema.

## 4) Implement Data Ingestion (Build-Time First)

- Add a Node script (e.g., `scripts/build-metrics-index.ts`) to parse Markdown and emit `metrics/index.json`.
- Validate frontmatter against schema during index generation (fail on invalid).
- Decide whether to run script as part of `npm run build` or separate `npm run metrics:build`.

## 5) CI Validation Setup (Minimal)

- Add a CI step to run the index generation + schema validation.
- Decide CI provider: GitHub Actions.
- Decide trigger: pull requests to `main`.

## Open Questions For Phase 1

- Confirm the icon format for phases (e.g., icon name, SVG path, or emoji).

## Implemented So Far

- Added `metrics/` structure with `metrics/phases.json`, `metrics/README.md`, and `metrics/templates/metric.md`.
- Added sample metric `metrics/plan-security-requirements-coverage.md`.
- Added `scripts/metrics-index.mjs` + `scripts/build-metrics-index.mjs` to validate metrics and generate `public/metrics/index.json`.
- Added `.github/PULL_REQUEST_TEMPLATE.md` and `.github/workflows/metrics-validation.yml`.
- Added `npm run metrics:build` and `npm test` (Node test runner) plus tests in `scripts/__tests__/metrics-index.test.mjs`.
- Updated `vite.config.ts` with a GitHub Pages base path env hook (`VITE_BASE_PATH`).
- Added `src/lib/metrics.ts` data loader and a Phase Overview UI in `src/App.vue`.
- Replaced base styles in `src/style.css` with the catalogue UI design.
- Added Vue Router with dedicated views for overview and metrics list plus a navbar shell.
- Added metric detail route with markdown rendering using `marked` + `DOMPurify`.
- Added related metrics to the detail view and linked source files to GitHub when `VITE_REPO_URL` is set.
- Added phase-card navigation to `/metrics` with preselected phase filters.
- Fixed phase filter interaction so it remains clickable alongside URL preselection.
- Migrated UI tests to `@testing-library/vue` + `happy-dom`.
- Added a shared metrics cache in `useMetricsCatalogue` to prevent duplicate fetches.
- Implemented the issue-form-based “Propose a metric” flow and automation:
  - Issue form at `.github/ISSUE_TEMPLATE/metric.yml`.
  - Action at `.github/workflows/metric-proposal.yml` to create PRs.
  - Script at `scripts/issue-to-metric.mjs` to generate metric markdown files.

## Requirements Added

- The full metrics list lives in its own route (`/metrics`).
- The app includes a global navbar to navigate between overview and metrics.
