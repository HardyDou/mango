<template>
  <el-main class="layout-main" :class="{ 'layout-main--fixed-shell': enableFixedShell }" :style="layoutMainStyle">
    <div class="layout-main-frame" :class="{ 'layout-main-frame--fixed-shell': enableFixedShell }">
      <el-scrollbar
        ref="layoutMainScrollbarRef"
        class="layout-main-scroll layout-backtop-header-fixed"
        :class="{ 'layout-main-scroll--content': enableFixedShell }"
        wrap-class="layout-main-scroll"
        view-class="layout-main-scroll"
      >
        <div class="layout-main-body">
          <div class="layout-main-content-shell" :class="{ 'layout-main-content-shell--enabled': enableFixedShell }">
            <div class="layout-main-content">
              <ShellRuntimeOutlet v-if="contentMode === 'runtime-outlet'" />
              <LayoutParentView v-else />
            </div>
          </div>
          <LayoutFooter v-if="showFooter && !enableEdgeFooter" />
        </div>
      </el-scrollbar>
      <LayoutFooter v-if="showFooter && enableEdgeFooter" />
    </div>
    <el-backtop :target="setBacktopClass" />
  </el-main>
</template>

<script setup lang="ts" name="layoutMain">
import { computed, defineAsyncComponent, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { getMangoAdminShellOptions } from '../../config';
import { useLayoutStore } from '../../stores/layout';
import ShellRuntimeOutlet from '../../runtime/ShellRuntimeOutlet.vue';

const LayoutParentView = defineAsyncComponent(() => import('../routerView/parent.vue'));
const LayoutFooter = defineAsyncComponent(() => import('./footer.vue'));
const contentMode = computed(() => getMangoAdminShellOptions().contentMode || 'router-view');
const layoutStore = useLayoutStore();
const { isFooter, layout } = storeToRefs(layoutStore);

const layoutMainScrollbarRef = ref();
const showFooter = computed(() => isFooter.value);
const enableFixedShell = computed(() => layout.value === 'defaults' || layout.value === 'columns');
const enableEdgeFooter = computed(() => ['defaults', 'classic', 'columns'].includes(layout.value));
const layoutMainStyle = computed(() => ({
  '--mango-layout-footer-height': showFooter.value ? '40px' : '0px',
}));

const setBacktopClass = computed(() => {
  const wrapRef = layoutMainScrollbarRef.value?.wrapRef;
  // el-backtop expects a CSS selector string when passed as prop
  if (wrapRef) {
    return '.layout-main-scroll';
  }
  return '';
});
</script>

<style scoped lang="scss">
.layout-main {
  --layout-content-space: 16px;
  --layout-content-safe-bottom: calc(var(--layout-content-space) + env(safe-area-inset-bottom, 0px));
}

.layout-main {
  padding: 0 !important;
  overflow: hidden;
  width: 100%;
  min-height: 0;
  flex: 1;
  background-color: var(--mango-bg-main);
  display: flex;
  flex-direction: column;
}

.layout-main-frame {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.layout-main-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.layout-main-body {
  min-height: 100%;
  padding: var(--layout-content-space) var(--layout-content-space) var(--layout-content-safe-bottom);
  background: var(--mango-bg-main);
  display: flex;
  flex-direction: column;
}

.layout-main-content {
  flex: 1;
  min-height: calc(
    100vh - var(--mango-header-height) - var(--mango-tags-view-height) - var(--mango-layout-footer-height) - var(
        --layout-content-space
      ) - var(--layout-content-safe-bottom)
  );
}

.layout-main-content-shell {
  min-height: 100%;
  padding: var(--layout-content-space) var(--layout-content-space) var(--layout-content-safe-bottom);
  box-sizing: border-box;
}

.layout-main-content-shell:not(.layout-main-content-shell--enabled) {
  padding: 0;
}

.layout-main-frame--fixed-shell .layout-main-body {
  padding: 0;
}

.layout-main-scroll--content {
  min-height: 0;
}

:deep(.router-view-parent.is-root) {
  min-height: calc(
    100vh - var(--mango-header-height) - var(--mango-tags-view-height) - var(--mango-layout-footer-height) - var(
        --layout-content-space
      ) - var(--layout-content-safe-bottom)
  );
}

.layout-backtop-header-fixed {
  height: 100% !important;
}

.layout-main--fixed-shell {
  min-height: 0;
}
</style>
