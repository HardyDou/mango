import { defaultFileSettings } from '../fileSettings';

describe('file settings defaults', () => {
  it('allows duplicate uploads by auto-renaming files by default', () => {
    expect(defaultFileSettings.duplicateNameStrategy).toBe('AUTO_RENAME');
    expect(defaultFileSettings.duplicateCheckDirectoryScoped).toBe(true);
  });
});
