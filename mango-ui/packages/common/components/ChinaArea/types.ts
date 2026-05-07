/**
 * ChinaArea Component Types
 */

export interface AreaNode {
  /** 数据库ID */
  id: number;
  /** 名称 */
  name: string;
  /** 父节点ID，0表示根节点 */
  parentId: number;
  /** 层级: 1-省, 2-市, 3-区/县, 4-街道 */
  level?: number;
  /** 是否热门: 1-是, 0-否 */
  hot?: string;
  /** 子节点 */
  children?: AreaNode[];
  /** 是否叶子节点 */
  leaf?: boolean;
}

export interface ChinaAreaProps {
  /** v-model binding - selected area id list */
  modelValue?: number[];
  /** Placeholder text */
  placeholder?: string;
  /** Linkage level: 3=province/city/district, 4=province/city/district/street */
  level?: number;
  /** Whether to show the full selected path */
  showAllLevels?: boolean;
  /** Whether to show hot cities first */
  showHot?: boolean;
  /** Disabled state */
  disabled?: boolean;
  /** Clearable */
  clearable?: boolean;
  /** Filterable - search functionality */
  filterable?: boolean;
  /** Collapse tags for multiple selection */
  collapseTags?: boolean;
  /** Separator for multiple values */
  separator?: string;
}

export interface ChinaAreaEmits {
  (e: 'update:modelValue', value: number[]): void;
  (e: 'change', value: number[]): void;
}

export interface ChinaAreaExpose {
  /** Get current selected area ids */
  getValue(): number[];
  /** Clear selection */
  clear(): void;
  /** Clear node cache to force reload */
  clearNodeCache(parentId: number): void;
}

export type ChinaAreaInstance = InstanceType<typeof import('./index.vue').default>;
