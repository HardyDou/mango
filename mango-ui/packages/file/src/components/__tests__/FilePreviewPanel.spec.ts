import { afterEach, describe, expect, it } from 'vitest';
import { createApp, defineComponent, h, nextTick, type App } from 'vue';
import FilePreviewPanel from '../FilePreviewPanel.vue';
import type { FilePreview } from '../../api/file';

const mountedApps: App[] = [];

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
  app.component('ElImage', defineComponent({ setup: () => () => h('img') }));
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

afterEach(() => {
  while (mountedApps.length) {
    mountedApps.pop()?.unmount();
  }
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
    const host = await mountPanel(createPreview({
      fileName: 'report.png',
      fileExt: 'png',
      contentType: 'image/png',
      directPreviewUrl: '/preview/report.png',
    }));

    expect(findNewWindowButton(host).disabled).toBe(false);
  });
});
