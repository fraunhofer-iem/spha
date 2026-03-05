# Metric Catalogue Schema

Each metric lives in its own Markdown file with YAML frontmatter followed by a Markdown body. The body is the metric description and can contain sections for rationale, measurement guidance, examples, and references.

## Required Frontmatter Fields

- `id`: unique string identifier (kebab-case preferred).
- `title`: human-readable metric name.
- `phase`: SSDLC phase id from `metrics/phases.json`.

## Optional Frontmatter Fields

- `thresholds`: list of threshold objects with `name`, `value`, and optional `description`.
- `tags`: list of strings for filtering.
- `related_tools`: list of tools related to the metric.
- `depends_on`: list of metric `id` values this metric depends on.
- `references`: list of URLs or citations.

## Phases

Phase metadata lives in `metrics/phases.json` and includes `id`, `name`, `description`, `icon`, and `order`.

## Validation

Run `npm run metrics:build` to validate metrics and generate `public/metrics/index.json`.
