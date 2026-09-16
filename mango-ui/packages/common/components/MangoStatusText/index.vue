<template>
  <span class="mango-status-text" :class="`mango-status-text--${tone}`">
    <slot />
  </span>
</template>

<script setup lang="ts">
import type { MangoStatusTextProps } from './types';

defineOptions({ name: 'MangoStatusText' });

withDefaults(defineProps<MangoStatusTextProps>(), {
  tone: 'neutral',
});
</script>

<style scoped>
.mango-status-text {
  display: inline;
  color: var(--mango-color-status-neutral);
  font-weight: 400;
  letter-spacing: 0;
}

.mango-status-text--primary {
  color: var(--mango-color-status-primary);
}

.mango-status-text--success {
  color: var(--mango-color-status-success);
}

.mango-status-text--warning {
  color: var(--mango-color-status-warning);
}

.mango-status-text--danger {
  color: var(--mango-color-status-danger);
}

.mango-status-text--info {
  color: var(--mango-color-status-info);
}

.mango-status-text--neutral {
  color: var(--mango-color-status-neutral);
}
</style>
