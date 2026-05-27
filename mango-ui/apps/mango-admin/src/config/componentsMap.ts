import {
  getPageLoader,
  type MangoPageLoader,
  normalizeComponentPath,
  registerDefaultAdminPages,
} from '@mango/admin-pages';
import { DEV_COMPONENT_DEMO_PAGES } from '@mango/admin-pages/dev-component-pages';

const devComponentPageLoaders: Record<string, MangoPageLoader> = {
  'demo/components/EditorView': () => import('@/views/demo/components/EditorView.vue'),
  'demo/components/CodeEditorView': () => import('@/views/demo/components/CodeEditorView.vue'),
  'demo/components/UploadView': () => import('@/views/demo/components/UploadView.vue'),
  'demo/components/ChartsView': () => import('@/views/demo/components/ChartsView.vue'),
  'demo/components/DirectiveView': () => import('@/views/demo/components/DirectiveView.vue'),
  'demo/components/ChatView': () => import('@/views/demo/components/ChatView.vue'),
  'demo/components/RealtimeView': () => import('@/views/demo/components/RealtimeView.vue'),
  'demo/components/ChinaAreaView': () => import('@/views/demo/components/ChinaAreaView.vue'),
  'demo/components/OrgSelectorView': () => import('@/views/demo/components/OrgSelectorView.vue'),
  'demo/components/WorkflowComponentsView': () => import('@/views/demo/components/WorkflowComponentsView.vue'),
  'demo/components/CaptchaView': () => import('@/views/demo/components/CaptchaView.vue'),
};

registerDefaultAdminPages({
  shellPages: {
    home: () => import('@/views/home/index.vue'),
    notFound: () => import('@/views/error/404.vue'),
  },
  registries: import.meta.env.DEV
    ? [
        {
          moduleCode: 'mango-shell',
          pages: DEV_COMPONENT_DEMO_PAGES.reduce<Record<string, MangoPageLoader>>((pages, page) => {
            pages[page.component] = devComponentPageLoaders[page.component];
            return pages;
          }, {}),
        },
      ]
    : [],
});

export const componentsMap: Record<string, any> = new Proxy({}, {
  get(_target, key: string) {
    return getPageLoader(undefined, key);
  },
  has(_target, key: string) {
    return Boolean(getPageLoader(undefined, key));
  },
});

export { normalizeComponentPath };
