import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import WorkflowNodeTimeline from '../WorkflowNodeTimeline.vue';
import WorkflowApprovalTimeline from '../WorkflowApprovalTimeline.vue';

vi.mock('@mango/common', () => ({
  RichTextViewer: {
    name: 'RichTextViewer',
    props: ['content'],
    template: '<div data-testid="rich-text-viewer" v-html="content" />',
  },
}));

const stubs = {
  'el-timeline': { template: '<div><slot /></div>' },
  'el-timeline-item': { template: '<div><slot /></div>' },
  'el-tag': { template: '<span><slot /></span>' },
  'el-collapse': { template: '<div><slot /></div>' },
  'el-collapse-item': { template: '<div><slot /></div>' },
  'el-empty': { template: '<div><slot /></div>' },
};

describe('workflow approval comments', () => {
  it('renders node timeline comments through the shared rich-text viewer', () => {
    const wrapper = mount(WorkflowNodeTimeline, {
      props: {
        node: { id: 'review', nodeName: '审核', nodeType: 'APPROVAL' },
        currentNodeKey: 'review',
        records: [
          {
            processInstanceId: 'process-1',
            taskDefinitionKey: 'review',
            action: 'REJECT',
            actionName: '退回',
            comment: '<p>补充材料</p>',
          },
        ],
      },
      global: { stubs },
    });

    expect(wrapper.find('[data-testid="rich-text-viewer"]').html()).toContain('<p>补充材料</p>');
  });

  it('renders approval timeline comments through the shared rich-text viewer', () => {
    const wrapper = mount(WorkflowApprovalTimeline, {
      props: {
        records: [
          {
            processInstanceId: 'process-1',
            action: 'REJECT',
            actionName: '退回',
            comment: '<p>补充材料</p>',
          },
        ],
      },
      global: { stubs },
    });

    expect(wrapper.find('[data-testid="rich-text-viewer"]').html()).toContain('<p>补充材料</p>');
  });
});
