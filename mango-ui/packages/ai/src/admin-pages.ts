import { registerModulePages } from '@mango/admin-extension/core';

let registered = false;

export function registerMangoAiAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-ai',
    pages: {
      'ai/models/index': () => import('./index').then((module) => module.AiModelsView),
      'ai/prompts/index': () => import('./index').then((module) => module.AiPromptsView),
      'ai/skills/index': () => import('./index').then((module) => module.AiSkillsView),
      'ai/services/index': () => import('./index').then((module) => module.AiServicesView),
      'ai/services/run/index': () => import('./index').then((module) => module.AiServiceRunView),
    },
    routes: [
      {
        path: '/ai/services/run',
        component: 'ai/services/run/index',
        menuName: 'AI 服务运行台',
        menuCode: 'ai:services:run',
        icon: 'VideoPlay',
        visible: 0,
      },
    ],
  });
}
