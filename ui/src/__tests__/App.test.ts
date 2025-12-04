import {afterEach, beforeEach, describe, expect, it, vi} from "vitest";
import {mount, type VueWrapper} from "@vue/test-utils";
import {createRouter, createWebHistory} from "vue-router";
import App from "../App.vue";

describe("App", () => {
    let wrapper: VueWrapper<any>;
    let consoleSpy: any;

    beforeEach(() => {
        consoleSpy = vi.spyOn(console, "log").mockImplementation(() => {
        });

        // Create a test router
        const router = createRouter({
            history: createWebHistory(),
            routes: [
                { path: '/', component: { template: '<div>Home</div>' } },
                { path: '/upload', component: { template: '<div>Upload</div>' } }
            ]
        });

        wrapper = mount(App, {
            global: {
                plugins: [router],
                stubs: {
                    'router-view': true,
                    'router-link': true
                }
            }
        });
    });

    afterEach(() => {
        if (wrapper) {
            wrapper.unmount();
        }
        consoleSpy.mockRestore();
    });

    describe("Component Initialization", () => {
        it("should mount successfully", () => {
            expect(wrapper.exists()).toBe(true);
        });

        it("should initialize with default sidebar state", () => {
            const vm = wrapper.vm as any;
            expect(vm.sidebarCollapsed).toBe(false);
        });

        it("should render Navbar component", () => {
            const navbar = wrapper.findComponent({name: "Navbar"});
            expect(navbar.exists()).toBe(true);
        });

        it("should render Sidebar component", () => {
            const sidebar = wrapper.findComponent({name: "Sidebar"});
            expect(sidebar.exists()).toBe(true);
        });

        it("should have correct container structure", () => {
            const container = wrapper.find(".container-fluid.mt-4");
            expect(container.exists()).toBe(true);
        });
    });

    describe("Router Integration", () => {
        it("should render router-view component", () => {
            const routerView = wrapper.find('router-view-stub');
            expect(routerView.exists()).toBe(true);
        });

        it("should have main content area with correct structure", () => {
            const mainContent = wrapper.find('.main-content');
            expect(mainContent.exists()).toBe(true);
            expect(mainContent.classes()).toContain('flex-grow-1');
        });
    });

    describe("Sidebar Toggle Functionality", () => {
        it("should handle sidebar toggle event", () => {
            const vm = wrapper.vm as any;

            // Initial state should be false
            expect(vm.sidebarCollapsed).toBe(false);

            // Call toggle function
            vm.onSidebarToggle(true);

            expect(vm.sidebarCollapsed).toBe(true);
        });

        it("should update main content margin based on sidebar state", async () => {
            const vm = wrapper.vm as any;

            // Toggle sidebar
            vm.onSidebarToggle(true);
            await wrapper.vm.$nextTick();

            const mainContent = wrapper.find('.main-content');
            expect(mainContent.attributes('style')).toContain('margin-left: 80px');

            // Toggle back
            vm.onSidebarToggle(false);
            await wrapper.vm.$nextTick();

            expect(mainContent.attributes('style')).toContain('margin-left: 250px');
        });
    });
});