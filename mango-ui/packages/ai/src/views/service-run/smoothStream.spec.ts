import { describe, expect, it } from 'vitest';
import {
  createSmoothTextStream,
  finalTextRemainder,
  streamSliceSize,
  type StreamFrameScheduler,
} from './smoothStream';

function controlledScheduler() {
  const frames = new Map<number, FrameRequestCallback>();
  let sequence = 0;
  const scheduler: StreamFrameScheduler = {
    request(callback) {
      sequence += 1;
      frames.set(sequence, callback);
      return sequence;
    },
    cancel(handle) {
      frames.delete(handle);
    },
  };
  return {
    scheduler,
    next() {
      const entry = frames.entries().next().value as [number, FrameRequestCallback] | undefined;
      if (!entry) return false;
      frames.delete(entry[0]);
      entry[1](performance.now());
      return true;
    },
    drain() {
      while (this.next()) continue;
    },
    pendingFrames: () => frames.size,
  };
}

describe('smoothStream', () => {
  it('把突发网络内容拆成连续帧，并在完成时排空全部文本', async () => {
    const frames = controlledScheduler();
    const updates: string[] = [];
    const stream = createSmoothTextStream((text) => updates.push(text), { scheduler: frames.scheduler });

    stream.push('这是一段由上游一次性返回的较长内容');
    expect(updates).toEqual([]);
    frames.next();
    expect(updates[0].length).toBeGreaterThan(0);
    expect(updates[0]).not.toBe('这是一段由上游一次性返回的较长内容');

    const completed = stream.complete();
    frames.drain();
    await completed;
    expect(updates.at(-1)).toBe('这是一段由上游一次性返回的较长内容');
  });

  it('取消后不再呈现尚未显示的内容', () => {
    const frames = controlledScheduler();
    const updates: string[] = [];
    const stream = createSmoothTextStream((text) => updates.push(text), { scheduler: frames.scheduler });

    stream.push('不会显示');
    stream.cancel();
    frames.drain();

    expect(updates).toEqual([]);
    expect(frames.pendingFrames()).toBe(0);
  });

  it('减少动态效果时立即呈现完整网络块', async () => {
    const frames = controlledScheduler();
    const updates: string[] = [];
    const stream = createSmoothTextStream((text) => updates.push(text), {
      reducedMotion: true,
      scheduler: frames.scheduler,
    });

    stream.push('立即显示');
    await stream.complete();

    expect(updates).toEqual(['立即显示']);
    expect(frames.pendingFrames()).toBe(0);
  });

  it('根据积压量和收尾状态自适应每帧输出量', () => {
    expect(streamSliceSize(4, false)).toBe(1);
    expect(streamSliceSize(40, false)).toBe(2);
    expect(streamSliceSize(300, false)).toBe(3);
    expect(streamSliceSize(300, true)).toBe(3);
    expect(streamSliceSize(800, true)).toBe(4);
  });

  it('供应商只在完成事件返回正文时仍把最终文本送入显示缓冲', () => {
    expect(finalTextRemainder('', '完整回答')).toBe('完整回答');
    expect(finalTextRemainder('已经收到', '已经收到完整回答')).toBe('完整回答');
    expect(finalTextRemainder('供应商原始文本', '服务端转换结果')).toBe('');
  });
});
