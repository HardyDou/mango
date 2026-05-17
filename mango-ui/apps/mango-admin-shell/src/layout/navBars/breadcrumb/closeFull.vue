<template>
  <div
    class="layout-breadcrumb-close-full"
    @click="toggleFullscreen"
  >
    <el-icon :size="20">
      <component
        :is="CloseIcon"
        v-if="isFullscreen"
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
import { FullScreen, Close } from '@element-plus/icons-vue';

const isFullscreen = ref(false);

// 使用 markRaw 包装图标组件
const CloseIcon = markRaw(Close);
const FullScreenIcon = markRaw(FullScreen);

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
    isFullscreen.value = true;
  } else {
    document.exitFullscreen();
    isFullscreen.value = false;
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
</style>
