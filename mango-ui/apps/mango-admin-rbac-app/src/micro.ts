import { createApp, type App as VueApp } from 'vue';
import { createMemoryHistory, createRouter, type Router } from 'vue-router';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'element-plus/dist/index.css';
import '@mango/common/theme/index.scss';
import '@mango/common/style.css';
import '@mango/rbac/style.css';
import type { MangoAppRuntime } from '@mango/app-runtime';
import { registerDefaultAdminPages } from '@mango/admin-pages';
import App from './App.vue';

let app: VueApp | undefined;
let router: Router | undefined;

export async function mount(container: HTMLElement, runtime: MangoAppRuntime) {
  container.innerHTML = '<div class="mango-rbac-root"></div>';
  const root = container.querySelector('.mango-rbac-root');
  if (!root) {
    return;
  }
  app = createApp(App);
  router = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/:pathMatch(.*)*',
        component: { render: () => null },
      },
    ],
  });
  await router.push(runtime.menu?.path || '/');
  await router.isReady();
  registerDefaultAdminPages();
  app.provide('mangoRuntime', runtime);
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
  }
  app.use(ElementPlus);
  app.use(router);
  app.mount(root);
}

export async function unmount() {
  app?.unmount();
  app = undefined;
  router = undefined;
}

export default { mount, unmount };
