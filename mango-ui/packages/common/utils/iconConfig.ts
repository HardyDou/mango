import * as ElementPlusIcons from '@element-plus/icons-vue';
import type { Component } from 'vue';

const elementPlusIconNames = [
  'HomeFilled',
  'User',
  'UserFilled',
  'Lock',
  'Search',
  'Setting',
  'Fold',
  'Expand',
  'Close',
  'Box',
  'Menu',
  'Document',
  'Edit',
  'Upload',
  'DataLine',
  'DataBoard',
  'Key',
  'Grid',
  'Coin',
  'List',
  'Platform',
  'Promotion',
  'Position',
  'Switch',
  'Collection',
  'Tickets',
  'DocumentChecked',
  'CircleCheck',
  'Message',
  'Operation',
  'Tools',
  'Clock',
  'Management',
  'MapLocation',
  'Code',
  'TrendCharts',
  'Pointer',
  'Guide',
  'OfficeBuilding',
  'Postcard',
  'ChatDotRound',
  'Connection',
  'FolderOpened',
  'Files',
  'UploadFilled',
  'Download',
  'View',
  'Delete',
  'CirclePlus',
] as const;

const elementPlusIcons = ElementPlusIcons as Record<string, Component | undefined>;

export const iconNames = Array.from(new Set([
  ...elementPlusIconNames,
  ...Object.keys(ElementPlusIcons),
]));

const explicitIconMap = elementPlusIconNames.reduce((acc, name) => {
  acc[name] = elementPlusIcons[name] as Component;
  return acc;
}, {} as Record<string, Component>);

export const iconMap: Record<string, Component | undefined> = new Proxy(explicitIconMap, {
  get(target, key: string) {
    return target[key] || elementPlusIcons[key];
  },
  has(target, key: string) {
    return Boolean(target[key] || elementPlusIcons[key]);
  },
});

export function getIcon(name: string): Component | undefined {
  return iconMap[name];
}
