<template>
  <div v-if="visible" class="context-menu">
    <div class="context-menu-item" @click="onRefresh">刷新</div>
    <div v-if="!isCurrentHomeTag" class="context-menu-item" @click="onClose">关闭</div>
    <div class="context-menu-item" @click="onCloseOthers">关闭其他</div>
    <div class="context-menu-item" @click="onCloseAll">关闭全部</div>
  </div>
</template>

<script setup lang="ts" name="contextMenu">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useTagsViewRoutes } from '../../../stores/tagsViewRoutes';
import { createTagSnapshot, getTagKey, isHomeTag, isSameTag, type MangoTagRoute } from '@mango/common/utils/tagsView';
import { normalizeRouteLocation, resolveClosedTagFallback, resolveTagLocation } from '../../../runtime/tagNavigation';

const props = defineProps<{
  tag: MangoTagRoute | null;
}>();

const emit = defineEmits(['close']);
const route = useRoute();
const router = useRouter();
const storesTagsViewRoutes = useTagsViewRoutes();
const visible = ref(true);
const isCurrentHomeTag = computed(() => isHomeTag(props.tag || undefined));

watch(
  () => props.tag,
  () => {
    visible.value = true;
  },
);

const onRefresh = () => {
  if (!props.tag) {
    return;
  }
  const currentTag = createTagSnapshot({
    path: route.path,
    name: route.name,
    query: route.query,
    params: route.params,
    hash: route.hash,
    meta: { ...route.meta },
  });
  if (isSameTag(props.tag, currentTag)) {
    window.dispatchEvent(new CustomEvent('mango-tags-view-refresh', { detail: { tabKey: props.tag.tabKey } }));
  } else {
    void router.replace(normalizeRouteLocation(resolveTagLocation(props.tag, true)));
  }
  emit('close');
};

const onClose = async () => {
  const tag = props.tag;
  if (!tag || isHomeTag(tag)) {
    emit('close');
    return;
  }
  const currentTag = createTagSnapshot({
    path: route.path,
    name: route.name,
    query: route.query,
    params: route.params,
    hash: route.hash,
    meta: { ...route.meta },
  });
  const fallback = resolveClosedTagFallback(storesTagsViewRoutes.tagsViewRoutes, tag, currentTag);
  const tags = storesTagsViewRoutes.tagsViewRoutes.filter((t) => getTagKey(t) !== getTagKey(tag));
  if (fallback) {
    await router.push(normalizeRouteLocation(fallback));
  }
  storesTagsViewRoutes.setTagsViewRoutes(tags);
  emit('close');
};

const onCloseOthers = () => {
  const tag = props.tag;
  if (!tag) {
    return;
  }
  const tags = storesTagsViewRoutes.tagsViewRoutes.filter((t) => getTagKey(t) === getTagKey(tag) || t.meta?.isAffix);
  storesTagsViewRoutes.setTagsViewRoutes(tags);
  router.push(normalizeRouteLocation(resolveTagLocation(tag)));
  emit('close');
};

const onCloseAll = () => {
  const tags = storesTagsViewRoutes.tagsViewRoutes.filter((t) => t.meta?.isAffix);
  storesTagsViewRoutes.setTagsViewRoutes(tags);
  router.push(normalizeRouteLocation(tags[0] ? resolveTagLocation(tags[0]) : '/home'));
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
