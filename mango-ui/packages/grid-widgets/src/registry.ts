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
    component: createRuntimeWidget(widget.component, runtime),
  };
}

function createRuntimeWidget(component: Component, runtime: MangoWidgetRuntimeContext): Component {
  return defineComponent({
    name: 'MangoGridRuntimeWidget',
    inheritAttrs: false,
    setup(_, { attrs }) {
      // runtime 是页面运行态上下文，不能进入 defaultProps，避免被布局组件保存到个人布局 JSON。
      return () => h(component, {
        ...attrs,
        runtime,
      });
    },
  });
}

function compareWidget(left: MangoGridWidgetDefinition, right: MangoGridWidgetDefinition): number {
  const orderDiff = (left.order ?? 0) - (right.order ?? 0);
  if (orderDiff !== 0) {
    return orderDiff;
  }
  const categoryDiff = (left.category || '').localeCompare(right.category || '', 'zh-CN');
  if (categoryDiff !== 0) {
    return categoryDiff;
  }
  return left.title.localeCompare(right.title, 'zh-CN');
}
