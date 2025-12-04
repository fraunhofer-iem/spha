import {beforeEach, describe, expect, it, vi} from "vitest";
import {mount, type VueWrapper} from "@vue/test-utils";
import {createRouter, createWebHistory} from "vue-router";
import {parse} from "../../util/Parser";
import {createMockFileList, createMockResult} from "../../__tests__/setup";
import ResultUpload from "../ResultUpload.vue";

// Mock the Parser module
vi.mock("../../util/Parser", () => ({
    parse: vi.fn(),
}));

describe("ResultSelection", () => {
    let wrapper: VueWrapper<any>;
    let mockParse: any;

    beforeEach(() => {
        mockParse = vi.mocked(parse);
        mockParse.mockClear();

        // Create a test router
        const router = createRouter({
            history: createWebHistory(),
            routes: [
                { path: '/', component: { template: '<div>Home</div>' } },
                { path: '/upload', component: { template: '<div>Upload</div>' } },
                { path: '/product/:id', name: 'product-details', component: { template: '<div>Product</div>' } }
            ]
        });

        wrapper = mount(ResultUpload, {
            global: {
                plugins: [router],
                stubs: {
                    'router-link': true,
                    'router-view': true
                }
            }
        });
    });

    describe("File Validation", () => {
        it("should reject multiple files", async () => {
            const mockFiles = [
                new File(["{}"], "file1.json", {type: "application/json"}),
                new File(["{}"], "file2.json", {type: "application/json"}),
            ];

            const mockFileList = createMockFileList(mockFiles);

            // Call handleFile directly through the component's method
            (wrapper.vm as any).handleFile(mockFileList);

            await wrapper.vm.$nextTick();

            expect(wrapper.text()).toContain("Please provide exactly one file.");
        });

        it("should reject non-JSON files by MIME type", async () => {
            const txtFile = new File(["content"], "file.txt", {type: "text/plain"});
            const mockFileList = createMockFileList([txtFile]);

            (wrapper.vm as any).handleFile(mockFileList);
            await wrapper.vm.$nextTick();

            expect(wrapper.text()).toContain(
                "Invalid file type. Please drop a .json file.",
            );
        });

        it("should reject non-JSON files by extension", async () => {
            const txtFile = new File(["content"], "file.txt", {type: ""}); // No MIME type
            const mockFileList = createMockFileList([txtFile]);

            (wrapper.vm as any).handleFile(mockFileList);
            await wrapper.vm.$nextTick();

            expect(wrapper.text()).toContain(
                "Invalid file type. Please drop a .json file.",
            );
        });

        it("should accept JSON files with correct MIME type", async () => {
            const jsonFile = new File(['{"test": true}'], "test.json", {
                type: "application/json",
            });
            const mockFileList = createMockFileList([jsonFile]);

            // Mock successful parsing
            const mockResult = createMockResult();
            mockParse.mockReturnValue(mockResult);

            // Mock FileReader
            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
                result: '{"test": true}',
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            (wrapper.vm as any).handleFile(mockFileList);

            // Simulate FileReader onload
            mockFileReader.onload({target: {result: '{"test": true}'}});

            await wrapper.vm.$nextTick();

            expect(wrapper.text()).toContain("JSON file processed successfully!");
        });

        it("should accept JSON files with .json extension even without MIME type", async () => {
            const jsonFile = new File(['{"test": true}'], "test.json", {type: ""});
            const mockFileList = createMockFileList([jsonFile]);

            const mockResult = createMockResult();
            mockParse.mockReturnValue(mockResult);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
                result: '{"test": true}',
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            (wrapper.vm as any).handleFile(mockFileList);

            mockFileReader.onload({target: {result: '{"test": true}'}});

            await wrapper.vm.$nextTick();

            expect(wrapper.text()).toContain("JSON file processed successfully!");
        });
    });

    describe("File Processing", () => {
        it("should handle valid JSON and successful parsing", async () => {
            const validJsonContent =
                '{"resultHierarchy": {"root": {}}, "origins": [], "projectInfo": {}}';
            const mockResult = createMockResult({
                healthScore: 85,
            });

            mockParse.mockReturnValue(mockResult);

            const jsonFile = new File([validJsonContent], "test.json", {
                type: "application/json",
            });
            const mockFileList = createMockFileList([jsonFile]);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            (wrapper.vm as any).handleFile(mockFileList);

            // Simulate successful file reading
            mockFileReader.onload({target: {result: validJsonContent}});

            await wrapper.vm.$nextTick();

            expect(mockParse).toHaveBeenCalledWith(JSON.parse(validJsonContent));
            expect(wrapper.text()).toContain("JSON file processed successfully!");
        });

        it("should handle invalid JSON format", async () => {
            const invalidJsonContent = "{ invalid json }";
            const jsonFile = new File([invalidJsonContent], "test.json", {
                type: "application/json",
            });
            const mockFileList = createMockFileList([jsonFile]);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            (wrapper.vm as any).handleFile(mockFileList);

            // Simulate file reading with invalid JSON
            mockFileReader.onload({target: {result: invalidJsonContent}});

            await wrapper.vm.$nextTick();

            expect(wrapper.text()).toContain(
                "Failed to parse JSON. Please ensure the file is valid.",
            );
        });

        it("should handle valid JSON but invalid result format", async () => {
            const validJsonContent = '{"someOtherData": "value"}';
            const jsonFile = new File([validJsonContent], "test.json", {
                type: "application/json",
            });
            const mockFileList = createMockFileList([jsonFile]);

            // Mock parse to return null (invalid format)
            mockParse.mockReturnValue(null);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            (wrapper.vm as any).handleFile(mockFileList);

            mockFileReader.onload({target: {result: validJsonContent}});

            await wrapper.vm.$nextTick();

            expect(wrapper.text()).toContain(
                "Failed to parse JSON. Please ensure the file is valid.",
            );
        });

        it("should handle file reading errors", async () => {
            const jsonFile = new File(['{"test": true}'], "test.json", {
                type: "application/json",
            });
            const mockFileList = createMockFileList([jsonFile]);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            (wrapper.vm as any).handleFile(mockFileList);

            // Simulate file reading error
            mockFileReader.onerror(new Error("File reading failed"));

            await wrapper.vm.$nextTick();

            // The component should handle the error gracefully
            expect(wrapper.vm).toBeDefined();
        });

        it("should handle FileReader result that is not a string", async () => {
            const jsonFile = new File(['{"test": true}'], "test.json", {
                type: "application/json",
            });
            const mockFileList = createMockFileList([jsonFile]);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            (wrapper.vm as any).handleFile(mockFileList);

            // Simulate FileReader returning non-string result
            mockFileReader.onload({target: {result: new ArrayBuffer(8)}});

            await wrapper.vm.$nextTick();

            expect(wrapper.text()).toContain(
                "Failed to parse JSON. Please ensure the file is valid.",
            );
        });
    });

    describe("Drag and Drop Interactions", () => {
        it("should handle drag enter events", () => {
            const dropZone = wrapper.find('[data-testid="drop-zone"]');
            dropZone.trigger("dragenter");

            expect(wrapper.vm.isDragging).toBe(true);
        });

        it("should handle drag over events", () => {
            const dropZone = wrapper.find('[data-testid="drop-zone"]');
            dropZone.trigger("dragover");

            expect(wrapper.vm.isDragging).toBe(true);
        });

        it("should handle drag leave events", () => {
            wrapper.vm.isDragging = true;
            const dropZone = wrapper.find('[data-testid="drop-zone"]');
            dropZone.trigger("dragleave");

            expect(wrapper.vm.isDragging).toBe(false);
        });

        it("should handle drop events with valid files", async () => {
            const jsonFile = new File(['{"test": true}'], "test.json", {
                type: "application/json",
            });
            const mockResult = createMockResult();
            mockParse.mockReturnValue(mockResult);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            const dropZone = wrapper.find('[data-testid="drop-zone"]');

            // Create mock dataTransfer object
            const mockDataTransfer = {
                files: createMockFileList([jsonFile]),
            };

            await dropZone.trigger("drop", {
                dataTransfer: mockDataTransfer,
            });
            mockFileReader.onload({target: {result: '{"test": true}'}});

            await wrapper.vm.$nextTick();

            expect(wrapper.vm.isDragging).toBe(false);
            expect(wrapper.text()).toContain("JSON file processed successfully!");
        });
    });

    describe("File Input Click Functionality", () => {
        it("should trigger file input when drop zone is clicked", async () => {
            const clickSpy = vi.fn();
            wrapper.vm.fileInput = {click: clickSpy};

            const dropZone = wrapper.find('[data-testid="drop-zone"]');
            await dropZone.trigger("click");

            expect(clickSpy).toHaveBeenCalled();
        });

        it("should handle file selection through input", async () => {
            const jsonFile = new File(['{"test": true}'], "test.json", {
                type: "application/json",
            });
            const mockResult = createMockResult();
            mockParse.mockReturnValue(mockResult);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            const fileInput = wrapper.find('input[type="file"]');

            // Mock the files property
            Object.defineProperty(fileInput.element, "files", {
                value: createMockFileList([jsonFile]),
                writable: false,
            });

            await fileInput.trigger("change");
            mockFileReader.onload({target: {result: '{"test": true}'}});

            await wrapper.vm.$nextTick();

            expect(wrapper.text()).toContain("JSON file processed successfully!");
        });

        it("should reset file input value after selection", async () => {
            const fileInput = wrapper.find('input[type="file"]');
            const inputElement = fileInput.element as HTMLInputElement;

            // Mock the value property to be settable
            const valueSetter = vi.fn();
            Object.defineProperty(inputElement, "value", {
                get: () => "",
                set: valueSetter,
                configurable: true,
            });

            const jsonFile = new File(['{"test": true}'], "test.json", {
                type: "application/json",
            });

            Object.defineProperty(inputElement, "files", {
                value: createMockFileList([jsonFile]),
                writable: false,
            });

            await fileInput.trigger("change");

            expect(valueSetter).toHaveBeenCalledWith("");
        });
    });

    describe("State Management", () => {
        it("should clear previous errors when new drag operation starts", () => {
            wrapper.vm.error = "Previous error";
            wrapper.vm.successMessage = "Previous success";

            const dropZone = wrapper.find('[data-testid="drop-zone"]');
            dropZone.trigger("dragenter");

            expect(wrapper.vm.error).toBe("");
            expect(wrapper.vm.successMessage).toBe("");
        });

        it("should display filename when file is selected", async () => {
            const jsonFile = new File(['{"test": true}'], "test-file.json", {
                type: "application/json",
            });
            const mockFileList = createMockFileList([jsonFile]);
            const mockResult = createMockResult();
            mockParse.mockReturnValue(mockResult);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            (wrapper.vm as any).handleFile(mockFileList);
            mockFileReader.onload({target: {result: '{"test": true}'}});

            await wrapper.vm.$nextTick();

            expect(wrapper.vm.fileName).toBe("test-file.json");
        });

        it("should handle undefined or null file lists gracefully", () => {
            (wrapper.vm as any).handleFile(undefined);
            expect(wrapper.vm.error).toBe("");

            (wrapper.vm as any).handleFile(null);
            expect(wrapper.vm.error).toBe("");
        });

        it("should reset state when handling new file", async () => {
            // Set initial state
            wrapper.vm.error = "Previous error";
            wrapper.vm.successMessage = "Previous success";
            wrapper.vm.fileName = "old-file.json";

            const jsonFile = new File(['{"test": true}'], "new-file.json", {
                type: "application/json",
            });
            const mockFileList = createMockFileList([jsonFile]);
            const mockResult = createMockResult();
            mockParse.mockReturnValue(mockResult);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            (wrapper.vm as any).handleFile(mockFileList);

            // State should be reset immediately
            expect(wrapper.vm.error).toBe("");
            expect(wrapper.vm.successMessage).toBe("");
            expect(wrapper.vm.fileName).toBe("new-file.json");

            mockFileReader.onload({target: {result: '{"test": true}'}});

            await wrapper.vm.$nextTick();

            expect(wrapper.vm.successMessage).toBe(
                "JSON file processed successfully!",
            );
        });
    });

    describe("Component Integration", () => {
        it("should handle processing failures gracefully", async () => {
            const jsonFile = new File(["invalid json"], "test.json", {
                type: "application/json",
            });
            const mockFileList = createMockFileList([jsonFile]);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            (wrapper.vm as any).handleFile(mockFileList);
            mockFileReader.onload({target: {result: "invalid json"}});

            await wrapper.vm.$nextTick();

            expect(wrapper.vm.error).toContain("Failed to parse JSON");
            expect(wrapper.vm.successMessage).toBe("");
        });

        it("should process valid files and show success message", async () => {
            const jsonFile = new File(['{"test": true}'], "test.json", {
                type: "application/json",
            });
            const mockFileList = createMockFileList([jsonFile]);
            const mockResult = createMockResult();
            mockParse.mockReturnValue(mockResult);

            const mockFileReader = {
                onload: null as any,
                onerror: null as any,
                readAsText: vi.fn(),
            };

            global.FileReader = vi.fn(() => mockFileReader) as any;

            (wrapper.vm as any).handleFile(mockFileList);
            mockFileReader.onload({target: {result: '{"test": true}'}});

            await wrapper.vm.$nextTick();

            expect(wrapper.vm.successMessage).toBe("JSON file processed successfully!");
            expect(wrapper.vm.error).toBe("");
        });
    });
});
