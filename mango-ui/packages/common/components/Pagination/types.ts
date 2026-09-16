export type PaginationAlign = 'left' | 'center' | 'right';

export interface PaginationChange {
  page: number;
  limit: number;
}

export interface PaginationProps {
  total?: number;
  page?: number;
  limit?: number;
  pageSizes?: number[];
  layout?: string;
  background?: boolean;
  pagerCount?: number;
  small?: boolean;
  disabled?: boolean;
  align?: PaginationAlign;
}

export interface PaginationEmits {
  (event: 'update:page', value: number): void;
  (event: 'update:limit', value: number): void;
  (event: 'pagination', value: PaginationChange): void;
}
