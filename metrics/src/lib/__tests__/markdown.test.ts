/*
 * Copyright (c) Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

import { describe, expect, it } from "vitest";
import { renderMarkdown } from "./../markdown";

function parse(html: string) {
  return new DOMParser().parseFromString(html, "text/html");
}

describe("renderMarkdown", () => {
  it("removes unsafe javascript links", () => {
    const html = renderMarkdown("[X](javascript:alert(1))");
    const doc = parse(html);
    const link = doc.querySelector("a");
    if (link) {
      const href = link.getAttribute("href") ?? "";
      expect(href.toLowerCase()).not.toContain("javascript:");
    }
  });

  it("adds rel/target to external links", () => {
    const html = renderMarkdown("[Example](https://example.com)");
    const doc = parse(html);
    const link = doc.querySelector("a");
    expect(link?.getAttribute("target")).toBe("_blank");
    expect(link?.getAttribute("rel")).toContain("noopener");
  });
});
