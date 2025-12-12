<script setup lang="ts">
import DashboardCard from "./DashboardCard.vue";
import {computed} from "vue";
import {background_light_grey} from "../assets/styles/Colors.ts";
import type {Tool} from "../model/Result.ts";

const props = defineProps<{
  tools: Array<Tool>;
}>();

// --- ICON RESOLUTION FIX ---
// Import all SVGs eagerly and get their resolved URLs
const iconModules = import.meta.glob(
    "../assets/img/supportedTools/*.svg",
    {eager: true, import: "default"}
);

// Build a normalized lookup map: toolName -> icon URL
const normalize = (str: string): string =>
    str.toLowerCase().replace(/[\s-_]/g, "").replace(/[^a-z0-9]/g, "");

const iconMap: Record<string, string> = {};
Object.entries(iconModules).forEach(([path, url]) => {
  const match = path.match(/\/([^/]+)\.svg$/);
  if (match && match[1]) {
    const normalizedName = normalize(match[1]);
    iconMap[normalizedName] = url as string;
  }
});

const getToolIconPath = (toolName: string): string | null => {
  return iconMap[normalize(toolName)] ?? null;
};
// --- END FIX ---

const formattedScanDates = computed(() => {
  return props.tools.map((tool) => {
    if (!tool.scanDate) {
      return "Last scan date not found";
    }

    const date = new Date(tool.scanDate);
    if (isNaN(date.getTime())) {
      return "Invalid date";
    }

    return date.toLocaleDateString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  });
});

const downloadAll = () => {
  const allFindings = props.tools.reduce((acc, tool) => {
    if (tool.findings && tool.findings.length > 0) {
      acc[tool.name] = {
        scanDate: tool.scanDate,
        findings: tool.findings,
        findingsCount: tool.findings.length,
      };
    }
    return acc;
  }, {} as Record<string, any>);

  if (Object.keys(allFindings).length === 0) {
    console.log("No findings to download from any tools");
    return;
  }

  const jsonData = JSON.stringify(allFindings, null, 2);
  const blob = new Blob([jsonData], {type: "application/json"});
  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = url;
  link.download = "all-tool-findings.json";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

const downloadToolFindings = (tool: Tool) => {
  if (!tool.findings || tool.findings.length === 0) {
    console.log("No findings to download for", tool.name);
    return;
  }

  const jsonData = JSON.stringify(tool.findings, null, 2);
  const blob = new Blob([jsonData], {type: "application/json"});
  const url = URL.createObjectURL(blob);

  const link = document.createElement("a");
  link.href = url;
  link.download = `${tool.name.toLowerCase().replace(/\s+/g, "-")}-findings.json`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};
</script>

<template>
  <DashboardCard
      title-style="start"
      title="Tool Results"
      icon="tools"
      :showButton="true"
      buttonText="Download All"
      @button-click="downloadAll"
  >
    <div class="list-group list-group-flush">
      <template v-for="(tool, idx) in props.tools" :key="tool.name">
        <div
            class="list-group-item d-flex justify-content-between w-100"
            :style="`background-color: ${background_light_grey}`"
        >
          <div class="row">
            <div class="col align-items-end d-flex justify-content-center align-self-center">
              <img
                  v-if="getToolIconPath(tool.name)"
                  :src="getToolIconPath(tool.name)!"
                  alt="Tool Icon"
                  width="60"
              />
              <div
                  v-else
                  class="text-muted d-flex align-items-center justify-content-center"
                  style="
                  width: 60px;
                  height: 60px;
                  border: 1px solid #dee2e6;
                  border-radius: 4px;
                "
              >
                <i class="bi bi-gear-fill"></i>
              </div>
            </div>
            <div class="col">
              <div class="text-muted">{{ tool.name }}</div>
              <div class="fw-bold fs-5">
                {{ tool.findings?.length ?? 0 }} Findings
              </div>
              <div class="text-muted">
                Last Updated: {{ formattedScanDates[idx] }}
              </div>
            </div>
          </div>
          <div class="align-self-center">
            <button
                type="button"
                class="text-primary-emphasis bg-primary-subtle btn"
                @click="downloadToolFindings(tool)"
            >
              <i class="bi bi-download pe-2"></i> Download
            </button>
          </div>
        </div>
      </template>
    </div>
  </DashboardCard>
</template>

<style scoped>
.bi {
  font-size: 1.5rem;
}
</style>
