import { claimUnhandledError } from '@mango/common/utils/errorHandling';
import { mangoMessage } from '@mango/common/utils/message';

export function reportUnhandledError(error: unknown): void {
  if (!claimUnhandledError(error)) {
    return;
  }
  mangoMessage.error('系统错误，请刷新页面');
}
