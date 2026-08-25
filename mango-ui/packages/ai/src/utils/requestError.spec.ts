import { describe, expect, it } from 'vitest';
import { isDialogCancellation, isRequestAborted, requestErrorMessage } from './requestError';

describe('AI request error helpers', () => {
  it('识别浏览器和 Mango HttpClient 的主动取消', () => {
    expect(isRequestAborted(new DOMException('aborted', 'AbortError'))).toBe(true);
    expect(isRequestAborted(Object.assign(new Error('Request aborted'), { name: 'HttpError', kind: 'aborted' }))).toBe(
      true,
    );
  });

  it('不把普通异常识别为取消并提供错误文案', () => {
    expect(isRequestAborted(new Error('network error'))).toBe(false);
    expect(isRequestAborted('aborted')).toBe(false);
    expect(requestErrorMessage(new Error('request failed'), '加载失败')).toBe('加载失败');
    expect(requestErrorMessage(null, '加载失败')).toBe('加载失败');
    expect(requestErrorMessage(new Error('Request failed with status code 500'), '模型暂时不可用')).toBe(
      '模型暂时不可用',
    );
    expect(requestErrorMessage(new Error('Network Error'), '网络连接失败')).toBe('网络连接失败');
  });

  it('只把 Element Plus 消息框的取消结果识别为用户取消', () => {
    expect(isDialogCancellation('cancel')).toBe(true);
    expect(isDialogCancellation('close')).toBe(true);
    expect(isDialogCancellation(new Error('delete failed'))).toBe(false);
  });
});
