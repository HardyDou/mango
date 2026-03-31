<template>
  <div
    v-if="visible"
    class="context-menu"
  >
    <div
      class="context-menu-item"
      @click="onRefresh"
    >
      刷新
    </div>
    <div
      class="context-menu-item"
      @click="onClose"
    >
      关闭
    </div>
    <div
      class="context-menu-item"
      @click="onCloseOthers"
    >
      关闭其他
    </div>
    <div
      class="context-menu-item"
      @click="onCloseAll"
    >
      关闭全部
    </div>
  </div>
</template>

<script setup lang="ts" name="contextMenu">
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useTagsViewRoutes } from '@/stores/tagsViewRoutes';

const props = defineProps<{
  tag: any;
}>();

const emit = defineEmits(['close']);
const router = useRouter();
const storesTagsViewRoutes = useTagsViewRoutes();
const visible = ref(true);

watch(
  () => props.tag,
  () => {
    visible.value = true;
  }
);

const onRefresh = () => {
  router.replace(props.tag.path);
  emit('close');
};

const onClose = () => {
  const tags = storesTagsViewRoutes.tagsViewRoutes.filter((t) => t.path !== props.tag.path);
  storesTagsViewRoutes.setTagsViewRoutes(tags);
  if (tags.length > 0) {
    router.push(tags[tags.length - 1]);
  }
  emit('close');
};

const onCloseOthers = () => {
  const tags = storesTagsViewRoutes.tagsViewRoutes.filter(
    (t) => t.path === props.tag.path || t.meta?.isAffix
  );
  storesTagsViewRoutes.setTagsViewRoutes(tags);
  router.push(props.tag.path);
  emit('close');
};

const onCloseAll = () => {
  const tags = storesTagsViewRoutes.tagsViewRoutes.filter((t) => t.meta?.isAffix);
  storesTagsViewRoutes.setTagsViewRoutes(tags);
  if (tags.length > 0) {
    router.push(tags[tags.length - 1]);
  } else {
    router.push('/home');
  }
  emit('close');
};
</script>

<style scoped lang="scss">
.context-menu {
  position: fixed;
  background: var(--mango-bg-overlay);
  border: 1px solid var(--mango-border-color);
  border-radius: 4px;
  box-shadow: var(--mango-shadow-light);
  z-index: 9999;
  min-width: 120px;

  .context-menu-item {
    padding: 8px 16px;
    font-size: 13px;
    cursor: pointer;
    color: var(--mango-text-color);

    &:hover {
      background: var(--mango-color-menu-hover);
      color: var(--mango-color-primary);
    }
  }
}
</style>
