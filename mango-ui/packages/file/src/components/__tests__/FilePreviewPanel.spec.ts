import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { createApp, defineComponent, h, nextTick, type App } from 'vue';
import FilePreviewPanel from '../FilePreviewPanel.vue';
import { fileApi, type FilePreviewLink, type FilePreviewRenderMode, type FileRecord } from '../../api/file';
import { clearPreviewLinkCache } from '../../utils/previewLinkCache';

const mountedApps: App[] = [];
const happyDomSettings = (
  window as unknown as Window & {
    happyDOM: { settings: { disableIframePageLoading: boolean } };
  }
).happyDOM.settings;
const iframePageLoadingDisabled = happyDomSettings.disableIframePageLoading;

const ElButtonStub = defineComponent({
  name: 'ElButton',
  props: {
    disabled: Boolean,
  },
  setup(props, { slots }) {
    return () => h('button', { disabled: props.disabled, type: 'button' }, slots.default?.());
  },
});

const ElTagStub = defineComponent({
  name: 'ElTag',
  setup(_props, { slots }) {
    return () => h('span', slots.default?.());
  },
});

const ElImageViewerStub = defineComponent({
  name: 'ElImageViewer',
  props: {
    urlList: {
      type: Array<string>,
      default: () => [],
    },
    infinite: Boolean,
    teleported: Boolean,
    closeOnPressEscape: Boolean,
  },
  setup(props) {
    return () =>
      h('div', { 'data-image-viewer': '' }, [
        h('div', { class: 'el-image-viewer__mask' }),
        h('button', { class: 'el-image-viewer__close', type: 'button' }),
        h('img', {
          class: 'el-image-viewer__img',
          src: props.urlList[0],
          'data-infinite': String(props.infinite),
          'data-teleported': String(props.teleported),
          'data-close-on-press-escape': String(props.closeOnPressEscape),
        }),
        h('div', { class: 'el-image-viewer__actions' }),
      ]);
  },
});

type PanelProps = Record<string, unknown>;

async function mountPanel(props: PanelProps = {}) {
  const host = document.createElement('div');
  document.body.appendChild(host);
  const app = createApp(FilePreviewPanel, {
    showActions: true,
    fitContainer: false,
    ...props,
  });
  app.component('ElButton', ElButtonStub);
  app.component('ElTag', ElTagStub);
  app.component('ElEmpty', defineComponent({ setup: () => () => h('div') }));
  app.component('ElImageViewer', ElImageViewerStub);
  app.component(
    'ElIcon',
    defineComponent({
      setup:
        (_props, { slots }) =>
        () =>
          h('span', slots.default?.()),
    }),
  );
  app.directive('loading', {
    mounted: (element, binding) => {
      element.setAttribute('data-loading', String(Boolean(binding.value)));
    },
    updated: (element, binding) => {
      element.setAttribute('data-loading', String(Boolean(binding.value)));
    },
  });
  app.mount(host);
  mountedApps.push(app);
  await nextTick();
  await nextTick();
  return host;
}

function unmountPanels() {
  while (mountedApps.length) {
    mountedApps.pop()?.unmount();
  }
}

function mockPreviewLink(overrides: Partial<FilePreviewLink> = {}) {
  return vi.spyOn(fileApi, 'previewLink').mockImplementation((fileId: string) =>
    Promise.resolve({
      fileId,
      fileName: 'report.pdf',
      previewUrl: `/api/file-preview/files/preview-entry?token=${fileId}`,
      renderMode: 'PDF' as FilePreviewRenderMode,
      expireSeconds: 300,
      ...overrides,
    }),
  );
}

function mockPreviewContent() {
  vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:preview-content');
  return vi.spyOn(fileApi, 'previewContent').mockResolvedValue({
    data: new Blob(['preview-content'], { type: 'application/octet-stream' }),
    headers: { 'content-type': 'application/octet-stream' },
  });
}

beforeAll(() => {
  happyDomSettings.disableIframePageLoading = true;
});

afterAll(() => {
  happyDomSettings.disableIframePageLoading = iframePageLoadingDisabled;
});

afterEach(() => {
  unmountPanels();
  vi.restoreAllMocks();
  clearPreviewLinkCache();
  document.body.innerHTML = '';
});

describe('FilePreviewPanel 渲染分派', () => {
  const cases: Array<[FilePreviewRenderMode, string]> = [
    ['PDF', 'iframe.preview-frame'],
    ['CONVERTING', '[data-converting]'],
    ['IMAGE', '[data-image-viewer]'],
    ['VIDEO', 'video.preview-media'],
    ['AUDIO', 'audio.preview-audio'],
    ['NOT_SUPPORTED', '[data-render-hint="NOT_SUPPORTED"]'],
    ['DOWNLOAD_ONLY', '[data-render-hint="DOWNLOAD_ONLY"]'],
  ];

  for (const [renderMode, selector] of cases) {
    it(`renderMode=${renderMode} 渲染对应容器`, async () => {
      mockPreviewLink({ renderMode });
      if (renderMode === 'IMAGE' || renderMode === 'VIDEO' || renderMode === 'AUDIO') {
        mockPreviewContent();
      }

      const host = await mountPanel({ fileId: '1' });

      await vi.waitFor(() => {
        expect(host.querySelector(`[data-render-mode="${renderMode}"]`)).not.toBeNull();
      });
      await vi.waitFor(() => {
        expect(host.querySelector(selector)).not.toBeNull();
      });
    });
  }

  it('转换中显示等待态', async () => {
    mockPreviewLink({ renderMode: 'CONVERTING' });

    const host = await mountPanel({ fileId: '1' });

    await vi.waitFor(() => {
      expect(host.querySelector('[data-converting]')?.textContent).toContain('文件正在转换');
    });
    expect(host.querySelector('iframe.preview-frame')).not.toBeNull();
  });

  it('不支持时显示下载提示而不是渲染容器', async () => {
    mockPreviewLink({ renderMode: 'NOT_SUPPORTED' });

    const host = await mountPanel({ fileId: '1', file: { id: '1', fileName: 'README', fileSize: 10 } as FileRecord });

    await vi.waitFor(() => {
      expect(host.querySelector('[data-render-hint="NOT_SUPPORTED"]')?.textContent).toContain(
        '该格式不支持在线预览，请下载原文件查看',
      );
    });
    expect(host.querySelector('iframe.preview-frame')).toBeNull();
    expect(host.querySelector('video')).toBeNull();
  });

  it('EXIF 图片等媒体内容走后端受限流而不解析地址', async () => {
    mockPreviewLink({ renderMode: 'IMAGE' });
    const previewContent = mockPreviewContent();

    const host = await mountPanel({ fileId: '1' });

    await vi.waitFor(() => {
      expect(
        host.querySelector('.preview-image-viewer [data-image-viewer] .el-image-viewer__img')?.getAttribute('src'),
      ).toBe('blob:preview-content');
    });
    expect(previewContent).toHaveBeenCalledWith('1');
  });

  it('旧地址入参仍能渲染并只提示一次弃用警告', async () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);

    const host = await mountPanel({
      previewUrl: '/legacy/preview.pdf',
      documentPreviewUrl: '/legacy/document.pdf',
      directPreviewUrl: '/legacy/direct.pdf',
    });

    await vi.waitFor(() => {
      expect(host.querySelector('iframe.preview-frame')?.getAttribute('src')).toContain('/legacy/preview.pdf');
    });
    expect(warn).toHaveBeenCalledTimes(1);
    expect(String(warn.mock.calls[0][0])).toContain('previewUrl');
    expect(String(warn.mock.calls[0][0])).toContain('下一个 major 移除');
  });

  it('未入库纯文本直接渲染文本内容', async () => {
    const host = await mountPanel({ textContent: '第一行\n第二行' });

    await nextTick();
    expect(host.querySelector('.preview-text')?.textContent).toContain('第一行');
    expect(host.querySelector('iframe')).toBeNull();
  });

  it('file 元信息只用于展示文件名', async () => {
    mockPreviewLink({ renderMode: 'DOWNLOAD_ONLY' });

    const host = await mountPanel({
      fileId: '1',
      file: { id: '1', fileName: '季度报表.xlsx', fileSize: 2048 } as FileRecord,
    });

    await vi.waitFor(() => {
      expect(host.querySelector('.preview-name')?.textContent).toBe('季度报表.xlsx');
    });
  });
});

describe('FilePreviewPanel preview-link 复用', () => {
  it('同一 fileId 重复打开只请求一次 preview-link', async () => {
    const previewLink = mockPreviewLink({ renderMode: 'PDF' });

    const first = await mountPanel({ fileId: '9' });
    await vi.waitFor(() => {
      expect(first.querySelector('iframe.preview-frame')).not.toBeNull();
    });
    unmountPanels();
    document.body.innerHTML = '';

    const second = await mountPanel({ fileId: '9' });
    await vi.waitFor(() => {
      expect(second.querySelector('iframe.preview-frame')).not.toBeNull();
    });

    expect(previewLink).toHaveBeenCalledTimes(1);
    expect(previewLink).toHaveBeenCalledWith('9');
  });

  it('并发打开同一 fileId 共享同一个在途请求', async () => {
    const previewLink = mockPreviewLink({ renderMode: 'PDF' });

    await mountPanel({ fileId: '11' });
    await mountPanel({ fileId: '11' });
    await vi.waitFor(() => {
      expect(previewLink).toHaveBeenCalledTimes(1);
    });
  });
});
