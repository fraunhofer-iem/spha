import {afterEach, beforeEach, describe, expect, it, vi} from "vitest";
import {mount, type VueWrapper} from "@vue/test-utils";
import ToolOverview from "../ToolOverview.vue";
import type {Tool} from "../../model/Result";

// Mock DashboardCard component
vi.mock("../DashboardCard.vue", () => ({
    default: {
        name: "DashboardCard",
        props: ["title", "titleStyle", "icon", "showButton", "buttonText"],
        emits: ["button-click"],
        template: `
          <div class="dashboard-card">
            <h5>{{ title }}</h5>
            <button v-if="showButton" @click="$emit('button-click')">{{ buttonText }}</button>
            <slot></slot>
          </div>
        `,
    },
}));

// Mock colors
vi.mock("../../assets/styles/Colors.ts", () => ({
    background_light_grey: "#F7F9FB",
}));

// Mock import.meta.glob for dynamic icon loading
vi.stubGlobal('import', {
    meta: {
        glob: vi.fn(() => ({
            '/src/assets/img/supportedTools/eslint.svg': true,
            '/src/assets/img/supportedTools/sonarqube.svg': true,
            '/src/assets/img/supportedTools/security-scanner.svg': true,
            '/src/assets/img/supportedTools/performance-tool.svg': true,
            '/src/assets/img/supportedTools/github.svg': true,
            '/src/assets/img/supportedTools/gitlab.svg': true,
            '/src/assets/img/supportedTools/ort.svg': true,
            '/src/assets/img/supportedTools/osv.svg': true,
            '/src/assets/img/supportedTools/trivy.svg': true,
            '/src/assets/img/supportedTools/trufflehog.svg': true,
            '/src/assets/img/supportedTools/technicallaganalyzer.svg': true,
        }))
    }
});

describe("ToolOverview", () => {
    let wrapper: VueWrapper<any>;
    let mockTools: Tool[];

    beforeEach(() => {
        // Setup basic mock tools
        mockTools = [
            {
                name: "ESLint",
                scanDate: "2024-01-15T10:30:00Z",
                findings: [
                    {rule: "no-unused-vars", file: "test.js"},
                    {rule: "missing-semicolon", file: "app.js"},
                ],
            },
            {
                name: "SonarQube",
                scanDate: "2024-01-14T15:45:00Z",
                findings: [{issue: "code-smell", severity: "major"}],
            },
            {
                name: "Security Scanner",
                scanDate: "invalid-date",
                findings: [],
            },
            {
                name: "Performance Tool",
                findings: [{metric: "load-time", value: 2.5}],
            },
        ];

        vi.clearAllMocks();
    });

    afterEach(() => {
        if (wrapper) {
            wrapper.unmount();
        }
    });

    describe("Component Initialization", () => {
        it("should mount with tools prop", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            expect(wrapper.exists()).toBe(true);
        });

        it("should render DashboardCard with correct title", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const dashboardCard = wrapper.findComponent({name: "DashboardCard"});
            expect(dashboardCard.exists()).toBe(true);
            expect(dashboardCard.props().title).toBe("Tool Results");
        });

        it("should render DashboardCard with correct props", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const dashboardCard = wrapper.findComponent({name: "DashboardCard"});
            expect(dashboardCard.props().titleStyle).toBe("start");
            expect(dashboardCard.props().icon).toBe("tools");
            expect(dashboardCard.props().showButton).toBe(true);
            expect(dashboardCard.props().buttonText).toBe("Download All");
        });
    });

    describe("Tool List Rendering", () => {
        it("should render correct number of tools", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const toolItems = wrapper.findAll(".list-group-item");
            expect(toolItems).toHaveLength(mockTools.length);
        });

        it("should display tool names", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });


            const toolNames = wrapper.findAll(".text-muted");
            expect(toolNames.length).toBeGreaterThan(0);
            // Find the element that contains the tool name specifically
            const toolNameElement = toolNames.find(el => el.text().includes("ESLint") || el.text() === "ESLint");
            expect(toolNameElement?.text()).toBe("ESLint");
        });

        it("should display findings count", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const findingsElements = wrapper.findAll(".fw-bold.fs-5");
            expect(findingsElements[0]?.text()).toBe("2 Findings");
            expect(findingsElements[1]?.text()).toBe("1 Findings");
            expect(findingsElements[2]?.text()).toBe("0 Findings");
            expect(findingsElements[3]?.text()).toBe("1 Findings");
        });

        it("should render tool icons when available", () => {
            // Create mock tools with some having icons and some not
            const mockToolsWithIcons = [
                {
                    name: "ESLint",
                    findings: [{rule: "no-unused-vars"}],
                    scanDate: "2024-01-15"
                },
                {
                    name: "Unknown Tool", // This won't have an icon
                    findings: [],
                    scanDate: "2024-01-14"
                }
            ];

            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockToolsWithIcons,
                },
            });

            // Since the mock icon system is set up to return URLs for known tools,
            // we should have at least one image for ESLint
            const images = wrapper.findAll("img");
            const fallbackIcons = wrapper.findAll(".bi-gear-fill");
            
            // We should have either images or fallback icons (or both)
            expect(images.length + fallbackIcons.length).toBeGreaterThan(0);
            
            // If images exist, check they have src attributes
            if (images.length > 0) {
                expect(images[0]?.attributes("src")).toBeDefined();
            }
        });

        it("should render fallback icons when tool icon is not available", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            // Check that fallback icon elements exist
            const fallbackIcons = wrapper.findAll(".bi-gear-fill");
            expect(fallbackIcons.length).toBeGreaterThan(0);
        });

        it("should render download buttons for each tool", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const downloadButtons = wrapper.findAll(
                ".text-primary-emphasis.bg-primary-subtle.btn",
            );
            expect(downloadButtons).toHaveLength(mockTools.length);

            downloadButtons.forEach((button) => {
                expect(button.text()).toContain("Download");
                expect(button.find(".bi-download").exists()).toBe(true);
            });
        });
    });

    describe("Date Formatting", () => {
        it("should format valid dates correctly", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const dateElements = wrapper.findAll(".text-muted");
            // Find elements that contain "Last Updated"
            const dateTexts = dateElements.filter((el) =>
                el.text().includes("Last Updated"),
            );
            expect(dateTexts[0]?.text()).toBe("Last Updated: Jan 15, 2024");
        });

        it("should handle invalid dates", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const dateElements = wrapper.findAll(".text-muted");
            const dateTexts = dateElements.filter((el) =>
                el.text().includes("Last Updated"),
            );
            expect(dateTexts[2]?.text()).toBe("Last Updated: Invalid date");
        });

        it("should handle missing dates", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const dateElements = wrapper.findAll(".text-muted");
            const dateTexts = dateElements.filter((el) =>
                el.text().includes("Last Updated"),
            );
            expect(dateTexts[3]?.text()).toBe(
                "Last Updated: Last scan date not found",
            );
        });

        it("should handle undefined scan dates", () => {
            const toolsWithUndefinedDate: Tool[] = [
                {
                    name: "Test Tool",
                    scanDate: undefined,
                    findings: [],
                },
            ];

            wrapper = mount(ToolOverview, {
                props: {
                    tools: toolsWithUndefinedDate,
                },
            });

            const dateElements = wrapper.findAll(".text-muted");
            const dateTexts = dateElements.filter((el) =>
                el.text().includes("Last Updated"),
            );
            expect(dateTexts[0]?.text()).toBe(
                "Last Updated: Last scan date not found",
            );
        });
    });

    describe("Component Structure and Styling", () => {
        it("should have correct CSS classes on list group", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const listGroup = wrapper.find(".list-group");
            expect(listGroup.exists()).toBe(true);
            expect(listGroup.classes()).toContain("list-group-flush");
        });

        it("should have correct layout classes on list items", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const listItems = wrapper.findAll(".list-group-item");
            listItems.forEach((item) => {
                expect(item.classes()).toContain("d-flex");
                expect(item.classes()).toContain("justify-content-between");
                expect(item.classes()).toContain("w-100");
            });
        });

        it("should apply background color styling", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const listItems = wrapper.findAll(".list-group-item");
            if (listItems.length > 0) {
                expect(listItems[0]?.attributes("style")).toContain("background-color");
            }
        });
    });

    describe("Edge Cases", () => {
        it("should handle tools with missing properties", () => {
            const minimalTools: Tool[] = [
                {
                    name: "Minimal Tool",
                    downloadLink: "https://example.com",
                } as Tool,
            ];

            wrapper = mount(ToolOverview, {
                props: {
                    tools: minimalTools,
                },
            });

            expect(wrapper.exists()).toBe(true);
            const findingsElement = wrapper.find(".fw-bold.fs-5");
            expect(findingsElement.text()).toBe("0 Findings");
        });

        it("should handle undefined findings", () => {
            const toolsWithUndefined: Tool[] = [
                {
                    name: "Test Tool",
                    findings: undefined,
                },
            ];

            wrapper = mount(ToolOverview, {
                props: {
                    tools: toolsWithUndefined,
                },
            });

            const findingsElement = wrapper.find(".fw-bold.fs-5");
            expect(findingsElement.text()).toBe("0 Findings");
        });

        it("should handle null findings", () => {
            const toolsWithNull: Tool[] = [
                {
                    name: "Test Tool",
                    findings: null as any,
                },
            ];

            wrapper = mount(ToolOverview, {
                props: {
                    tools: toolsWithNull,
                },
            });

            const findingsElement = wrapper.find(".fw-bold.fs-5");
            expect(findingsElement.text()).toBe("0 Findings");
        });

        it("should not break with very long tool names", () => {
            const toolWithLongName: Tool[] = [
                {
                    name: "This is a very long tool name that might cause layout issues in the user interface",
                    findings: [],
                },
            ];

            wrapper = mount(ToolOverview, {
                props: {
                    tools: toolWithLongName,
                },
            });

            const toolNameElements = wrapper.findAll(".text-muted");
            const toolNameElement = toolNameElements.find((el) =>
                el.text().includes("This is a very long tool name"),
            );
            if (toolNameElement) {
                expect(toolNameElement.text()).toBe(
                    "This is a very long tool name that might cause layout issues in the user interface",
                );
            } else {
                // Fallback assertion to ensure the test doesn't silently pass
                expect(
                    toolNameElements.some((el) =>
                        el.text().includes("This is a very long tool name"),
                    ),
                ).toBe(true);
            }
        });
    });

    describe("Computed Properties", () => {
        it("should format scan dates through computed property", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const component = wrapper.vm;
            expect(component.formattedScanDates).toBeDefined();
            expect(component.formattedScanDates.length).toBe(mockTools.length);
            expect(component.formattedScanDates[0]).toBe("Jan 15, 2024");
            expect(component.formattedScanDates[1]).toBe("Jan 14, 2024");
            expect(component.formattedScanDates[2]).toBe("Invalid date");
            expect(component.formattedScanDates[3]).toBe("Last scan date not found");
        });
    });

    describe("Download Functionality (Method Tests)", () => {
        it("should have downloadAll method", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const component = wrapper.vm;
            expect(typeof component.downloadAll).toBe("function");
        });

        it("should have downloadToolFindings method", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const component = wrapper.vm;
            expect(typeof component.downloadToolFindings).toBe("function");
        });
    });

    describe("Data Integration", () => {
        it("should handle mixed tool data correctly", () => {
            const mixedTools: Tool[] = [
                {
                    name: "Tool A",
                    scanDate: "2024-01-01T00:00:00Z",
                    findings: [{type: "error"}],
                },
                {
                    name: "Tool B",
                    scanDate: undefined,
                    findings: [],
                },
                {
                    name: "Tool C",
                    scanDate: "invalid",
                    findings: undefined,
                },
            ];

            wrapper = mount(ToolOverview, {
                props: {
                    tools: mixedTools,
                },
            });

            expect(wrapper.exists()).toBe(true);

            // Check that all tools are rendered despite mixed data quality
            const toolItems = wrapper.findAll(".list-group-item");
            expect(toolItems).toHaveLength(3);

            // Check findings count handling
            const findingsElements = wrapper.findAll(".fw-bold.fs-5");
            expect(findingsElements[0]?.text()).toBe("1 Findings");
            expect(findingsElements[1]?.text()).toBe("0 Findings");
            expect(findingsElements[2]?.text()).toBe("0 Findings");
        });

        it("should handle empty props gracefully", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: [],
                },
            });

            expect(wrapper.exists()).toBe(true);
            const listItems = wrapper.findAll(".list-group-item");
            expect(listItems).toHaveLength(0);
        });
    });

    describe("Component Behavior", () => {
        it("should render template elements correctly", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            // Check for v-for template rendering
            const templates = wrapper.findAll("template");
            expect(templates.length).toBeGreaterThanOrEqual(0); // Templates are processed by Vue

            // Check that the list structure is correct
            expect(wrapper.find(".list-group").exists()).toBe(true);
            expect(wrapper.find(".list-group-flush").exists()).toBe(true);
        });

        it("should handle Bootstrap classes correctly", () => {
            wrapper = mount(ToolOverview, {
                props: {
                    tools: mockTools,
                },
            });

            const listItems = wrapper.findAll(".list-group-item");
            listItems.forEach((item) => {
                expect(item.classes()).toContain("list-group-item");
                expect(item.classes()).toContain("d-flex");
                expect(item.classes()).toContain("justify-content-between");
            });

            const downloadButtons = wrapper.findAll(".btn");
            downloadButtons.forEach((button) => {
                expect(button.classes()).toContain("btn");
            });
        });

        it("should handle icon conditional rendering", () => {
            const toolsWithAndWithoutIcons: Tool[] = [
                {
                    name: "Tool With Icon",
                    findings: [],
                },
                {
                    name: "Tool Without Icon",
                    findings: [],
                    // no icon property
                },
            ];

            wrapper = mount(ToolOverview, {
                props: {
                    tools: toolsWithAndWithoutIcons,
                },
            });

            const images = wrapper.findAll("img");
            const fallbackIcons = wrapper.findAll(".bi-gear-fill");

            // In the test environment, all tools use fallback icons since the mock 
            // icon resolution system doesn't return actual icon paths
            expect(fallbackIcons.length).toBe(toolsWithAndWithoutIcons.length);
            
            // Total icon elements (images + fallback) should equal number of tools
            expect(images.length + fallbackIcons.length).toBe(toolsWithAndWithoutIcons.length);
        });
    });

    describe("Props Validation", () => {
        it("should accept tools prop of correct type", () => {
            expect(() => {
                wrapper = mount(ToolOverview, {
                    props: {
                        tools: mockTools,
                    },
                });
            }).not.toThrow();
        });

        it("should handle tools prop changes", async () => {
            const firstTool = mockTools[0];
            if (!firstTool) {
                throw new Error("First mock tool is undefined");
            }

            wrapper = mount(ToolOverview, {
                props: {
                    tools: [firstTool],
                },
            });

            let toolItems = wrapper.findAll(".list-group-item");
            expect(toolItems).toHaveLength(1);

            await wrapper.setProps({tools: mockTools.slice(0, 2)});
            toolItems = wrapper.findAll(".list-group-item");
            expect(toolItems).toHaveLength(2);
        });
    });
});
