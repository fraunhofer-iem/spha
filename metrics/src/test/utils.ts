import { render } from "@testing-library/vue";
import { createMemoryHistory, createRouter, type Router, type RouteRecordRaw } from "vue-router";
import { vi } from "vitest";
import { nextTick } from "vue";

export async function renderWithRouter(
  component: any,
  {
    route = "/",
    routes = [{ path: "/", component }],
  }: { route?: string; routes?: RouteRecordRaw[] } = {},
) {
  const router: Router = createRouter({
    history: createMemoryHistory(),
    routes,
  });

  router.push(route);
  await router.isReady();

  return {
    router,
    ...render(component, {
      global: {
        plugins: [router],
      },
    }),
  };
}

export function mockFetch<T>(payload: T) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    json: async () => payload,
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

export async function flushPromises() {
  await Promise.resolve();
  await Promise.resolve();
  await nextTick();
  await nextTick();
  await new Promise((resolve) => setTimeout(resolve, 0));
}

export async function waitFor(
  predicate: () => boolean,
  { timeout = 1000, interval = 10 }: { timeout?: number; interval?: number } = {},
) {
  const start = Date.now();
  while (!predicate()) {
    await flushPromises();
    if (predicate()) return;
    if (Date.now() - start > timeout) {
      throw new Error("Timed out waiting for condition.");
    }
    await new Promise((resolve) => setTimeout(resolve, interval));
  }
}
