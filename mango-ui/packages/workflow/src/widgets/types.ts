import type { MangoWidgetRuntimeContext } from '@mango/grid-widgets';

export interface MangoWidgetNavigateTarget {
  path?: string;
  name?: string;
  url?: string;
  pageType?: string;
  raw?: unknown;
}

export interface MyTodoWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  todoPath?: string;
}

export interface MyProcessWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  processPath?: string;
}

export interface MyTaskWidgetProps {
  runtime?: MangoWidgetRuntimeContext;
  taskPath?: string;
}
