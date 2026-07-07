import { h, render } from 'vue';
import { describe, expect, it } from 'vitest';
import { mergeGridWidgets, resolveWidgetAccess, type MangoGridWidgetDefinition, type MangoWidgetRuntimeContext } from '@mango/grid-widgets';
import { workflowMyProcessWidgets } from '../my-process';
import { workflowMyTaskWidgets } from '../my-task';
import { workflowMyTodoWidgets } from '../my-todo';

function createRuntime(options: {
  permissions?: string[];
  menus?: unknown[];
} = {}): MangoWidgetRuntimeContext {
  return {
    pageCode: 'home',
    user: {
      permissions: options.permissions || [],
    },
    menus: options.menus || [],
  };
}

function createWorkflowMenu(paths: string[]): unknown[] {
  return [
    {
      path: '/workflow',
      children: [
        {
          path: 'task',
          children: paths.map(path => ({ path })),
        },
      ],
    },
  ];
}

function renderRuntimeWidget(widget: MangoGridWidgetDefinition): string {
  const container = document.createElement('div');
  render(h(widget.component!), container);
  const text = container.textContent || '';
  render(null, container);
  return text;
}

describe('workflow home widgets access', () => {
  it('allows my-process only when task permission and initiated page are both available', () => {
    const widget = workflowMyProcessWidgets[0];
    const runtime = createRuntime({
      permissions: ['workflow:task:list'],
      menus: createWorkflowMenu(['initiated']),
    });

    expect(resolveWidgetAccess(widget, runtime)).toMatchObject({
      allowed: true,
      requiredPermissionCodes: ['workflow:task:list'],
      requiredRoutePaths: ['/workflow/task/initiated'],
      missingPermissionCodes: [],
      missingRoutePaths: [],
    });

    const mergedWidget = mergeGridWidgets({ widgets: [widget], runtime })[0];
    expect(mergedWidget.disabled).toBe(false);
  });

  it('renders an in-card missing-permission state when permission is missing', () => {
    const widget = workflowMyProcessWidgets[0];
    const runtime = createRuntime({
      permissions: [],
      menus: createWorkflowMenu(['initiated']),
    });
    const mergedWidget = mergeGridWidgets({ widgets: [widget], runtime })[0];

    expect(mergedWidget.disabled).toBe(true);
    expect(renderRuntimeWidget(mergedWidget)).toContain('缺少权限');
    expect(renderRuntimeWidget(mergedWidget)).toContain('workflow:task:list');
  });

  it('renders an in-card missing-permission state when route entry is missing', () => {
    const widget = workflowMyTaskWidgets[0];
    const runtime = createRuntime({
      permissions: ['workflow:task:list'],
      menus: createWorkflowMenu(['todo']),
    });
    const mergedWidget = mergeGridWidgets({ widgets: [widget], runtime })[0];

    expect(mergedWidget.disabled).toBe(true);
    expect(resolveWidgetAccess(widget, runtime).missingRoutePaths).toEqual(['/workflow/task/done']);
    expect(renderRuntimeWidget(mergedWidget)).toContain('缺少页面入口');
  });

  it('requires all linked pages for todo widget interactions', () => {
    const widget = workflowMyTodoWidgets[0];
    const runtime = createRuntime({
      permissions: ['workflow:task:list'],
      menus: createWorkflowMenu(['todo', 'copied']),
    });

    expect(resolveWidgetAccess(widget, runtime)).toMatchObject({
      allowed: true,
      requiredRoutePaths: ['/workflow/task/todo', '/workflow/task/copied'],
      missingRoutePaths: [],
    });
  });

  it('supports explicit access config for custom business widgets', () => {
    const widget: MangoGridWidgetDefinition = {
      type: 'business.risk-review',
      title: '风控审批',
      component: { render: () => h('div', '风控审批内容') },
      access: {
        mode: 'all',
        permissionCodes: ['risk:review:list', 'risk:review:approve'],
        routePaths: ['/risk/review'],
      },
    };

    expect(resolveWidgetAccess(widget, createRuntime({
      permissions: ['risk:review:list'],
      menus: [{ path: '/risk/review' }],
    }))).toMatchObject({
      allowed: false,
      missingPermissionCodes: ['risk:review:approve'],
      missingRoutePaths: [],
    });

    const mergedWidget = mergeGridWidgets({
      widgets: [widget],
      runtime: createRuntime({
        permissions: ['risk:review:list', 'risk:review:approve'],
        menus: [{ path: '/risk/review' }],
      }),
    })[0];

    expect(mergedWidget.disabled).toBe(false);
    expect(renderRuntimeWidget(mergedWidget)).toContain('风控审批内容');
  });
});
