import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import NoticeDetailDialog from '../NoticeDetailDialog.vue';

const message = {
  id: 'message-1',
  title: '流程已完成：费用报销',
  content: '流程费用报销已完成。',
  userId: '1001',
  priority: 'NORMAL' as const,
  readStatus: 'UNREAD' as const,
  bizName: '流程完成',
  bizType: 'workflow.process.completed',
  createTime: '2026-07-30 10:00:00',
  subject: { subjectName: '费用报销' },
  data: { businessKey: 'EXP-1001', applicantName: '张三' },
  actions: [
    {
      id: 'action-1',
      actionCode: 'OPEN_WORKFLOW',
      actionLabel: '查看已办',
      interactionType: 'ROUTE' as const,
      status: 'AVAILABLE' as const,
    },
  ],
};

function mountDialog(actions = message.actions) {
  return mount(NoticeDetailDialog, {
    props: { modelValue: true, message: { ...message, actions } },
    global: {
      stubs: {
        ElDialog: {
          props: ['modelValue', 'title'],
          emits: ['update:modelValue'],
          template: '<section><h2>{{ title }}</h2><slot /><footer><slot name="footer" /></footer></section>',
        },
        ElTag: { template: '<span class="tag"><slot /></span>' },
        ElDescriptions: { template: '<dl><slot /></dl>' },
        ElDescriptionsItem: { props: ['label'], template: '<div><dt>{{ label }}</dt><dd><slot /></dd></div>' },
        ElButton: { emits: ['click'], template: '<button type="button" @click="$emit(\'click\')"><slot /></button>' },
      },
    },
  });
}

describe('NoticeDetailDialog', () => {
  it('按 label:value 展示消息并固定输出关闭和一个主操作', async () => {
    const wrapper = mountDialog();

    expect(wrapper.get('h2').text()).toBe('流程完成');
    expect(wrapper.text()).toContain('消息类型：流程完成');
    expect(wrapper.text()).toContain('消息内容：流程费用报销已完成。');
    expect(wrapper.text()).toContain('消息时间：2026-07-30 10:00:00');
    expect(wrapper.text()).not.toContain('当前状态');
    expect(wrapper.text()).not.toContain('业务对象');
    expect(wrapper.text()).not.toContain('申请单');
    expect(wrapper.findAll('footer button').map((button) => button.text())).toEqual(['关闭', '查看申请']);

    await wrapper.get('[data-test="notice-primary-action"]').trigger('click');
    expect(wrapper.emitted('action')?.[0]?.[0]).toMatchObject({ actionCode: 'OPEN_WORKFLOW' });
  });

  it('没有有效动作时只显示关闭按钮', () => {
    const wrapper = mountDialog([]);
    expect(wrapper.findAll('footer button').map((button) => button.text())).toEqual(['关闭']);
  });

  it('通过白名单 HTML 展示字段并移除危险内容', () => {
    const wrapper = mount(NoticeDetailDialog, {
      props: {
        modelValue: true,
        message: {
          ...message,
          bizName: '<strong>审批通知</strong>',
          content: '<p onclick="alert(1)">请<em>及时处理</em><script>alert(2)</script></p>',
        },
      },
      global: {
        stubs: {
          ElDialog: {
            props: ['modelValue', 'title'],
            template: '<section><h2>{{ title }}</h2><slot /><footer><slot name="footer" /></footer></section>',
          },
          ElButton: { template: '<button type="button"><slot /></button>' },
        },
      },
    });

    expect(wrapper.get('h2').text()).toBe('审批通知');
    expect(wrapper.find('.notice-detail__value strong').text()).toBe('审批通知');
    expect(wrapper.find('.notice-detail__value em').text()).toBe('及时处理');
    expect(wrapper.html()).not.toContain('onclick');
    expect(wrapper.html()).not.toContain('<script');
  });
});
