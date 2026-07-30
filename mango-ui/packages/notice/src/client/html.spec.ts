import { describe, expect, it } from 'vitest';
import { noticePlainText, sanitizeNoticeHtml } from './html';

describe('notice html', () => {
  it('保留基础格式并移除脚本、事件、样式和危险链接', () => {
    const result = sanitizeNoticeHtml(
      '<p onclick="alert(1)" style="color:red">请<strong>审批</strong>' +
        '<script>alert(2)</script><a href="javascript:alert(3)" target="_blank">详情</a></p>',
    );

    expect(result).toBe('<p>请<strong>审批</strong><a>详情</a></p>');
    expect(result).not.toContain('onclick');
    expect(result).not.toContain('javascript:');
    expect(result).not.toContain('script');
  });

  it('安全链接保留并为新窗口补充隔离属性', () => {
    expect(sanitizeNoticeHtml('<a href="https://example.com" target="_blank">查看</a>')).toBe(
      '<a href="https://example.com" target="_blank" rel="noopener noreferrer">查看</a>',
    );
    expect(sanitizeNoticeHtml('<a href="//evil.example/path">外部地址</a>')).toBe('<a>外部地址</a>');
  });

  it('标题可转换为无标签纯文本', () => {
    expect(noticePlainText('<strong>审批</strong> 通知')).toBe('审批 通知');
  });
});
