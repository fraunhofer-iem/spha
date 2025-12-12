<script setup lang="ts">

import {computed} from "vue";

interface DashboardCardProps {
  title: string;
  titleStyle?: string; // centered vs start
  showButton?: boolean;
  buttonText?: string;
}

const props = withDefaults(defineProps<DashboardCardProps>(), {
  titleStyle: 'center',
  showButton: false,
  buttonText: '',
})

const emit = defineEmits<
    (e: 'button-click') => void
>();

const titleCentering = computed(() => {
  if (props.showButton) {
    return 'justify-content-between'
  } else {
    return 'justify-content-center'
  }
})

const handleButtonClick = () => {
  emit('button-click');
};

</script>

<template>
  <div :class="`card text-${titleStyle} dashboard-card h-100`">
    <div :class="`card-header ${titleCentering} d-flex`">
      <h5 class="card-title pt-4 fw-bold">
        {{ title }}
      </h5>
      <div class="align-self-center ps-4 pe-4 pt-4">
        <button
            v-if="showButton"
            class="btn btn-outline-primary"
            @click="handleButtonClick"
        >
          {{ buttonText }}
        </button>
      </div>
    </div>
    <!-- Card Body -->
    <div class="card-body">
      <slot></slot>
    </div>

    <!-- Card Footer (optional) -->
    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>
