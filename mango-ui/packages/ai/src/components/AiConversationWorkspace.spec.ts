import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import AiConversationWorkspace from './AiConversationWorkspace.vue';

const buttonStub = {
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
};

describe('AiConversationWorkspace', () => {
  it('通过独立组件契约呈现会话并派发会话动作', async () => {
    const session = {
      id: 'session-1',
      title: '合同五要素识别',
      persisted: true,
      messageCount: 2,
      messages: [
        {
          id: 'message-1',
          role: 'assistant' as const,
          contentParts: [{ type: 'RICH_TEXT' as const, text: '结果' }],
          modelName: 'gpt-5.6-sol',
          providerCode: 'openai-compatible',
          thinkingEnabled: true,
        },
      ],
    };
    const wrapper = mount(AiConversationWorkspace, {
      props: {
        conversations: [session],
        activeConversationId: 'session-1',
        activeConversation: session,
        sending: false,
        conversationLoading: false,
        welcomeTitle: '开始处理',
        welcomeDescription: '输入内容',
        suggestions: [],
      },
      slots: {
        message: '<span class="message-slot">消息内容</span>',
        composer: '<div class="composer-slot">输入器</div>',
      },
      global: {
        stubs: {
          ElButton: buttonStub,
          ElIcon: { template: '<i><slot /></i>' },
          ElTooltip: { template: '<span><slot /></span>' },
          ElTag: { template: '<span><slot /></span>' },
          ElSkeleton: true,
          ElDrawer: { template: '<aside><slot /></aside>' },
        },
      },
    });

    expect(wrapper.text()).toContain('合同五要素识别');
    expect(wrapper.text()).toContain('消息内容');
    expect(wrapper.text()).toContain('输入器');
    expect(wrapper.get('[data-state="ai.service-run.message-model"]').text()).toContain('gpt-5.6-sol');
    expect(wrapper.get('[data-state="ai.service-run.message-model"]').text()).toContain('深度思考');
    await wrapper.get('[data-action="ai.service-run.new-conversation"]').trigger('click');
    expect(wrapper.emitted('create')).toHaveLength(1);
    await wrapper.get('[data-record-key="session-1"]').trigger('click');
    expect(wrapper.emitted('select')).toEqual([['session-1']]);
  });

  it('支持收起并恢复桌面会话侧栏', async () => {
    const wrapper = mount(AiConversationWorkspace, {
      props: {
        conversations: [],
        activeConversationId: '',
        sending: false,
        conversationLoading: false,
        welcomeTitle: '有什么可以帮忙的？',
        welcomeDescription: '输入问题',
        suggestions: [],
      },
      slots: { composer: '<div />' },
      global: {
        stubs: {
          ElButton: buttonStub,
          ElIcon: { template: '<i><slot /></i>' },
          ElTooltip: { template: '<span><slot /></span>' },
          ElTag: { template: '<span><slot /></span>' },
          ElSkeleton: true,
          ElDrawer: { template: '<aside><slot /></aside>' },
        },
      },
    });

    expect(wrapper.find('[data-action="ai.conversation-workspace.expand"]').exists()).toBe(false);
    await wrapper.get('[data-action="ai.conversation-workspace.collapse"]').trigger('click');
    expect(wrapper.classes()).toContain('is-sidebar-collapsed');
    await wrapper.get('[data-action="ai.conversation-workspace.expand"]').trigger('click');
    expect(wrapper.classes()).not.toContain('is-sidebar-collapsed');
    expect(wrapper.find('[data-action="ai.conversation-workspace.expand"]').exists()).toBe(false);
  });

  it('发送后立即呈现可见的模型连接状态', () => {
    const session = {
      id: 'draft-1',
      title: '新对话',
      persisted: false,
      messageCount: 1,
      messages: [
        {
          id: 'assistant-1',
          role: 'assistant' as const,
          contentParts: [],
          generationStatus: 'connecting' as const,
        },
      ],
    };
    const wrapper = mount(AiConversationWorkspace, {
      props: {
        conversations: [session],
        activeConversationId: session.id,
        activeConversation: session,
        sending: true,
        conversationLoading: false,
        welcomeTitle: '开始处理',
        welcomeDescription: '输入内容',
        suggestions: [],
      },
      slots: { message: '<span />', composer: '<div />' },
      global: {
        stubs: {
          ElButton: buttonStub,
          ElIcon: { template: '<i><slot /></i>' },
          ElTooltip: { template: '<span><slot /></span>' },
          ElTag: { template: '<span><slot /></span>' },
          ElSkeleton: true,
          ElDrawer: { template: '<aside><slot /></aside>' },
        },
      },
    });

    expect(wrapper.get('[data-state="ai.service-run.connecting"]').text()).toContain('正在连接模型');
  });
});
