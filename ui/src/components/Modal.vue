<script setup lang="ts">

interface ModalProps {
  title: string;
  show?: boolean;
}

withDefaults(defineProps<ModalProps>(), {
  show: false,
});

const emit = defineEmits<{
  (e: 'close'): void;
}>();

const handleClose = () => {
  emit('close');
};

const handleBackdropClick = (event: Event) => {
  if (event.target === event.currentTarget) {
    handleClose();
  }
};

</script>

<template>
  <div
      v-if="show"
      class="modal fade show"
      style="display: block;"
      tabindex="-1"
      role="dialog"
      aria-labelledby="modalTitle"
      aria-modal="true"
      @click="handleBackdropClick"
  >
    <div class="modal-dialog" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <h5 id="modalTitle" class="modal-title">{{ title }}</h5>
          <button
              type="button"
              class="btn-close"
              aria-label="Close"
              @click="handleClose"
          ></button>
        </div>
        <div class="modal-body">
          <slot></slot>
        </div>
      </div>
    </div>
  </div>
  <div v-if="show" class="modal-backdrop fade show"></div>
</template>

<style scoped>
.modal {
  z-index: 1050;
}

.modal-backdrop {
  z-index: 1040;
}

.modal-dialog {
  max-width: 800px;
  width: 90%;
}

.modal-content {
  min-height: 800px;
}
</style>