<template>
  <div class="tags-view-container">
    <el-scrollbar class="tags-view-scrollbar" @scroll="onScroll">
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
          @click.prevent="closeSelectedTag(tag)"
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
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { Close } from '@element-plus/icons-vue';
import { useTagsViewRoutes } from '@/stores/tagsViewRoutes';
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
    contextMenuVisible.value = false;
  }
);
</script>

<style scoped lang="scss">
.tags-view-container {
  height: var(--mango-tags-view-height);
  background: var(--mango-bg-color);
  border-bottom: 1px solid var(--mango-border-color);
  display: flex;
  align-items: center;
  padding: 0 8px;

  .tags-view-scrollbar {
    flex: 1;
    overflow-x: auto;
    white-space: nowrap;

    &::-webkit-scrollbar {
      height: 4px;
    }
  }

  .tags-view-item {
    display: inline-flex;
    align-items: center;
    height: 28px;
    padding: 0 10px;
    margin-right: 4px;
    font-size: 12px;
    color: var(--mango-text-color-regular);
    background: transparent;
    border-radius: 4px;
    text-decoration: none;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: var(--mango-color-menu-hover);
      color: var(--mango-text-color);
    }

    &.active {
      background: var(--mango-color-primary-lighter);
      color: var(--mango-color-primary);
    }

    .close-icon {
      margin-left: 4px;
      font-size: 10px;

      &:hover {
        background: rgba(0, 0, 0, 0.1);
        border-radius: 50%;
      }
    }
  }
}
</style>
