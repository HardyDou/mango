import { describe, expect, it } from 'vitest';
import { buildEditMetadata, extensionOf, toBizMetaText, validateEditInput } from '../fileEdit';
import type { FileEditForm } from '../fileEdit';

function form(overrides: Partial<FileEditForm> = {}): FileEditForm {
  return {
    fileName: 'report.docx',
    purpose: ' attachment ',
    accessLevel: 'PRIVATE',
    bizType: ' guarantee ',
    bizId: ' B-1 ',
    bizMeta: '{"contract":"A-1"}',
    directoryId: '0',
    ...overrides,
  };
}

describe('文件编辑表单', () => {
  it('buildEditMetadata 去除首尾空格并保留目录标识', () => {
    const metadata = buildEditMetadata(form());

    expect(metadata).toEqual({
      fileName: 'report.docx',
      purpose: 'attachment',
      accessLevel: 'PRIVATE',
      bizType: 'guarantee',
      bizId: 'B-1',
      bizMeta: '{"contract":"A-1"}',
      directoryId: '0',
    });
  });

  it('extensionOf 统一转小写并处理无扩展名', () => {
    expect(extensionOf('report.DOCX')).toBe('docx');
    expect(extensionOf('report')).toBe('');
    expect(extensionOf('report.')).toBe('');
  });

  it('toBizMetaText 把对象序列化为可编辑 JSON', () => {
    expect(toBizMetaText({ contract: 'A-1' })).toContain('"contract"');
    expect(toBizMetaText(undefined)).toBe('');
  });

  it('validateEditInput 拒绝扩展名变化', () => {
    const metadata = buildEditMetadata(form({ fileName: 'report.pdf' }));

    expect(validateEditInput(metadata, 'docx')).toBe('文件名扩展名必须与原文件一致（docx）');
  });

  it('validateEditInput 拒绝非法业务参数 JSON', () => {
    const metadata = buildEditMetadata(form({ bizMeta: '{oops' }));

    expect(validateEditInput(metadata, 'docx')).toBe('业务参数必须是合法 JSON');
  });

  it('validateEditInput 拒绝超过大小限制的新文件', () => {
    const metadata = buildEditMetadata(form());

    expect(validateEditInput(metadata, 'docx', { newFileSize: 20, maxSize: 10 })).toBe('新文件大小超过限制');
  });

  it('validateEditInput 在信息合法时返回 null', () => {
    const metadata = buildEditMetadata(form());

    expect(validateEditInput(metadata, 'docx', { newFileSize: 5, maxSize: 10 })).toBeNull();
  });
});
