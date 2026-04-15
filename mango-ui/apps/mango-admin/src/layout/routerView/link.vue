<template>
  <div class="router-view-link">
    <a
      :href="url"
      target="_blank"
      rel="noopener"
    >
      <slot />
    </a>
  </div>
</template>

<script setup lang="ts" name="RouterViewLink">
defineProps<{
  url: string;
}>();
</script>

<style scoped lang="scss">
.router-view-link {
  width: 100%;
  height: 100%;
}
</style>
