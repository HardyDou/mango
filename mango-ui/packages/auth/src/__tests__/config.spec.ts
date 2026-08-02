import { defineComponent, isReactive } from 'vue';
import { describe, expect, it } from 'vitest';
import {
  getMangoAuthConfig,
  getMangoAuthProfileSections,
  installMangoAuth,
  registerMangoAuthProfileSections,
} from '../config';

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

  it('registers profile sections with raw components', () => {
    const page = { name: 'NoticeProfileSection', template: '<div />' };

    registerMangoAuthProfileSections([
      { key: 'notice-test-section', label: '测试消息页', group: '消息中心', component: page },
    ]);

    const section = getMangoAuthProfileSections().value.find((item) => item.key === 'notice-test-section');
    expect(section?.component).toBe(page);
    expect(isReactive(section?.component)).toBe(false);
  });
});
