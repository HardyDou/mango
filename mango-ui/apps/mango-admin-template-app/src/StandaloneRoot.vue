<template>
  <router-view v-if="route.path === '/login'" />
  <div
    v-else-if="debugShell"
    class="template-standalone"
  >
    <aside class="template-standalone__aside">
      <div class="template-standalone__brand">
        <strong>Template</strong>
        <span>模板中心</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activePath"
          class="template-standalone__menu"
          @select="handleMenuSelect"
        >
          <TemplateMenuNode
            v-for="menu in menus"
            :key="menu.path || menu.menuCode"
            :menu="menu"
          />
        </el-menu>
      </el-scrollbar>
    </aside>
    <main
      v-loading="loading"
      class="template-standalone__main"
    >
      <header class="template-standalone__header">
        <div>
          <h1>{{ activeMenu?.menuName || '模板中心' }}</h1>
          <p>{{ activeMenu?.path || 'mango-template' }}</p>
        </div>
      </header>
      <section class="template-standalone__content">
        <TemplateRuntimeRoot
          :menu="activeMenu"
          :empty-description="emptyDescription"
        />
      </section>
    </main>
  </div>
  <main
    v-else
    v-loading="loading"
    class="template-page-only"
  >
    <TemplateRuntimeRoot
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
import TemplateRuntimeRoot from './TemplateRuntimeRoot.vue';
import { resolveTemplateComponent } from './templateRuntimeMap';
import { MenuTypeEnum, type TemplateMenu } from './types';
import { LOGIN_REDIRECT_KEY } from './router';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const menus = ref<TemplateMenu[]>([]);
const DEBUG_SHELL_KEY = 'MANGO_TEMPLATE_DEBUG_SHELL';

const activePath = computed(() => route.path);
const debugShell = computed(() => route.query.debugShell === '1' || sessionStorage.getItem(DEBUG_SHELL_KEY) === '1');
const activeMenu = computed(() => findMenuByPath(menus.value, route.path) || (route.path === '/' ? firstPageMenu(menus.value[0]) : undefined));
const emptyDescription = computed(() => {
  if (loading.value) {
    return '正在加载模板中心页面';
  }
  if (route.path !== '/' && route.path !== '/home') {
    return `当前路径不属于模板中心子应用：${route.path}`;
  }
  return '请选择模板中心菜单';
});

const TemplateMenuNode = defineComponent({
  name: 'TemplateMenuNode',
  props: {
    menu: {
      type: Object as PropType<TemplateMenu>,
      required: true,
    },
  },
  setup(props: { menu: TemplateMenu }) {
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
    const response = await get<TemplateMenu[]>('/authorization/menus/user', {
      params: { fmt: 'tree', appCode: 'internal-admin' },
    });
    menus.value = normalizeTemplateMenus(response || []);
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

function normalizeTemplateMenus(source: TemplateMenu[]) {
  return filterVisible(source)
    .map(pickTemplateBranch)
    .filter(Boolean) as TemplateMenu[];
}

function filterVisible(source: TemplateMenu[]): TemplateMenu[] {
  return source
    .filter(menu => menu.menuType !== MenuTypeEnum.BUTTON && menu.visible !== 0)
    .map(menu => ({
      ...menu,
      children: menu.children ? filterVisible(menu.children) : [],
    }));
}

function pickTemplateBranch(menu: TemplateMenu): TemplateMenu | undefined {
  const children = (menu.children || [])
    .map(pickTemplateBranch)
    .filter(Boolean) as TemplateMenu[];
  const isTemplatePage = Boolean(resolveTemplateComponent(menu.component));
  if (!isTemplatePage && children.length === 0) {
    return undefined;
  }
  return {
    ...menu,
    children,
  };
}

function renderMenuNode(menu: TemplateMenu): any {
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

function firstPageMenu(menu?: TemplateMenu): TemplateMenu | undefined {
  if (!menu) {
    return undefined;
  }
  if (resolveTemplateComponent(menu.component)) {
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

function findMenuByPath(source: TemplateMenu[], path: string): TemplateMenu | undefined {
  for (const menu of source) {
    if (menu.path === path && resolveTemplateComponent(menu.component)) {
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
.template-page-only {
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

.template-standalone {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

.template-standalone__aside {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-lighter);
}

.template-standalone__brand {
  padding: 18px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.template-standalone__brand strong {
  display: block;
  color: var(--el-text-color-primary);
  font-size: 18px;
  letter-spacing: 0;
}

.template-standalone__brand span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.template-standalone__menu {
  border-right: 0;
}

.template-standalone__main {
  min-width: 0;
}

.template-standalone__header {
  padding: 18px 24px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.template-standalone__header h1 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 0;
}

.template-standalone__header p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.template-standalone__content {
  min-width: 0;
}
</style>
