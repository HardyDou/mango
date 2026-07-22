import { describe, expect, it } from 'vitest';
import { managedImageToken, renderManagedHtml, serializeManagedHtml } from '../managedImages';

describe('managedImages', () => {
  it('对外只序列化为 mango-file token，并移除非托管图片', () => {
    const result = serializeManagedHtml(
      '<p><img src="https://preview.example/image.png" data-file-id="123"></p>' +
        '<img src="data:image/png;base64,abc">' +
        '<img src="https://third-party.example/image.png">',
    );

    expect(result.invalidImageCount).toBe(2);
    expect(result.html).toContain('src="mango-file:123"');
    expect(result.html).not.toContain('data:image');
    expect(result.html).not.toContain('third-party.example');
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
});
