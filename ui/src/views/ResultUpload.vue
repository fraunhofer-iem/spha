<script setup lang="ts">
import {ref} from "vue";
import {parse} from "../util/Parser";
import {store} from "../store.ts";
import {useRouter} from "vue-router";

const router = useRouter();
// Reactive variables
const isDragging = ref(false);
const error = ref("");
const successMessage = ref("");
const fileName = ref("");
const fileInput = ref<HTMLInputElement | null>(null);

const onDragEnter = () => {
  isDragging.value = true;
  error.value = ""; // Clear previous errors
  successMessage.value = "";
};

const onDragOver = () => {
  isDragging.value = true;
};

const onDragLeave = () => {
  isDragging.value = false;
};

const onDrop = (event: DragEvent) => {
  isDragging.value = false;
  const files = event.dataTransfer?.files;
  handleFile(files);
};

// --- File Input Click Handler ---

const triggerFileInput = () => {
  fileInput.value?.click();
};

const onFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const files = target?.files ?? undefined;
  handleFile(files);
  if (target) {
    target.value = "";
  }
};


const handleFile = (files: FileList | undefined) => {
  if (!files) {
    return;
  }
  // Reset state
  error.value = "";
  successMessage.value = "";
  fileName.value = "";

  // 1. Validation: Check if exactly one file is provided
  if (files.length !== 1) {
    error.value = "Please provide exactly one file.";
    return;
  }

  const file = files[0];
  if (!file) {
    error.value = "No file provided.";
    return;
  }

  // 2. Validation: Check if the file is a JSON file
  if (file.type !== "application/json" && !file.name.endsWith(".json")) {
    error.value = "Invalid file type. Please drop a .json file.";
    return;
  }

  fileName.value = file.name;

  // 3. File Reading
  const reader = new FileReader();

  reader.onload = (e) => {
    try {
      const raw = e.target?.result;
      if (typeof raw !== "string") {
        throw new Error("Failed to read the file.");
      }
      // 4. JSON Parsing
      const jsonData = JSON.parse(raw);
      const result = parse(jsonData);
      if (!result) {
        throw new Error("Invalid result format.");
      }
      successMessage.value = "JSON file processed successfully!";
      // 5. Emit data to parent component


      const product = store.addResult(result);
      // ToDo: calls tore
      router.push({name: 'product-details', params: {id: product.id}});
    } catch (err) {
      console.error(err);
      error.value = `Failed to parse JSON. Please ensure the file is valid.`;
    }
  };

  reader.onerror = () => {
    error.value = "Failed to read the file.";
  };

  reader.readAsText(file);
};
</script>
<template>
  <div class="mt-5">
    <div
        data-testid="drop-zone"
        :class="[
                'p-5 rounded-3 bg-light border border-3 border-dashed text-center',
                { 'border-primary bg-primary-subtle shadow-sm': isDragging },
            ]"
        @dragenter.prevent="onDragEnter"
        @dragover.prevent="onDragOver"
        @dragleave.prevent="onDragLeave"
        @drop.prevent="onDrop"
        @click="triggerFileInput"
        style="cursor: pointer; transition: all 0.3s ease"
    >
      <!-- Hidden file input for clicking -->
      <input
          ref="fileInput"
          type="file"
          accept=".json"
          style="display: none"
          @change="onFileSelect"
      />

      <div class="text-secondary">
        <i class="bi bi-file-earmark-code fs-1 mb-3"></i>
        <p v-if="!fileName" class="mb-0">
          Drag & drop a SPHA result file here, or click to select.
        </p>
        <p v-if="fileName" class="fw-bold text-dark mb-0">
          File: {{ fileName }}
        </p>
        <p v-if="error" class="text-danger fw-bold mt-2 mb-0">
          {{ error }}
        </p>
        <p v-if="successMessage" class="text-success fw-bold mt-2 mb-0">
          {{ successMessage }}
        </p>
      </div>
    </div>
  </div>
</template>
