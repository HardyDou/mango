<template>
  <div class="router-view-iframe">
    <iframe
      :src="url"
      frameborder="0"
      class="iframe"
      :style="iframeStyle"
      sandbox="allow-scripts allow-same-origin"
    />
  </div>
</template>

<script setup lang="ts" name="RouterViewIframe">
import { computed } from 'vue';

const props = defineProps<{
  url: string;
  iframeStyle?: Record<string, string>;
}>();

const defaultStyle = {
  width: '100%',
  height: '100%',
};
</script>

<style scoped lang="scss">
.router-view-iframe {
  width: 100%;
  height: 100%;

  .iframe {
    width: 100%;
    height: 100%;
    border: none;
  }
}
</style>
