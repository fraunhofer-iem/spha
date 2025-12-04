<script setup lang="ts">
import { computed } from "vue";
import DashboardCard from "./DashboardCard.vue";
import type { RepoInfo } from "../model/Result.ts";

const props = withDefaults(defineProps<RepoInfo>(), {
    projectUrl: "Project URL not found",
});

const formattedContributors = computed(() => {
    // Access props via the 'props' object.
    if (props.contributors === undefined) {
        return "Number of Contributors not found";
    }
    return props.contributors.toLocaleString();
});

const formattedStars = computed(() => {
    if (props.stars === undefined) {
        return "Number of Stars not found";
    }
    return props.stars.toLocaleString();
});

// Format last commit date
const formattedLastCommitDate = computed(() => {
    if (!props.lastCommitDate) {
        return "Last commit date not found";
    }

    const date = new Date(props.lastCommitDate);
    if (isNaN(date.getTime())) {
        return "Invalid date";
    }

    return date.toLocaleDateString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
    });
});
</script>
<template>
    <DashboardCard title="Project Overview">
        <div class="container text-center align-content-center h-100">
            <div class="row border-bottom">
                <!-- Stars -->
                <div
                    class="col pb-4 border-end d-flex justify-content-center stat-item"
                >
                    <div class="row">
                        <div class="col">
                            <i class="bi bi-star icon-circle me-4"></i>
                        </div>
                        <div class="col text-center">
                            <div class="fs-4 fw-semibold">
                                {{ formattedStars }}
                            </div>
                            <div class="text-muted stat-label">Stars</div>
                        </div>
                    </div>
                </div>
                <!-- Last Commit -->
                <div class="col pb-4 stat-item d-flex justify-content-center">
                    <div
                        class="row d-flex justify-content-center align-items-center"
                    >
                        <div class="col">
                            <i class="bi icon-circle bi-clock-history me-4"></i>
                        </div>
                        <div class="col">
                            <div class="fs-5 fw-semibold">
                                {{ formattedLastCommitDate }}
                            </div>
                            <div class="text-muted stat-label">Last Commit</div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="row">
                <!-- Contributors -->
                <div
                    class="col border-end stat-item pt-4 d-flex justify-content-center"
                >
                    <div class="row">
                        <div class="col">
                            <i class="bi icon-circle bi-person me-4"></i>
                        </div>
                        <div class="col">
                            <div class="fs-4 fw-semibold">
                                {{ formattedContributors }}
                            </div>
                            <div class="text-muted stat-label">
                                Contributors
                            </div>
                        </div>
                    </div>
                </div>

                <div class="col pt-4">
                    <label
                        for="projectUrlInput"
                        class="form-label text-muted stat-label"
                        >Project Repository</label
                    >
                    <div class="input-group">
                        <input
                            type="text"
                            id="projectUrlInput"
                            class="form-control"
                            :value="projectUrl"
                            readonly
                        />
                        <a
                            :href="projectUrl"
                            target="_blank"
                            class="btn btn-outline-secondary"
                            role="button"
                            :aria-label="`Open project repository at ${projectUrl} in new tab`"
                            rel="noopener noreferrer"
                        >
                            <i class="bi bi-box-arrow-up-right"></i>
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </DashboardCard>
</template>

<style scoped>
.stat-label {
    font-size: 1rem; /* Make labels slightly larger than small */
}

.icon-circle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background-color: #e9ecf1;
    color: #3d4bf6;
    width: 4rem; /* Adjust size */
    height: 4rem;
    border-radius: 50%; /* Makes it circular */
    font-size: 2rem; /* Adjust icon size */
}
</style>
