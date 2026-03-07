import { createRouter, createWebHistory } from "vue-router";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "overview",
      component: () => import("./views/OverviewView.vue"),
    },
    {
      path: "/about",
      name: "about",
      component: () => import("./views/AboutView.vue"),
    },
    {
      path: "/metrics",
      name: "metrics",
      component: () => import("./views/MetricsView.vue"),
    },
    {
      path: "/metrics/:id",
      name: "metric-detail",
      component: () => import("./views/MetricDetailView.vue"),
    },
    {
      path: "/graph",
      name: "metric-graph",
      component: () => import("./views/MetricGraphView.vue"),
    },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

export default router;
