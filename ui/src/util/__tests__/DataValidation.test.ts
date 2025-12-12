import {describe, expect, it} from "vitest";
import type {Kpi, Language, RepoInfo, Result, Threshold, Tool,} from "../../model/Result";

// Data validation utilities for testing edge cases and data integrity
describe("Data Validation Utilities", () => {
    describe("Result Structure Validation", () => {
        const createValidResult = (): Result => ({
            createdAt: "2024-01-15",
            healthScore: 75,
            repoInfo: {
                projectName: "Test Project",
                stars: 100,
                contributors: 5,
                lastCommitDate: "2024-01-15",
                projectUrl: "https://github.com/test/repo",
                repoLanguages: [
                    {name: "TypeScript", size: 70},
                    {name: "JavaScript", size: 30},
                ],
            },
            root: {
                displayName: "Root KPI",
                score: 75,
                id: "root",
                children: [],
            },
            tools: [],
        });

        it("should validate complete valid result structure", () => {
            const result = createValidResult();
            expect(result).toBeDefined();
            expect(result.healthScore).toBeTypeOf("number");
            expect(result.repoInfo).toBeTypeOf("object");
            expect(result.root).toBeTypeOf("object");
            expect(Array.isArray(result.tools)).toBe(true);
        });

        it("should detect invalid health scores", () => {
            const testCases = [
                {healthScore: NaN, description: "NaN health score"},
                {healthScore: Infinity, description: "Infinite health score"},
                {
                    healthScore: -Infinity,
                    description: "Negative infinite health score",
                },
                {healthScore: "75" as any, description: "String health score"},
                {healthScore: null as any, description: "Null health score"},
                {
                    healthScore: undefined as any,
                    description: "Undefined health score",
                },
            ];

            testCases.forEach(({healthScore, description}) => {
                expect(isValidHealthScore(healthScore), description).toBe(false);
            });
        });

        it("should validate health score ranges", () => {
            const validScores = [0, 50, 100, 75.5];
            const invalidScores = [-1, 101, -50, 150];

            validScores.forEach((score) => {
                expect(
                    isValidHealthScore(score),
                    `Score ${score} should be valid`,
                ).toBe(true);
            });

            invalidScores.forEach((score) => {
                expect(
                    isValidHealthScore(score),
                    `Score ${score} should be invalid`,
                ).toBe(false);
            });
        });
    });

    describe("KPI Structure Validation", () => {
        const createValidKpi = (): Kpi => ({
            displayName: "Test KPI",
            score: 80,
            id: "test-kpi",
            children: [],
        });

        it("should validate complete KPI structure", () => {
            const kpi = createValidKpi();
            expect(isValidKpi(kpi)).toBe(true);
        });

        it("should detect missing required fields", () => {
            const testCases = [
                {...createValidKpi(), displayName: undefined as any},
                {...createValidKpi(), score: undefined as any},
                {...createValidKpi(), id: undefined as any},
                {...createValidKpi(), children: undefined as any},
            ];

            testCases.forEach((kpi) => {
                expect(isValidKpi(kpi)).toBe(false);
            });
        });

        it("should validate KPI score ranges", () => {
            const validKpis = [
                {...createValidKpi(), score: 0},
                {...createValidKpi(), score: 50},
                {...createValidKpi(), score: 100},
                {...createValidKpi(), score: 75.5},
            ];

            const invalidKpis = [
                {...createValidKpi(), score: -1},
                {...createValidKpi(), score: 101},
                {...createValidKpi(), score: NaN},
                {...createValidKpi(), score: "80" as any},
            ];

            validKpis.forEach((kpi) => {
                expect(isValidKpi(kpi)).toBe(true);
            });

            invalidKpis.forEach((kpi) => {
                expect(isValidKpi(kpi)).toBe(false);
            });
        });

        it("should validate nested KPI structures", () => {
            const nestedKpi: Kpi = {
                ...createValidKpi(),
                children: [
                    createValidKpi(),
                    {
                        ...createValidKpi(),
                        children: [createValidKpi()],
                    },
                ],
            };

            expect(isValidKpiHierarchy(nestedKpi)).toBe(true);
        });

        it("should detect invalid nested KPI structures", () => {
            const invalidNestedKpi: Kpi = {
                ...createValidKpi(),
                children: [
                    {...createValidKpi(), score: -5}, // Invalid score
                    createValidKpi(),
                ],
            };

            expect(isValidKpiHierarchy(invalidNestedKpi)).toBe(false);
        });

        it("should validate KPI threshold structures", () => {
            const kpiWithThresholds: Kpi = {
                ...createValidKpi(),
                thresholds: [
                    {name: "acceptable", value: 60},
                    {name: "good", value: 80},
                ],
            };

            expect(isValidKpi(kpiWithThresholds)).toBe(true);
        });

        it("should detect invalid threshold structures", () => {
            const invalidThresholds = [
                [{name: "", value: 60}], // Empty name
                [{name: "test", value: -1}], // Negative value
                [{name: "test", value: NaN}], // NaN value
                [{name: null as any, value: 60}], // Null name
                [{name: "test", value: "60" as any}], // String value
            ];

            invalidThresholds.forEach((thresholds) => {
                const kpi: Kpi = {
                    ...createValidKpi(),
                    thresholds: thresholds as Threshold[],
                };
                expect(isValidKpi(kpi)).toBe(false);
            });
        });
    });

    describe("Repository Information Validation", () => {
        const createValidRepoInfo = (): RepoInfo => ({
            projectName: "Test Project",
            stars: 100,
            contributors: 5,
            lastCommitDate: "2024-01-15",
            projectUrl: "https://github.com/test/repo",
            repoLanguages: [
                {name: "TypeScript", size: 70},
                {name: "JavaScript", size: 30},
            ],
        });

        it("should validate complete repository information", () => {
            const repoInfo = createValidRepoInfo();
            expect(isValidRepoInfo(repoInfo)).toBe(true);
        });

        it("should handle optional fields correctly", () => {
            const repoInfoWithOptionalUndefined: RepoInfo = {
                repoLanguages: [],
                // All other fields are optional and undefined
            };

            expect(isValidRepoInfo(repoInfoWithOptionalUndefined)).toBe(true);
        });

        it("should validate star counts", () => {
            const validStarCounts = [0, 100, 1000000];
            const invalidStarCounts = [-1, NaN, "100" as any, null as any];

            validStarCounts.forEach((stars) => {
                const repoInfo = {...createValidRepoInfo(), stars};
                expect(isValidRepoInfo(repoInfo)).toBe(true);
            });

            invalidStarCounts.forEach((stars) => {
                const repoInfo = {...createValidRepoInfo(), stars};
                expect(isValidRepoInfo(repoInfo)).toBe(false);
            });
        });

        it("should validate contributor counts", () => {
            const validContributorCounts = [0, 1, 50, 1000];
            const invalidContributorCounts = [-1, NaN, "5" as any];

            validContributorCounts.forEach((contributors) => {
                const repoInfo = {...createValidRepoInfo(), contributors};
                expect(isValidRepoInfo(repoInfo)).toBe(true);
            });

            invalidContributorCounts.forEach((contributors) => {
                const repoInfo = {...createValidRepoInfo(), contributors};
                expect(isValidRepoInfo(repoInfo)).toBe(false);
            });
        });

        it("should validate project URLs", () => {
            const validUrls = [
                "https://github.com/user/repo",
                "https://gitlab.com/user/repo",
                "https://bitbucket.org/user/repo",
                "N/A", // Special case
                undefined, // Optional field
            ];

            const invalidUrls = [
                "not-a-url",
                "ftp://invalid-protocol.com",
                "", // Empty string might be invalid
                123 as any, // Wrong type
            ];

            validUrls.forEach((projectUrl) => {
                const repoInfo = {...createValidRepoInfo(), projectUrl};
                expect(isValidRepoInfo(repoInfo), `URL: ${projectUrl}`).toBe(true);
            });

            invalidUrls.forEach((projectUrl) => {
                const repoInfo = {...createValidRepoInfo(), projectUrl};
                expect(isValidRepoInfo(repoInfo), `URL: ${projectUrl}`).toBe(false);
            });
        });

        it("should validate date formats", () => {
            const validDates = [
                "2024-01-15",
                "2024-01-15T10:30:00Z",
                "2024-01-15T10:30:00",
                "N/A",
                undefined,
            ];

            const invalidDates = [
                "invalid-date",
                "32-01-2024", // Invalid day
                "2024-13-01", // Invalid month
                "", // Empty string
                123 as any, // Wrong type
            ];

            validDates.forEach((lastCommitDate) => {
                const repoInfo = {...createValidRepoInfo(), lastCommitDate};
                expect(isValidRepoInfo(repoInfo), `Date: ${lastCommitDate}`).toBe(true);
            });

            invalidDates.forEach((lastCommitDate) => {
                const repoInfo = {...createValidRepoInfo(), lastCommitDate};
                expect(isValidRepoInfo(repoInfo), `Date: ${lastCommitDate}`).toBe(
                    false,
                );
            });
        });

        it("should validate language arrays", () => {
            const validLanguageArrays = [
                [],
                [{name: "TypeScript", size: 100}],
                [
                    {name: "TypeScript", size: 60},
                    {name: "JavaScript", size: 40},
                ],
            ];

            const invalidLanguageArrays = [
                [{name: "", size: 50}], // Empty name
                [{name: "TypeScript", size: -10}], // Negative size
                [{name: "TypeScript", size: NaN}], // NaN size
                [{name: null as any, size: 50}], // Null name
                null as any, // Null array
                "not-an-array" as any, // Wrong type
            ];

            validLanguageArrays.forEach((repoLanguages) => {
                const repoInfo = {...createValidRepoInfo(), repoLanguages};
                expect(isValidRepoInfo(repoInfo)).toBe(true);
            });

            invalidLanguageArrays.forEach((repoLanguages) => {
                const repoInfo = {...createValidRepoInfo(), repoLanguages};
                expect(isValidRepoInfo(repoInfo)).toBe(false);
            });
        });
    });

    describe("Tool Validation", () => {
        const createValidTool = (): Tool => ({
            name: "ESLint",
            findings: [{}, {}, {}, {}, {}],
            scanDate: "2024-01-15",
            description: "JavaScript linting tool",
        });

        it("should validate complete tool structure", () => {
            const tool = createValidTool();
            expect(isValidTool(tool)).toBe(true);
        });

        it("should handle optional fields", () => {
            const minimalTool: Tool = {
                name: "TestTool",
                // Other fields are optional
            };

            expect(isValidTool(minimalTool)).toBe(true);
        });

        it("should validate findings array", () => {
            const validFindings = [[], [{}], Array(100).fill({}), undefined];
            const invalidFindings = [-1, NaN, "5" as any, {}];

            validFindings.forEach((findings) => {
                const tool = {...createValidTool(), findings};
                expect(isValidTool(tool), `Findings: ${JSON.stringify(findings)}`).toBe(
                    true,
                );
            });

            invalidFindings.forEach((findings) => {
                const tool = {...createValidTool(), findings};
                expect(isValidTool(tool), `Findings: ${JSON.stringify(findings)}`).toBe(
                    false,
                );
            });
        });

        it("should validate tool names", () => {
            const validNames = ["ESLint", "SonarQube", "Tool Name"];
            const invalidNames = ["", null as any, undefined as any, 123 as any];

            validNames.forEach((name) => {
                const tool = {...createValidTool(), name};
                expect(isValidTool(tool), `Name: ${name}`).toBe(true);
            });

            invalidNames.forEach((name) => {
                const tool = {...createValidTool(), name};
                expect(isValidTool(tool), `Name: ${name}`).toBe(false);
            });
        });
    });

    describe("Language Structure Validation", () => {
        it("should validate language objects", () => {
            const validLanguages = [
                {name: "TypeScript", size: 60.5},
                {name: "JavaScript", size: 39.5},
                {name: "Python", size: 0},
            ];

            validLanguages.forEach((language) => {
                expect(isValidLanguage(language)).toBe(true);
            });
        });

        it("should detect invalid language objects", () => {
            const invalidLanguages = [
                {name: "", size: 50}, // Empty name
                {name: "TypeScript", size: -10}, // Negative size
                {name: "TypeScript", size: NaN}, // NaN size
                {name: null as any, size: 50}, // Null name
                {name: "TypeScript"}, // Missing size
                {size: 50}, // Missing name
            ];

            invalidLanguages.forEach((language) => {
                expect(isValidLanguage(language as Language)).toBe(false);
            });
        });

        it("should validate language size totals", () => {
            const validLanguagesSets = [
                [{name: "TypeScript", size: 100}],
                [
                    {name: "TypeScript", size: 60},
                    {name: "JavaScript", size: 40},
                ],
                [], // Empty array is valid
            ];

            validLanguagesSets.forEach((languages) => {
                expect(isValidLanguageSet(languages)).toBe(true);
            });
        });
    });

    describe("Data Sanitization", () => {
        it("should sanitize string inputs", () => {
            const inputs = [
                {input: "  Normal String  ", expected: "Normal String"},
                {input: "", expected: ""},
                {input: null as any, expected: null},
                {input: undefined as any, expected: undefined},
            ];

            inputs.forEach(({input, expected}) => {
                expect(sanitizeString(input)).toBe(expected);
            });
        });

        it("should sanitize numeric inputs", () => {
            const inputs = [
                {input: 42, expected: 42},
                {input: 0, expected: 0},
                {input: -1, expected: -1},
                {input: NaN, expected: null},
                {input: Infinity, expected: null},
                {input: "42" as any, expected: 42},
                {input: "not-a-number" as any, expected: null},
            ];

            inputs.forEach(({input, expected}) => {
                expect(sanitizeNumber(input)).toBe(expected);
            });
        });

        it("should sanitize URL inputs", () => {
            const inputs = [
                {
                    input: "https://github.com/user/repo",
                    expected: "https://github.com/user/repo",
                },
                {input: "  https://example.com  ", expected: "https://example.com"},
                {input: "not-a-url", expected: "not-a-url"}, // Keep as-is for display
                {input: "", expected: ""},
                {input: null as any, expected: null},
            ];

            inputs.forEach(({input, expected}) => {
                expect(sanitizeUrl(input)).toBe(expected);
            });
        });
    });

    describe("Deep Validation", () => {
        it("should perform deep validation of nested structures", () => {
            const complexResult: Result = {
                healthScore: 75,
                createdAt: "2024-01-15",
                repoInfo: {
                    projectName: "Complex Project",
                    stars: 500,
                    contributors: 10,
                    lastCommitDate: "2024-01-15",
                    projectUrl: "https://github.com/user/complex",
                    repoLanguages: [
                        {name: "TypeScript", size: 60},
                        {name: "JavaScript", size: 40},
                    ],
                },
                root: {
                    displayName: "Root",
                    score: 75,
                    id: "root",
                    children: [
                        {
                            displayName: "Child 1",
                            score: 80,
                            id: "child1",
                            children: [],
                        },
                        {
                            displayName: "Child 2",
                            score: 70,
                            id: "child2",
                            children: [
                                {
                                    displayName: "Grandchild",
                                    score: 85,
                                    id: "grandchild",
                                    children: [],
                                },
                            ],
                        },
                    ],
                },
                tools: [
                    {
                        name: "ESLint",
                        findings: [{}, {}, {}],
                    },
                ],
            };

            expect(isValidResult(complexResult)).toBe(true);
        });

        it("should detect errors in nested structures", () => {
            const invalidResult: Result = {
                createdAt: "2024-01-15",
                healthScore: 75,
                repoInfo: {
                    projectName: "Invalid Project",
                    stars: -1, // Invalid
                    contributors: 10,
                    lastCommitDate: "invalid-date", // Invalid
                    projectUrl: "https://github.com/user/invalid",
                    repoLanguages: [
                        {name: "", size: 60}, // Invalid - empty name
                    ],
                },
                root: {
                    displayName: "Root",
                    score: 150, // Invalid - over 100
                    id: "root",
                    children: [],
                },
                tools: [],
            };

            expect(isValidResult(invalidResult)).toBe(false);
        });
    });
});

// Helper validation functions (these would typically be in a separate utility file)
function isValidHealthScore(score: any): boolean {
    return (
        typeof score === "number" &&
        !isNaN(score) &&
        isFinite(score) &&
        score >= 0 &&
        score <= 100
    );
}

function isValidKpi(kpi: any): boolean {
    if (!kpi || typeof kpi !== "object") return false;

    if (typeof kpi.displayName !== "string" || !kpi.displayName) return false;
    if (!isValidHealthScore(kpi.score)) return false;
    if (typeof kpi.id !== "string" || !kpi.id) return false;
    if (!Array.isArray(kpi.children)) return false;

    if (kpi.thresholds) {
        if (!Array.isArray(kpi.thresholds)) return false;
        for (const threshold of kpi.thresholds) {
            if (!isValidThreshold(threshold)) return false;
        }
    }

    return true;
}

function isValidKpiHierarchy(kpi: Kpi): boolean {
    if (!isValidKpi(kpi)) return false;

    for (const child of kpi.children) {
        if (!isValidKpiHierarchy(child)) return false;
    }

    return true;
}

function isValidThreshold(threshold: any): boolean {
    return (
        threshold &&
        typeof threshold.name === "string" &&
        threshold.name.length > 0 &&
        typeof threshold.value === "number" &&
        !isNaN(threshold.value) &&
        isFinite(threshold.value) &&
        threshold.value >= 0
    );
}

function isValidRepoInfo(repoInfo: any): boolean {
    if (!repoInfo || typeof repoInfo !== "object") return false;

    if (!Array.isArray(repoInfo.repoLanguages)) return false;

    // Optional fields validation
    if (
        repoInfo.projectName !== undefined &&
        typeof repoInfo.projectName !== "string"
    )
        return false;
    if (repoInfo.stars !== undefined && !isValidNumber(repoInfo.stars, 0))
        return false;
    if (
        repoInfo.contributors !== undefined &&
        !isValidNumber(repoInfo.contributors, 0)
    )
        return false;
    if (
        repoInfo.lastCommitDate !== undefined &&
        !isValidDate(repoInfo.lastCommitDate)
    )
        return false;
    if (
        repoInfo.projectUrl !== undefined &&
        !isValidUrlString(repoInfo.projectUrl)
    )
        return false;

    // Validate languages array
    for (const language of repoInfo.repoLanguages) {
        if (!isValidLanguage(language)) return false;
    }

    return true;
}

function isValidTool(tool: any): boolean {
    if (!tool || typeof tool !== "object") return false;

    if (typeof tool.name !== "string" || !tool.name) return false;

    if (tool.findings !== undefined && !Array.isArray(tool.findings))
        return false;
    if (tool.scanDate !== undefined && typeof tool.scanDate !== "string")
        return false;
    if (tool.icon !== undefined && typeof tool.icon !== "string") return false;
    if (tool.description !== undefined && typeof tool.description !== "string")
        return false;

    return true;
}

function isValidLanguage(language: any): boolean {
    return (
        language &&
        typeof language.name === "string" &&
        language.name.length > 0 &&
        typeof language.size === "number" &&
        !isNaN(language.size) &&
        isFinite(language.size) &&
        language.size >= 0
    );
}

function isValidLanguageSet(languages: Language[]): boolean {
    if (!Array.isArray(languages)) return false;

    for (const language of languages) {
        if (!isValidLanguage(language)) return false;
    }

    return true;
}

function isValidResult(result: any): boolean {
    if (!result || typeof result !== "object") return false;

    return (
        isValidHealthScore(result.healthScore) &&
        isValidRepoInfo(result.repoInfo) &&
        isValidKpiHierarchy(result.root) &&
        Array.isArray(result.tools) &&
        result.tools.every((tool: any) => isValidTool(tool))
    );
}

function isValidNumber(value: any, min?: number, max?: number): boolean {
    if (typeof value !== "number" || isNaN(value) || !isFinite(value))
        return false;
    if (min !== undefined && value < min) return false;
    if (max !== undefined && value > max) return false;
    return true;
}

function isValidDate(date: any): boolean {
    if (typeof date !== "string") return false;
    if (date === "N/A") return true; // Special cases
    if (date === "") return false; // Empty string is invalid

    // Basic date format validation
    const dateRegex =
        /^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}:\d{2}(Z|[+-]\d{2}:\d{2})?)?$/;
    if (!dateRegex.test(date)) return false;

    // Validate actual date values
    const dateOnly = date.split("T")[0]; // Get just the date part
    if (!dateOnly) return false;

    const testDate = new Date(dateOnly + "T00:00:00.000Z");
    const dateParts = dateOnly.split("-").map(Number);
    if (dateParts.length !== 3) return false;
    if (dateParts.some((part) => isNaN(part))) return false;

    const year = dateParts[0];
    const month = dateParts[1];
    const day = dateParts[2];

    // Ensure all parts are defined
    if (year === undefined || month === undefined || day === undefined) {
        return false;
    }

    // Check if the parsed date matches the input (catches invalid dates like 2024-13-01)
    return (
        testDate.getUTCFullYear() === year &&
        testDate.getUTCMonth() === month - 1 &&
        testDate.getUTCDate() === day
    );
}

function isValidUrlString(url: any): boolean {
    if (typeof url !== "string") return false;
    if (url === "N/A") return true; // Special cases
    if (url === "") return false; // Empty string is invalid

    try {
        const urlObj = new URL(url);
        // Only allow http and https protocols for project URLs
        return urlObj.protocol === "http:" || urlObj.protocol === "https:";
    } catch {
        return false;
    }
}

function sanitizeString(input: any): string | null | undefined {
    if (input === null || input === undefined) return input;
    if (typeof input !== "string") return String(input);
    return input.trim();
}

function sanitizeNumber(input: any): number | null {
    if (typeof input === "number" && isFinite(input)) return input;
    if (typeof input === "string") {
        const num = parseFloat(input);
        return isFinite(num) ? num : null;
    }
    return null;
}

function sanitizeUrl(input: any): string | null {
    if (input === null || input === undefined) return input;
    if (typeof input !== "string") return String(input);
    return input.trim();
}
