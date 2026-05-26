import { fileToken, normalizeFileId } from '../file';

describe('file references', () => {
  it('normalizes only backend-compatible file ids', () => {
    expect(normalizeFileId('123')).toBe('123');
    expect(normalizeFileId(' 123 ')).toBe('123');
    expect(normalizeFileId(123)).toBe('123');
    expect(normalizeFileId('mango-file:123')).toBe('123');
    expect(normalizeFileId('mango-file: 123 ')).toBe('123');
    expect(normalizeFileId({ id: '456' } as any)).toBe('456');
  });

  it('rejects empty values, icon names, text values and access urls', () => {
    expect(normalizeFileId()).toBe('');
    expect(normalizeFileId('')).toBe('');
    expect(normalizeFileId('0')).toBe('');
    expect(normalizeFileId('file')).toBe('');
    expect(normalizeFileId('CollectionTag')).toBe('');
    expect(normalizeFileId('mango-file:file')).toBe('');
    expect(normalizeFileId('/api/file/files/download?id=123')).toBe('');
    expect(normalizeFileId('https://example.com/icon.png')).toBe('');
  });

  it('does not create file tokens for invalid ids', () => {
    expect(fileToken('123')).toBe('mango-file:123');
    expect(fileToken('file')).toBe('');
    expect(fileToken('CollectionTag')).toBe('');
  });
});
