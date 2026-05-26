import { createApp, nextTick } from 'vue';
import StartProcess from '../index.vue';
import { workflowApi } from '../../../api/workflow';
import { fileApi } from '@mango/file';

const mocks = vi.hoisted(() => ({
  routerPush: vi.fn(),
  definitionsPage: vi.fn(),
  preview: vi.fn(),
  fileRuntimeUrl: vi.fn(() => 'https://cdn.example.com/workflow-icon.png'),
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mocks.routerPush }),
}));

vi.mock('../../../api/workflow', async () => {
  const actual = await vi.importActual<any>('../../../api/workflow');
  return {
    ...actual,
    workflowApi: {
      ...actual.workflowApi,
      definitionsPage: mocks.definitionsPage,
    },
  };
});

vi.mock('@mango/file', async () => {
  const actual = await vi.importActual<any>('@mango/file');
  return {
    ...actual,
    fileApi: {
      ...actual.fileApi,
      preview: mocks.preview,
    },
    fileRuntimeUrl: mocks.fileRuntimeUrl,
  };
});

describe('start process workflow icons', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.definitionsPage.mockResolvedValue({
      list: [
        workflowDefinition({ id: '1', definitionName: '未配置Logo流程', icon: '' }),
        workflowDefinition({ id: '2', definitionName: '历史file文本流程', icon: 'file' }),
        workflowDefinition({ id: '3', definitionName: '历史图标名流程', icon: 'CollectionTag' }),
        workflowDefinition({ id: '4', definitionName: '文件Logo流程', icon: '123' }),
        workflowDefinition({ id: '5', definitionName: '历史受保护URL流程', icon: '/api/file/files/download?id=file' }),
        workflowDefinition({ id: '6', definitionName: '直连图片流程', icon: 'https://cdn.example.com/direct-icon.png' }),
      ],
      total: 6,
      pageNum: 1,
      pageSize: 50,
    });
    mocks.preview.mockResolvedValue({
      id: '123',
      fileName: 'workflow-icon.png',
      fileSize: 12,
      previewable: true,
      previewUrl: '',
      downloadUrl: '',
      directPreviewUrl: 'https://cdn.example.com/workflow-icon.png',
    });
  });

  it('does not preview workflow icons that are empty or not valid file ids', async () => {
    const { el, unmount } = await mountStartProcess();
    await flushPromises();

    expect(workflowApi.definitionsPage).toHaveBeenCalledWith(expect.objectContaining({
      publishedOnly: true,
      status: 'PUBLISHED',
    }));
    expect(fileApi.preview).toHaveBeenCalledTimes(1);
    expect(fileApi.preview).toHaveBeenCalledWith('123');
    expect(el.textContent).toContain('未配置Logo流程');
    expect(el.textContent).toContain('历史file文本流程');
    expect(el.textContent).toContain('历史图标名流程');
    expect(el.textContent).toContain('文件Logo流程');
    expect(el.textContent).toContain('历史受保护URL流程');
    expect(el.textContent).toContain('直连图片流程');
    const imageUrls = Array.from(el.querySelectorAll('img')).map(item => item.getAttribute('src') || '');
    expect(imageUrls).toContain('https://cdn.example.com/direct-icon.png');
    expect(imageUrls.some(item => item.includes('/api/file/files'))).toBe(false);
    unmount();
  });
});

function workflowDefinition(overrides: Record<string, unknown>) {
  return {
    id: '',
    categoryId: '1',
    categoryName: '通用流程',
    icon: '',
    definitionName: '流程',
    definitionKey: 'workflow_key',
    processDefinitionId: 'process:1:1',
    processDefinitionVersion: 1,
    designerJson: '{}',
    formJson: '',
    status: 'PUBLISHED',
    ...overrides,
  };
}

async function mountStartProcess() {
  const host = document.createElement('div');
  document.body.appendChild(host);
  const app = createApp(StartProcess);
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
  app.directive('loading', {});
  app.component('ElCard', { template: '<section><slot name="header" /><slot /></section>' });
  app.component('ElTag', { template: '<span><slot /></span>' });
  app.component('ElForm', { template: '<form><slot /></form>' });
  app.component('ElFormItem', { props: ['label'], template: '<label><span>{{ label }}</span><slot /></label>' });
  app.component('ElInput', { props: ['modelValue'], emits: ['update:modelValue'], template: '<input :value="modelValue" />' });
  app.component('ElButton', { props: ['disabled'], emits: ['click'], template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>' });
  app.component('ElEmpty', { props: ['description'], template: '<div>{{ description }}</div>' });
  app.component('ElIcon', { template: '<i><slot /></i>' });
  app.component('ElDialog', { props: ['modelValue', 'title'], template: '<div v-if="modelValue"><h2>{{ title }}</h2><slot /><slot name="footer" /></div>' });
  app.component('ElDivider', { template: '<hr />' });
  app.component('ElAlert', { props: ['title'], template: '<div>{{ title }}</div>' });
  app.component('ElSelect', { template: '<select><slot /></select>' });
  app.component('ElOption', { props: ['label'], template: '<option>{{ label }}</option>' });
  app.component('ElCollapse', { template: '<div><slot /></div>' });
  app.component('ElCollapseItem', { template: '<div><slot /></div>' });
}

async function flushPromises() {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
  await nextTick();
  await Promise.resolve();
}
