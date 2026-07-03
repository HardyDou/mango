import { describe, expect, it } from 'vitest';
import { isFileDownloadEndpointUrl, isPreviewDisplayUrl } from '../previewUrl';

describe('file preview url', () => {
  it('rejects file download endpoints as preview display urls', () => {
    expect(isPreviewDisplayUrl('/api/file/files/download?id=1')).toBe(false);
    expect(isPreviewDisplayUrl('/file/files/download?id=1')).toBe(false);
    expect(isPreviewDisplayUrl('https://example.com/api/file/files/download?id=1')).toBe(false);
  });

  it('keeps actual preview urls available for inline preview', () => {
    expect(isPreviewDisplayUrl('/api/file-preview/files/preview-entry?token=abc')).toBe(true);
    expect(isPreviewDisplayUrl('/preview/image.png')).toBe(true);
    expect(isPreviewDisplayUrl('blob:http://localhost/preview')).toBe(true);
  });

  it('recognizes only the file download endpoint path', () => {
    expect(isFileDownloadEndpointUrl('/api/file/files/download?id=1')).toBe(true);
    expect(isFileDownloadEndpointUrl('/api/file/files/download-history?id=1')).toBe(false);
  });
});
