<template>
  <div class="tags-view-container">
    <el-scrollbar
      class="tags-view-scrollbar"
      @scroll="onScroll"
    >
      <router-link
        v-for="tag in visitedViews"
        :key="tag.path"
        :to="{ path: tag.path, query: tag.query }"
        :class="isActive(tag) ? 'tags-view-item active' : 'tags-view-item'"
        @contextmenu.prevent="openContextMenu($event, tag)"
        @click="refreshPage(tag)"
      >
        <span>{{ tag.meta?.title || tag.name }}</span>
        <el-icon
          v-if="!tag.meta?.isAffix"
          class="close-icon"
          @click.prevent.stop="closeSelectedTag(tag)"
        >
          <Close />
        </el-icon>
      </router-link>
    </el-scrollbar>

    <ContextMenu
      v-if="contextMenuVisible"
      :style="{ left: contextMenuLeft + 'px', top: contextMenuTop + 'px' }"
      :tag="contextMenuTag"
      @close="contextMenuVisible = false"
    />
  </div>
</template>

<script setup lang="ts" name="tagsView">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { Close } from '@element-plus/icons-vue';
import { useTagsViewRoutes } from '../../../stores/tagsViewRoutes';
import { isHomeTag } from '@mango/common/utils/tagsView';
import ContextMenu from './contextmenu.vue';

const route = useRoute();
const router = useRouter();
const storesTagsViewRoutes = useTagsViewRoutes();
const { tagsViewRoutes } = storeToRefs(storesTagsViewRoutes);

const contextMenuVisible = ref(false);
const contextMenuLeft = ref(0);
const contextMenuTop = ref(0);
const contextMenuTag = ref<any>(null);

const visitedViews = computed(() => tagsViewRoutes.value);

const addCurrentRoute = () => {
  if (!route.name || route.meta?.isHide) {
    return;
  }
  const exists = tagsViewRoutes.value.some((tag) => tag.path === route.path);
  if (exists) {
    return;
  }
  storesTagsViewRoutes.setTagsViewRoutes([
    ...tagsViewRoutes.value,
    {
      path: route.path,
      name: route.name,
      query: route.query,
      params: route.params,
      meta: { ...route.meta },
    } as any,
  ]);
};

const isActive = (tag: any) => {
  return tag.path === route.path;
};

const openContextMenu = (e: MouseEvent, tag: any) => {
  contextMenuLeft.value = e.clientX;
  contextMenuTop.value = e.clientY;
  contextMenuTag.value = tag;
  contextMenuVisible.value = true;
};

const closeSelectedTag = (tag: any) => {
  if (isHomeTag(tag)) {
    return;
  }
  const idx = visitedViews.value.findIndex((t) => t.path === tag.path);
  if (idx > -1) {
    const newTags = [...visitedViews.value];
    newTags.splice(idx, 1);
    storesTagsViewRoutes.setTagsViewRoutes(newTags);
    if (isActive(tag) && newTags.length > 0) {
      router.push(newTags[newTags.length - 1]);
    }
  }
};

const refreshPage = (tag: any) => {
  router.push(tag);
};

const onScroll = () => {
  contextMenuVisible.value = false;
};

// Close context menu on click outside
watch(
  () => route.path,
  () => {
    addCurrentRoute();
    contextMenuVisible.value = false;
  },
  { immediate: true }
);
</script>

<style scoped lang="scss">
.tags-view-container {
  height: 100%;
  min-width: 0;
  background: transparent;
  display: flex;
  align-items: center;
  padding: 0;

  .tags-view-scrollbar {
    flex: 1;
    min-width: 0;
    height: 100%;
    overflow-x: auto;
    white-space: nowrap;

    &::-webkit-scrollbar {
      height: 4px;
    }
  }

  .tags-view-item {
    display: inline-flex;
    align-items: center;
    height: 32px;
    padding: 0 12px;
    margin: 6px 4px 0 0;
    font-size: 12px;
    color: var(--mango-text-color-regular);
    background: transparent;
    border: 1px solid transparent;
    border-bottom: 0;
    border-radius: 6px 6px 0 0;
    text-decoration: none;
    cursor: pointer;
    transition: all 0.2s;
    position: relative;

    &:hover {
      background: var(--mango-bg-main);
      border-color: var(--mango-border-light);
      color: var(--mango-text-color);
    }

    &.active {
      background: var(--mango-bg-main);
      border-color: var(--mango-border-color);
      color: var(--mango-color-primary);
      box-shadow: inset 0 2px 0 var(--mango-color-primary);
      font-weight: 500;
    }

    &.active::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: -1px;
      height: 1px;
      background: var(--mango-bg-main);
    }

    .close-icon {
      width: 14px;
      height: 14px;
      margin-left: 6px;
      font-size: 10px;
      border-radius: 50%;

      &:hover {
        background: rgba(0, 0, 0, 0.08);
      }
    }
  }
}
</style>
