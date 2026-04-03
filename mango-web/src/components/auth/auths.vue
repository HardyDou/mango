<template>
  <Auth v-bind="$props">
    <template v-if="$slots.default">
      <slot />
    </template>
    <template #no-auth>
      <slot name="no-auth">
        <el-empty description="无权限访问" />
      </slot>
    </template>
  </Auth>
</template>

<script setup lang="ts">
import Auth from './auth.vue';

defineProps<{
  value?: string;
  mode?: 'all' | 'any';
}>();

defineSlots<{
  default?: any;
  'no-auth'?: any;
}>();
</script>
