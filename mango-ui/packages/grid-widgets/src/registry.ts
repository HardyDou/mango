import { defineComponent, h } from 'vue';
import type { Component } from 'vue';
import type {
  GridWidgetAccessState,
  MangoGridWidgetDefinition,
  MangoWidgetRuntimeContext,
  MergeGridWidgetsOptions,
} from './types';

export function mergeGridWidgets(options: MergeGridWidgetsOptions): MangoGridWidgetDefinition[] {
  const runtime = options.runtime;
  const sources = [
    ...(options.widgets || []),
    ...(options.systemWidgets || []),
    ...(options.businessWidgets || []),
  ].map(widget => withRuntime(widget, runtime));
  const widgetMap = new Map<string, MangoGridWidgetDefinition>();

  sources.forEach((widget) => {
    if (!widget?.type) {
      return;
    }
    const existed = widgetMap.get(widget.type);
    if (existed) {
      // 同一个 type 只保留先注册的小组件，避免业务侧覆盖系统组件时出现隐式替换。
      options.onDuplicate?.(widget.type, existed, widget);
      return;
    }
    widgetMap.set(widget.type, widget);
  });

  return Array.from(widgetMap.values()).sort(compareWidget);
}

function withRuntime(widget: MangoGridWidgetDefinition, runtime?: MangoWidgetRuntimeContext): MangoGridWidgetDefinition {
  if (!runtime || !widget?.component) {
    return widget;
  }
  const accessState = resolveWidgetAccess(widget, runtime);
  return {
    ...widget,
    disabled: Boolean(widget.disabled || !accessState.allowed),
    component: createRuntimeWidget(widget, runtime),
  };
}

function createRuntimeWidget(widget: MangoGridWidgetDefinition, runtime: MangoWidgetRuntimeContext): Component {
  return defineComponent({
    name: 'MangoGridRuntimeWidget',
    inheritAttrs: false,
    setup(_, { attrs }) {
      // runtime 是页面运行态上下文，不能进入 defaultProps，避免被布局组件保存到个人布局 JSON。
      return () => {
        const accessState = resolveWidgetAccess(widget, runtime);
        if (!accessState.allowed) {
          return h(MangoWidgetPermissionFallback, {
            widgetTitle: widget.title,
            accessState,
          });
        }
        return h(widget.component as Component, {
          ...attrs,
          runtime,
        });
      };
    },
  });
}

const MangoWidgetPermissionFallback = defineComponent({
  name: 'MangoWidgetPermissionFallback',
  props: {
    widgetTitle: {
      type: String,
      default: '小组件',
    },
    accessState: {
      type: Object as () => GridWidgetAccessState,
      required: true,
    },
  },
  setup(props) {
    return () => h('div', {
      class: 'mango-grid-widget-permission-fallback',
      'data-state': 'missing-permission',
    }, [
      h('div', { class: 'mango-grid-widget-permission-fallback__badge' }, '缺少权限'),
      h('strong', props.widgetTitle),
      h('span', formatAccessMessage(props.accessState)),
    ]);
  },
});

export function resolveWidgetAccess(
  widget: MangoGridWidgetDefinition,
  runtime: MangoWidgetRuntimeContext,
): GridWidgetAccessState {
  const permissionCodes = uniqueStrings([
    ...(widget.access?.permissionCodes || []),
    ...(widget.visibility?.widgetPermissionCodes || []),
  ]);
  const routePaths = uniqueStrings([
    ...(widget.access?.routePaths || []),
    ...(widget.visibility?.routePaths || []),
  ]);
  const permissions = new Set(runtime.user?.permissions || []);
  const mode = widget.access?.mode || widget.visibility?.mode || 'any';
  const missingPermissionCodes = permissionCodes.filter(code => !permissions.has(code));
  const allowedByPermission = !permissionCodes.length
    || (mode === 'all'
      ? missingPermissionCodes.length === 0
      : missingPermissionCodes.length < permissionCodes.length);
  const runtimeRoutePaths = collectRuntimeRoutePaths(runtime.menus || []);
  const missingRoutePaths = routePaths.filter(path => !runtimeRoutePaths.has(normalizePath(path)));
  const allowedByRoute = missingRoutePaths.length === 0;

  return {
    allowed: allowedByPermission && allowedByRoute,
    mode,
    requiredPermissionCodes: permissionCodes,
    missingPermissionCodes,
    requiredRoutePaths: routePaths,
    missingRoutePaths,
  };
}

function formatAccessMessage(accessState: GridWidgetAccessState): string {
  if (accessState.missingRoutePaths.length) {
    return `当前账号缺少页面入口：${accessState.missingRoutePaths.join(' / ')}`;
  }
  if (accessState.requiredPermissionCodes.length) {
    const codes = accessState.mode === 'all'
      ? accessState.missingPermissionCodes
      : accessState.requiredPermissionCodes;
    return `需要权限：${codes.join(' / ')}`;
  }
  return '当前账号无权使用该小组件';
}

function collectRuntimeRoutePaths(menus: unknown[], parentPath = ''): Set<string> {
  const routePaths = new Set<string>();
  menus.forEach((item) => {
    if (!isRecord(item)) {
      return;
    }
    const rawPath = typeof item.path === 'string' ? item.path : '';
    const currentPath = rawPath ? joinRoutePath(parentPath, rawPath) : parentPath;
    if (currentPath) {
      routePaths.add(normalizePath(currentPath));
    }
    if (rawPath.startsWith('/')) {
      routePaths.add(normalizePath(rawPath));
    }
    if (Array.isArray(item.children)) {
      collectRuntimeRoutePaths(item.children, currentPath).forEach(path => routePaths.add(path));
    }
  });
  return routePaths;
}

function joinRoutePath(parentPath: string, path: string): string {
  if (!path) {
    return parentPath;
  }
  if (path.startsWith('/')) {
    return path;
  }
  const parent = normalizePath(parentPath);
  return normalizePath(parent ? `${parent}/${path}` : path);
}

function normalizePath(path: string): string {
  const trimmed = path.trim();
  if (!trimmed) {
    return '';
  }
  const withSlash = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
  return withSlash.replace(/\/+/g, '/').replace(/\/$/, '') || '/';
}

function uniqueStrings(values: string[]): string[] {
  return Array.from(new Set(values.map(value => value.trim()).filter(Boolean)));
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function compareWidget(left: MangoGridWidgetDefinition, right: MangoGridWidgetDefinition): number {
  const orderDiff = (left.order ?? 0) - (right.order ?? 0);
  if (orderDiff !== 0) {
    return orderDiff;
  }
  const domainDiff = widgetDomainLabel(left).localeCompare(widgetDomainLabel(right), 'zh-CN');
  if (domainDiff !== 0) {
    return domainDiff;
  }
  const groupDiff = (left.groupName || '').localeCompare(right.groupName || '', 'zh-CN');
  if (groupDiff !== 0) {
    return groupDiff;
  }
  return left.title.localeCompare(right.title, 'zh-CN');
}

function widgetDomainLabel(widget: MangoGridWidgetDefinition): string {
  return widget.businessDomainName || widget.domainName || widget.category || widget.businessDomainCode || widget.domainCode || widget.moduleCode || '';
}
