import { describe, expect, it } from 'vitest';
import {
  isBackendFileContentUrl,
  isFileDownloadEndpointUrl,
  isPreviewDisplayUrl,
} from '../previewUrl';

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

  it.each([
    '/api/file/files/preview-content?id=file-1',
    '/file/files/preview-content?id=file-1',
    '/api/file/local-objects/local/report.pdf',
    '/file/local-objects/local/report.pdf',
  ])('routes backend file content through an authenticated blob: %s', (url) => {
    expect(isBackendFileContentUrl(url)).toBe(true);
  });

  it('keeps external presigned storage urls available for direct access', () => {
    const presignedUrl = 'https://storage.example.com/files/report.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=signed';

    expect(isBackendFileContentUrl(presignedUrl)).toBe(false);
    expect(isPreviewDisplayUrl(presignedUrl)).toBe(true);
  });
});
