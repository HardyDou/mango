import { describe, expect, it } from 'vitest';

import { DATE_FORMAT_PRESETS, dateExample } from './dateFormat';

describe('numgen date format editor', () => {
  it('offers the business formats including MMdd', () => {
    expect(DATE_FORMAT_PRESETS).toContain('MMdd');
    expect(DATE_FORMAT_PRESETS).toContain('yyyy');
  });

  it('renders MMdd and custom date patterns in the page example', () => {
    expect(dateExample('MMdd')).toBe('0523');
    expect(dateExample('yyyy年MMdd日')).toBe('2026年0523日');
    expect(dateExample('yyyyMMddHHmmss')).toBe('20260523143059');
  });

  it('returns an empty example until a format is entered', () => {
    expect(dateExample()).toBe('');
    expect(dateExample('  ')).toBe('');
  });
});
