Metric Feedback Button – Implementation Plan

1. Goals
   Add a feedback button to each metric detail page.
   Open a GitHub Issue Form in a new tab with:
   Prefilled title
   Prefilled metric metadata (via query params)
   Pre-applied label metric-feedback
   Capture structured feedback (question / improvement / general).
   Require no backend, OAuth, or in-app submission flow.
   Success Criteria
   Clicking “Give feedback” opens a valid GitHub issue creation page.
   The correct template is selected.
   The issue title contains the metric ID.
   The issue is automatically labeled metric-feedback.
2. Non-Goals
   No in-app storage of feedback.
   No backend API interaction.
   No duplicate detection or automation workflows.
   No changes to metric schema or ingestion logic.
   No dynamic fetching of commit SHA or permalinks.
3. Architectural Decisions
   Use GitHub Issue Forms (.github/ISSUE_TEMPLATE/metric-feedback.yml).
   Use query-based template selection:
   /issues/new?template=metric-feedback.yml
   Use a single repository label: metric-feedback.
   Repository base URL resolution order:
   import.meta.env.VITE_REPO_URL
   Fallback to the same constant used in proposeMetric
   The button exists only in MetricDetailView.vue.
   Metric ID must always be present in the issue title.
   Metric page URL should be passed as a query parameter and prefilled into the form.
4. UX Specification
   4.1 Placement
   Button appears:
   Near metric title OR
   In the action section of the metric detail header.
   Label: “Give feedback”
   Style: secondary action (not primary CTA)
   4.2 Behavior
   On click:
   Opens new tab:
   target="\_blank"
   rel="noopener noreferrer"
   Navigates to GitHub issue creation page.
   4.3 Prefilled Title Format
   Feedback: [<metric-id>] <metric-title>
   Example:
   Feedback: [M-001] Dependency Coverage
   4.4 Edge Case Handling
   If metric title is undefined:
   Feedback: [M-001]
   If repo URL is missing:
   Button disabled
   Tooltip: “Feedback requires repository configuration.”
5. GitHub Issue Form Specification
   File:
   .github/ISSUE_TEMPLATE/metric-feedback.yml
   Required Fields
   Dropdown: feedback type
   Question
   Improvement
   General feedback
   Input: Metric ID (required)
   Textarea: Feedback description (required)
   Optional Fields
   Metric title
   Page URL
   Template Metadata
   labels: ["metric-feedback"]
   Acceptance Criteria
   Template is selectable via ?template=metric-feedback.yml
   Label is auto-applied
   Metric ID is required before submission
6. URL Construction Contract
   Base Format
   ${repoUrl}/issues/new
   ?template=metric-feedback.yml
   &labels=metric-feedback
   &title=<encodedTitle>
   &metric_id=<encodedMetricId>
   &metric_title=<encodedMetricTitle>
   &page_url=<encodedPageUrl>
   Encoding Requirements
   Use encodeURIComponent for all query parameters.
   No manual string concatenation without encoding.
   Do not include undefined parameters.
   Acceptance Criteria
   Generated URL is valid when pasted into browser.
   All parameters are properly URL-encoded.
   No double encoding occurs.
7. Technical Implementation
   7.1 Repository URL Resolver
   Create helper:
   src/lib/repository.ts
   Responsibilities:
   Export getRepositoryBaseUrl(): string | null
   Resolve in this order:
   VITE_REPO_URL
   fallback constant
   Normalize:
   Remove trailing slash
   7.2 Feedback URL Builder
   Create:
   src/lib/metricFeedback.ts
   Export:
   buildMetricFeedbackUrl(metric: {
   id: string
   title?: string
   pageUrl?: string
   }): string | null
   Responsibilities:
   Retrieve repo URL
   Return null if unavailable
   Build properly encoded URL
   Enforce title format contract
   7.3 UI Integration
   File:
   MetricDetailView.vue
   Steps:
   Import buildMetricFeedbackUrl
   Compute feedback URL
   Render button only if URL exists
   Disabled state if URL is null
8. Testing Strategy
   8.1 Unit Tests – URL Builder
   Cases:
   Normal metric
   Missing title
   Special characters in title
   Missing repo URL
   Ensure label and template are present
   Ensure correct encoding
   8.2 Component Tests
   Button renders for valid repo config.
   Button has correct href.
   Button disabled if repo URL missing.
9. Deliverables
   GitHub Issue Form YAML
   repository.ts
   metricFeedback.ts
   Feedback button in MetricDetailView.vue
   Unit tests
   Component tests
10. Definition of Done
    Clicking button opens correct GitHub issue form.
    Issue contains:
    Prefilled title
    Label metric-feedback
    Metric metadata
    All tests pass.
    No TypeScript errors.
    No lint violations.
