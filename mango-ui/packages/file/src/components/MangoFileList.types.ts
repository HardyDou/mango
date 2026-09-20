export interface MangoFileListFile {
  key?: string | number;
  fileId?: string | number;
  fileName?: string;
  fileUrl?: string;
  fileSize?: number;
  version?: string | number;
  uploadedAt?: string;
}

export interface MangoFileListRow extends Record<string, unknown> {
  key: string | number;
}

export type MangoFileListColumnType = 'text' | 'files' | 'custom';
export type MangoFileListFileMeta = 'fileSize' | 'version' | 'uploadedAt';

export interface MangoFileListCellStyle {
  backgroundColor?: string;
  borderRight?: string;
  color?: string;
  fontSize?: string | number;
  fontWeight?: string | number;
  height?: string | number;
  lineHeight?: string | number;
  padding?: string;
  textAlign?: 'left' | 'center' | 'right';
}

export interface MangoFileListTableStyle {
  headerCellStyle?: MangoFileListCellStyle;
  bodyCellStyle?: MangoFileListCellStyle;
  borderRadius?: string | number;
  contentMinHeight?: string | number;
  contentPadding?: string;
  contentLineHeight?: string | number;
}

export interface MangoFileListColumn {
  key: string;
  label: string;
  prop?: string;
  type?: MangoFileListColumnType;
  width?: number | string;
  minWidth?: number | string;
  align?: 'left' | 'center' | 'right';
  fixed?: boolean | 'left' | 'right';
  showOverflowTooltip?: boolean;
  emptyText?: string;
  mergeAdjacent?: boolean;
  mergeBy?: string[];
  slot?: string;
  fileActionsSlot?: string;
  fileMeta?: MangoFileListFileMeta[];
  showPreview?: boolean;
  showDownload?: boolean;
}

export interface MangoFileListProps {
  rows: MangoFileListRow[];
  columns: MangoFileListColumn[];
  emptyText?: string;
  selectable?: boolean;
  selectableWithoutFile?: boolean;
  mergeAdjacentRows?: boolean;
  tableStyle?: MangoFileListTableStyle;
}

export interface MangoFileListCellSlotProps {
  row: MangoFileListRow;
  rowIndex: number;
  column: MangoFileListColumn;
  value: unknown;
  displayValue: string;
}

export interface MangoFileListFileActionsSlotProps {
  row: MangoFileListRow;
  rowIndex: number;
  column: MangoFileListColumn;
  file: MangoFileListFile;
  fileIndex: number;
  hasFile: boolean;
}
