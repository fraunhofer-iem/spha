import { createRouter, createWebHistory } from "vue-router";
import OverviewView from "./views/OverviewView.vue";
import AboutView from "./views/AboutView.vue";
import MetricsView from "./views/MetricsView.vue";
import MetricDetailView from "./views/MetricDetailView.vue";
import MetricGraphView from "./views/MetricGraphView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "overview",
      component: OverviewView,
    },
    {
      path: "/about",
      name: "about",
      component: AboutView,
    },
    {
      path: "/metrics",
      name: "metrics",
      component: MetricsView,
    },
    {
      path: "/metrics/:id",
      name: "metric-detail",
      component: MetricDetailView,
    },
    {
      path: "/graph",
      name: "metric-graph",
      component: MetricGraphView,
    },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

export default router;
