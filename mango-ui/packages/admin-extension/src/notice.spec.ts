import { afterEach, describe, expect, it } from 'vitest';
import {
  getMangoNoticeNavigationProvider,
  registerMangoNoticeNavigationProvider,
  resetMangoNoticeNavigationProvider,
} from './notice';

afterEach(() => {
  resetMangoNoticeNavigationProvider();
});

describe('notice navigation provider', () => {
  it('registers and reads the host target resolver', () => {
    const provider = { resolveTargetPath: (targetKey: string) => targetKey };

    registerMangoNoticeNavigationProvider(provider);

    expect(getMangoNoticeNavigationProvider()).toBe(provider);
  });

  it('resets the host target resolver', () => {
    registerMangoNoticeNavigationProvider({ resolveTargetPath: () => '/target' });

    resetMangoNoticeNavigationProvider();

    expect(getMangoNoticeNavigationProvider()).toBeUndefined();
  });
});
