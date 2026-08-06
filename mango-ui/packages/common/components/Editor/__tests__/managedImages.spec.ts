import { describe, expect, it } from 'vitest';
import {
  collectManagedFileIds,
  managedAttachmentHtml,
  managedImageToken,
  renderManagedHtml,
  serializeManagedHtml,
} from '../managedImages';

describe('managedImages', () => {
  it('只序列化托管节点，并保留用户写入的图片地址', () => {
    const result = serializeManagedHtml(
      '<p><img src="https://preview.example/image.png" data-file-id="123"></p>' +
        '<img src="data:image/png;base64,abc">' +
        '<img src="https://third-party.example/image.png">',
    );

    expect(result.invalidAssetCount).toBe(0);
    expect(result.html).toContain('src="mango-file:123"');
    expect(result.html).toContain('data:image');
    expect(result.html).toContain('third-party.example');
  });

  it('拒绝 data-file-id 与 token 不一致的图片', () => {
    const result = serializeManagedHtml('<img src="mango-file:123" data-file-id="456">');

    expect(result.invalidImageCount).toBe(1);
    expect(result.html).toBe('');
  });

  it('编辑态可用 preview URL 回显，但再次序列化仍只输出 token', () => {
    const previewIds = new Map<string, string>();
    const html = renderManagedHtml(
      `<p><img src="${managedImageToken('123')}" alt="photo"></p>`,
      new Map([['123', 'https://preview.example/image.png']]),
      previewIds,
    );

    expect(html).toContain('src="https://preview.example/image.png"');
    expect(serializeManagedHtml(html, previewIds).html).toContain('src="mango-file:123"');
  });

  it('附件编辑态使用预览地址，持久化时只保存文件 token', () => {
    const html = managedAttachmentHtml('456', '/api/file/files/preview-content?id=456', '报告.pdf');
    const serialized = serializeManagedHtml(html).html;

    expect(serialized).toContain('href="mango-file:456"');
    expect(serialized).toContain('data-file-kind="attachment"');
    expect(serialized).toContain('target="_blank"');
    expect(serialized).toContain('报告.pdf');
    expect(collectManagedFileIds(serialized)).toEqual(['456']);

    const rendered = renderManagedHtml(
      serialized,
      new Map([
        [
          '456',
          {
            url: '/api/file/files/download?id=456',
            previewUrl: '/api/file/files/preview-content?id=456',
            downloadUrl: '/api/file/files/download?id=456',
          },
        ],
      ]),
    );
    expect(rendered).toContain('href="/api/file/files/preview-content?id=456"');
  });
});
