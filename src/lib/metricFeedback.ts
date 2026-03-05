import { getRepositoryBaseUrl } from "./repository";

type MetricFeedbackInput = {
  id: string;
  title?: string;
  pageUrl?: string;
};

export function buildMetricFeedbackUrl(metric: MetricFeedbackInput): string | null {
  const repoUrl = getRepositoryBaseUrl();
  if (!repoUrl) return null;

  const trimmedTitle = metric.title?.trim();
  const feedbackTitle = trimmedTitle
    ? `Feedback: [${metric.id}] ${trimmedTitle}`
    : `Feedback: [${metric.id}]`;

  const params: Array<[string, string]> = [
    ["template", "metric-feedback.yml"],
    ["labels", "metric-feedback"],
    ["title", feedbackTitle],
    ["metric_id", metric.id],
  ];

  if (trimmedTitle) {
    params.push(["metric_title", trimmedTitle]);
  }

  if (metric.pageUrl) {
    params.push(["page_url", metric.pageUrl]);
  }

  const query = params
    .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
    .join("&");

  return `${repoUrl}/issues/new?${query}`;
}
