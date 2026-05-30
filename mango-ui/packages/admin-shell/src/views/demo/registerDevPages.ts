import { DEV_COMPONENT_DEMO_PAGES, type MangoPageLoader } from '@mango/admin-pages';
import { registerModulePages } from '@mango/admin-pages/core';

const devComponentPageLoaders: Record<string, MangoPageLoader> = {
  'demo/components/EditorView': () => import('./components/EditorView.vue'),
  'demo/components/CodeEditorView': () => import('./components/CodeEditorView.vue'),
  'demo/components/UploadView': () => import('./components/UploadView.vue'),
  'demo/components/ChartsView': () => import('./components/ChartsView.vue'),
  'demo/components/DirectiveView': () => import('./components/DirectiveView.vue'),
  'demo/components/ChatView': () => import('./components/ChatView.vue'),
  'demo/components/RealtimeView': () => import('./components/RealtimeView.vue'),
  'demo/components/ChinaAreaView': () => import('./components/ChinaAreaView.vue'),
  'demo/components/OrgSelectorView': () => import('./components/OrgSelectorView.vue'),
  'demo/components/WorkflowComponentsView': () => import('./components/WorkflowComponentsView.vue'),
  'demo/components/CaptchaView': () => import('./components/CaptchaView.vue'),
};

export function registerMangoAdminShellDevPages() {
  registerModulePages({
    moduleCode: 'mango-shell',
    pages: DEV_COMPONENT_DEMO_PAGES.reduce<Record<string, MangoPageLoader>>((pages, page) => {
      pages[page.component] = devComponentPageLoaders[page.component];
      return pages;
    }, {}),
  });
}
