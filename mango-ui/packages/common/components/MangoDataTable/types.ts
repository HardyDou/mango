import type { TableInstance } from 'element-plus';
import type { PaginationAlign } from '../Pagination/types';
import type { MangoStatusTone } from '../MangoStatusText/types';

export type MangoTableMode = 'flat' | 'expand';
export type MangoTableExpandType = 'list';
export type MangoTableColumnType = 'text' | 'status' | 'tag' | 'button' | 'input' | 'select' | 'radio' | 'custom';
export type MangoTableOptionValue = string | number | boolean;
export type MangoTableActionTone = Exclude<MangoStatusTone, 'neutral'>;

export interface MangoTableHeaderCellStyle {
  backgroundColor?: string;
  color?: string;
  fontSize?: string | number;
  fontWeight?: string | number;
  height?: string | number;
  padding?: string;
  textAlign?: 'left' | 'center' | 'right';
}

export type MangoTableProps = Record<string, unknown> & {
  headerCellStyle?: never;
  'header-cell-style'?: never;
};

export interface MangoTableOption {
  label: string;
  value: MangoTableOptionValue;
  disabled?: boolean;
  tone?: MangoStatusTone;
}

export interface MangoTableRowContext<Row extends object> {
  row: Row;
  rowIndex: number;
}

export interface MangoTableCellContext<Row extends object> extends MangoTableRowContext<Row> {
  column: MangoTableColumn<Row>;
  field: string;
  value: unknown;
}

export interface MangoTableCellChangeContext<Row extends object> extends MangoTableCellContext<Row> {
  previousValue: unknown;
}

export interface MangoTableActionContext<Row extends object> extends MangoTableRowContext<Row> {
  key: string;
  tableKey?: string;
}

export type MangoTableCondition<Row extends object> = boolean | ((context: MangoTableRowContext<Row>) => boolean);
export type MangoTableCellCondition<Row extends object> = boolean | ((context: MangoTableCellContext<Row>) => boolean);

export interface MangoTableColumn<Row extends object> {
  field: string;
  label: string;
  type?: MangoTableColumnType;
  width?: number | string;
  minWidth?: number | string;
  fixed?: boolean | 'left' | 'right';
  align?: 'left' | 'center' | 'right';
  sortable?: boolean;
  resizable?: boolean;
  expandable?: boolean;
  hidden?: boolean;
  showOverflowTooltip?: boolean;
  emptyText?: string;
  options?: MangoTableOption[];
  loading?: boolean;
  slot?: string;
  props?: Record<string, unknown>;
  formatter?: (context: MangoTableCellContext<Row>) => unknown;
  disabled?: MangoTableCellCondition<Row>;
}

export interface MangoTableAction<Row extends object> {
  key: string;
  text: string;
  tone?: MangoTableActionTone;
  visible?: MangoTableCondition<Row>;
  disabled?: MangoTableCondition<Row>;
  loading?: MangoTableCondition<Row>;
  props?: Record<string, unknown>;
}

export interface MangoTableOperation<Row extends object> {
  visible?: boolean;
  label?: string;
  width?: number | string;
  minWidth?: number | string;
  fixed?: 'left' | 'right';
  align?: 'left' | 'center' | 'right';
  resizable?: boolean;
  moreCount?: number;
  actions: MangoTableAction<Row>[];
}

export interface MangoTablePagination {
  visible?: boolean;
  page: number;
  limit: number;
  total: number;
  pageSizes?: number[];
  align?: PaginationAlign;
  layout?: string;
  props?: Record<string, unknown>;
}

export interface MangoTableExpand {
  type?: MangoTableExpandType;
  labelWidth?: number | string;
  emptyText?: string;
}

export interface MangoTablePageChangeContext {
  page: number;
  limit: number;
}

export interface MangoDataTableProps<Row extends object> {
  rows: Row[];
  columns: MangoTableColumn<Row>[];
  rowKey: Extract<keyof Row, string> | ((row: Row) => string);
  tableKey?: string;
  loading?: boolean;
  error?: string;
  errorTitle?: string;
  retryText?: string;
  retryable?: boolean;
  emptyText?: string;
  card?: boolean;
  headerCellStyle?: MangoTableHeaderCellStyle;
  resizable?: boolean;
  mode?: MangoTableMode;
  showModeSwitch?: boolean;
  flatModeLabel?: string;
  expandModeLabel?: string;
  expand?: MangoTableExpand;
  showIndex?: boolean;
  showSelection?: boolean;
  selectable?: (row: Row, index: number) => boolean;
  tableProps?: MangoTableProps;
  operation?: MangoTableOperation<Row>;
  pagination?: MangoTablePagination;
  onCellChange?: (context: MangoTableCellChangeContext<Row>) => void;
  onAction?: (context: MangoTableActionContext<Row>) => void;
  onPageChange?: (context: MangoTablePageChangeContext) => void;
  onSelectionChange?: (rows: Row[]) => void;
  onModeChange?: (mode: MangoTableMode) => void;
  onRetry?: () => void;
}

export interface MangoDataTableEmits<Row extends object> {
  (event: 'cell-change', context: MangoTableCellChangeContext<Row>): void;
  (event: 'action', context: MangoTableActionContext<Row>): void;
  (event: 'page-change', context: MangoTablePageChangeContext): void;
  (event: 'selection-change', rows: Row[]): void;
  (event: 'update:mode', mode: MangoTableMode): void;
  (event: 'mode-change', mode: MangoTableMode): void;
  (event: 'retry'): void;
}

export interface MangoDataTableExpose<Row extends object> {
  tableRef: TableInstance | undefined;
  clearSelection: () => void;
  toggleRowSelection: (row: Row, selected?: boolean) => void;
  toggleRowExpansion: (row: Row, expanded?: boolean) => void;
}
