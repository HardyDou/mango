import { defineComponent, h } from 'vue';
import type { Component } from 'vue';
import type { MangoGridWidgetDefinition, MangoWidgetRuntimeContext, MergeGridWidgetsOptions } from './types';

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
  return {
    ...widget,
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
        if (!hasWidgetPermission(widget, runtime)) {
          return h(MangoWidgetPermissionFallback, {
            widgetTitle: widget.title,
            permissionCodes: widget.visibility?.widgetPermissionCodes || [],
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
    permissionCodes: {
      type: Array as () => string[],
      default: () => [],
    },
  },
  setup(props) {
    return () => h('div', {
      class: 'mango-grid-widget-permission-fallback',
      'data-state': 'missing-permission',
    }, [
      h('div', { class: 'mango-grid-widget-permission-fallback__badge' }, '缺少权限'),
      h('strong', props.widgetTitle),
      h('span', props.permissionCodes.length
        ? `需要权限：${props.permissionCodes.join(' / ')}`
        : '当前账号无权使用该小组件'),
    ]);
  },
});

function hasWidgetPermission(widget: MangoGridWidgetDefinition, runtime: MangoWidgetRuntimeContext): boolean {
  const permissionCodes = widget.visibility?.widgetPermissionCodes?.filter(Boolean) || [];
  if (!permissionCodes.length) {
    return true;
  }
  const permissions = new Set(runtime.user?.permissions || []);
  const mode = widget.visibility?.mode || 'any';
  if (mode === 'all') {
    return permissionCodes.every(code => permissions.has(code));
  }
  return permissionCodes.some(code => permissions.has(code));
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
