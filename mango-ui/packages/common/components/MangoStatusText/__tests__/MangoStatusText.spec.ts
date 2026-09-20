import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import MangoStatusText from '../index.vue';
import type { MangoStatusTone } from '../types';

describe('MangoStatusText', () => {
  it.each<MangoStatusTone>(['primary', 'success', 'warning', 'danger', 'info', 'neutral'])(
    'renders the %s semantic tone without deriving business text',
    (tone) => {
      const wrapper = mount(MangoStatusText, {
        props: { tone },
        slots: { default: '消费方文案' },
      });

      expect(wrapper.text()).toBe('消费方文案');
      expect(wrapper.classes()).toContain(`mango-status-text--${tone}`);
    },
  );

  it('defaults to the neutral theme tone', () => {
    const wrapper = mount(MangoStatusText, {
      slots: { default: '未指定状态' },
    });

    expect(wrapper.classes()).toContain('mango-status-text--neutral');
  });

  it('declares overridable status variables in every public theme profile', () => {
    const variableNames = [
      '--mango-color-status-primary',
      '--mango-color-status-success',
      '--mango-color-status-warning',
      '--mango-color-status-danger',
      '--mango-color-status-info',
      '--mango-color-status-neutral',
    ];
    const themeFiles = [
      '../../../theme/index.css',
      '../../../theme/light.scss',
      '../../../theme/dark.scss',
      '../../../theme/admin-standard.css',
      '../../../theme/admin-compact.css',
    ];

    themeFiles.forEach((themeFile) => {
      const source = readFileSync(new URL(themeFile, import.meta.url), 'utf8');
      variableNames.forEach((variableName) => expect(source).toContain(variableName));
    });

    const componentSource = readFileSync(resolve(process.cwd(), 'components/MangoStatusText/index.vue'), 'utf8');
    expect(componentSource).not.toMatch(/color:\s*#[\da-f]{3,8}/i);
  });
});
