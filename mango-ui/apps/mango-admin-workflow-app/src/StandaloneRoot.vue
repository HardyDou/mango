<template>
  <router-view v-if="route.path === '/login'" />
  <div
    v-else-if="debugShell"
    class="workflow-standalone"
  >
    <aside class="workflow-standalone__aside">
      <div class="workflow-standalone__brand">
        <strong>Workflow</strong>
        <span>协同办公</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activePath"
          class="workflow-standalone__menu"
          @select="handleMenuSelect"
        >
          <WorkflowMenuNode
            v-for="menu in menus"
            :key="menu.path || menu.menuCode"
            :menu="menu"
          />
        </el-menu>
      </el-scrollbar>
    </aside>
    <main
      v-loading="loading"
      class="workflow-standalone__main"
    >
      <header class="workflow-standalone__header">
        <div>
          <h1>{{ activeMenu?.menuName || 'Workflow 协同办公' }}</h1>
          <p>{{ activeMenu?.path || 'mango-workflow' }}</p>
        </div>
      </header>
      <section class="workflow-standalone__content">
        <WorkflowRuntimeRoot
          :menu="activeMenu"
          :empty-description="emptyDescription"
        />
      </section>
    </main>
  </div>
  <main
    v-else
    v-loading="loading"
    class="workflow-page-only"
  >
    <WorkflowRuntimeRoot
      :menu="activeMenu"
      :empty-description="emptyDescription"
    />
  </main>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, nextTick, onMounted, ref, watch, type PropType } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElIcon, ElMenuItem, ElSubMenu } from 'element-plus';
import { get, Session } from '@mango/common';
import * as Icons from '@element-plus/icons-vue';
import WorkflowRuntimeRoot from './WorkflowRuntimeRoot.vue';
import { resolveWorkflowComponent } from './workflowRuntimeMap';
import { MenuTypeEnum, type WorkflowMenu } from './types';
import { LOGIN_REDIRECT_KEY } from './router';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const menus = ref<WorkflowMenu[]>([]);
const DEBUG_SHELL_KEY = 'MANGO_WORKFLOW_DEBUG_SHELL';

const activePath = computed(() => route.path);
const debugShell = computed(() => route.query.debugShell === '1' || sessionStorage.getItem(DEBUG_SHELL_KEY) === '1');
const activeMenu = computed(() => findMenuByPath(menus.value, route.path) || (route.path === '/' ? firstPageMenu(menus.value[0]) : undefined));
const emptyDescription = computed(() => {
  if (loading.value) {
    return '正在加载 Workflow 页面';
  }
  if (route.path !== '/' && route.path !== '/home') {
    return `当前路径不属于 Workflow 子应用：${route.path}`;
  }
  return '请选择 Workflow 菜单';
});

const WorkflowMenuNode = defineComponent({
  name: 'WorkflowMenuNode',
  props: {
    menu: {
      type: Object as PropType<WorkflowMenu>,
      required: true,
    },
  },
  setup(props: { menu: WorkflowMenu }) {
    return () => renderMenuNode(props.menu);
  },
});

onMounted(loadMenus);

watch(
  () => route.path,
  (path) => {
    if (path !== '/login' && menus.value.length === 0 && Session.getToken()) {
      void loadMenus();
    }
  }
);

watch(
  () => route.query.debugShell,
  (value) => {
    if (value === '1') {
      sessionStorage.setItem(DEBUG_SHELL_KEY, '1');
    } else if (value === '0') {
      sessionStorage.removeItem(DEBUG_SHELL_KEY);
    }
  },
  { immediate: true }
);

async function loadMenus() {
  await nextTick();
  if (!Session.getToken()) {
    sessionStorage.setItem(LOGIN_REDIRECT_KEY, route.fullPath);
    await router.replace('/login');
    return;
  }
  loading.value = true;
  try {
    const response = await get<WorkflowMenu[]>('/authorization/menus/user', {
      params: { fmt: 'tree', appCode: 'internal-admin' },
    });
    menus.value = normalizeWorkflowMenus(response || []);
    const selected = findMenuByPath(menus.value, route.path) || (route.path === '/' ? firstPageMenu(menus.value[0]) : undefined);
    if (selected?.path && route.path === '/') {
      await router.replace({
        path: selected.path,
        query: route.query,
      });
    }
  } catch (error) {
    menus.value = [];
  } finally {
    loading.value = false;
  }
}

function normalizeWorkflowMenus(source: WorkflowMenu[]) {
  return filterVisible(source)
    .map(pickWorkflowBranch)
    .filter(Boolean) as WorkflowMenu[];
}

function filterVisible(source: WorkflowMenu[]): WorkflowMenu[] {
  return source
    .filter(menu => menu.menuType !== MenuTypeEnum.BUTTON && menu.visible !== 0)
    .map(menu => ({
      ...menu,
      children: menu.children ? filterVisible(menu.children) : [],
    }));
}

function pickWorkflowBranch(menu: WorkflowMenu): WorkflowMenu | undefined {
  const children = (menu.children || [])
    .map(pickWorkflowBranch)
    .filter(Boolean) as WorkflowMenu[];
  const isWorkflowPage = Boolean(resolveWorkflowComponent(menu.component));
  if (!isWorkflowPage && children.length === 0) {
    return undefined;
  }
  return {
    ...menu,
    children,
  };
}

function renderMenuNode(menu: WorkflowMenu): any {
  const children = menu.children || [];
  const icon = menu.icon ? (Icons as any)[menu.icon] : undefined;
  const title = [
    icon ? h(ElIcon, null, () => h(icon)) : null,
    h('span', null, menu.menuName),
  ];
  if (children.length > 0) {
    return h(
      ElSubMenu,
      { index: menu.path || menu.menuCode },
      {
        title: () => title,
        default: () => children.map(renderMenuNode),
      }
    );
  }
  return h(
    ElMenuItem as any,
    { index: menu.path || '/' },
    () => title
  );
}

function handleMenuSelect(path: string) {
  if (!path || path === route.path) {
    return;
  }
  void router.push({
    path,
    query: route.query,
  });
}

function firstPageMenu(menu?: WorkflowMenu): WorkflowMenu | undefined {
  if (!menu) {
    return undefined;
  }
  if (resolveWorkflowComponent(menu.component)) {
    return menu;
  }
  for (const child of menu.children || []) {
    const page = firstPageMenu(child);
    if (page) {
      return page;
    }
  }
  return undefined;
}

function findMenuByPath(source: WorkflowMenu[], path: string): WorkflowMenu | undefined {
  for (const menu of source) {
    if (menu.path === path) {
      return menu;
    }
    const child = findMenuByPath(menu.children || [], path);
    if (child) {
      return child;
    }
  }
  return undefined;
}
</script>

<style scoped>
.workflow-standalone {
  display: flex;
  min-height: 100vh;
  background: var(--mango-bg-main, #f5f7fb);
}

.workflow-standalone__aside {
  width: 240px;
  flex: 0 0 240px;
  background: var(--mango-menu-bg, #fff);
  border-right: 1px solid var(--mango-border-light, #ebeef5);
}

.workflow-standalone__brand {
  height: 56px;
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 0 18px;
  border-bottom: 1px solid var(--mango-border-light, #ebeef5);
}

.workflow-standalone__brand strong {
  color: var(--mango-color-primary, #409eff);
  font-size: 20px;
}

.workflow-standalone__brand span {
  color: var(--mango-text-color-secondary, #909399);
  font-size: 13px;
}

.workflow-standalone__menu {
  border-right: 0;
}

.workflow-standalone__main {
  flex: 1;
  min-width: 0;
}

.workflow-standalone__header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid var(--mango-border-light, #ebeef5);
}

.workflow-standalone__header h1 {
  margin: 0;
  color: var(--mango-text-color, #303133);
  font-size: 16px;
  font-weight: 600;
}

.workflow-standalone__header p {
  margin: 2px 0 0;
  color: var(--mango-text-color-secondary, #909399);
  font-size: 12px;
}

.workflow-standalone__content {
  padding: 16px;
}

.workflow-page-only {
  min-height: 100vh;
  padding: 16px;
  background: var(--mango-bg-main, #f5f7fb);
}
</style>
