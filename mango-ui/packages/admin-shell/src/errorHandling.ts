import type { App as VueApp, ComponentPublicInstance } from 'vue';
import { mangoMessage } from '@mango/common/utils/message';

const ELEMENT_PLUS_MESSAGE_BOX_CANCELLATIONS = new Set(['cancel', 'close']);

export function isElementPlusMessageBoxCancellation(error: unknown): boolean {
  if (typeof error === 'string') {
    return ELEMENT_PLUS_MESSAGE_BOX_CANCELLATIONS.has(error);
  }
  if (!error || typeof error !== 'object' || !('action' in error)) {
    return false;
  }
  return typeof error.action === 'string' && ELEMENT_PLUS_MESSAGE_BOX_CANCELLATIONS.has(error.action);
}

export function installShellErrorHandler(app: VueApp): void {
  app.config.errorHandler = (error, instance, info) => {
    if (isElementPlusMessageBoxCancellation(error)) {
      return;
    }
    reportVueError(error, instance, info);
  };
}

function reportVueError(error: unknown, instance: ComponentPublicInstance | null, info: string): void {
  console.error('[mango-shell] Vue error:', error);
  console.error('[mango-shell] component:', instance);
  console.error('[mango-shell] info:', info);
  if (error && typeof error === 'object' && 'response' in error) {
    return;
  }
  mangoMessage.error('系统错误，请刷新页面');
}
