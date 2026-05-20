/**
 * OrgSelector Component Types
 */

import type { ApiId } from '@mango/api-schema';

export interface OrgNode {
  id: ApiId;
  name: string;
  parentId: ApiId;
  sort?: number;
  children?: OrgNode[];
}

export interface OrgSelectorProps {
  /** v-model binding - selected organization IDs */
  modelValue?: ApiId | ApiId[];
  /** Whether multiple selection is enabled */
  multiple?: boolean;
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
  (e: 'update:modelValue', value: ApiId | ApiId[] | undefined): void;
  (e: 'change', value: ApiId | ApiId[] | undefined): void;
}

export interface OrgSelectorExpose {
  /** Open the dialog */
  open(): void;
  /** Close the dialog */
  close(): void;
  /** Get selected organization IDs */
  getValue(): ApiId[];
  /** Clear selection */
  clear(): void;
}

export type OrgSelectorInstance = InstanceType<typeof import('./index.vue').default>;
