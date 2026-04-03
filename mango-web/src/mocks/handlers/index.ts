/**
 * Mock Handlers Index - 汇总所有 mock handlers
 */

import { authHandlers } from './auth';
import { dictHandlers } from './dict';
import { captchaHandlers } from './captcha';
import { systemHandlers } from './system';
import { businessHandlers } from './business';

export const handlers = [
  ...authHandlers,
  ...dictHandlers,
  ...captchaHandlers,
  ...systemHandlers,
  ...businessHandlers,
];
