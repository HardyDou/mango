export type MangoDescriptionListDisplay = 'key-value' | 'table';
export type MangoDescriptionValue = string | number | boolean | null | undefined;
export type MangoDescriptionItemComponentType = 'rich-text-preview';

export interface MangoDescriptionItem {
  key: string;
  label: string;
  value: MangoDescriptionValue;
  span?: number;
  emptyText?: string;
  slot?: string;
  componentType?: MangoDescriptionItemComponentType;
}

export interface MangoDescriptionGroupHeader {
  title: string;
  description?: string;
  extra?: MangoDescriptionValue;
  extraSlot?: string;
  extraPlacement?: 'after-description' | 'end';
}

export interface MangoDescriptionGroup {
  key: string;
  header?: MangoDescriptionGroupHeader;
  items: MangoDescriptionItem[];
}

export interface MangoDescriptionItemSlotProps {
  item: MangoDescriptionItem;
  index: number;
  group: MangoDescriptionGroup;
  groupIndex: number;
  value: MangoDescriptionValue;
  displayValue: string;
}

export interface MangoDescriptionGroupExtraSlotProps {
  group: MangoDescriptionGroup;
  groupIndex: number;
  header: MangoDescriptionGroupHeader;
  value: MangoDescriptionValue;
  displayValue: string;
}

interface MangoDescriptionListCommonProps {
  display?: MangoDescriptionListDisplay;
  column?: number;
  labelWidth?: string | number;
  emptyText?: string;
  emptyDescription?: string;
  richTextPreviewComponent?: Component;
}

export type MangoDescriptionListProps = MangoDescriptionListCommonProps &
  ({ items: MangoDescriptionItem[]; groups?: never } | { groups: MangoDescriptionGroup[]; items?: never });
import type { Component } from 'vue';
