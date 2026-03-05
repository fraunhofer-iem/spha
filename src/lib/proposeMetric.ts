import { DEFAULT_REPO_URL } from "./repository";

const ISSUE_TEMPLATE = "metric.yml";

export function getProposeMetricUrl(): string {
  const template = encodeURIComponent(ISSUE_TEMPLATE);
  return `${DEFAULT_REPO_URL}/issues/new?template=${template}`;
}
