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
    <slot name="no-auth">
      <el-empty description="无权限访问" />
    </slot>
  </div>
</template>

<script setup lang="ts" name="Auth">
import { computed } from 'vue';
import { hasPermission } from '@/utils/authFunction';

const props = withDefaults(defineProps<{
  value?: string;
  /** 权限模式：all=全部拥有，any=拥有任意一个 */
  mode?: 'all' | 'any';
}>(), {
  mode: 'any'
});

const hasAuth = computed(() => {
  if (!props.value) return true;

  const permissions = props.value.split(',').map(p => p.trim());

  if (props.mode === 'all') {
    return permissions.every((p) => hasPermission(p));
  }

  return permissions.some((p) => hasPermission(p));
});
</script>

<style scoped lang="scss">
.auth-container,
.auth-empty {
  width: 100%;
  height: 100%;
}
</style>
