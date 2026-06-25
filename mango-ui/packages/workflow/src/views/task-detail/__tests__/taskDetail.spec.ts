import { createApp, nextTick, reactive } from 'vue';
import TaskDetail from '../index.vue';
import { workflowApi } from '../../../api/workflow';
import { registerBusinessApprovalComponents } from '../../../components/businessApproval';

const routeQuery = reactive<Record<string, any>>({ taskId: 'task-1' });
const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  messageWarning: vi.fn(),
  messageError: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: routeQuery }),
  useRouter: () => ({ push: mocks.push }),
}));

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<any>('element-plus');
  return {
    ...actual,
    ElMessage: {
      warning: mocks.messageWarning,
      error: mocks.messageError,
      success: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn(() => Promise.resolve()),
    },
  };
});

vi.mock('@mango/common', () => ({
  UserSelector: {
    name: 'UserSelector',
    props: {
      modelValue: [String, Array],
      multiple: Boolean,
      mode: String,
      placeholder: String,
      title: String,
    },
    emits: ['update:modelValue'],
    template: '<div class="mock-user-selector" :data-multiple="multiple ? \'true\' : \'false\'">{{ title }}</div>',
  },
}));

vi.mock('../../../api/workflow', async () => {
  const actual = await vi.importActual<any>('../../../api/workflow');
  return {
    ...actual,
    workflowApi: {
      taskDetail: vi.fn(),
      businessApplyByProcessInstance: vi.fn(),
      completeTask: vi.fn(() => Promise.resolve(true)),
      transferTask: vi.fn(() => Promise.resolve(true)),
      addSignTask: vi.fn(() => Promise.resolve(true)),
      claimTask: vi.fn(() => Promise.resolve(true)),
      unclaimTask: vi.fn(() => Promise.resolve(true)),
    },
  };
});

describe('workflow task detail', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    routeQuery.taskId = 'task-1';
    delete routeQuery.processInstanceId;
    delete routeQuery.mode;
    delete routeQuery.from;
    delete routeQuery.returnPath;
    delete routeQuery.returnQuery;
  });

  it('renders custom approval component from formJson instead of JSON preview', async () => {
    registerBusinessApprovalComponents({
      'workflow.test.approve': {
        component: {
          props: ['context'],
          template: '<div class="custom-approval">自定义审批表单{{ context.records.length }}</div>',
        },
      },
    });
    vi.mocked(workflowApi.taskDetail).mockResolvedValueOnce(taskDetail({
      formJson: JSON.stringify({
        mode: 'CUSTOM_PAGE',
        customConfig: { approvePageKey: 'workflow.test.approve' },
      }),
      renderConfig: {},
    }) as any);
    vi.mocked(workflowApi.businessApplyByProcessInstance).mockRejectedValueOnce(new Error('no apply'));

    const { el, unmount } = await mountTaskDetail();

    expect(el.querySelector('.custom-approval')).toBeTruthy();
    expect(el.querySelector('.json-preview')).toBeFalsy();
    unmount();
  });

  it('allows business registration to customize or hide the record panel', async () => {
    registerBusinessApprovalComponents({
      'workflow.test.custom-record': {
        component: { template: '<div class="custom-approval">自定义审批表单</div>' },
        recordPanelMode: 'CUSTOM',
        recordPanelComponent: {
          props: ['context'],
          template: '<div class="custom-record-panel">业务审批记录{{ context.records[0].comment }}</div>',
        },
      },
      'workflow.test.hidden-record': {
        component: { template: '<div class="hidden-record-approval">隐藏右侧</div>' },
        recordPanelMode: 'HIDDEN',
      },
    });
    vi.mocked(workflowApi.taskDetail)
      .mockResolvedValueOnce(taskDetail({
        formJson: JSON.stringify({
          mode: 'CUSTOM_PAGE',
          customConfig: { approvePageKey: 'workflow.test.custom-record' },
        }),
        records: [{ action: 'COMPLETE', actionName: '通过', processInstanceId: 'proc-1', comment: '同意' }],
      }) as any)
      .mockResolvedValueOnce(taskDetail({
        formJson: JSON.stringify({
          mode: 'CUSTOM_PAGE',
          customConfig: { approvePageKey: 'workflow.test.hidden-record' },
        }),
        records: [{ action: 'COMPLETE', actionName: '通过', processInstanceId: 'proc-2', comment: '隐藏记录' }],
      }) as any);
    vi.mocked(workflowApi.businessApplyByProcessInstance).mockRejectedValue(new Error('no apply'));

    const { el, unmount } = await mountTaskDetail();
    expect(el.querySelector('.custom-record-panel')?.textContent).toContain('同意');
    expect(el.textContent).not.toContain('通过 ·');

    routeQuery.taskId = 'task-2';
    await flushPromises();
    expect(el.querySelector('.record-panel')).toBeFalsy();
    expect(el.querySelector('.hidden-record-approval')).toBeTruthy();
    unmount();
  });

  it('reloads detail when route task id changes', async () => {
    vi.mocked(workflowApi.taskDetail)
      .mockResolvedValueOnce(taskDetail({
        formJson: JSON.stringify([
          { type: 'input', field: 'firstField', title: '第一字段' },
        ]),
      }) as any)
      .mockResolvedValueOnce(taskDetail({
        task: {
          id: 'task-2',
          taskName: '复核',
          taskDefinitionKey: 'review',
          processInstanceId: 'proc-2',
          businessKey: 'BIZ-2',
        },
        process: {
          processInstanceId: 'proc-2',
          processName: '第二流程',
          processKey: 'second_process',
          businessKey: 'BIZ-2',
        },
        formJson: JSON.stringify([
          { type: 'input', field: 'secondField', title: '第二字段' },
        ]),
      }) as any);
    vi.mocked(workflowApi.businessApplyByProcessInstance).mockRejectedValue(new Error('no apply'));

    const { el, unmount } = await mountTaskDetail();
    expect(el.textContent).toContain('第一字段');

    routeQuery.taskId = 'task-2';
    await flushPromises();

    expect(workflowApi.taskDetail).toHaveBeenLastCalledWith('task-2');
    expect(el.textContent).toContain('第二字段');
    expect(el.textContent).not.toContain('第一字段');
    unmount();
  });

  it('returns to safe business return path before workflow list fallback', async () => {
    routeQuery.returnPath = '/guarantee/risk/reviews';
    routeQuery.returnQuery = 'scope=TODO&tab=pending';
    routeQuery.from = 'done';
    vi.mocked(workflowApi.taskDetail).mockResolvedValueOnce(taskDetail() as any);
    vi.mocked(workflowApi.businessApplyByProcessInstance).mockRejectedValue(new Error('no apply'));

    const { el, unmount } = await mountTaskDetail();
    clickButton(el, '返回');

    expect(mocks.push).toHaveBeenCalledWith({
      path: '/guarantee/risk/reviews',
      query: { scope: 'TODO', tab: 'pending' },
    });
    unmount();
  });

  it.each([
    ['external URL', 'https://example.com/workflow'],
    ['protocol-relative URL', '//example.com/workflow'],
    ['empty path', ''],
  ])('rejects unsafe returnPath and keeps workflow fallback for %s', async (_caseName, returnPath) => {
    routeQuery.returnPath = returnPath;
    routeQuery.from = 'done';
    vi.mocked(workflowApi.taskDetail).mockResolvedValueOnce(taskDetail() as any);
    vi.mocked(workflowApi.businessApplyByProcessInstance).mockRejectedValue(new Error('no apply'));

    const { el, unmount } = await mountTaskDetail();
    clickButton(el, '返回');

    expect(mocks.push).toHaveBeenCalledWith('/workflow/task/done');
    unmount();
  });

  it('keeps original workflow list fallback when returnPath is absent', async () => {
    routeQuery.from = 'initiated';
    vi.mocked(workflowApi.taskDetail).mockResolvedValueOnce(taskDetail() as any);
    vi.mocked(workflowApi.businessApplyByProcessInstance).mockRejectedValue(new Error('no apply'));

    const { el, unmount } = await mountTaskDetail();
    clickButton(el, '返回');

    expect(mocks.push).toHaveBeenCalledWith('/workflow/task/initiated');
    unmount();
  });

  it('returns to business source after task action succeeds', async () => {
    routeQuery.returnPath = '/guarantee/risk/reviews';
    routeQuery.returnQuery = 'scope=TODO';
    vi.mocked(workflowApi.taskDetail).mockResolvedValueOnce(taskDetail() as any);
    vi.mocked(workflowApi.businessApplyByProcessInstance).mockRejectedValue(new Error('no apply'));

    const { el, unmount } = await mountTaskDetail();
    clickButton(el, '通过');
    await flushPromises();

    expect(workflowApi.completeTask).toHaveBeenCalledWith({
      taskId: 'task-1',
      comment: '',
      variables: {},
    });
    expect(mocks.push).toHaveBeenCalledWith({
      path: '/guarantee/risk/reviews',
      query: { scope: 'TODO' },
    });
    unmount();
  });

  it('uses user selector for transfer and add sign actions', async () => {
    vi.mocked(workflowApi.taskDetail).mockResolvedValueOnce(taskDetail({
      renderConfig: {
        nodeActions: {
          transfer: { enabled: true, label: '转办', order: 1 },
          addSign: { enabled: true, label: '加签', order: 2 },
        },
      },
    }) as any);
    vi.mocked(workflowApi.businessApplyByProcessInstance).mockRejectedValue(new Error('no apply'));
    const { el, unmount } = await mountTaskDetail();

    clickButton(el, '转办');
    await flushPromises();
    expect(el.querySelector('.mock-user-selector')?.textContent).toContain('选择转办人员');
    expect(el.querySelector('.mock-user-selector')?.getAttribute('data-multiple')).toBe('false');

    clickButton(el, '取消');
    await flushPromises();
    clickButton(el, '加签');
    await flushPromises();
    expect(el.querySelector('.mock-user-selector')?.textContent).toContain('选择加签人员');
    expect(el.querySelector('.mock-user-selector')?.getAttribute('data-multiple')).toBe('true');
    unmount();
  });

  it('renders claim actions only from backend claim flags', async () => {
    vi.mocked(workflowApi.taskDetail)
      .mockResolvedValueOnce(taskDetail({
        task: {
          id: 'assigned-task',
          taskName: '普通审批',
          taskDefinitionKey: 'approve',
          processInstanceId: 'proc-1',
          businessKey: 'BIZ-1',
          assigneeName: 'admin',
          claimable: false,
          unclaimable: false,
        },
      }) as any)
      .mockResolvedValueOnce(taskDetail({
        task: {
          id: 'candidate-task',
          taskName: '候选审批',
          taskDefinitionKey: 'approve',
          processInstanceId: 'proc-2',
          businessKey: 'BIZ-2',
          claimable: true,
          unclaimable: false,
        },
      }) as any)
      .mockResolvedValueOnce(taskDetail({
        task: {
          id: 'claimed-task',
          taskName: '已认领审批',
          taskDefinitionKey: 'approve',
          processInstanceId: 'proc-3',
          businessKey: 'BIZ-3',
          assigneeName: 'admin',
          claimable: false,
          unclaimable: true,
        },
      }) as any);
    vi.mocked(workflowApi.businessApplyByProcessInstance).mockRejectedValue(new Error('no apply'));

    const { el, unmount } = await mountTaskDetail();
    expect(buttonTexts(el)).not.toContain('认领');
    expect(buttonTexts(el)).not.toContain('释放');

    routeQuery.taskId = 'candidate-task';
    await flushPromises();
    expect(buttonTexts(el)).toContain('认领');
    expect(buttonTexts(el)).not.toContain('释放');

    routeQuery.taskId = 'claimed-task';
    await flushPromises();
    expect(buttonTexts(el)).not.toContain('认领');
    expect(buttonTexts(el)).toContain('释放');
    unmount();
  });

  it('defaults approval form fields to readonly when node permissions are not explicit', async () => {
    vi.mocked(workflowApi.taskDetail).mockResolvedValueOnce(taskDetail({
      formJson: JSON.stringify([
        { type: 'input', field: 'title', title: '标题' },
        { type: 'textarea', field: 'reason', title: '说明' },
      ]),
      variables: {
        title: '候选任务详情示例',
        reason: '认领节点只读验证',
      },
      formPermissions: {},
    }) as any);
    vi.mocked(workflowApi.businessApplyByProcessInstance).mockRejectedValue(new Error('no apply'));

    const { el, unmount } = await mountTaskDetail();

    expect(el.textContent).toContain('候选任务详情示例');
    expect(el.textContent).toContain('认领节点只读验证');
    expect(el.querySelectorAll('.readonly-value')).toHaveLength(2);
    expect(el.querySelectorAll('textarea')).toHaveLength(1);
    unmount();
  });
});

function taskDetail(overrides: Record<string, any> = {}) {
  return {
    task: {
      id: 'task-1',
      taskName: '经理审批',
      taskDefinitionKey: 'manager_approve',
      processInstanceId: 'proc-1',
      businessKey: 'BIZ-1',
    },
    process: {
      processInstanceId: 'proc-1',
      processName: '测试流程',
      processKey: 'test_process',
      businessKey: 'BIZ-1',
    },
    formCode: '',
    formJson: '',
    variables: { businessType: 'TEST' },
    formPermissions: {},
    renderConfig: {
      renderMode: 'DYNAMIC_FORM',
      businessType: 'TEST',
      processInstanceId: 'proc-1',
      nodeActions: {},
      ...(overrides.renderConfig || {}),
    },
    records: [],
    ...overrides,
  };
}

async function mountTaskDetail() {
  const host = document.createElement('div');
  document.body.appendChild(host);
  const app = createApp(TaskDetail);
  registerElementStubs(app);
  app.mount(host);
  await flushPromises();
  return {
    el: host,
    unmount: () => {
      app.unmount();
      host.remove();
    },
  };
}

function registerElementStubs(app: ReturnType<typeof createApp>) {
  app.component('ElCard', { template: '<div><slot name="header" /><slot /></div>' });
  app.directive('loading', {});
  app.component('ElEmpty', { template: '<div />' });
  app.component('ElTag', { template: '<span><slot /></span>' });
  app.component('ElButton', { props: ['disabled'], template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>' });
  app.component('ElTooltip', { template: '<span><slot /></span>' });
  app.component('ElDescriptions', { template: '<div><slot /></div>' });
  app.component('ElDescriptionsItem', { template: '<div><slot /></div>' });
  app.component('ElAlert', { props: ['title'], template: '<div class="el-alert">{{ title }}</div>' });
  app.component('ElForm', { template: '<form><slot /></form>' });
  app.component('ElFormItem', { props: ['label'], template: '<div><span class="form-item-label">{{ label }}</span><slot /></div>' });
  app.component('ElInput', { template: '<textarea />' });
  app.component('ElTimeline', { template: '<div><slot /></div>' });
  app.component('ElTimelineItem', { template: '<div><slot /></div>' });
  app.component('ElDialog', { props: ['modelValue'], template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>' });
}

async function flushPromises() {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
  await nextTick();
  await Promise.resolve();
  await nextTick();
}

function clickButton(el: HTMLElement, text: string) {
  const button = [...el.querySelectorAll('button')].find(item => item.textContent?.trim() === text) as HTMLElement | undefined;
  expect(button).toBeTruthy();
  button?.click();
}

function buttonTexts(el: HTMLElement) {
  return [...el.querySelectorAll('button')].map(item => item.textContent?.trim() || '');
}
