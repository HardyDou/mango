<template>
  <section v-loading="pageLoading" class="router-view-parent is-root shell-runtime-outlet">
    <div
      v-if="showRuntimeBadge && runtimeDecision"
      class="shell-runtime-badge"
      :title="runtimeDecision.entry || runtimeDecision.runtimeCode || runtimeDecision.resolvedPageType"
    >
      <span>{{ runtimeDecision.resolvedPageType === 'MICRO_ROUTE' ? 'MICRO' : 'LOCAL' }}</span>
      <strong>{{ runtimeDecision.runtimeCode || runtimeDecision.moduleCode || 'local' }}</strong>
    </div>
    <el-empty v-if="!activeMenu" description="暂无可访问菜单" />
    <div ref="containerRef" class="shell-runtime-content" />
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useRuntimeHost } from './runtimeHost';
import {
  containsMenuPath,
  createNotFoundRouteMenu,
  resolveDirectoryRouteRedirect,
  useMenuHost,
  type ShellRouteMenu,
} from './menuHost';
import { useRoutesList } from '../stores/routesList';
import { useTagsViewRoutes } from '../stores/tagsViewRoutes';
import { createTagSnapshot } from '@mango/common/utils/tagsView';

const route = useRoute();
const router = useRouter();
const containerRef = ref<HTMLElement>();
const routesListStore = useRoutesList();
const tagsViewStore = useTagsViewRoutes();
const { activeTopRoutePath } = storeToRefs(routesListStore);
const { loading, runtimeDecision, loadRuntimeApps, mountMenu, refreshTab, disposeTab, dispose } = useRuntimeHost(
  containerRef,
  router,
);
const { menuLoading, menus, activeMenu, loadMenus, selectMenu } = useMenuHost();

const pageLoading = computed(() => loading.value || menuLoading.value);
const showRuntimeBadge = computed(() => import.meta.env.DEV && route.query.runtimeDebug === '1');
let mountingTabKey = '';
let mountedFullPath = '';
let knownTagKeys = new Set<string>();

async function initShellRuntime() {
  try {
    await Promise.all([loadRuntimeApps(), loadMenus()]);
    routesListStore.setRoutesList(menus.value);

    await renderCurrentRoute();
  } catch (error) {
    console.error('[mango-shell] failed to initialize shell runtime', error);
  }
}

watch(
  () => route.fullPath,
  async () => {
    if (!menus.value.length) {
      return;
    }
    await renderCurrentRoute();
  },
);

function findTopPath(path: string) {
  return menus.value.find((menu) => containsMenuPath(menu, path))?.path;
}

function resolveMenuPath(path: string): string {
  if (path.startsWith('/home/')) {
    return '/home';
  }
  return path;
}

function ensureTag(menu: ShellRouteMenu) {
  tagsViewStore.upsertTag(
    createTagSnapshot({
      path: route.path,
      name: route.name || menu.sourceMenu?.menuCode || menu.sourceMenu?.menuName,
      query: route.query,
      params: route.params,
      hash: route.hash,
      meta: {
        ...menu.sourceMenu?.meta,
        title: menu.sourceMenu?.meta?.title || menu.sourceMenu?.menuName,
        icon: menu.sourceMenu?.meta?.icon || menu.sourceMenu?.icon,
        isAffix: menu.sourceMenu?.meta?.isAffix,
      },
    }),
  );
}

watch(
  () => tagsViewStore.tagsViewRoutes.map((tag) => tag.tabKey),
  (keys) => {
    const currentKeys = new Set(keys);
    knownTagKeys.forEach((key) => {
      if (!currentKeys.has(key)) {
        disposeTab(key);
      }
    });
    knownTagKeys = currentKeys;
  },
  { immediate: true },
);

async function renderCurrentRoute() {
  const menuPath = resolveMenuPath(route.path);
  const currentMenu = selectMenu(menuPath);
  const redirectPath = resolveDirectoryRouteRedirect(currentMenu, menuPath);
  if (redirectPath) {
    await router.replace(redirectPath);
    return;
  }

  const targetMenu = currentMenu || createNotFoundRouteMenu(route.path);
  activeTopRoutePath.value = findTopPath(targetMenu.path) || activeTopRoutePath.value;
  if (currentMenu) {
    ensureTag(targetMenu);
  }
  await mountShellMenu(targetMenu);
}

async function mountShellMenu(menu: ShellRouteMenu) {
  const currentTag = createTagSnapshot({
    path: route.path,
    name: route.name,
    query: route.query,
    params: route.params,
    hash: route.hash,
    meta: { ...route.meta },
  });
  if (!menu?.path || mountedFullPath === route.fullPath || mountingTabKey === currentTag.tabKey) {
    return;
  }
  mountingTabKey = currentTag.tabKey;
  try {
    const tag = tagsViewStore.tagsViewRoutes.find((item) => item.tabKey === currentTag.tabKey);
    await mountMenu(menu, tag?.tabKey || currentTag.tabKey);
    mountedFullPath = route.fullPath;
  } finally {
    mountingTabKey = '';
  }
}

onMounted(initShellRuntime);
onMounted(() => {
  window.addEventListener('mango-tags-view-refresh', onRefreshRequest);
});
onBeforeUnmount(() => {
  void dispose();
  window.removeEventListener('mango-tags-view-refresh', onRefreshRequest);
});

function onRefreshRequest(event: Event) {
  const tabKey = (event as CustomEvent<{ tabKey?: string }>).detail?.tabKey;
  if (tabKey) {
    void refreshTab(tabKey);
  }
}
</script>

<style scoped>
.shell-runtime-outlet {
  position: relative;
  min-height: calc(
    100vh - var(--mango-header-height) - var(--mango-tags-view-height) - var(--mango-layout-footer-height) - 32px
  );
}

.shell-runtime-content {
  min-height: inherit;
}

.shell-runtime-badge {
  position: absolute;
  right: 12px;
  top: 12px;
  z-index: 20;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: min(420px, calc(100% - 24px));
  height: 26px;
  padding: 0 10px;
  border: 1px solid var(--mango-border-color);
  border-radius: 4px;
  background: var(--mango-bg-overlay);
  color: var(--mango-text-color-secondary);
  box-shadow: var(--mango-shadow-light);
  font-size: 12px;
  pointer-events: none;
}

.shell-runtime-badge span {
  color: var(--mango-color-primary);
  font-weight: 600;
}

.shell-runtime-badge strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}
</style>
