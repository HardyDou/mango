import type { MangoWidgetRuntimeContext } from '@mango/grid-widgets';

export interface MessageCenterCategory {
  key: string;
  label: string;
  bizGroup?: string;
  bizType?: string;
  priority?: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  color?: string;
}

export interface MessageCenterWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  messageCenterPath?: string;
  pageSize?: number;
  categories?: MessageCenterCategory[];
}
