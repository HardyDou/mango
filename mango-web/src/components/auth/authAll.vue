<template>
  <div class="auth-all-container">
    <slot v-if="hasAllAuth" />
    <slot
      v-else
      name="no-auth"
    >
      <el-empty description="无权限访问" />
    </slot>
  </div>
</template>

<script setup lang="ts" name="AuthAll">
import { computed } from 'vue';
import { hasPermission } from '@/utils/authFunction';

const props = defineProps<{
  value: string;
}>();

const hasAllAuth = computed(() => {
  if (!props.value) return true;

  const permissions = props.value.split(',').map((p) => p.trim());
  return permissions.every((p) => hasPermission(p));
});
</script>

<style scoped lang="scss">
.auth-all-container {
  width: 100%;
  height: 100%;
}
</style>
