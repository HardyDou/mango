import { describe, expect, it } from 'vitest';
import { attachmentSupport, contentPartType, validateAttachment } from './attachmentSupport';

describe('AI 会话附件能力', () => {
  it('只向文件选择器暴露当前模型真实支持的格式', () => {
    const support = attachmentSupport(['TEXT', 'IMAGE', 'FILE']);

    expect(support.labels).toEqual(['图片', 'PDF', '文本文件']);
    expect(support.accept).toContain('image/*');
    expect(support.accept).toContain('application/pdf');
    expect(support.accept).not.toContain('video/*');
    expect(support.accept).not.toContain('audio/');
  });

  it('上传前拒绝模型不支持的视频且不把未知格式降级为普通文件', () => {
    const video = new File(['video'], 'demo.mp4', { type: 'video/mp4' });
    const executable = new File(['bin'], 'unknown.bin', { type: 'application/octet-stream' });

    expect(validateAttachment(video, ['TEXT', 'IMAGE'], 0)).toMatchObject({
      accepted: false,
      message: expect.stringContaining('不支持视频输入'),
    });
    expect(contentPartType(executable)).toBeUndefined();
  });

  it('上传前执行单文件20MB和单条消息40MB边界', () => {
    const large = new File([new Uint8Array(20 * 1024 * 1024 + 1)], 'large.pdf', { type: 'application/pdf' });
    const pdf = new File([new Uint8Array(2 * 1024 * 1024)], 'next.pdf', { type: 'application/pdf' });

    expect(validateAttachment(large, ['FILE'], 0).message).toContain('超过20MB');
    expect(validateAttachment(pdf, ['FILE'], 39 * 1024 * 1024).message).toContain('总大小不能超过40MB');
  });
});
