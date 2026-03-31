<template>
  <div class="auths-container">
    <slot v-if="hasAuth" />
    <slot
      v-else
      name="no-auth"
    >
      <el-empty description="无权限访问" />
    </slot>
  </div>
</template>

<script setup lang="ts" name="Auths">
import { computed } from 'vue';
import { hasPermission } from '@/utils/authFunction';

const props = defineProps<{
  value: string;
  /** 权限模式：all=全部拥有，any=拥有任意一个（默认） */
  mode?: 'all' | 'any';
}>();

const hasAuth = computed(() => {
  if (!props.value) return true;

  const permissions = props.value.split(',').map((p) => p.trim());

  if (props.mode === 'all') {
    return permissions.every((p) => hasPermission(p));
  }

  return permissions.some((p) => hasPermission(p));
});
</script>

<style scoped lang="scss">
.auths-container {
  width: 100%;
  height: 100%;
}
</style>
