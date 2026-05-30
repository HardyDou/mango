import { createApp, type App as VueApp } from 'vue';
import { registerLocalApp } from '@mango/app-runtime';
import { registerShellPages } from '@mango/admin-pages/core';
import { installShellApp } from '../appBootstrap';
import { getMangoAdminShellOptions } from '../config';

let mountedLocalApp: VueApp | undefined;
let registered = false;

export function registerShellLocalApps() {
  if (registered) {
    return;
  }
  registered = true;

  for (const app of getMangoAdminShellOptions().localApps || []) {
    registerLocalApp(app);
  }

  registerShellPages({
    home: () => import('../views/home/index.vue'),
    notFound: () => import('../views/error/404.vue'),
  });

  registerLocalApp({
    appCode: 'internal-admin',
    name: '内部管理后台本地入口',
    async mount(container: HTMLElement) {
      const { AppView, MenuView, RoleView } = await import('@mango/rbac');
      const LocalWorkbench = {
        components: { AppView, MenuView, RoleView },
        template: `
          <el-tabs class="local-tabs" model-value="app">
            <el-tab-pane label="应用管理" name="app"><AppView /></el-tab-pane>
            <el-tab-pane label="菜单管理" name="menu"><MenuView /></el-tab-pane>
            <el-tab-pane label="角色管理" name="role"><RoleView /></el-tab-pane>
          </el-tabs>
        `,
      };
      mountedLocalApp = createApp(LocalWorkbench);
      installShellApp(mountedLocalApp);
      mountedLocalApp.mount(container);
    },
    async unmount() {
      mountedLocalApp?.unmount();
      mountedLocalApp = undefined;
    },
  });
}
