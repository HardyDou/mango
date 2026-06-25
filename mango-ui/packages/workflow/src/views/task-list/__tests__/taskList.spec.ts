import { computed, createApp, h, inject, nextTick, provide, reactive } from 'vue';
import TaskList from '../index.vue';
import { workflowApi } from '../../../api/workflow';

const route = reactive<{ path: string; fullPath: string; query: Record<string, any> }>({
  path: '/workflow/task/todo',
  fullPath: '/workflow/task/todo',
  query: {},
});
const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  confirm: vi.fn(() => Promise.resolve()),
  messageSuccess: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ push: mocks.push }),
}));

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<any>('element-plus');
  return {
    ...actual,
    ElMessage: {
      success: mocks.messageSuccess,
    },
    ElMessageBox: {
      confirm: mocks.confirm,
    },
  };
});

vi.mock('../../../api/workflow', async () => {
  const actual = await vi.importActual<any>('../../../api/workflow');
  return {
    ...actual,
    workflowApi: {
      todoTasks: vi.fn(),
      doneTasks: vi.fn(),
      copiedTasks: vi.fn(),
      initiatedProcesses: vi.fn(),
      businessAppliesPage: vi.fn(),
      readCopiedTask: vi.fn(),
      claimTask: vi.fn(() => Promise.resolve(true)),
    },
  };
});

describe('workflow task list', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    route.path = '/workflow/task/todo';
    route.fullPath = '/workflow/task/todo';
    route.query = {};
  });

  it('splits assigned todo tasks and claimable tasks by tabs', async () => {
    vi.mocked(workflowApi.todoTasks)
      .mockResolvedValueOnce(pageResult([
        { id: 'assigned-task', taskName: '经理审批', claimable: false },
      ]) as any)
      .mockResolvedValueOnce(pageResult([
        { id: 'claimable-task', taskName: '候选审批', claimable: true },
      ]) as any);

    const { el, unmount } = await mountTaskList();

    expect(workflowApi.todoTasks).toHaveBeenLastCalledWith(expect.objectContaining({
      todoType: 'ASSIGNED',
    }));
    expect(el.textContent).toContain('经理审批');
    expect(buttonTexts(el)).toContain('处理');
    expect(buttonTexts(el)).not.toContain('领取');

    clickTab(el, '待领取');
    await flushPromises();

    expect(workflowApi.todoTasks).toHaveBeenLastCalledWith(expect.objectContaining({
      todoType: 'CLAIMABLE',
      pageNum: 1,
    }));
    expect(el.textContent).toContain('候选审批');
    expect(buttonTexts(el)).toContain('详情');
    expect(buttonTexts(el)).toContain('领取');
    unmount();
  });

  it('opens claimable detail and allows claiming from list', async () => {
    vi.mocked(workflowApi.todoTasks)
      .mockResolvedValueOnce(pageResult([]) as any)
      .mockResolvedValueOnce(pageResult([
        { id: 'claimable-task', taskName: '候选审批', claimable: true },
      ]) as any)
      .mockResolvedValueOnce(pageResult([]) as any);

    const { el, unmount } = await mountTaskList();
    clickTab(el, '待领取');
    await flushPromises();

    clickButton(el, '详情');
    expect(mocks.push).toHaveBeenCalledWith({
      path: '/workflow/task/detail',
      query: { from: 'todo', taskId: 'claimable-task' },
    });

    clickButton(el, '领取');
    await flushPromises();

    expect(mocks.confirm).toHaveBeenCalledWith('确认领取当前任务？', '领取任务', { type: 'warning' });
    expect(workflowApi.claimTask).toHaveBeenCalledWith('claimable-task');
    expect(mocks.messageSuccess).toHaveBeenCalledWith('领取成功');
    expect(workflowApi.todoTasks).toHaveBeenLastCalledWith(expect.objectContaining({
      todoType: 'CLAIMABLE',
    }));
    unmount();
  });

  it('loads initiated applications by business apply statuses', async () => {
    route.path = '/workflow/task/initiated';
    route.fullPath = '/workflow/task/initiated?statuses=APPROVED';
    route.query = { statuses: 'APPROVED' };
    vi.mocked(workflowApi.businessAppliesPage).mockResolvedValueOnce(pageResult([
      {
        id: 'apply-1',
        applyTitle: '合同审批',
        businessKey: 'HT-001',
        processName: '合同流程',
        processDefinitionKey: 'contract_process',
        applyStatusName: '已完成',
        createdAt: '2026-06-24 10:00:00',
      },
    ]) as any);

    const { el, unmount } = await mountTaskList();

    expect(workflowApi.businessAppliesPage).toHaveBeenCalledWith(expect.objectContaining({
      statuses: ['APPROVED'],
    }));
    expect(el.textContent).toContain('合同审批');
    expect(el.textContent).toContain('已完成');
    unmount();
  });
});

async function mountTaskList() {
  const host = document.createElement('div');
  document.body.appendChild(host);
  const app = createApp(TaskList);
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
  const tableRowsKey = Symbol('tableRows');
  app.component('ElCard', { template: '<div><slot name="header" /><slot /></div>' });
  app.directive('loading', {});
  app.component('ElTag', { template: '<span><slot /></span>' });
  app.component('ElForm', { template: '<form><slot /></form>' });
  app.component('ElFormItem', { props: ['label'], template: '<label>{{ label }}<slot /></label>' });
  app.component('ElInput', { props: ['modelValue'], emits: ['update:modelValue'], template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />' });
  app.component('ElButton', { props: ['disabled'], emits: ['click'], template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>' });
  app.component('ElTabs', {
    props: ['modelValue'],
    emits: ['update:modelValue', 'tabChange'],
    template: '<div class="tabs"><slot /></div>',
  });
  app.component('ElTabPane', {
    props: ['label', 'name'],
    template: '<button class="tab-pane" @click="$parent?.$emit(\'update:modelValue\', name); $parent?.$emit(\'tabChange\', name)">{{ label }}</button>',
  });
  app.component('ElTable', {
    props: ['data'],
    setup(props, { slots }) {
      provide(tableRowsKey, computed(() => props.data || []));
      return () => h('table', slots.default?.());
    },
  });
  app.component('ElTableColumn', {
    props: ['prop', 'label'],
    setup(props, { slots }) {
      const rows = inject<ReturnType<typeof computed<any[]>>>(tableRowsKey, computed(() => []));
      return () => h('tbody', rows.value.map(row => h('tr', [
        h('td', slots.default ? slots.default({ row }) : String(row?.[props.prop] ?? '')),
      ])));
    },
  });
  app.component('ElPagination', { template: '<div />' });
}

function pageResult(list: any[]) {
  return {
    list,
    total: list.length,
    pageNum: 1,
    pageSize: 10,
  };
}

async function flushPromises() {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
  await nextTick();
  await Promise.resolve();
  await nextTick();
}

function clickTab(el: HTMLElement, text: string) {
  const tab = [...el.querySelectorAll('.tab-pane')].find(item => item.textContent?.trim() === text) as HTMLElement | undefined;
  expect(tab).toBeTruthy();
  tab?.click();
}

function buttonTexts(el: HTMLElement) {
  return [...el.querySelectorAll('button')].map(item => item.textContent?.trim() || '');
}

function clickButton(el: HTMLElement, text: string) {
  const button = [...el.querySelectorAll('button')].find(item => item.textContent?.trim() === text) as HTMLElement | undefined;
  expect(button).toBeTruthy();
  button?.click();
}
