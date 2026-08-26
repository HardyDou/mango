import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import ChatMessageContent from './ChatMessageContent.vue';

describe('ChatMessageContent', () => {
  it('渲染标题、列表、代码和真实换行', () => {
    const wrapper = mount(ChatMessageContent, {
      props: { content: '## 能力\n\n- 分析\n- 总结\n\n```json\n{"ok":true}\n```' },
    });

    expect(wrapper.find('h2').text()).toBe('能力');
    expect(wrapper.findAll('li').map((item) => item.text())).toEqual(['分析', '总结']);
    expect(wrapper.find('code').text()).toContain('{"ok":true}');
  });

  it('不执行模型输出中的 HTML 或危险链接', () => {
    const wrapper = mount(ChatMessageContent, {
      props: { content: '<script>alert(1)</script> [危险](javascript:alert(1))' },
    });

    expect(wrapper.find('script').exists()).toBe(false);
    expect(wrapper.html()).not.toContain('href="javascript:');
  });
});
