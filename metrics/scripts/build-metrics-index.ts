import path from "node:path";
import { buildMetricsIndex } from "./metrics-index.ts";

async function main() {
  const { payload, outputPath } = await buildMetricsIndex();
  const relativePath = path.relative(process.cwd(), outputPath).split(path.sep).join("/");
  console.log(`Wrote ${payload.metrics.length} metrics to ${relativePath}`);
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error);
  process.exit(1);
});
