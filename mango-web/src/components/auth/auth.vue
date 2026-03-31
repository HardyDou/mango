<template>
  <div
    v-if="hasAuth"
    class="auth-container"
  >
    <slot />
  </div>
  <div
    v-else
    class="auth-empty"
  >
    <el-empty description="无权限访问" />
  </div>
</template>

<script setup lang="ts" name="Auth">
import { computed } from 'vue';
import { hasPermission } from '@/utils/authFunction';

const props = defineProps<{
  value?: string;
  /** 权限模式：all=全部拥有，any=拥有任意一个 */
  mode?: 'all' | 'any';
}>();

const hasAuth = computed(() => {
  if (!props.value) return true;

  const permissions = props.value.split(',');

  if (props.mode === 'any') {
    return permissions.some((p) => hasPermission(p.trim()));
  }

  return permissions.every((p) => hasPermission(p.trim()));
});
</script>

<style scoped lang="scss">
.auth-container,
.auth-empty {
  width: 100%;
  height: 100%;
}
</style>
