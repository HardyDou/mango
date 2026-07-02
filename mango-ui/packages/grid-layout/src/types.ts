import type { Component } from 'vue';

export const GRID_LAYOUT_SCHEMA_VERSION = 1;

export interface GridLayoutRect {
  x: number;
  y: number;
  w: number;
  h: number;
  minW?: number;
  minH?: number;
  maxW?: number;
  maxH?: number;
}

export interface GridLayoutItem {
  id: string;
  widgetType: string;
  layout: GridLayoutRect;
  title?: string;
  props?: Record<string, unknown>;
  showTitle?: boolean;
  padding?: boolean;
  locked?: boolean;
}

export interface GridLayoutValue {
  schemaVersion: typeof GRID_LAYOUT_SCHEMA_VERSION;
  pageCode: string;
  items: GridLayoutItem[];
}

export interface GridWidgetDefinition {
  type: string;
  title: string;
  description?: string;
  category?: string;
  businessDomainCode?: string;
  businessDomainName?: string;
  domainCode?: string;
  domainName?: string;
  moduleCode?: string;
  groupName?: string;
  icon?: Component;
  component?: Component;
  defaultLayout?: Partial<GridLayoutRect>;
  defaultProps?: Record<string, unknown>;
  showTitle?: boolean;
  padding?: boolean;
  disabled?: boolean;
  tags?: string[];
}

export interface GridLayoutOptions {
  columns: number;
  rowHeight: number;
  gap: number;
  defaultWidth: number;
  defaultHeight: number;
  compact?: boolean;
  verticalGapRows?: number;
}

export interface GridLayoutPersonalVO {
  id?: string;
  tenantId?: string;
  userId?: string;
  pageCode: string;
  schemaVersion: number;
  layoutJson: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveGridLayoutPersonalCommand {
  pageCode: string;
  layoutJson: string;
}
