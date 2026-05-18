import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mangoMessage } from '../message';

const mocks = vi.hoisted(() => {
  const close = vi.fn();
  return {
    close,
    elMessage: vi.fn(() => ({ close })),
  };
});

vi.mock('element-plus', () => ({
  ElMessage: mocks.elMessage,
}));

describe('mangoMessage', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
  });

  it('merges duplicate messages in the same time window', () => {
    mangoMessage.error('系统错误，请刷新页面');
    mangoMessage.error('系统错误，请刷新页面');
    mangoMessage.error('系统错误，请刷新页面');

    expect(mocks.elMessage).toHaveBeenCalledTimes(3);
    expect(mocks.close).toHaveBeenCalledTimes(2);
    expect(mocks.elMessage).toHaveBeenLastCalledWith({
      type: 'error',
      message: '系统错误，请刷新页面（3次）',
    });
  });

  it('allows the same message again after the time window', () => {
    mangoMessage.error('系统错误，请刷新页面');
    vi.advanceTimersByTime(1600);
    mangoMessage.error('系统错误，请刷新页面');

    expect(mocks.elMessage).toHaveBeenCalledTimes(2);
    expect(mocks.elMessage).toHaveBeenLastCalledWith({
      type: 'error',
      message: '系统错误，请刷新页面',
    });
  });

  it('keeps only the latest visible message when different messages arrive together', () => {
    mangoMessage.error('系统错误，请刷新页面');
    mangoMessage.warning('路由加载失败，请重新登录');

    expect(mocks.close).toHaveBeenCalledTimes(1);
    expect(mocks.elMessage).toHaveBeenCalledTimes(2);
    expect(mocks.elMessage).toHaveBeenLastCalledWith({
      type: 'warning',
      message: '路由加载失败，请重新登录',
    });
  });
});
