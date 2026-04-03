/**
 * OrgSelector Component Types
 */

export interface OrgNode {
  id: number;
  name: string;
  parentId: number;
  sort?: number;
  children?: OrgNode[];
}

export interface OrgSelectorProps {
  /** v-model binding - selected organization IDs */
  modelValue?: number[];
  /** Placeholder text */
  placeholder?: string;
  /** Dialog title */
  title?: string;
  /** Whether to show organization name in tags */
  showTagNames?: boolean;
  /** Maximum number of selections (0 = unlimited) */
  max?: number;
  /** Disabled state */
  disabled?: boolean;
  /** Dialog width */
  width?: string | number;
}

export interface OrgSelectorEmits {
  (e: 'update:modelValue', value: number[]): void;
  (e: 'change', value: number[]): void;
}

export interface OrgSelectorExpose {
  /** Open the dialog */
  open(): void;
  /** Close the dialog */
  close(): void;
  /** Get selected organization IDs */
  getValue(): number[];
  /** Clear selection */
  clear(): void;
}

export type OrgSelectorInstance = InstanceType<typeof import('./index.vue').default>;
