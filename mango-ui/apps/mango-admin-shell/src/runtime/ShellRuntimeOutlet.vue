<template>
  <section
    v-loading="pageLoading"
    class="router-view-parent is-root shell-runtime-outlet"
  >
    <div
      v-if="showRuntimeBadge && runtimeDecision"
      class="shell-runtime-badge"
      :title="runtimeDecision.entry || runtimeDecision.runtimeCode || runtimeDecision.resolvedPageType"
    >
      <span>{{ runtimeDecision.resolvedPageType === 'MICRO_ROUTE' ? 'MICRO' : 'LOCAL' }}</span>
      <strong>{{ runtimeDecision.runtimeCode || runtimeDecision.moduleCode || 'local' }}</strong>
    </div>
    <el-empty
      v-if="!activeMenu"
      description="暂无可访问菜单"
    />
    <div
      ref="containerRef"
      class="shell-runtime-content"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { storeToRefs } from 'pinia';
import { useRuntimeHost } from './runtimeHost';
import { containsMenuPath, useMenuHost, type ShellRouteMenu } from './menuHost';
import { useRoutesList } from '../stores/routesList';
import { useTagsViewRoutes } from '../stores/tagsViewRoutes';

const route = useRoute();
const router = useRouter();
const containerRef = ref<HTMLElement>();
const routesListStore = useRoutesList();
const tagsViewStore = useTagsViewRoutes();
const { activeTopRoutePath } = storeToRefs(routesListStore);
const {
  loading,
  runtimeDecision,
  loadRuntimeApps,
  mountMenu,
  dispose,
} = useRuntimeHost(containerRef, router);
const {
  menuLoading,
  menus,
  activeMenu,
  loadMenus,
  selectMenu,
} = useMenuHost();

const pageLoading = computed(() => loading.value || menuLoading.value);
const showRuntimeBadge = computed(() => import.meta.env.DEV && route.query.runtimeDebug === '1');
let mountingPath = '';
let mountedFullPath = '';

async function initShellRuntime() {
  try {
    const [runtimeOk, firstMenu] = await Promise.all([
      loadRuntimeApps(),
      loadMenus(),
    ]);
    routesListStore.setRoutesList(menus.value);

    const currentMenu = selectMenu(route.path);
    const targetMenu = currentMenu || firstMenu || activeMenu.value;
    if (targetMenu) {
      activeTopRoutePath.value = findTopPath(targetMenu.path) || activeTopRoutePath.value;
      ensureTag(targetMenu);
      if (!currentMenu && route.path !== targetMenu.path) {
        await router.replace(targetMenu.path);
        return;
      }
      await mountShellMenu(targetMenu);
    }

    if (!runtimeOk) {
      ElMessage.error('加载运行配置失败，请先登录');
    }
  } catch (error) {
    ElMessage.error('加载菜单失败，请重新登录');
  }
}

watch(
  () => route.fullPath,
  async () => {
    if (!menus.value.length) {
      return;
    }
    const menu = selectMenu(route.path);
    if (!menu) {
      return;
    }
    activeTopRoutePath.value = findTopPath(menu.path) || activeTopRoutePath.value;
    ensureTag(menu);
    await mountShellMenu(menu);
  }
);

function findTopPath(path: string) {
  return menus.value.find(menu => containsMenuPath(menu, path))?.path;
}

function ensureTag(menu: ShellRouteMenu) {
  const exists = tagsViewStore.tagsViewRoutes.some(tag => tag.path === menu.path);
  if (exists) {
    return;
  }
  tagsViewStore.setTagsViewRoutes([
    ...tagsViewStore.tagsViewRoutes,
    {
      path: menu.path,
      name: menu.name || menu.sourceMenu?.menuCode || menu.sourceMenu?.menuName,
      meta: {
        title: menu.meta?.title || menu.sourceMenu?.menuName,
        icon: menu.meta?.icon || menu.sourceMenu?.icon,
        isAffix: menu.meta?.isAffix,
      },
    },
  ]);
}

async function mountShellMenu(menu: ShellRouteMenu) {
  if (!menu?.path || mountedFullPath === route.fullPath || mountingPath === menu.path) {
    return;
  }
  mountingPath = menu.path;
  try {
    await mountMenu(menu);
    mountedFullPath = route.fullPath;
  } finally {
    mountingPath = '';
  }
}

onMounted(initShellRuntime);
onBeforeUnmount(() => {
  void dispose();
});
</script>

<style scoped>
.shell-runtime-outlet {
  position: relative;
  min-height: calc(100vh - var(--mango-header-height) - var(--mango-tags-view-height) - 32px);
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
