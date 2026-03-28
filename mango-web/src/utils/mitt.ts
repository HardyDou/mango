import mitt, { Emitter, EventType } from 'mitt';

type Events = {
  'updataDate': void;
  'collapses': boolean;
  'mobile': boolean;
  'layoutMobile': boolean;
  'layoutMobileResize': { isMobile: boolean; windowWidth: number };
  'setIsThreeMenu': boolean;
  'openGlobalCollapse': boolean;
  'setSendColumnsChildren': any;
  'restoreDefault': void;
};

export const mittBus: Emitter<Events> = mitt<Events>();

// 挂载到 window 方便调试
if (typeof window !== 'undefined') {
  (window as any).__MANGO_MITT_BUS__ = mittBus;
}
