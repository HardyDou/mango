import type { TagProps } from 'element-plus';

export interface MangoDetailSummaryTag {
  key: string;
  label: unknown;
  type?: TagProps['type'];
  effect?: TagProps['effect'];
}

export interface MangoDetailSummaryField {
  key: string;
  label: string;
  value: unknown;
}

export interface MangoDetailSummaryProps {
  dataSurface?: string;
  title?: unknown;
  tags?: MangoDetailSummaryTag[];
  fields?: MangoDetailSummaryField[];
}
