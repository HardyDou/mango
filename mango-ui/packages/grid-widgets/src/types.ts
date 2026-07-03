import type { GridWidgetDefinition } from '@mango/grid-layout';
import type { Component } from 'vue';

export type MangoGridWidgetSource = 'mango' | 'business';
export type MangoGridWidgetVisibilityMode = 'any' | 'all';
export type MangoWidgetRuntimeMode = 'host' | 'sub-app' | 'standalone';

export interface GridWidgetVisibility {
  mode?: MangoGridWidgetVisibilityMode;
  widgetPermissionCodes?: string[];
}

export interface MangoGridWidgetDefinition extends GridWidgetDefinition {
  source?: MangoGridWidgetSource;
  businessDomainCode?: string;
  businessDomainName?: string;
  moduleCode?: string;
  order?: number;
  visibility?: GridWidgetVisibility;
}

export interface MergeGridWidgetsOptions {
  systemWidgets?: MangoGridWidgetDefinition[];
  businessWidgets?: MangoGridWidgetDefinition[];
  widgets?: MangoGridWidgetDefinition[];
  runtime?: MangoWidgetRuntimeContext;
  onDuplicate?: (type: string, kept: MangoGridWidgetDefinition, ignored: MangoGridWidgetDefinition) => void;
}

export interface MangoWidgetRuntimeUser {
  userId?: string | number;
  username?: string;
  nickname?: string;
  avatar?: string;
  deptName?: string;
  orgName?: string;
  roles?: string[];
  permissions?: string[];
  appCode?: string;
  lastLoginTime?: string;
}

export interface MangoWidgetRuntimeTenant {
  tenantId?: string | number;
  tenantCode?: string;
  tenantName?: string;
}

export interface MangoWidgetNavigateTarget {
  path?: string;
  name?: string;
  url?: string;
  pageType?: string;
  raw?: unknown;
}

export interface MangoWidgetRuntimeContext {
  pageCode: string;
  mode?: MangoWidgetRuntimeMode;
  user?: MangoWidgetRuntimeUser;
  tenant?: MangoWidgetRuntimeTenant;
  menus?: unknown[];
  navigate?: (target: MangoWidgetNavigateTarget) => void | Promise<void>;
}

export interface QuickEntryMenuItem {
  id: string;
  title: string;
  path: string;
  url?: string;
  icon?: Component;
  iconName?: string;
  moduleCode?: string;
  appCode?: string;
  pageType?: string;
  raw?: unknown;
}

export type QuickEntryMenuResolver = (
  menus: unknown[],
  runtime?: MangoWidgetRuntimeContext,
) => QuickEntryMenuItem[];

export interface QuickEntryWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  menus?: unknown[];
  resolveMenus?: QuickEntryMenuResolver;
  storageKey?: string;
  maxDefaultItems?: number;
  navigate?: (item: QuickEntryMenuItem) => void | Promise<void>;
}

export interface MessageCenterCategory {
  key: string;
  label: string;
  bizGroup?: string;
  bizType?: string;
  priority?: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  color?: string;
}

export interface MessageCenterWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  messageCenterPath?: string;
  pageSize?: number;
  categories?: MessageCenterCategory[];
}

export interface UserProfileWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  profilePath?: string;
  passwordPath?: string;
}

export interface MyTodoWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  todoPath?: string;
}

export interface MyProcessWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  processPath?: string;
}

export interface MyTaskWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  taskPath?: string;
}

export interface CalendarWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  calendarCode?: string;
}
