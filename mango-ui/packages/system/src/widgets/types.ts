import type { Component } from 'vue';
import type { MangoWidgetRuntimeContext } from '@mango/grid-widgets';

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

export interface UserProfileWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  profilePath?: string;
  passwordPath?: string;
}
