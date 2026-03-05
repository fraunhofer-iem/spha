# Publish Checklist

## Repository Cleanup And Configuration

- Confirm repository name, description, and default branch in GitHub.
- Update README with setup, contribution, and publishing instructions.
- Update the hardcoded issue creation link in `src/lib/proposeMetric.ts` to the correct repo URL for this project.
- Verify `.github/ISSUE_TEMPLATE/metric.yml` matches the final metric schema fields.
- Verify `metrics/` folder structure and `metrics/README.md` are complete and up to date.

## Build And Release Readiness

- Run `npm install` and ensure `npm run test` passes locally.
- Run `npm run metrics:build` and confirm `public/metrics/index.json` is generated.
- Validate that `VITE_BASE_PATH` is set correctly for GitHub Pages (e.g., `/repo-name/`).

## CI And GitHub Pages Deployment

- Add a GitHub Actions workflow that:
- Runs `npm ci`.
- Runs `npm run test`.
- Runs `npm run metrics:build` and `npm run build`.
- Deploys the built SPA to GitHub Pages.
- Configure GitHub Pages to deploy from GitHub Actions.
- Set `VITE_BASE_PATH` to `/metric-catalogue/` during build for GitHub Pages.

## Automation Workflows

- Verify `metric-proposal` workflow permissions (`contents: write`, `pull-requests: write`).
- Confirm the automation script writes metrics into `metrics/<phase>/` directories.
- Test the end-to-end issue → PR flow in the target repo.

## Final Review

- Smoke-test UI in production build.
- Confirm routing works on GitHub Pages (direct links to `/metrics` and `/metrics/:id`).
- Confirm the “Propose a metric” button opens the correct issue form in the target repo.
