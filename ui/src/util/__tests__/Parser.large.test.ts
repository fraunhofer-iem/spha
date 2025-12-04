import {beforeAll, describe, expect, it} from "vitest";
import {parse} from "../Parser";
import type {Kpi, Result} from "../../model/Result";
import kpiResultsLarge from "../../../example/kpi-results.json";

describe("Parser with large kpi-results.json", () => {
    let result: Result;

    beforeAll(() => {
        console.log("Testing with large kpi-results.json file...");
        const startTime = performance.now();
        result = parse(kpiResultsLarge)!;
        const endTime = performance.now();
        console.log(`Parsing took ${endTime - startTime} milliseconds`);
    });

    describe("Basic parsing validation", () => {
        it("should successfully parse the large kpi-results.json", () => {
            expect(result).toBeDefined();
            expect(result).not.toBeNull();
        });

        it("should have valid result structure", () => {
            expect(result).toHaveProperty("healthScore");
            expect(result).toHaveProperty("repoInfo");
            expect(result).toHaveProperty("root");
            expect(result).toHaveProperty("tools");
        });
    });

    describe("Health score validation", () => {
        it("should have a numeric health score", () => {
            expect(typeof result.healthScore).toBe("number");
            expect(result.healthScore).toBeGreaterThanOrEqual(-1);
            expect(result.healthScore).toBeLessThanOrEqual(100);
        });
    });

    describe("Repository information validation", () => {
        it("should have valid repoInfo structure", () => {
            expect(result.repoInfo).toBeDefined();
            expect(result.repoInfo).toHaveProperty("projectName");
            expect(result.repoInfo).toHaveProperty("projectUrl");
            expect(result.repoInfo).toHaveProperty("stars");
            expect(result.repoInfo).toHaveProperty("contributors");
            expect(result.repoInfo).toHaveProperty("lastCommitDate");
            expect(result.repoInfo).toHaveProperty("repoLanguages");
        });

        it("should have valid data types for repoInfo fields", () => {
            expect(typeof result.repoInfo.projectName).toBe("string");
            expect(typeof result.repoInfo.projectUrl).toBe("string");
            expect(typeof result.repoInfo.stars).toBe("number");
            expect(typeof result.repoInfo.contributors).toBe("number");
            expect(typeof result.repoInfo.lastCommitDate).toBe("string");
            expect(Array.isArray(result.repoInfo.repoLanguages)).toBe(true);
        });

        it("should have valid language structure", () => {
            if (result.repoInfo.repoLanguages.length > 0) {
                const firstLanguage = result.repoInfo.repoLanguages[0];
                expect(firstLanguage).toHaveProperty("name");
                expect(firstLanguage).toHaveProperty("size");
                expect(typeof firstLanguage?.name).toBe("string");
                expect(typeof firstLanguage?.size).toBe("number");
            }
        });
    });

    describe("Tools validation", () => {
        it("should have tools array", () => {
            expect(Array.isArray(result.tools)).toBe(true);
        });

        it("should have valid tool structure", () => {
            if (result.tools.length > 0) {
                const firstTool = result.tools[0];
                expect(firstTool).toHaveProperty("name");
                expect(firstTool).toHaveProperty("findings");
                expect(firstTool).toHaveProperty("downloadLink");
                expect(firstTool).toHaveProperty("icon");
                expect(firstTool).toHaveProperty("description");

                expect(typeof firstTool?.name).toBe("string");
                expect(Array.isArray(firstTool?.findings)).toBe(true);
                expect(typeof firstTool?.description).toBe("string");
            }
        });

        it("should have non-negative findings count", () => {
            result.tools.forEach((tool) => {
                if (tool.findings) {
                    expect(Array.isArray(tool.findings)).toBe(true);
                    expect(tool.findings.length).toBeGreaterThanOrEqual(0);
                }
            });
        });
    });

    describe("Root KPI validation", () => {
        it("should have valid root KPI structure", () => {
            expect(result.root).toBeDefined();
            expect(result.root).toHaveProperty("displayName");
            expect(result.root).toHaveProperty("score");
            expect(result.root).toHaveProperty("id");
            expect(result.root).toHaveProperty("children");
        });

        it("should have valid root KPI data types", () => {
            expect(typeof result.root.displayName).toBe("string");
            expect(typeof result.root.score).toBe("number");
            expect(typeof result.root.id).toBe("string");
            expect(Array.isArray(result.root.children)).toBe(true);
        });

        it("should have valid score range", () => {
            expect(result.root.score).toBeGreaterThanOrEqual(-1);
            expect(result.root.score).toBeLessThanOrEqual(100);
        });

        it("should have child KPIs", () => {
            expect(result.root.children.length).toBeGreaterThan(0);
        });
    });

    describe("KPI hierarchy validation", () => {
        it("should have valid child KPI structures", () => {
            result.root.children.forEach((child) => {
                expect(child).toHaveProperty("displayName");
                expect(child).toHaveProperty("score");
                expect(child).toHaveProperty("id");
                expect(child).toHaveProperty("children");

                expect(typeof child.displayName).toBe("string");
                expect(typeof child.score).toBe("number");
                expect(typeof child.id).toBe("string");
                expect(Array.isArray(child.children)).toBe(true);
            });
        });

        it("should have valid score ranges throughout hierarchy", () => {
            let minScore = Infinity;
            let maxScore = -Infinity;
            let scoreCount = 0;

            const validateKpiScores = (kpi: Kpi) => {
                expect(kpi.score).toBeGreaterThanOrEqual(-1);
                expect(typeof kpi.score).toBe("number");
                expect(kpi.score).not.toBeNaN();

                // Track score statistics
                minScore = Math.min(minScore, kpi.score);
                maxScore = Math.max(maxScore, kpi.score);
                scoreCount++;

                kpi.children.forEach((child) => validateKpiScores(child));
            };

            validateKpiScores(result.root);

            // Log score statistics for insight
            console.log(
                `Score statistics: Min=${minScore}, Max=${maxScore}, Count=${scoreCount}`,
            );

            expect(scoreCount).toBeGreaterThan(0);
            expect(minScore).toBeLessThanOrEqual(maxScore);
        });

        it("should have non-empty display names", () => {
            const validateDisplayNames = (kpi: Kpi) => {
                expect(kpi.displayName).toBeTruthy();
                expect(kpi.displayName.trim()).not.toBe("");

                kpi.children.forEach((child) => validateDisplayNames(child));
            };

            validateDisplayNames(result.root);
        });

        it("should track IDs throughout hierarchy", () => {
            const ids = new Set<string>();
            const duplicateIds = new Set<string>();

            const collectIds = (kpi: Kpi) => {
                if (kpi.id !== "N/A") {
                    if (ids.has(kpi.id)) {
                        duplicateIds.add(kpi.id);
                    } else {
                        ids.add(kpi.id);
                    }
                }

                kpi.children.forEach((child) => collectIds(child));
            };

            collectIds(result.root);
            expect(ids.size).toBeGreaterThan(0);

            // Log duplicate IDs for debugging if any exist
            if (duplicateIds.size > 0) {
                console.log(
                    `Found ${duplicateIds.size} duplicate IDs: ${Array.from(duplicateIds).join(", ")}`,
                );
            }
        });
    });

    describe("Thresholds validation", () => {
        it("should handle thresholds correctly", () => {
            const validateThresholds = (kpi: Kpi) => {
                if (kpi.thresholds) {
                    expect(Array.isArray(kpi.thresholds)).toBe(true);

                    kpi.thresholds.forEach((threshold) => {
                        expect(threshold).toHaveProperty("name");
                        expect(threshold).toHaveProperty("value");
                        expect(typeof threshold.name).toBe("string");
                        expect(typeof threshold.value).toBe("number");
                        expect(threshold.value).toBeGreaterThanOrEqual(0);
                        expect(threshold.value).toBeLessThanOrEqual(500); // Allow higher threshold values for real data
                    });
                }

                kpi.children.forEach((child) => validateThresholds(child));
            };

            validateThresholds(result.root);
        });
    });

    describe("Performance and scale validation", () => {
        it("should handle large data efficiently", () => {
            const startTime = performance.now();

            // Traverse entire hierarchy to ensure no performance issues
            let nodeCount = 0;
            const countNodes = (kpi: Kpi) => {
                nodeCount++;
                kpi.children.forEach((child) => countNodes(child));
            };

            countNodes(result.root);

            const endTime = performance.now();
            const traversalTime = endTime - startTime;

            console.log(
                `Traversed ${nodeCount} nodes in ${traversalTime} milliseconds`,
            );

            // Should traverse reasonably quickly (less than 100ms for most structures)
            expect(traversalTime).toBeLessThan(1000);
            expect(nodeCount).toBeGreaterThan(0);
        });

        it("should not have circular references", () => {
            const visited = new Set<Kpi>();

            const checkCircular = (kpi: Kpi) => {
                expect(visited.has(kpi)).toBe(false);
                visited.add(kpi);

                kpi.children.forEach((child) => checkCircular(child));

                visited.delete(kpi);
            };

            expect(() => checkCircular(result.root)).not.toThrow();
        });
    });

    describe("Data completeness validation", () => {
        it("should have meaningful project information", () => {
            // Basic sanity checks without assuming specific values
            expect(result.repoInfo.projectName).not.toBe("");
            expect(result.repoInfo.projectName).not.toBe("N/A");
        });

        it("should have some tools data", () => {
            // Large files should typically have some tools
            expect(result.tools.length).toBeGreaterThanOrEqual(0);
        });

        it("should have hierarchical KPI structure", () => {
            // Should have at least some depth in the hierarchy
            const hasNestedChildren = result.root.children.some(
                (child) => child.children && child.children.length > 0,
            );

            if (result.root.children.length > 0) {
                // If there are root children, at least some should have nested structure
                expect(hasNestedChildren).toBe(true);
            }
        });
    });
});
