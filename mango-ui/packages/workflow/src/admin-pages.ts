import { registerModulePages } from '@mango/admin-pages/core';
import { workflowMyProcessWidgets } from './widgets/my-process';
import { workflowMyTaskWidgets } from './widgets/my-task';
import { workflowMyTodoWidgets } from './widgets/my-todo';

const workflowWidgets = [
  ...workflowMyTaskWidgets,
  ...workflowMyTodoWidgets,
  ...workflowMyProcessWidgets,
];

let registered = false;

export function registerMangoWorkflowAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-workflow',
    pages: {
      'workflow/definition/index': () => import('./index').then(m => m.WorkflowDefinitionView),
      'system/workflow-definition/index': () => import('./index').then(m => m.WorkflowDefinitionView),
      'workflow/template/index': () => import('./index').then(m => m.WorkflowTemplateView),
      'workflow-template/index': () => import('./index').then(m => m.WorkflowTemplateView),
      'workflow/task/todo/index': () => import('./index').then(m => m.WorkflowTaskListView),
      'workflow/task/initiated/index': () => import('./index').then(m => m.WorkflowTaskListView),
      'workflow/task/done/index': () => import('./index').then(m => m.WorkflowTaskListView),
      'workflow/task/copied/index': () => import('./index').then(m => m.WorkflowTaskListView),
      'workflow/task-list/index': () => import('./index').then(m => m.WorkflowTaskListView),
      'workflow/task/detail/index': () => import('./index').then(m => m.WorkflowTaskDetailView),
      'workflow/start-process/index': () => import('./index').then(m => m.WorkflowStartProcessView),
      'workflow/custom-apply/index': () => import('./index').then(m => m.WorkflowCustomApplyView),
    },
    routes: [
      {
        path: '/workflow/task/detail',
        component: 'workflow/task/detail/index',
        menuName: '任务详情',
        menuCode: 'workflow:task:detail',
        icon: 'DocumentChecked',
      },
      {
        path: '/workflow/custom-apply',
        component: 'workflow/custom-apply/index',
        menuName: '自定义申请',
        menuCode: 'workflow:custom-apply',
        icon: 'DocumentAdd',
      },
    ],
  });

  return {
    businessDomainCode: 'WORKFLOW',
    businessDomainName: '工作流',
    groupName: '工作流',
    widgets: workflowWidgets,
  };
}
