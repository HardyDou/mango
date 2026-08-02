import { defineComponent, isReactive } from 'vue';
import { describe, expect, it } from 'vitest';
import { getMangoAuthConfig, installMangoAuth } from '../config';

describe('Mango auth config', () => {
  it('keeps injected slot components outside the reactive proxy', () => {
    const theme = defineComponent({ name: 'TestTheme', template: '<div />' });

    installMangoAuth(undefined, {
      profile: {
        slots: { theme },
      },
    });

    const configuredTheme = getMangoAuthConfig().profile?.slots?.theme;
    expect(configuredTheme).toBe(theme);
    expect(isReactive(configuredTheme)).toBe(false);
  });
});
