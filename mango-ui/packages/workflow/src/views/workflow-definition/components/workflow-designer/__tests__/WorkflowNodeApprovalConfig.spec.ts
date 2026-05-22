import { createApp, reactive, nextTick } from 'vue';
import WorkflowNodeApprovalConfig from '../WorkflowNodeApprovalConfig.vue';
import { defaultApprovalConfig } from '../../../../../api/workflow';

describe('WorkflowNodeApprovalConfig', () => {
  it('shows pass ratio input for countersign mode only', async () => {
    const config = reactive({
      ...defaultApprovalConfig(),
      assigneeType: 'SPECIFIED_USER' as const,
      assigneeIds: ['admin', 'zhangsan'],
      approvalMode: 'COUNTERSIGN' as const,
      passRatio: 60,
    });
    const { el, unmount } = await mountConfig(config);

    expect(el.textContent).toContain('% 通过后节点通过');
    expect(el.textContent).toContain('60');

    config.approvalMode = 'OR_SIGN';
    await nextTick();

    expect(el.textContent).not.toContain('% 通过后节点通过');
    unmount();
  });
});

async function mountConfig(config: any) {
  const host = document.createElement('div');
  document.body.appendChild(host);
  const app = createApp(WorkflowNodeApprovalConfig, {
    config,
    assigneeTypeOptions: [{ label: '指定成员', value: 'SPECIFIED_USER' }],
    userOptions: [],
    roleOptions: [],
    postOptions: [],
    orgTreeOptions: [],
    targetLoading: { users: false, roles: false, posts: false, orgs: false },
    formVariables: [],
    showModeConfig: true,
  });
  registerElementStubs(app);
  app.mount(host);
  await nextTick();
  return {
    el: host,
    unmount: () => {
      app.unmount();
      host.remove();
    },
  };
}

function registerElementStubs(app: ReturnType<typeof createApp>) {
  app.component('ElCollapse', { template: '<div><slot /></div>' });
  app.component('ElCollapseItem', { props: ['title', 'name'], template: '<section><h3>{{ title }}</h3><slot /></section>' });
  app.component('ElRadioGroup', { props: ['modelValue'], template: '<div><slot /></div>' });
  app.component('ElRadio', { props: ['label'], template: '<label><slot /></label>' });
  app.component('ElInputNumber', { props: ['modelValue'], template: '<span class="input-number">{{ modelValue }}</span>' });
  app.component('ElSelect', { template: '<div />' });
  app.component('ElOption', { template: '<div />' });
  app.component('ElTreeSelect', { template: '<div />' });
  app.component('ElSwitch', { template: '<div />' });
  app.component('ElInput', { template: '<input />' });
}
