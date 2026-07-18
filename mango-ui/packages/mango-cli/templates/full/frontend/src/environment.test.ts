import { describe, expect, it } from 'vitest';
import { splitEnvList } from './environment';

describe('splitEnvList', () => {
  it('normalizes comma-separated runtime allowlists', () => {
    expect(splitEnvList(' https://a.example , ,https://b.example ')).toEqual([
      'https://a.example',
      'https://b.example',
    ]);
  });

  it('returns an empty allowlist when the setting is absent', () => {
    expect(splitEnvList()).toEqual([]);
  });
});
