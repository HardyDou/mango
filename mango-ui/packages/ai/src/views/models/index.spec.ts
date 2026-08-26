import { describe, expect, it } from 'vitest';
import AiModelsView from './index.vue';

describe('AiModelsView', () => {
  it('暴露模型管理页面组件并使用稳定页面标识', () => {
    expect(AiModelsView).toBeDefined();
    expect(AiModelsView.__name).toBe('index');
  });
});
