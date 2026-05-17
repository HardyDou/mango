/**
 * 图标配置
 * @description 统一管理所有图标资源，支持 Element Plus 图标组件和 SVG 图标
 */
import * as ElementPlusIcons from '@element-plus/icons-vue';
import type { Component } from 'vue';

// Element Plus 图标映射表
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

// 导出所有支持的图标名称列表
export const iconNames = [...elementPlusIconNames];

// 图标映射表：通过图标名称查找图标组件
export const iconMap: Record<string, Component> = elementPlusIconNames.reduce(
  (acc, name) => {
    acc[name] = ElementPlusIcons[name] as Component;
    return acc;
  },
  {} as Record<string, Component>
);

// 便捷函数：获取图标组件
export function getIcon(name: string): Component | undefined {
  return iconMap[name];
}
