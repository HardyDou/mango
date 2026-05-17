import { createApp, type App as VueApp } from 'vue';
import ElementPlus from 'element-plus';
import { registerLocalApp } from '@mango/app-runtime';
import { AppView, MenuView, RoleView } from '@mango/rbac';

let mountedLocalApp: VueApp | undefined;
let registered = false;

export function registerShellLocalApps() {
  if (registered) {
    return;
  }
  registered = true;

  registerLocalApp({
    appCode: 'internal-admin',
    name: '内部管理后台本地入口',
    async mount(container) {
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
      mountedLocalApp.use(ElementPlus);
      mountedLocalApp.mount(container);
    },
    async unmount() {
      mountedLocalApp?.unmount();
      mountedLocalApp = undefined;
    },
  });
}
