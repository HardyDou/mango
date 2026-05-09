<template>
  <div
    v-if="showTagsView"
    class="layout-workspace-nav"
  >
    <div class="layout-workspace-tags">
      <TagsView />
    </div>
  </div>
</template>

<script setup lang="ts" name="workspaceNav">
import { computed, defineAsyncComponent } from 'vue';

const props = withDefaults(defineProps<{
  tagsView?: boolean;
}>(), {
  tagsView: undefined,
});

const TagsView = defineAsyncComponent(() => import('../tagsView/tagsView.vue'));

const showTagsView = computed(() => props.tagsView ?? true);
</script>

<style scoped lang="scss">
.layout-workspace-nav {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  width: 100%;
  height: 40px;
  min-height: 40px;
  padding: 0 8px;
  background: var(--mango-bg-color);
  border-bottom: 1px solid var(--mango-border-color);
  box-sizing: border-box;
}

.layout-workspace-tags {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

@media screen and (max-width: 1000px) {
  .layout-workspace-nav {
    height: 40px;
    min-height: 40px;
    padding: 0 8px;
  }

  .layout-workspace-tags {
    width: 100%;
    height: 100%;
  }
}
</style>
