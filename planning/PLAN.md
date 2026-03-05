# Metric Catalogue Website Plan

## Goals

- Build a web UI to browse a security metrics catalogue, with default SSDLC phase overview and top-level metrics.
- Provide a full metrics list with filtering by properties such as related tools, tags, thresholds, phases, and dependencies.
- Store metrics as Markdown in a GitHub repository, accessible without the website.
- Support proposing new metrics by generating a pull request in the GitHub repo.
- Provide templates for a metric Markdown file and a pull request.

## Decisions

- The metrics Markdown files and website sources live in the same repository.
- The website will be hosted on GitHub Pages.
- No GitHub OAuth. The website will describe the PR workflow and link to a prefilled PR template.
- Schema validation in CI is required for Markdown metrics.

## Proposed Data Model

- Directory structure: `metrics/` with one Markdown file per metric and optional `phases/` overview.
- Frontmatter schema in each metric file, including: `id`, `title`, `phase`, `description`, `thresholds`, `tags`, `related_tools`, `depends_on`, `references`.
- Optional `metrics/index.json` generated from Markdown frontmatter for faster client-side loading.
- Backward-compatible: metrics remain readable in raw Markdown on GitHub.

## UI And UX Plan

1. App Shell
1. Global navbar with links to Overview and Metrics list routes.
1. Dedicated routes: `/` for overview, `/metrics` for full list.

1. Phase Overview Screen
1. Display SSDLC phases as cards or a visual timeline.
1. Show top-level metrics per phase with quick drill-down.
1. Provide a global search and a “view all metrics” entry point.

1. Metrics List Screen
1. Full list view with filter panel and search.
1. Filters by phase, tags, thresholds, related tools, and dependencies.
1. Support multi-select filters and clear-all.
1. Show key metadata at a glance, with detail page or drawer.

1. Metric Detail View
1. Render Markdown content with structured metadata.
1. Show dependency graph or list of child metrics.
1. Links to related tools and references.

## Contribution And PR Flow

### Issue-Driven Workflow (No Git Required)

1. UI links to a GitHub Issue Form (`/issues/new?template=metric.yml`) for structured input.
1. User submits the issue; it receives label `metric-proposal`.
1. GitHub Action triggers on `issues.opened` and label.
1. Action parses the issue body, generates a metric markdown file, creates a branch, commits, and opens a PR.
1. Maintainers review and merge the PR.

### Required Repo Assets

1. Issue Form in `.github/ISSUE_TEMPLATE/metric.yml`.
1. Automation workflow in `.github/workflows/metric-proposal.yml`.
1. Parser script (e.g., `scripts/issue-to-metric.mjs`) to turn issue content into metric files.
1. Optional PR template in `.github/PULL_REQUEST_TEMPLATE.md`.

## Implementation Steps

1. Repository Setup And Structure
1. Add `metrics/` directory with initial example metrics and phase taxonomy.
1. Add metric Markdown template file and PR template.

1. Data Ingestion Layer
1. Parse Markdown + frontmatter at build time or runtime.
1. Build a normalized in-memory model for filtering.
1. Add optional JSON index generation script for speed.

1. Frontend Screens
1. Add app shell + routing to split overview and metrics list.
1. Build phase overview and top-level metrics view.
1. Build full list with filters, search, and summary cards.
1. Build metric detail view and dependency display.

1. Contribution Workflow
1. Build “Propose a metric” form and validation.
1. Implement PR creation flow using the pre-filled PR link.
1. Add success and error states.

1. Quality And Docs
1. Add schema validation (required CI check).
1. Document metric format and contribution steps in README.

## Deliverables

- Metric Markdown template and PR template.
- Vue UI for phase overview, list, filters, and detail.
- Metrics stored in GitHub as Markdown and accessible directly.
- Contribution flow that produces a PR.

## Next Confirmation Needed

- Confirm exact metadata fields and SSDLC phase taxonomy.
- Confirm the CI validation approach and tooling.

## Test Concept

### Goals

1. Prevent regressions in core UX flows (filters, routing, detail view, markdown rendering).
2. Keep tests stable and fast to run locally and in CI.
3. Cover data integrity (schema validation) and UI behavior separately.

### Test Layers

1. Data/Schema Tests (already implemented)
1. Node-based tests for the metrics index builder and schema validation.
1. Continue to validate dependencies, duplicate IDs, and empty bodies.

1. Component + Interaction Tests
1. Add UI tests for filters, routing, and detail view behavior using a lightweight component test runner.
1. Tooling: `@testing-library/vue` + `vitest` with `happy-dom`.

1. End-to-End (E2E) Smoke Tests
1. Run against a built preview server.
1. Focus on navigation and filtering flows, not visual styling.

### Core Test Scenarios (Must-Have)

1. Overview → Metrics navigation
1. Click a phase card in overview.
1. Assert metrics list opens with the phase filter preselected.

1. Phase filter toggle
1. Check a phase checkbox and confirm list updates.
1. Uncheck the same checkbox and confirm list resets.

1. Tag + Tool filtering
1. Select a tag and a tool and verify list contains matching metrics.
1. Clear filters and verify all metrics return.

1. Search behavior
1. Search matches metric title, id, tags, and tools.
1. Search empty state displayed when no matches.

1. Metric detail view
1. Open metric card from list.
1. Ensure markdown renders and “Related metrics” links navigate to another metric.

1. Source file link behavior
1. With `VITE_REPO_URL` set, verify the source file link points to GitHub.
1. Without the env var, ensure plain text fallback renders.

### Test Data Strategy

1. Use the sample metrics in `metrics/` as fixtures for tests.
1. Add a dedicated “test-only” metrics set if more deterministic fixtures are needed.

### CI Integration

1. Run data/schema tests on every PR (already in GitHub Actions).
1. Add UI/E2E test job on PRs once tooling is finalized.
