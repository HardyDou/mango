import mitt, { Emitter } from 'mitt';

interface ColumnChildren {
  id?: number | string;
  path?: string;
  title?: string;
  [key: string]: unknown;
}

type Events = {
  'updataDate': void;
  'collapses': boolean;
  'mobile': boolean;
  'layoutMobile': boolean;
  'layoutMobileResize': { isMobile: boolean; windowWidth: number; layout?: 'defaults' | 'classic' | 'transverse' | 'columns' };
  'setIsThreeMenu': boolean;
  'openGlobalCollapse': boolean;
  'setSendColumnsChildren': ColumnChildren;
  'restoreDefault': void;
};

export const mittBus: Emitter<Events> = mitt<Events>();

// 挂载到 window 方便调试
if (typeof window !== 'undefined') {
  (window as Window & { __MANGO_MITT_BUS__?: Emitter<Events> }).__MANGO_MITT_BUS__ = mittBus;
}
