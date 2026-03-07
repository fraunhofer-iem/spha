/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

import { describe, expect, it, afterEach } from "vitest";
import { loadMetricsIndex } from "./../metrics";
import { mockFetch } from "./../../test/utils";
import { vi } from "vitest";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("loadMetricsIndex", () => {
  it("returns parsed metrics index", async () => {
    const payload = {
      generated_at: "2024-01-01T00:00:00Z",
      phases: [
        {
          id: "plan",
          name: "Plan",
          description: "Planning",
          icon: "calendar",
          order: 1,
        },
      ],
      metrics: [
        {
          id: "metric-a",
          title: "Metric A",
          phase: "plan",
          markdown: "Body",
          source_path: "metrics/plan/metric-a.md",
          search_text: "metric a metric-a body plan",
          parents: [],
          children: [],
          missing_dependencies: [],
        },
      ],
      dependency_cycles: [],
    };

    mockFetch(payload);
    const result = await loadMetricsIndex();
    expect(result).toEqual(payload);
  });

  it("throws when the metrics index is malformed", async () => {
    mockFetch({ invalid: true });
    await expect(loadMetricsIndex()).rejects.toThrow("Metrics index is malformed.");
  });
});
