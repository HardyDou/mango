import { describe, expect, it } from 'vitest';

import { defaultFileSettings } from '../fileSettings';

describe('file settings defaults', () => {
  it('allows duplicate uploads by auto-renaming files by default', () => {
    expect(defaultFileSettings.duplicateNameStrategy).toBe('AUTO_RENAME');
    expect(defaultFileSettings.duplicateCheckDirectoryScoped).toBe(true);
    expect(defaultFileSettings.multipartEnabled).toBe(true);
    expect(defaultFileSettings.multipartThreshold).toBe(20 * 1024 * 1024);
  });
});
