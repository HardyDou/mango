import { registerModulePages } from '@mango/admin-extension/core';

let registered = false;

/**
 * 注册信息采集后台页面。
 *
 * <p>页面路由由后端 `AUTH_MENU` 配置驱动（`information-common-menu.json`），包内不再注册隐藏路由：
 * 表单详情、表单配置、采集记录处理都是 `visible: 0` 的隐藏菜单，路径固定并用 `query.id` 定位表单。
 */
export function registerMangoInformationAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-information',
    pages: {
      'information/forms/index': () => import('./views/FormsView.vue').then((module) => module.default),
      'information/forms/detail': () => import('./views/FormDetailView.vue').then((module) => module.default),
      'information/forms/config': () => import('./views/FormConfigView.vue').then((module) => module.default),
      'information/submissions/index': () => import('./views/SubmissionsView.vue').then((module) => module.default),
      'information/forms/submissions': () => import('./views/SubmissionsView.vue').then((module) => module.default),
      'information/hosted/index': () => import('./views/HostedFormView.vue').then((module) => module.default),
    },
  });
}
