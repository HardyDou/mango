<template>
  <router-view v-if="route.path === '/login'" />
  <div
    v-else-if="debugShell"
    class="cms-standalone"
  >
    <aside class="cms-standalone__aside">
      <div class="cms-standalone__brand">
        <strong>Cms</strong>
        <span>CMS 内容中心</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activePath"
          class="cms-standalone__menu"
          @select="handleMenuSelect"
        >
          <CmsMenuNode
            v-for="menu in menus"
            :key="menu.path || menu.menuCode"
            :menu="menu"
          />
        </el-menu>
      </el-scrollbar>
    </aside>
    <main
      v-loading="loading"
      class="cms-standalone__main"
    >
      <header class="cms-standalone__header">
        <div>
          <h1>{{ activeMenu?.menuName || 'CMS 内容中心' }}</h1>
          <p>{{ activeMenu?.path || 'mango-cms' }}</p>
        </div>
      </header>
      <section class="cms-standalone__content">
        <CmsRuntimeRoot
          :menu="activeMenu"
          :empty-description="emptyDescription"
        />
      </section>
    </main>
  </div>
  <main
    v-else
    v-loading="loading"
    class="cms-page-only"
  >
    <CmsRuntimeRoot
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
import CmsRuntimeRoot from './CmsRuntimeRoot.vue';
import { resolveCmsComponent } from './cmsRuntimeMap';
import { normalizeCmsMenus } from './cmsMenuPolicy';
import type { CmsMenu } from './types';
import { LOGIN_REDIRECT_KEY } from './router';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const menus = ref<CmsMenu[]>([]);
const DEBUG_SHELL_KEY = 'MANGO_CMS_DEBUG_SHELL';

const activePath = computed(() => route.path);
const debugShell = computed(() => route.query.debugShell === '1' || sessionStorage.getItem(DEBUG_SHELL_KEY) === '1');
const activeMenu = computed(() => findMenuByPath(menus.value, route.path) || (route.path === '/' ? firstPageMenu(menus.value[0]) : undefined));
const emptyDescription = computed(() => {
  if (loading.value) {
    return '正在加载CMS 内容中心页面';
  }
  if (route.path !== '/' && route.path !== '/home') {
    return `当前路径不属于CMS 内容中心子应用：${route.path}`;
  }
  return '请选择CMS 内容中心菜单';
});

const CmsMenuNode = defineComponent({
  name: 'CmsMenuNode',
  props: {
    menu: {
      type: Object as PropType<CmsMenu>,
      required: true,
    },
  },
  setup(props: { menu: CmsMenu }) {
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
    const response = await get<CmsMenu[]>('/authorization/menus/user', {
      params: { fmt: 'tree', appCode: 'internal-admin' },
    });
    menus.value = normalizeCmsMenus(response || [], component => Boolean(resolveCmsComponent(component)));
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

function renderMenuNode(menu: CmsMenu): any {
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

function firstPageMenu(menu?: CmsMenu): CmsMenu | undefined {
  if (!menu) {
    return undefined;
  }
  if (resolveCmsComponent(menu.component)) {
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

function findMenuByPath(source: CmsMenu[], path: string): CmsMenu | undefined {
  for (const menu of source) {
    if (menu.path === path && resolveCmsComponent(menu.component)) {
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
.cms-page-only {
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

.cms-standalone {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

.cms-standalone__aside {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-lighter);
}

.cms-standalone__brand {
  padding: 18px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.cms-standalone__brand strong {
  display: block;
  color: var(--el-text-color-primary);
  font-size: 18px;
  letter-spacing: 0;
}

.cms-standalone__brand span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.cms-standalone__menu {
  border-right: 0;
}

.cms-standalone__main {
  min-width: 0;
}

.cms-standalone__header {
  padding: 18px 24px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.cms-standalone__header h1 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 0;
}

.cms-standalone__header p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.cms-standalone__content {
  min-width: 0;
}
</style>
