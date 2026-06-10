<template>
  <div :class="containerClasses">
    <el-scrollbar
      class="tags-view-scrollbar"
      @scroll="onScroll"
    >
      <router-link
        v-for="tag in visitedViews"
        :key="tag.path"
        :to="{ path: tag.path, query: tag.query }"
        :class="getTagItemClasses(tag)"
        @contextmenu.prevent="openContextMenu($event, tag)"
      >
        <el-icon
          v-if="showTagIcon && resolveTagIcon(tag)"
          class="tag-icon"
        >
          <component :is="resolveTagIcon(tag)" />
        </el-icon>
        <span class="tag-title">{{ tag.meta?.title || tag.name }}</span>
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
import { useRoute, useRouter, type RouteRecordRaw } from 'vue-router';
import { storeToRefs } from 'pinia';
import { Close } from '@element-plus/icons-vue';
import { iconMap } from '@mango/common/utils/iconConfig';
import { useTagsViewRoutes } from '../../../stores/tagsViewRoutes';
import { useLayoutStore } from '../../../stores/layout';
import { DEFAULT_TAGS_STYLE, normalizeTagsStyle, usePreferencesStore } from '../../../stores/preferences';
import { isHomeTag } from '@mango/common/utils/tagsView';
import { resolveClosedTagFallback } from '../../../runtime/tagNavigation';
import ContextMenu from './contextmenu.vue';

const route = useRoute();
const router = useRouter();
const storesTagsViewRoutes = useTagsViewRoutes();
const layoutStore = useLayoutStore();
const preferencesStore = usePreferencesStore();
const { tagsViewRoutes } = storeToRefs(storesTagsViewRoutes);
const { isTagsviewIcon } = storeToRefs(layoutStore);
const { tagsStyle } = storeToRefs(preferencesStore);

const contextMenuVisible = ref(false);
const contextMenuLeft = ref(0);
const contextMenuTop = ref(0);
const contextMenuTag = ref<any>(null);

const visitedViews = computed(() => tagsViewRoutes.value);
const showTagIcon = computed(() => isTagsviewIcon.value);
const resolvedTagsStyle = computed(() => normalizeTagsStyle(tagsStyle.value));
const containerClasses = computed(() => ['tags-view-container', resolvedTagsStyle.value || DEFAULT_TAGS_STYLE]);

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

const getTagItemClasses = (tag: RouteRecordRaw) => {
  return [
    'tags-view-item',
    resolvedTagsStyle.value || DEFAULT_TAGS_STYLE,
    {
      active: isActive(tag),
      'has-icon': showTagIcon.value && Boolean(resolveTagIcon(tag)),
    },
  ];
};

const resolveTagIcon = (tag: RouteRecordRaw) => {
  const iconName = typeof tag.meta?.icon === 'string' ? tag.meta.icon : '';
  return iconName ? iconMap[iconName] : undefined;
};

const openContextMenu = (e: MouseEvent, tag: any) => {
  contextMenuLeft.value = e.clientX;
  contextMenuTop.value = e.clientY;
  contextMenuTag.value = tag;
  contextMenuVisible.value = true;
};

const closeSelectedTag = async (tag: any) => {
  if (isHomeTag(tag)) {
    return;
  }
  const fallback = resolveClosedTagFallback(visitedViews.value, tag, route.path);
  const newTags = visitedViews.value.filter((t) => t.path !== tag.path);
  if (fallback) {
    await router.push(fallback);
  }
  storesTagsViewRoutes.setTagsViewRoutes(newTags);
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
    gap: 6px;

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

    .tag-icon {
      flex-shrink: 0;
      font-size: 13px;
    }

    .tag-title {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .close-icon {
      width: 14px;
      height: 14px;
      margin-left: 2px;
      font-size: 10px;
      border-radius: 50%;
      flex-shrink: 0;

      &:hover {
        background: rgba(0, 0, 0, 0.08);
      }
    }
  }

  .tags-view-item.tags-style-capsule {
    height: 30px;
    margin-top: 5px;
    border-bottom: 1px solid transparent;
    border-radius: 16px;
    background: var(--mango-bg-color);
    border-color: var(--mango-border-color);

    &:hover {
      background: var(--mango-bg-overlay);
      border-color: color-mix(in srgb, var(--mango-color-primary) 20%, var(--mango-border-color));
    }

    &.active {
      color: #fff;
      background: var(--mango-color-primary);
      border-color: var(--mango-color-primary);
      box-shadow: none;
    }

    &.active::after {
      display: none;
    }

    .close-icon:hover {
      background: rgba(255, 255, 255, 0.18);
    }
  }

  .tags-view-item.tags-style-card {
    height: 30px;
    margin-top: 5px;
    border-radius: 4px;
    border-bottom: 1px solid var(--mango-border-color);
    background: var(--mango-bg-color);

    &:hover {
      border-color: var(--mango-color-primary);
      color: var(--mango-color-primary);
    }

    &.active {
      border-color: var(--mango-color-primary);
      background: color-mix(in srgb, var(--mango-color-primary) 8%, var(--mango-bg-main));
      box-shadow: inset 3px 0 0 var(--mango-color-primary);
      color: var(--mango-color-primary);
    }

    &.active::after {
      display: none;
    }
  }

  .tags-view-item.tags-style-classic {
    border-radius: 6px 6px 0 0;
  }
}
</style>
