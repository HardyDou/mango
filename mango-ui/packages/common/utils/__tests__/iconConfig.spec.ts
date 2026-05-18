import { describe, expect, it } from 'vitest';
import { getIcon, iconMap, iconNames } from '../iconConfig';

describe('iconConfig', () => {
  it('falls back to all Element Plus icons for menu icon names', () => {
    expect(getIcon('Monitor')).toBeTruthy();
    expect(getIcon('DocumentCopy')).toBeTruthy();
    expect(iconMap.Monitor).toBeTruthy();
    expect(iconMap.DocumentCopy).toBeTruthy();
    expect(iconNames).toContain('Monitor');
    expect(iconNames).toContain('DocumentCopy');
  });
});
