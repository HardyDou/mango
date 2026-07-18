import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { createApp, defineComponent, h, nextTick, type App } from 'vue';
import FilePreviewPanel from '../FilePreviewPanel.vue';
import { fileApi, type FilePreview } from '../../api/file';

const mountedApps: App[] = [];
const happyDomSettings = (window as Window & {
  happyDOM: { settings: { disableIframePageLoading: boolean } };
}).happyDOM.settings;
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

const ElImageStub = defineComponent({
  name: 'ElImage',
  props: {
    src: String,
  },
  setup(props) {
    return () => h('img', { src: props.src });
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

async function mountPanel(preview: FilePreview) {
  const host = document.createElement('div');
  document.body.appendChild(host);
  const app = createApp(FilePreviewPanel, {
    preview,
    showActions: true,
  });
  app.component('ElButton', ElButtonStub);
  app.component('ElTag', ElTagStub);
  app.component('ElSkeleton', defineComponent({ setup: () => () => h('div') }));
  app.component('ElEmpty', defineComponent({ setup: () => () => h('div') }));
  app.component('ElImage', ElImageStub);
  app.component('ElIcon', defineComponent({ setup: (_props, { slots }) => () => h('span', slots.default?.()) }));
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
  it('keeps the new window preview button rendered when preview url is unavailable', async () => {
    const host = await mountPanel(createPreview({
      fileName: 'report.docx',
      fileExt: 'docx',
      contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      previewable: false,
    }));

    expect(findNewWindowButton(host).disabled).toBe(true);
  });

  it('enables the new window preview button when preview url is available', async () => {
    const previewContent = vi.spyOn(fileApi, 'previewContent');
    const host = await mountPanel(createPreview({
      fileName: 'report.png',
      fileExt: 'png',
      contentType: 'image/png',
      directPreviewUrl: '/preview/report.png',
    }));

    expect(findNewWindowButton(host).disabled).toBe(false);
    expect(previewContent).not.toHaveBeenCalled();
  });

  it.each([
    ['PDF', 'report.pdf', 'pdf', 'application/pdf', 'iframe'],
    ['image', 'diagram.png', 'png', 'image/png', 'img'],
    ['video', 'demo.mp4', 'mp4', 'video/mp4', 'video'],
    ['audio', 'recording.mp3', 'mp3', 'audio/mpeg', 'audio'],
  ])('loads %s without a direct URL as an authenticated blob', async (
    _label,
    fileName,
    fileExt,
    contentType,
    selector,
  ) => {
    const objectUrl = `blob:preview-${fileExt}`;
    const createObjectUrl = vi.spyOn(URL, 'createObjectURL').mockReturnValue(objectUrl);
    const revokeObjectUrl = vi.spyOn(URL, 'revokeObjectURL');
    const previewContent = vi.spyOn(fileApi, 'previewContent').mockResolvedValue({
      data: new Blob(['preview-content'], { type: contentType }),
      headers: { 'content-type': contentType },
    });
    const previewLink = vi.spyOn(fileApi, 'previewLink');

    const host = await mountPanel(createPreview({
      fileName,
      fileExt,
      contentType,
      previewUrl: 'http://127.0.0.1:18045/file/files/preview-content?id=file-1',
      documentPreviewUrl: 'http://127.0.0.1:18045/file-preview/files/preview?fileId=file-1',
    }));

    await vi.waitFor(() => {
      expect(previewContent).toHaveBeenCalledWith('file-1');
      expect(host.querySelector(selector)?.getAttribute('src')).toBe(objectUrl);
    });
    expect(createObjectUrl).toHaveBeenCalledOnce();
    expect(previewLink).not.toHaveBeenCalled();

    mountedApps.pop()?.unmount();
    expect(revokeObjectUrl).toHaveBeenCalledWith(objectUrl);
  });

  it('loads a complex document through the tokenized preview service link', async () => {
    const previewLink = vi.spyOn(fileApi, 'previewLink').mockResolvedValue({
      fileId: 'file-1',
      fileName: 'report.docx',
      previewUrl: '/api/file-preview/files/preview-entry?token=preview-token',
    });
    const previewContent = vi.spyOn(fileApi, 'previewContent');

    const host = await mountPanel(createPreview({
      fileName: 'report.docx',
      fileExt: 'docx',
      contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      previewUrl: 'http://127.0.0.1:18045/file/files/preview-content?id=file-1',
      documentPreviewUrl: 'http://127.0.0.1:18045/file-preview/files/preview?fileId=file-1',
    }));

    await vi.waitFor(() => {
      expect(previewLink).toHaveBeenCalledWith('file-1');
      expect(host.querySelector('iframe')?.getAttribute('src'))
        .toBe('/api/file-preview/files/preview-entry?token=preview-token');
    });
    expect(previewContent).not.toHaveBeenCalled();
  });
});
