import { getRepoBranch } from "./config";
import { getRepositoryBaseUrl } from "./repository";

type MetricFeedbackInput = {
  id: string;
  title?: string;
  sourcePath?: string;
};

export function buildMetricFeedbackUrl(metric: MetricFeedbackInput): string | null {
  const repoUrl = getRepositoryBaseUrl();
  if (!repoUrl) return null;

  const trimmedTitle = metric.title?.trim();
  const feedbackTitle = trimmedTitle
    ? `Comment: [${metric.id}] ${trimmedTitle}`
    : `Comment: [${metric.id}]`;

  const labels = ["metric-feedback"];

  const params: Array<[string, string]> = [
    ["template", "metric-feedback.yml"],
    ["labels", labels.join(",")],
    ["title", feedbackTitle],
    ["metric_id", metric.id],
  ];

  if (trimmedTitle) {
    params.push(["metric_title", trimmedTitle]);
  }

  if (metric.sourcePath) {
    const branch = getRepoBranch();
    params.push(["page_url", `${repoUrl}/blob/${branch}/${metric.sourcePath}`]);
  }

  const query = params.map(([key, value]) => `${key}=${encodeURIComponent(value)}`).join("&");

  return `${repoUrl}/issues/new?${query}`;
}
