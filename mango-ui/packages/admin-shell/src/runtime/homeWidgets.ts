import { shallowRef } from 'vue';
import type { ShallowRef } from 'vue';
import type { MangoGridWidgetDefinition } from '@mango/grid-widgets';

const registeredHomeWidgets = shallowRef<MangoGridWidgetDefinition[]>([]);

export interface RegisterMangoAdminHomeWidgetsOptions {
  businessDomainCode?: string;
  businessDomainName?: string;
  groupName?: string;
  /** @deprecated use businessDomainCode. */
  moduleCode?: string;
  /** @deprecated use businessDomainName. */
  moduleName?: string;
}

export function registerMangoAdminHomeWidgets(
  widgets: MangoGridWidgetDefinition[] = [],
  options: RegisterMangoAdminHomeWidgetsOptions = {},
): void {
  if (widgets.length === 0) {
    return;
  }
  const widgetMap = new Map<string, MangoGridWidgetDefinition>();
  registeredHomeWidgets.value.forEach((widget) => {
    if (widget.type) {
      widgetMap.set(widget.type, widget);
    }
  });
  widgets.forEach((widget) => {
    const normalizedWidget = normalizeHomeWidget(widget, options);
    if (!normalizedWidget?.type || widgetMap.has(normalizedWidget.type)) {
      return;
    }
    widgetMap.set(normalizedWidget.type, normalizedWidget);
  });
  registeredHomeWidgets.value = Array.from(widgetMap.values());
}

export function getMangoAdminHomeWidgets(): MangoGridWidgetDefinition[] {
  return registeredHomeWidgets.value;
}

export function useMangoAdminHomeWidgets(): ShallowRef<MangoGridWidgetDefinition[]> {
  return registeredHomeWidgets;
}

export function resetMangoAdminHomeWidgetsForTest(): void {
  registeredHomeWidgets.value = [];
}

function normalizeHomeWidget(
  widget: MangoGridWidgetDefinition,
  options: RegisterMangoAdminHomeWidgetsOptions,
): MangoGridWidgetDefinition {
  const businessDomainCode = widget.businessDomainCode || widget.domainCode || widget.moduleCode || options.businessDomainCode || options.moduleCode;
  const businessDomainName = widget.businessDomainName || widget.domainName || options.businessDomainName || options.moduleName || businessDomainCode;
  const groupName = widget.groupName || options.groupName || widget.category;
  return {
    ...widget,
    source: widget.source || (businessDomainCode ? 'business' : widget.source),
    businessDomainCode,
    businessDomainName,
    domainCode: widget.domainCode || businessDomainCode,
    domainName: widget.domainName || businessDomainName,
    moduleCode: widget.moduleCode || businessDomainCode,
    groupName,
    category: widget.category || businessDomainName || groupName,
  };
}
