<template>
  <div class="layout-breadcrumb-close-full" @click="toggleFullscreen">
    <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏' " placement="bottom">
      <el-icon>
        <Close v-if="isFullscreen" />
        <FullScreen v-else />
      </el-icon>
    </el-tooltip>
  </div>
</template>

<script setup lang="ts" name="breadcrumbCloseFull">
import { ref, onMounted, onUnmounted } from 'vue';
import { FullScreen, Close } from '@element-plus/icons-vue';

const isFullscreen = ref(false);

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
  color: var(--mango-text-color-regular);
  transition: color 0.2s;

  &:hover {
    color: var(--mango-color-primary);
  }
}
</style>
