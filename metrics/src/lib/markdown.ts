import DOMPurify from "dompurify";
import { marked } from "marked";

marked.setOptions({
  gfm: true,
  breaks: false,
});

const ALLOWED_URI_REGEXP = /^(?:(?:https?|mailto|tel):|\/|#)/i;
let hooksInstalled = false;

function ensureHooksInstalled() {
  if (hooksInstalled) return;
  hooksInstalled = true;

  DOMPurify.addHook("afterSanitizeAttributes", (node) => {
    if (typeof Element === "undefined") return;
    if (!(node instanceof Element)) return;
    if (node.tagName !== "A") return;

    const href = node.getAttribute("href") ?? "";
    const isExternal = /^https?:/i.test(href);

    if (isExternal) {
      node.setAttribute("target", "_blank");
      node.setAttribute("rel", "noopener noreferrer");
    } else {
      node.removeAttribute("target");
      node.removeAttribute("rel");
    }
  });
}

export function renderMarkdown(markdown: string): string {
  const rawHtml = marked.parse(markdown, { async: false }) as string;
  ensureHooksInstalled();
  return DOMPurify.sanitize(rawHtml, {
    USE_PROFILES: { html: true },
    ALLOWED_URI_REGEXP,
  });
}
