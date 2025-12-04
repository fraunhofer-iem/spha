import {beforeEach, describe, expect, it} from "vitest";
import {parse} from "./../Parser";
import type {Kpi, RepoInfo, Result} from "../../model/Result";
import kpiResultsSmall from "../../../example/kpi-results-small.json";
import kpiNew from "../../../example/kpi-new.json";

describe("Parser", () => {
    describe("parse function", () => {
        it("should successfully parse valid kpi-results-small.json", () => {
            const result = parse(kpiResultsSmall);

            expect(result).toBeDefined();
            expect(result).not.toBeNull();
        });

        it("should successfully parse valid kpi-new.json", () => {
            const result = parse(kpiNew);

            expect(result).toBeDefined();
            expect(result).not.toBeNull();
        });

        it("should return undefined for invalid data", () => {
            const invalidData = {invalid: "data"};
            const result = parse(invalidData);

            expect(result).toBeUndefined();
        });

        it("should return undefined when missing resultHierarchy", () => {
            const invalidData = {
                origins: [],
                projectInfo: {},
            };
            const result = parse(invalidData);

            expect(result).toBeUndefined();
        });

        it("should return undefined when missing origins", () => {
            const invalidData = {
                resultHierarchy: {root: {}},
                projectInfo: {},
            };
            const result = parse(invalidData);

            expect(result).toBeUndefined();
        });

        it("should return undefined when missing projectInfo", () => {
            const invalidData = {
                resultHierarchy: {root: {}},
                origins: [],
            };
            const result = parse(invalidData);

            expect(result).toBeUndefined();
        });
    });

    describe("Result structure", () => {
        let result: Result;

        beforeEach(() => {
            result = parse(kpiResultsSmall)!;
        });

        it("should have correct healthScore", () => {
            expect(result.healthScore).toBe(73);
        });

        it("should have correct repoInfo structure", () => {
            const expectedRepoInfo: RepoInfo = {
                stars: 127,
                lastCommitDate: "2024-01-15T10:30:00Z",
                contributors: 8,
                projectUrl: "https://github.com/example/awesome-project",
                projectName: "Awesome Project",
                version: "1.5.2",
                repoLanguages: [
                    {name: "TypeScript", size: 65.2},
                    {name: "JavaScript", size: 20.8},
                    {name: "CSS", size: 10.5},
                    {name: "HTML", size: 3.5},
                ],
            };

            expect(result.repoInfo).toEqual(expectedRepoInfo);
        });

        it("should parse tools from origins correctly", () => {
            expect(result.tools).toHaveLength(3);

            const eslintTool = result.tools.find((tool) => tool.name === "ESLint");
            expect(eslintTool).toBeDefined();
            expect(eslintTool?.findings).toHaveLength(2);

            const sonarTool = result.tools.find((tool) => tool.name === "SonarQube");
            expect(sonarTool).toBeDefined();
            expect(sonarTool?.findings).toHaveLength(1);

            const jestTool = result.tools.find((tool) => tool.name === "Jest");
            expect(jestTool).toBeDefined();
            expect(jestTool?.findings).toHaveLength(1);
        });

        it("should have correct root KPI structure", () => {
            expect(result.root).toBeDefined();
            expect(result.root.displayName).toBe("Software Quality Assessment");
            expect(result.root.score).toBe(73);
            expect(result.root.id).toBe("software-quality-root");
            expect(result.root.children).toHaveLength(5);
        });
    });

    describe("KPI hierarchy parsing", () => {
        let result: Result;

        beforeEach(() => {
            result = parse(kpiResultsSmall)!;
        });

        it("should parse main KPI categories correctly", () => {
            const kpiNames = result.root.children.map((kpi) => kpi.displayName);

            expect(kpiNames).toContain("Code Quality");
            expect(kpiNames).toContain("Test Coverage");
            expect(kpiNames).toContain("Security");
            expect(kpiNames).toContain("Maintainability");
            expect(kpiNames).toContain("Performance");
        });

        it("should parse Code Quality KPI correctly", () => {
            const codeQualityKpi = result.root.children.find(
                (kpi) => kpi.displayName === "Code Quality",
            );

            expect(codeQualityKpi).toBeDefined();
            expect(codeQualityKpi!.score).toBe(78);
            expect(codeQualityKpi!.id).toBe("code-quality");
            expect(codeQualityKpi!.children).toHaveLength(2);

            const complexityKpi = codeQualityKpi!.children.find(
                (kpi) => kpi.displayName === "Cyclomatic Complexity",
            );
            expect(complexityKpi).toBeDefined();
            expect(complexityKpi!.score).toBe(82);

            const duplicationKpi = codeQualityKpi!.children.find(
                (kpi) => kpi.displayName === "Code Duplication",
            );
            expect(duplicationKpi).toBeDefined();
            expect(duplicationKpi!.score).toBe(74);
        });

        it("should parse Test Coverage KPI correctly", () => {
            const testCoverageKpi = result.root.children.find(
                (kpi) => kpi.displayName === "Test Coverage",
            );

            expect(testCoverageKpi).toBeDefined();
            expect(testCoverageKpi!.score).toBe(86);
            expect(testCoverageKpi!.children).toHaveLength(2);

            const unitCoverageKpi = testCoverageKpi!.children.find(
                (kpi) => kpi.displayName === "Unit Test Coverage",
            );
            expect(unitCoverageKpi!.score).toBe(89);

            const integrationCoverageKpi = testCoverageKpi!.children.find(
                (kpi) => kpi.displayName === "Integration Test Coverage",
            );
            expect(integrationCoverageKpi!.score).toBe(67);
        });

        it("should parse Security KPI correctly", () => {
            const securityKpi = result.root.children.find(
                (kpi) => kpi.displayName === "Security",
            );

            expect(securityKpi).toBeDefined();
            expect(securityKpi!.score).toBe(91);
            expect(securityKpi!.children).toHaveLength(1);

            const vulnerabilitiesKpi = securityKpi!.children.find(
                (kpi) => kpi.displayName === "Known Vulnerabilities",
            );
            expect(vulnerabilitiesKpi!.score).toBe(96);
        });

        it("should parse Maintainability KPI correctly", () => {
            const maintainabilityKpi = result.root.children.find(
                (kpi) => kpi.displayName === "Maintainability",
            );

            expect(maintainabilityKpi).toBeDefined();
            expect(maintainabilityKpi!.score).toBe(55);
            expect(maintainabilityKpi!.children).toHaveLength(2);

            const techDebtKpi = maintainabilityKpi!.children.find(
                (kpi) => kpi.displayName === "Technical Debt",
            );
            expect(techDebtKpi!.score).toBe(48);

            const documentationKpi = maintainabilityKpi!.children.find(
                (kpi) => kpi.displayName === "Documentation Coverage",
            );
            expect(documentationKpi!.score).toBe(61);
        });

        it("should parse Performance KPI correctly", () => {
            const performanceKpi = result.root.children.find(
                (kpi) => kpi.displayName === "Performance",
            );

            expect(performanceKpi).toBeDefined();
            expect(performanceKpi!.score).toBe(77);
            expect(performanceKpi!.children).toHaveLength(2);

            const loadTimeKpi = performanceKpi!.children.find(
                (kpi) => kpi.displayName === "Page Load Time",
            );
            expect(loadTimeKpi!.score).toBe(79);

            const bundleSizeKpi = performanceKpi!.children.find(
                (kpi) => kpi.displayName === "Bundle Size Optimization",
            );
            expect(bundleSizeKpi!.score).toBe(74);
        });
    });

    describe("Thresholds parsing", () => {
        let result: Result;

        beforeEach(() => {
            result = parse(kpiResultsSmall)!;
        });

        it("should parse root KPI thresholds correctly", () => {
            expect(result.root.thresholds).toBeDefined();
            expect(result.root.thresholds).toHaveLength(2);

            const goodThreshold = result.root.thresholds!.find(
                (t) => t.name === "good",
            );
            expect(goodThreshold).toBeDefined();
            expect(goodThreshold!.value).toBe(70);

            const excellentThreshold = result.root.thresholds!.find(
                (t) => t.name === "excellent",
            );
            expect(excellentThreshold).toBeDefined();
            expect(excellentThreshold!.value).toBe(85);
        });

        it("should parse KPI thresholds for child nodes", () => {
            const codeQualityKpi = result.root.children.find(
                (kpi) => kpi.displayName === "Code Quality",
            );

            expect(codeQualityKpi!.thresholds).toBeDefined();
            expect(codeQualityKpi!.thresholds).toHaveLength(2);

            const acceptableThreshold = codeQualityKpi!.thresholds!.find(
                (t) => t.name === "acceptable",
            );
            expect(acceptableThreshold!.value).toBe(60);

            const goodThreshold = codeQualityKpi!.thresholds!.find(
                (t) => t.name === "good",
            );
            expect(goodThreshold!.value).toBe(75);
        });

        it("should handle missing thresholds gracefully", () => {
            // Find a KPI that might not have thresholds or verify empty thresholds are handled
            const allKpis = getAllKpis(result.root);
            const kpisWithoutThresholds = allKpis.filter(
                (kpi) => !kpi.thresholds || kpi.thresholds.length === 0,
            );

            // If there are KPIs without thresholds, they should have undefined thresholds
            kpisWithoutThresholds.forEach((kpi) => {
                expect(kpi.thresholds).toBeUndefined();
            });
        });
    });

    describe("Edge cases and data validation", () => {
        it("should handle missing optional fields in projectInfo", () => {
            const dataWithMissingFields = {
                ...kpiResultsSmall,
                projectInfo: {
                    name: "Test Project",
                    usedLanguages: [],
                    url: undefined,
                    stars: undefined,
                    numberOfContributors: undefined,
                    lastCommitDate: undefined,
                },
            };

            const result = parse(dataWithMissingFields);
            expect(result).toBeDefined();
            expect(result!.repoInfo.stars).toBe(-1);
            expect(result!.repoInfo.lastCommitDate).toBe("N/A");
            expect(result!.repoInfo.contributors).toBe(-1);
            expect(result!.repoInfo.projectUrl).toBe("N/A");
            expect(result!.repoInfo.projectName).toBe("Test Project");
        });

        it("should handle missing score in KPI node", () => {
            const dataWithMissingScore = {
                ...kpiResultsSmall,
                resultHierarchy: {
                    root: {
                        ...kpiResultsSmall.resultHierarchy.root,
                        result: {}, // Missing score
                    },
                },
            };

            const result = parse(dataWithMissingScore);
            expect(result).toBeDefined();
            expect(result!.healthScore).toBe(-1);
        });

        it("should handle empty origins array", () => {
            const dataWithEmptyOrigins = {
                ...kpiResultsSmall,
                origins: [],
            };

            const result = parse(dataWithEmptyOrigins);
            expect(result).toBeDefined();
            expect(result!.tools).toHaveLength(0);
        });

        it("should handle origins with missing fields", () => {
            const dataWithIncompleteOrigins = {
                ...kpiResultsSmall,
                origins: [
                    {toolInfo: {name: undefined, description: undefined}, origins: []},
                    {toolInfo: {name: "TestTool", description: "Test description"}, origins: undefined},
                ],
            };

            const result = parse(dataWithIncompleteOrigins);
            expect(result).toBeDefined();
            expect(result!.tools).toHaveLength(2);

            const firstTool = result!.tools[0];
            expect(firstTool?.name).toBe("N/A");
            expect(firstTool?.description).toBe("");
            expect(firstTool?.findings).toHaveLength(0);

            const secondTool = result!.tools[1];
            expect(secondTool?.name).toBe("TestTool");
            expect(secondTool?.description).toBe("Test description");
            expect(secondTool?.findings).toHaveLength(0);
        });
    });
});

// Helper function to get all KPIs recursively
function getAllKpis(root: Kpi): Kpi[] {
    const kpis: Kpi[] = [root];

    for (const child of root.children) {
        kpis.push(...getAllKpis(child));
    }

    return kpis;
}
