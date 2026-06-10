<template>
  <div
    class="layout-breadcrumb-close-full"
    @click="toggleFullscreen"
  >
    <el-icon :size="20">
      <span
        v-if="isFullscreen"
        class="fullscreen-exit-icon"
        :style="fullscreenExitIconStyle"
      />
      <component
        :is="FullScreenIcon"
        v-else
      />
    </el-icon>
  </div>
</template>

<script setup lang="ts" name="breadcrumbCloseFull">
import { ref, onMounted, onUnmounted, markRaw } from 'vue';
import type { CSSProperties } from 'vue';
import { FullScreen } from '@element-plus/icons-vue';
import fullscreenExitIconUrl from '../../../assets/icons/fullscreen-exit.svg';

const isFullscreen = ref(false);
const FullScreenIcon = markRaw(FullScreen);
const fullscreenExitIconStyle: CSSProperties = {
  '--fullscreen-exit-icon': `url(${fullscreenExitIconUrl})`,
};

const toggleFullscreen = async () => {
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen?.();
      return;
    }
    if (document.fullscreenEnabled && document.documentElement.requestFullscreen) {
      await document.documentElement.requestFullscreen();
    }
  } finally {
    handleFullscreenChange();
  }
};

const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement;
};

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange);
});

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange);
});
</script>

<style scoped lang="scss">
.layout-breadcrumb-close-full {
  display: flex;
  align-items: center;
  padding: 0 12px;
  height: 40px;
  cursor: pointer;
  color: var(--mango-color-top-bar);
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.8;
  }
}

.fullscreen-exit-icon {
  display: inline-block;
  width: 1.1em;
  height: 1.1em;
  background-color: currentColor;
  -webkit-mask: var(--fullscreen-exit-icon) center / contain no-repeat;
  mask: var(--fullscreen-exit-icon) center / contain no-repeat;
}
</style>
