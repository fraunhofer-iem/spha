# Metric Feedback Button – Refined TODO

## Chunk 1 – GitHub Issue Form (Repository Setup)

- [x] Create `.github/ISSUE_TEMPLATE/metric-feedback.yml`.
- [x] Add required dropdown: feedback type.
- [x] Add required input: metric ID.
- [x] Add optional input: metric title.
- [x] Add optional input: page URL.
- [x] Add required textarea: feedback content.
- [x] Add metadata labels: `["metric-feedback"]`.
- [ ] Ensure repository has label: `metric-feedback` (manual in GitHub UI).

Acceptance Criteria

- [ ] Visiting `/issues/new?template=metric-feedback.yml` selects the correct form. (manual)
- [ ] Submitting auto-applies `metric-feedback`. (manual)
- [ ] Metric ID cannot be empty. (manual)

## Chunk 2 – Repository URL Resolver

- [x] Add [repository.ts](/Users/janniclasstruwer/git/metric-catalogue/src/lib/repository.ts).
- [x] Implement `getRepositoryBaseUrl()` with env-first resolution.
- [x] Normalize trailing slashes.
- [x] Strip `/issues` suffix.
- [x] Return null for empty-string env.
- [x] Add unit tests for env variations.

Acceptance Criteria

- [x] Always returns clean base like `https://github.com/org/repo`.
- [x] Never returns trailing slash.
- [x] Never returns undefined.

## Chunk 3 – Feedback URL Builder (Core Logic)

- [x] Add [metricFeedback.ts](/Users/janniclasstruwer/git/metric-catalogue/src/lib/metricFeedback.ts).
- [x] Implement `buildMetricFeedbackUrl()` with title contract.
- [x] Include template, labels, title, metric_id, metric_title, page_url.
- [x] Encode with `encodeURIComponent`.
- [x] Omit undefined parameters.
- [x] Preserve parameter order.

Acceptance Criteria

- [x] Output URL works when pasted into browser (unit-tested).
- [x] All values correctly encoded.
- [x] No undefined appears in URL.
- [x] Returns null if repo URL missing.

## Chunk 4 – Unit Tests for URL Builder

- [x] Add [metricFeedback.test.ts](/Users/janniclasstruwer/git/metric-catalogue/src/lib/__tests__/metricFeedback.test.ts).
- [x] Normal metric (id + title + pageUrl).
- [x] Missing title.
- [x] Special characters in title.
- [x] Missing repo URL.
- [x] Assert template + label + encoded title + metric ID.

Acceptance Criteria

- [x] Branch coverage covers title present/absent and repo missing.
- [x] Tests fail if encoding removed.

## Chunk 5 – UI Integration

- [x] Import `buildMetricFeedbackUrl` in [MetricDetailView.vue](/Users/janniclasstruwer/git/metric-catalogue/src/views/MetricDetailView.vue).
- [x] Compute feedback URL with page URL.
- [x] Render button only when metric exists.
- [x] Render disabled button with tooltip if URL is null.
- [x] Ensure `target="_blank"` and `rel="noopener noreferrer"`.
- [x] Add styling for actions and disabled state.

Acceptance Criteria

- [x] Button visible on metric detail page.
- [x] Clicking opens GitHub in a new tab.
- [x] Disabled state works correctly when repo URL is missing.

## Chunk 6 – Component Tests

- [x] Enabled button renders when repo configured.
- [x] Button has correct href.
- [x] Disabled button renders when repo URL missing.

## Chunk 7 – Regression Safety

- [x] Ensure proposeMetric flow unchanged (tests pass).
- [x] Run full test suite.
- [x] Fix lint/TypeScript issues if any.
- [x] Confirm no unused imports introduced.

## Final Definition of Done

- [ ] GitHub Issue Form works manually.
- [x] Button opens correctly constructed URL.
- [x] All unit and component tests pass.
- [x] No TypeScript errors.
- [ ] No lint errors (not run).
- [x] No absolute file paths remain in code.
- [x] No changes to metric schema.
