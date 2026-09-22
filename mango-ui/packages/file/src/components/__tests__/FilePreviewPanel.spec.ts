import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { createApp, defineComponent, h, nextTick, type App } from 'vue';
import FilePreviewPanel from '../FilePreviewPanel.vue';
import { fileApi, type FilePreview } from '../../api/file';

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

function createPreview(overrides: Partial<FilePreview> = {}): FilePreview {
  return {
    id: 'file-1',
    fileName: 'report.pdf',
    fileExt: 'pdf',
    fileSize: 1024,
    contentType: 'application/pdf',
    previewable: true,
    previewUrl: '',
    downloadUrl: '',
    ...overrides,
  };
}

async function mountPanel(
  preview: FilePreview | null,
  options: { fileId?: string; fitContainer?: boolean; showActions?: boolean } = {},
) {
  const host = document.createElement('div');
  document.body.appendChild(host);
  const app = createApp(FilePreviewPanel, {
    fileId: options.fileId,
    preview,
    showActions: options.showActions ?? true,
    fitContainer: options.fitContainer ?? false,
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

function findNewWindowButton(host: HTMLElement): HTMLButtonElement {
  const buttons = Array.from(host.querySelectorAll('button'));
  const button = buttons.find((item) => item.textContent?.includes('新窗口预览'));
  if (!button) {
    throw new Error('New window preview button was not rendered.');
  }
  return button;
}

beforeAll(() => {
  happyDomSettings.disableIframePageLoading = true;
});

afterAll(() => {
  happyDomSettings.disableIframePageLoading = iframePageLoadingDisabled;
});

afterEach(() => {
  while (mountedApps.length) {
    mountedApps.pop()?.unmount();
  }
  vi.restoreAllMocks();
  document.body.innerHTML = '';
});

describe('FilePreviewPanel', () => {
  it('shows the default loading state while preview metadata is loading', async () => {
    let resolvePreview!: (value: FilePreview) => void;
    const previewRequest = vi.spyOn(fileApi, 'preview').mockReturnValue(
      new Promise((resolve) => {
        resolvePreview = resolve;
      }),
    );
    const host = await mountPanel(null, { fileId: '1' });
    const panel = host.querySelector('.file-preview-panel');

    await vi.waitFor(() => expect(previewRequest).toHaveBeenCalledWith('1'));
    expect(panel?.getAttribute('data-loading')).toBe('true');

    resolvePreview(createPreview({ directPreviewUrl: '/preview/report.pdf' }));
    await vi.waitFor(() => {
      expect(panel?.getAttribute('data-loading')).toBe('false');
      expect(host.querySelector('.preview-stage')).not.toBeNull();
    });
  });

  it('passes the authenticated preview endpoint directly to the image viewer', async () => {
    const host = await mountPanel(
      createPreview({
        fileName: 'diagram.png',
        fileExt: 'png',
        contentType: 'image/png',
      }),
    );
    await vi.waitFor(() => {
      expect(host.querySelector('.preview-image-viewer [data-image-viewer] .el-image-viewer__img')?.getAttribute('src')).toBe(
        'http://localhost:3000/api/file/files/preview-content?id=file-1',
      );
    });
    expect(vi.spyOn(fileApi, 'previewContent')).not.toHaveBeenCalled();
  });

  it('keeps a direct authenticated preview URL when content is protected', async () => {
    const host = await mountPanel(
      createPreview({
        fileName: 'diagram.png',
        fileExt: 'png',
        contentType: 'image/png',
      }),
    );
    await vi.waitFor(() => {
      expect(host.querySelector('.preview-image-viewer')).not.toBeNull();
    });
    expect(host.querySelector('.preview-error-state')).toBeNull();
  });

  it('keeps the natural-height mode by default', async () => {
    const host = await mountPanel(createPreview({ directPreviewUrl: '/preview/report.pdf' }));

    expect(host.querySelector('.file-preview-panel--fit-container')).toBeNull();
    expect(host.querySelector('.preview-stage')).not.toBeNull();
  });

  it('enables the container-fill class without requiring layout variables', async () => {
    const host = await mountPanel(createPreview({ directPreviewUrl: '/preview/report.pdf' }), { fitContainer: true });

    const panel = host.querySelector('.file-preview-panel');
    expect(panel?.classList.contains('file-preview-panel--fit-container')).toBe(true);
    expect(panel?.getAttribute('style')).toBeNull();
  });

  it('keeps the stage as the only flex region when actions are hidden', async () => {
    const host = await mountPanel(createPreview({ directPreviewUrl: '/preview/report.pdf' }), {
      fitContainer: true,
      showActions: false,
    });

    expect(host.querySelector('.preview-actions')).toBeNull();
    expect(host.querySelector('.preview-stage')).not.toBeNull();
  });

  it.each([
    [
      'image',
      createPreview({
        fileName: 'diagram.png',
        fileExt: 'png',
        contentType: 'image/png',
        directPreviewUrl: '/preview/diagram.png',
      }),
      '.preview-image-viewer',
    ],
    [
      'video',
      createPreview({
        fileName: 'demo.mp4',
        fileExt: 'mp4',
        contentType: 'video/mp4',
        directPreviewUrl: '/preview/demo.mp4',
      }),
      'video.preview-media',
    ],
    [
      'audio',
      createPreview({
        fileName: 'recording.mp3',
        fileExt: 'mp3',
        contentType: 'audio/mpeg',
        directPreviewUrl: '/preview/recording.mp3',
      }),
      'audio.preview-audio',
    ],
    [
      'Office document',
      createPreview({
        fileName: 'report.docx',
        fileExt: 'docx',
        contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        documentPreviewUrl: '/document/report.docx',
      }),
      'iframe.preview-frame',
    ],
  ])('renders %s content inside the container-fill stage', async (_label, preview, selector) => {
    const host = await mountPanel(preview, { fitContainer: true });

    expect(host.querySelector(`.preview-stage > ${selector}`)).not.toBeNull();
  });

  it('renders images as a persistent inline Element Plus viewer by default', async () => {
    const host = await mountPanel(
      createPreview({
        fileName: 'diagram.png',
        fileExt: 'png',
        contentType: 'image/png',
        directPreviewUrl: '/preview/diagram.png',
      }),
    );

    const viewerImage = host.querySelector('.preview-image-viewer [data-image-viewer] .el-image-viewer__img');
    expect(viewerImage?.getAttribute('src')).toBe('http://localhost:3000/preview/diagram.png');
    expect(viewerImage?.getAttribute('data-infinite')).toBe('false');
    expect(viewerImage?.getAttribute('data-teleported')).toBe('false');
    expect(viewerImage?.getAttribute('data-close-on-press-escape')).toBe('false');
    expect(host.querySelector('.preview-image-viewer .el-image-viewer__actions')).not.toBeNull();
    expect(host.querySelector('el-image')).toBeNull();
  });

  it('keeps empty and download-only states in fill-mode containers', async () => {
    const emptyHost = await mountPanel(null, { fitContainer: true });
    expect(emptyHost.querySelector('.preview-empty')).not.toBeNull();

    mountedApps.pop()?.unmount();
    emptyHost.remove();

    const placeholderHost = await mountPanel(
      createPreview({
        fileName: 'report.docx',
        fileExt: 'docx',
        contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        previewable: false,
      }),
      { fitContainer: true },
    );
    expect(placeholderHost.querySelector('.preview-stage > .preview-placeholder')).not.toBeNull();
  });

  it('keeps the new window preview button rendered when preview url is unavailable', async () => {
    const host = await mountPanel(
      createPreview({
        fileName: 'report.docx',
        fileExt: 'docx',
        contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        previewable: false,
      }),
    );

    expect(findNewWindowButton(host).disabled).toBe(true);
  });

  it('enables the new window preview button when preview url is available', async () => {
    const previewContent = vi.spyOn(fileApi, 'previewContent');
    const host = await mountPanel(
      createPreview({
        fileName: 'report.png',
        fileExt: 'png',
        contentType: 'image/png',
        directPreviewUrl: '/preview/report.png',
      }),
    );

    expect(findNewWindowButton(host).disabled).toBe(false);
    expect(previewContent).not.toHaveBeenCalled();
  });

  it.each([
    ['image', 'diagram.png', 'png', 'image/png', '.el-image-viewer__img'],
    ['video', 'demo.mp4', 'mp4', 'video/mp4', 'video'],
    ['audio', 'recording.mp3', 'mp3', 'audio/mpeg', 'audio'],
  ])(
    'loads %s directly from the authenticated preview URL',
    async (_label, fileName, fileExt, contentType, selector) => {
      const previewContent = vi.spyOn(fileApi, 'previewContent').mockResolvedValue({
        data: new Blob(['preview-content'], { type: contentType }),
        headers: { 'content-type': contentType },
      });
      const previewLink = vi.spyOn(fileApi, 'previewLink');

      const host = await mountPanel(
        createPreview({
          fileName,
          fileExt,
          contentType,
          previewUrl: 'http://127.0.0.1:18045/file/files/preview-content?id=file-1',
          documentPreviewUrl: 'http://127.0.0.1:18045/file-preview/files/preview?fileId=file-1',
        }),
      );

      await vi.waitFor(() => {
        expect(host.querySelector(selector)?.getAttribute('src')).toBe(
          'http://127.0.0.1:18045/file/files/preview-content?id=file-1',
        );
      });
      expect(previewContent).not.toHaveBeenCalled();
      expect(previewLink).not.toHaveBeenCalled();
    },
  );

  it('loads a local image backend direct URL without creating a blob', async () => {
    const previewContent = vi.spyOn(fileApi, 'previewContent').mockResolvedValue({
      data: new Blob(['preview-content'], { type: 'image/png' }),
      headers: { 'content-type': 'image/png' },
    });

    const host = await mountPanel(
      createPreview({
        fileName: 'report.png',
        fileExt: 'png',
        contentType: 'image/png',
        directPreviewUrl: 'http://127.0.0.1:18002/file/local-objects/local/report.png',
      }),
    );

    await vi.waitFor(() => {
      expect(host.querySelector('.preview-image-viewer [data-image-viewer] .el-image-viewer__img')?.getAttribute('src')).toBe(
        'http://127.0.0.1:18002/file/local-objects/local/report.png',
      );
    });
    expect(previewContent).not.toHaveBeenCalled();
  });

  it('loads PDF through the tokenized preview-entry instead of preview-content', async () => {
    const previewLink = vi.spyOn(fileApi, 'previewLink').mockResolvedValue({
      fileId: 'file-1',
      fileName: 'report.pdf',
      previewUrl: '/api/file-preview/files/preview-entry?token=preview-token',
    });
    const previewContent = vi.spyOn(fileApi, 'previewContent');

    const host = await mountPanel(
      createPreview({
        fileName: 'report.pdf',
        fileExt: 'pdf',
        contentType: 'application/pdf',
        previewUrl: 'http://127.0.0.1:18045/file/files/preview-content?id=file-1',
        documentPreviewUrl: 'http://127.0.0.1:18045/file-preview/files/preview?fileId=file-1',
      }),
    );

    await vi.waitFor(() => {
      expect(previewLink).toHaveBeenCalledWith('file-1');
      expect(host.querySelector('iframe')?.getAttribute('src')).toBe(
        '/api/file-preview/files/preview-entry?token=preview-token',
      );
    });
    expect(previewContent).not.toHaveBeenCalled();
  });

  it('loads a complex document through the tokenized preview service link', async () => {
    const previewLink = vi.spyOn(fileApi, 'previewLink').mockResolvedValue({
      fileId: 'file-1',
      fileName: 'report.docx',
      previewUrl: '/api/file-preview/files/preview-entry?token=preview-token',
    });
    const previewContent = vi.spyOn(fileApi, 'previewContent');

    const host = await mountPanel(
      createPreview({
        fileName: 'report.docx',
        fileExt: 'docx',
        contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        previewUrl: 'http://127.0.0.1:18045/file/files/preview-content?id=file-1',
        documentPreviewUrl: 'http://127.0.0.1:18045/file-preview/files/preview?fileId=file-1',
      }),
    );

    await vi.waitFor(() => {
      expect(previewLink).toHaveBeenCalledWith('file-1');
      expect(host.querySelector('iframe')?.getAttribute('src')).toBe(
        '/api/file-preview/files/preview-entry?token=preview-token',
      );
    });
    expect(previewContent).not.toHaveBeenCalled();
  });
});
