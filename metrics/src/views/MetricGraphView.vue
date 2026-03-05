<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import * as d3 from "d3";
import { useMetricsCatalogue } from "../lib/useMetricsCatalogue";
import { renderMarkdown } from "../lib/markdown";
import type { Metric } from "../lib/metrics";

const { metrics, phases, loading, error, metricById, phaseLabelMap } = useMetricsCatalogue();

const svgRef = ref<SVGSVGElement | null>(null);
const containerRef = ref<HTMLDivElement | null>(null);
const containerWidth = ref(0);
const showAll = ref(false);
const selectedMetricId = ref<string | null>(null);
const selectedPhaseId = ref<string | null>(null);
const isExpanded = ref(false);

const isMobile = ref(false);
const maxInitialNodes = computed(() => (isMobile.value ? 16 : 30));

const displayedMetrics = computed(() => {
  if (showAll.value || metrics.value.length <= maxInitialNodes.value) {
    return metrics.value;
  }
  return metrics.value.slice(0, maxInitialNodes.value);
});

const isLimited = computed(() => metrics.value.length > maxInitialNodes.value && !showAll.value);

const phaseColorMap = computed(() => {
  const domain = phases.value.map((phase) => phase.id);
  const scale = d3.scaleOrdinal<string, string>(d3.schemeTableau10).domain(domain);
  const map = new Map<string, string>();
  for (const phase of phases.value) {
    map.set(phase.id, scale(phase.id));
  }
  return map;
});

type GraphNode = d3.SimulationNodeDatum & {
  id: string;
  label: string;
  phase?: string;
  role: "metric" | "phase" | "root";
};

type GraphLink = d3.SimulationLinkDatum<GraphNode> & {
  source: string | GraphNode;
  target: string | GraphNode;
  role: "phase-link" | "dependency";
};

const nodes = computed<GraphNode[]>(() => {
  const metricNodes = displayedMetrics.value.map((metric) => ({
    id: metric.id,
    label: metric.title,
    phase: metric.phase,
    role: "metric" as const,
  }));

  const phaseNodes = phases.value.map((phase) => ({
    id: `phase:${phase.id}`,
    label: phase.name,
    phase: phase.id,
    role: "phase" as const,
  }));

  return [{ id: "root", label: "SSDLC", role: "root" as const }, ...phaseNodes, ...metricNodes];
});

const links = computed<GraphLink[]>(() => {
  const visibleMetricIds = new Set(displayedMetrics.value.map((metric) => metric.id));
  const result: GraphLink[] = [];

  for (const phase of phases.value) {
    result.push({ source: "root", target: `phase:${phase.id}`, role: "phase-link" });
  }

  for (const metric of displayedMetrics.value) {
    result.push({ source: `phase:${metric.phase}`, target: metric.id, role: "phase-link" });
  }

  for (const metric of displayedMetrics.value) {
    for (const dependency of metric.depends_on ?? []) {
      if (!visibleMetricIds.has(dependency)) continue;
      if (dependency === metric.id) continue;
      result.push({ source: dependency, target: metric.id, role: "dependency" });
    }
  }

  return result;
});

const selectedMetric = computed(() => {
  if (!selectedMetricId.value) return null;
  return metricById.value.get(selectedMetricId.value) ?? null;
});

const selectedPhase = computed(() => {
  if (!selectedPhaseId.value) return null;
  return phases.value.find((phase) => phase.id === selectedPhaseId.value) ?? null;
});

const selectedDescription = computed(() => {
  if (!selectedMetric.value) return "";
  return extractDescription(selectedMetric.value);
});

const highlightedNodeIds = computed(() => {
  if (!selectedPhaseId.value) return null;
  const set = new Set<string>();
  set.add(`phase:${selectedPhaseId.value}`);
  for (const metric of displayedMetrics.value) {
    if (metric.phase === selectedPhaseId.value) {
      set.add(metric.id);
    }
  }
  return set;
});

function extractDescription(metric: Metric): string {
  const markdown = metric.markdown ?? "";
  if (typeof document !== "undefined") {
    const html = renderMarkdown(markdown);
    const temp = document.createElement("div");
    temp.innerHTML = html;
    const firstParagraph = temp.querySelector("p");

    return (firstParagraph?.textContent ?? temp.textContent ?? "").trim();
  }

  const lines = markdown
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .filter((line) => !line.startsWith("#"));
  return lines[0]?.replace(/[*_`>]/g, "").trim() ?? "";
}

let simulation: d3.Simulation<GraphNode, undefined> | null = null;

function stopSimulation() {
  simulation?.stop();
  simulation = null;
}

function renderGraph() {
  if (!svgRef.value || !containerRef.value) return;

  const width = containerRef.value.clientWidth || 720;
  const compactHeight = Math.max(320, Math.round(width * 0.8));
  const regularHeight = Math.max(440, Math.round(width * 0.65));
  const expandedHeight = Math.max(
    isMobile.value ? 420 : 560,
    Math.round(window.innerHeight * (isMobile.value ? 0.6 : 0.75)),
  );
  const height = isExpanded.value ? expandedHeight : isMobile.value ? compactHeight : regularHeight;
  const centerX = width / 2;
  const centerY = height / 2;
  const phaseRadius = Math.min(width, height) * (isMobile.value ? 0.24 : 0.28);

  const svg = d3.select(svgRef.value);
  svg.selectAll("*").remove();
  svg.attr("viewBox", `0 0 ${width} ${height}`);

  svg.on("click", (event) => {
    if (event.target !== svgRef.value) return;
    selectedMetricId.value = null;
    selectedPhaseId.value = null;
  });

  const zoomGroup = svg.append("g").attr("class", "catalogue-graph__viewport");

  const zoom = d3
    .zoom<SVGSVGElement, unknown>()
    .scaleExtent([isMobile.value ? 0.35 : 0.5, isMobile.value ? 2.0 : 2.4])
    .on("zoom", (event) => {
      zoomGroup.attr("transform", event.transform);
    });

  svg.call(
    zoom as unknown as (selection: d3.Selection<SVGSVGElement, unknown, null, undefined>) => void,
  );

  stopSimulation();

  const phaseAnchors = new Map<string, { x: number; y: number }>();
  if (phases.value.length > 0) {
    const step = (Math.PI * 2) / phases.value.length;
    phases.value.forEach((phase, index) => {
      const angle = -Math.PI / 2 + index * step;
      phaseAnchors.set(phase.id, {
        x: centerX + Math.cos(angle) * phaseRadius,
        y: centerY + Math.sin(angle) * phaseRadius,
      });
    });
  }

  const nodeList = nodes.value.map((node) => {
    const cloned = { ...node } as GraphNode;
    if (node.role === "root") {
      cloned.fx = centerX;
      cloned.fy = centerY;
    } else if (node.role === "phase") {
      const anchor = phaseAnchors.get(node.phase ?? "");
      if (anchor) {
        cloned.fx = anchor.x;
        cloned.fy = anchor.y;
      }
    }
    return cloned;
  });
  const linkList = links.value.map((link) => ({ ...link }));
  const highlightSet = highlightedNodeIds.value;

  const linkSelection = zoomGroup
    .append("g")
    .attr("class", "catalogue-graph__links")
    .selectAll("line")
    .data(linkList)
    .join("line")
    .attr("class", (link) => {
      if (!highlightSet) return "";
      const sourceId = typeof link.source === "string" ? link.source : link.source.id;
      const targetId = typeof link.target === "string" ? link.target : link.target.id;
      if (highlightSet.has(sourceId) && highlightSet.has(targetId)) {
        return "is-highlighted";
      }
      if (
        link.role === "phase-link" &&
        (highlightSet.has(sourceId) || highlightSet.has(targetId))
      ) {
        return "is-highlighted";
      }
      return "is-dim";
    })
    .attr("stroke", (link) =>
      link.role === "dependency" ? "rgba(90, 76, 242, 0.18)" : "rgba(40, 40, 80, 0.16)",
    )
    .attr("stroke-width", (link) => (link.role === "dependency" ? 1.2 : 1.6));

  const nodeSelection = zoomGroup
    .append("g")
    .attr("class", "catalogue-graph__nodes")
    .selectAll("g")
    .data(nodeList)
    .join("g")
    .attr("class", (node) => {
      const classes = ["catalogue-graph__node", `catalogue-graph__node--${node.role}`];
      if (node.id === selectedMetricId.value) classes.push("is-selected");
      if (highlightSet) {
        classes.push(highlightSet.has(node.id) ? "is-highlighted" : "is-dim");
      }
      return classes.join(" ");
    })
    .style("cursor", (node) =>
      node.role === "metric" || node.role === "phase" ? "pointer" : "default",
    )
    .on("click", (event, node) => {
      event.stopPropagation();
      if (node.role === "metric") {
        selectedMetricId.value = node.id;
        selectedPhaseId.value = null;
        return;
      }
      if (node.role === "phase") {
        selectedPhaseId.value = node.phase ?? null;
        selectedMetricId.value = null;
        return;
      }
      selectedMetricId.value = null;
      selectedPhaseId.value = null;
    });

  const symbol = d3.symbol();

  nodeSelection
    .filter((node) => node.role === "root" || node.role === "phase")
    .append("path")
    .attr("d", (node) => {
      if (node.role === "root") {
        return symbol.type(d3.symbolDiamond).size(520)();
      }
      return symbol.type(d3.symbolSquare).size(360)();
    })
    .attr("fill", (node) => {
      if (node.role === "root") return "#1f1749";
      return phaseColorMap.value.get(node.phase ?? "") ?? "#7a6bff";
    })
    .attr("stroke", "#ffffff")
    .attr("stroke-width", 2.4);

  nodeSelection
    .filter((node) => node.role === "metric")
    .append("circle")
    .attr("r", 12)
    .attr("fill", (node) => phaseColorMap.value.get(node.phase ?? "") ?? "#7a6bff")
    .attr("stroke", "#ffffff")
    .attr("stroke-width", 2);

  nodeSelection
    .append("text")
    .attr("x", 16)
    .attr("y", 4)
    .text((node) => node.label);

  const labelRadius = (node: GraphNode) => {
    const labelLength = node.label?.length ?? 0;
    const base = node.role === "root" ? 34 : node.role === "phase" ? 28 : 22;
    const scale = isMobile.value ? 2.1 : 2.6;
    return base + Math.min(70, labelLength * scale);
  };

  const phaseLinkDistance = isMobile.value ? 100 : 120;
  const dependencyDistance = isMobile.value ? 120 : 140;

  simulation = d3
    .forceSimulation(nodeList)
    .force(
      "link",
      d3
        .forceLink<GraphNode, GraphLink>(linkList)
        .id((node) => node.id)
        .distance((link) => (link.role === "phase-link" ? phaseLinkDistance : dependencyDistance))
        .strength((link) => (link.role === "phase-link" ? 0.95 : 0.7)),
    )
    .force(
      "charge",
      d3.forceManyBody<GraphNode>().strength((node) => (node.role === "root" ? -520 : -320)),
    )
    .force("center", d3.forceCenter(centerX, centerY))
    .force(
      "phase-cluster",
      d3
        .forceX<GraphNode>()
        .x((node) => {
          if (node.role === "metric") {
            return phaseAnchors.get(node.phase ?? "")?.x ?? centerX;
          }
          return typeof node.x === "number" ? node.x : centerX;
        })
        .strength((node) => (node.role === "metric" ? 0.08 : 0)),
    )
    .force(
      "phase-cluster-y",
      d3
        .forceY<GraphNode>()
        .y((node) => {
          if (node.role === "metric") {
            return phaseAnchors.get(node.phase ?? "")?.y ?? centerY;
          }
          return typeof node.y === "number" ? node.y : centerY;
        })
        .strength((node) => (node.role === "metric" ? 0.08 : 0)),
    )
    .force(
      "collide",
      d3.forceCollide<GraphNode>().radius((node) => labelRadius(node)),
    )
    .on("tick", () => {
      linkSelection
        .attr("x1", (link) => (link.source as GraphNode).x ?? 0)
        .attr("y1", (link) => (link.source as GraphNode).y ?? 0)
        .attr("x2", (link) => (link.target as GraphNode).x ?? 0)
        .attr("y2", (link) => (link.target as GraphNode).y ?? 0);

      nodeSelection.attr("transform", (node) => `translate(${node.x ?? 0}, ${node.y ?? 0})`);
    });
}

let resizeObserver: ResizeObserver | null = null;
let mobileListener: (() => void) | null = null;

onMounted(() => {
  const updateIsMobile = () => {
    isMobile.value = window.innerWidth <= 720;
  };
  updateIsMobile();
  window.addEventListener("resize", updateIsMobile, { passive: true });
  mobileListener = () => window.removeEventListener("resize", updateIsMobile);
  if (containerRef.value) {
    containerWidth.value = containerRef.value.clientWidth || 0;
    if (typeof ResizeObserver !== "undefined") {
      resizeObserver = new ResizeObserver((entries) => {
        for (const entry of entries) {
          if (entry.contentRect.width) {
            containerWidth.value = entry.contentRect.width;
          }
        }
      });
      resizeObserver.observe(containerRef.value);
    }
  }
  void nextTick(renderGraph);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  stopSimulation();
  mobileListener?.();
});

watch(
  [nodes, links, containerWidth, showAll, selectedMetricId, selectedPhaseId, isExpanded, isMobile],
  () => {
    void nextTick(renderGraph);
  },
);

watch(displayedMetrics, (nextMetrics) => {
  if (!selectedMetricId.value) return;
  if (nextMetrics.some((metric) => metric.id === selectedMetricId.value)) return;
  selectedMetricId.value = null;
});
</script>

<template>
  <section
    class="catalogue-graph section section--light"
    :class="isExpanded ? 'catalogue-graph--expanded' : ''"
  >
    <div class="container catalogue-graph__container">
      <div class="section-header">
        <div>
          <p class="eyebrow">Catalogue</p>
          <h2>Metrics graph</h2>
        </div>
        <div class="section-subtitle">
          <p>Explore the dependency network across all security metrics.</p>
          <p class="count" v-if="metrics.length">{{ metrics.length }} metrics total</p>
        </div>
      </div>

      <div v-if="loading" class="state">Loading metrics catalogue…</div>
      <div v-else-if="error" class="state state--error">{{ error }}</div>
      <div v-else class="catalogue-graph__layout">
        <div class="catalogue-graph__surface">
          <div class="catalogue-graph__controls">
            <p v-if="isLimited" class="catalogue-graph__notice">
              Showing {{ displayedMetrics.length }} of {{ metrics.length }} metrics. More nodes are
              available.
            </p>
            <button
              v-if="isLimited"
              class="catalogue-graph__action"
              type="button"
              @click="showAll = true"
            >
              Show all metrics
            </button>
            <p v-else class="catalogue-graph__hint">Scroll or pinch to zoom, drag to pan.</p>
            <button
              v-if="!isMobile"
              class="catalogue-graph__action"
              type="button"
              @click="isExpanded = !isExpanded"
            >
              {{ isExpanded ? "Collapse view" : "Expand view" }}
            </button>
          </div>
          <div ref="containerRef" class="catalogue-graph__canvas">
            <svg ref="svgRef" role="img" aria-label="Metrics dependency graph"></svg>
          </div>
        </div>
        <aside class="catalogue-graph__panel">
          <div v-if="!selectedMetric && !selectedPhase" class="catalogue-graph__placeholder">
            <p class="eyebrow">Details</p>
            <h3>Select a metric or phase</h3>
            <p>Click a node to see its description and jump to the detailed view.</p>
          </div>
          <div v-else-if="selectedPhase" class="catalogue-graph__details">
            <p class="metric-phase">SSDLC phase</p>
            <h3>{{ selectedPhase.name }}</h3>
            <p class="catalogue-graph__description">{{ selectedPhase.description }}</p>
          </div>
          <div v-else class="catalogue-graph__details">
            <p class="metric-phase">
              {{ phaseLabelMap.get(selectedMetric?.phase ?? "") ?? selectedMetric?.phase }}
            </p>
            <h3>{{ selectedMetric?.title }}</h3>
            <p class="metric-id">{{ selectedMetric?.id }}</p>
            <p v-if="selectedDescription" class="catalogue-graph__description">
              {{ selectedDescription }}
            </p>
            <RouterLink
              v-if="selectedMetric"
              class="catalogue-graph__link"
              :to="`/metrics/${selectedMetric.id}`"
            >
              View metric details
            </RouterLink>
          </div>
        </aside>
      </div>
    </div>
  </section>
</template>
