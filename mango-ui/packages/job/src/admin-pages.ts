import { registerModulePages } from '@mango/admin-pages/core';

let registered = false;

export function registerMangoJobAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-job',
    pages: {
      'job/definition/index': () => import('./index').then(m => m.JobDefinitionView),
      'job/instance/index': () => import('./index').then(m => m.JobInstanceView),
      'job/log/index': () => import('./index').then(m => m.JobLogView),
      'job/worker/index': () => import('./index').then(m => m.JobWorkerView),
      'job/engine/index': () => import('./index').then(m => m.JobEngineView),
    },
  });
}
