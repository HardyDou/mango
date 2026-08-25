import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import ChatMessageParts from './ChatMessageParts.vue';

describe('ChatMessageParts', () => {
  it('在同一消息中渲染文本、富文本、结构化结果和文件内容块', () => {
    const wrapper = mount(ChatMessageParts, {
      props: {
        parts: [
          { type: 'TEXT', text: '用户文本' },
          { type: 'RICH_TEXT', text: '## 富文本标题' },
          { type: 'STRUCTURED_DATA', dataJson: '{"status":"ok"}' },
          { type: 'IMAGE', fileId: '10', fileName: 'photo.png', contentType: 'image/png', fileSize: 3 },
        ],
      },
      global: {
        stubs: {
          ElButton: { template: '<button><slot /></button>' },
          ElIcon: { template: '<i><slot /></i>' },
          ChatFilePart: {
            props: ['part'],
            template: '<span class="file-part">{{ part.type }}:{{ part.fileName }}</span>',
          },
        },
      },
    });

    expect(wrapper.text()).toContain('用户文本');
    expect(wrapper.find('h2').text()).toBe('富文本标题');
    expect(wrapper.find('pre').text()).toContain('"status": "ok"');
    expect(wrapper.find('.file-part').text()).toBe('IMAGE:photo.png');
  });

  it('流式生成期间使用稳定文本节点，完成后再渲染 Markdown', async () => {
    const wrapper = mount(ChatMessageParts, {
      props: { parts: [{ type: 'RICH_TEXT', text: '## 正在生成' }], streaming: true },
    });

    expect(wrapper.find('.mango-ai-message-parts__stream-text').text()).toBe('## 正在生成');
    expect(wrapper.find('h2').exists()).toBe(false);

    await wrapper.setProps({ streaming: false });
    expect(wrapper.find('.mango-ai-message-parts__stream-text').exists()).toBe(false);
    expect(wrapper.find('h2').text()).toBe('正在生成');
  });
});
