<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import * as d3 from "d3";
import type { Metric, Phase } from "../lib/metrics";

const props = defineProps<{
  metric: Metric;
  parents: Metric[];
  children: Metric[];
  phases: Phase[];
}>();

type GraphNodeBase = {
  id: string;
  label: string;
  phase: string;
  role: "center" | "parent" | "child";
};

type GraphNode = GraphNodeBase & {
  x: number;
  y: number;
};

type GraphLink = {
  source: string;
  target: string;
};

const router = useRouter();
const svgRef = ref<SVGSVGElement | null>(null);
const containerRef = ref<HTMLDivElement | null>(null);
const isOpen = ref(true);
const containerWidth = ref(0);

const hasDependencies = computed(() => props.parents.length > 0 || props.children.length > 0);

const phaseColorMap = computed(() => {
  const domain = props.phases.map((phase) => phase.id);
  const scale = d3.scaleOrdinal<string, string>(d3.schemeTableau10).domain(domain);
  const map = new Map<string, string>();
  for (const phase of props.phases) {
    map.set(phase.id, scale(phase.id));
  }
  return map;
});

const nodes = computed<GraphNodeBase[]>(() => {
  const base: GraphNodeBase[] = [
    {
      id: props.metric.id,
      label: props.metric.title,
      phase: props.metric.phase,
      role: "center",
    },
  ];

  props.parents.forEach((parent, _) => {
    base.push({
      id: parent.id,
      label: parent.title,
      phase: parent.phase,
      role: "parent",
    });
  });

  props.children.forEach((child, _) => {
    base.push({
      id: child.id,
      label: child.title,
      phase: child.phase,
      role: "child",
    });
  });

  return base;
});

const links = computed<GraphLink[]>(() => {
  const result: GraphLink[] = [];
  for (const parent of props.parents) {
    result.push({ source: parent.id, target: props.metric.id });
  }
  for (const child of props.children) {
    result.push({ source: props.metric.id, target: child.id });
  }
  return result;
});

function radialPositions(count: number, radius: number, startAngle: number) {
  if (count <= 0) return [] as { x: number; y: number }[];
  const step = (Math.PI * 2) / count;
  return Array.from({ length: count }, (_, index) => {
    const angle = startAngle + index * step;
    return {
      x: Math.cos(angle) * radius,
      y: Math.sin(angle) * radius,
    };
  });
}

function renderGraph() {
  if (!svgRef.value || !containerRef.value || !isOpen.value) return;

  const width = containerRef.value.clientWidth || 600;
  const isNarrow = width < 520;
  const height = Math.max(isNarrow ? 220 : 260, Math.round(width * (isNarrow ? 0.75 : 0.6)));
  const maxRadius = Math.max(
    isNarrow ? 96 : 120,
    Math.min(width, height) / 2 - (isNarrow ? 24 : 36),
  );
  const parentRadius = Math.max(isNarrow ? 68 : 80, maxRadius * 0.6);
  const childRadius = Math.max(isNarrow ? 90 : 110, maxRadius * 0.9);
  const centerX = width / 2;
  const centerY = height / 2;

  const svg = d3.select(svgRef.value);
  svg.selectAll("*").remove();
  svg.attr("viewBox", `0 0 ${width} ${height}`);

  const parentPositions = radialPositions(props.parents.length, parentRadius, -Math.PI / 2);
  const childPositions = radialPositions(props.children.length, childRadius, -Math.PI / 2);

  const nodeList: GraphNode[] = nodes.value.map((node) => {
    if (node.role === "center") {
      return { ...node, x: 0, y: 0 };
    }
    if (node.role === "parent") {
      const index = props.parents.findIndex((parent) => parent.id === node.id);
      const position = parentPositions[index] ?? { x: 0, y: 0 };
      return { ...node, x: position.x, y: position.y };
    }
    const index = props.children.findIndex((child) => child.id === node.id);
    const position = childPositions[index] ?? { x: 0, y: 0 };
    return { ...node, x: position.x, y: position.y };
  });

  const graphGroup = svg
    .append("g")
    .attr("class", "metric-graph__group")
    .attr("transform", `translate(${centerX}, ${centerY})`);

  const nodeIndex = new Map(nodeList.map((node) => [node.id, node]));

  graphGroup
    .append("g")
    .attr("class", "metric-graph__links")
    .selectAll("line")
    .data(links.value)
    .join("line")
    .attr("x1", (link) => nodeIndex.get(link.source)?.x ?? 0)
    .attr("y1", (link) => nodeIndex.get(link.source)?.y ?? 0)
    .attr("x2", (link) => nodeIndex.get(link.target)?.x ?? 0)
    .attr("y2", (link) => nodeIndex.get(link.target)?.y ?? 0)
    .attr("stroke", "rgba(90, 76, 242, 0.25)")
    .attr("stroke-width", 1.4);

  const nodeGroup = graphGroup
    .append("g")
    .attr("class", "metric-graph__nodes")
    .selectAll("g")
    .data(nodeList)
    .join("g")
    .attr("class", (node) => `metric-graph__node metric-graph__node--${node.role}`)
    .attr("transform", (node) => `translate(${node.x}, ${node.y})`)
    .style("cursor", (node) => (node.role === "center" ? "default" : "pointer"))
    .on("click", (_, node) => {
      if (node.role === "center") return;
      void router.push(`/metrics/${node.id}`);
    });

  nodeGroup
    .append("circle")
    .attr("r", (node) => (node.role === "center" ? 18 : 12))
    .attr("fill", (node) => phaseColorMap.value.get(node.phase) ?? "#7a6bff")
    .attr("stroke", "#ffffff")
    .attr("stroke-width", 2);

  nodeGroup
    .append("text")
    .attr("x", (node) => (node.x >= 0 ? 16 : -16))
    .attr("y", 4)
    .attr("text-anchor", (node) => (node.x >= 0 ? "start" : "end"))
    .text((node) => node.label);
}

let resizeObserver: ResizeObserver | null = null;

onMounted(() => {
  if (typeof window !== "undefined" && window.innerWidth <= 720) {
    isOpen.value = false;
  }
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
});

watch([nodes, links, containerWidth, isOpen], () => {
  void nextTick(renderGraph);
});
</script>

<template>
  <section class="metric-graph">
    <button
      class="filter-toggle metric-graph__toggle"
      type="button"
      :aria-expanded="isOpen"
      aria-controls="dependency-graph-panel"
      @click="isOpen = !isOpen"
    >
      <span>Dependency graph</span>
      <span class="toggle-meta">
        <span class="toggle-text">{{ isOpen ? "collapse" : "expand" }}</span>
        <span class="toggle-indicator" :class="isOpen ? 'is-open' : ''">+</span>
      </span>
    </button>

    <div v-if="isOpen" id="dependency-graph-panel" class="metric-graph__panel">
      <div v-if="!hasDependencies" class="state">No dependencies to visualize yet.</div>
      <div v-else ref="containerRef" class="metric-graph__canvas">
        <svg ref="svgRef" role="img" aria-label="Metric dependency graph"></svg>
      </div>
    </div>
  </section>
</template>
