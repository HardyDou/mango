import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import Editor from '../index.vue';
import { getUploadedFileDetail, importRemoteImage, uploadImage } from '../../../api/upload';

const { editorState, fakeEditor } = vi.hoisted(() => ({
  editorState: { html: '<p>content</p>' },
  fakeEditor: {
    getHtml: vi.fn(() => editorState.html),
    getText: vi.fn(() => 'content'),
    setHtml: vi.fn(),
    dangerouslyInsertHtml: vi.fn((html: string) => {
      editorState.html = html;
    }),
    enable: vi.fn(),
    disable: vi.fn(),
    destroy: vi.fn(),
    blur: vi.fn(),
  },
}));

vi.mock('@wangeditor/editor-for-vue', () => ({
  Editor: {
    name: 'Editor',
    props: ['modelValue', 'defaultConfig', 'mode', 'disabled'],
    emits: ['on-created', 'on-change', 'update:modelValue'],
    template: '<div class="editor-content"></div>',
    mounted() {
      this.$emit('on-created', fakeEditor);
    },
  },
  Toolbar: {
    name: 'Toolbar',
    props: ['editor', 'defaultConfig', 'mode'],
    template: '<div class="editor-toolbar"></div>',
  },
}));

vi.mock('../../../api/upload', () => ({
  fileToken: (id?: string) => (id ? `mango-file:${id}` : ''),
  getUploadedFileDetail: vi.fn(),
  importRemoteImage: vi.fn(),
  uploadImage: vi.fn(),
}));

describe('Editor 组件单元测试', () => {
  it('支持 toolbar-actions slot', async () => {
    const wrapper = mount(Editor, {
      slots: {
        'toolbar-actions': '<button data-testid="attachment-action">附件</button>',
      },
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.find('[data-testid="attachment-action"]').exists()).toBe(true);
    expect(wrapper.find('.editor-toolbar-actions').exists()).toBe(true);
  });

  beforeEach(() => {
    editorState.html = '<p>content</p>';
    vi.mocked(uploadImage).mockResolvedValue({
      id: '1935600000000000001',
      url: 'https://example.com/image.png',
      fileName: 'image.png',
      fileSize: 1024,
    });
    vi.mocked(importRemoteImage).mockResolvedValue({
      id: '1935600000000000002',
      url: 'https://example.com/remote.png',
      previewUrl: 'https://example.com/remote.png',
      fileName: 'remote.png',
      fileSize: 2048,
    });
    vi.mocked(getUploadedFileDetail).mockResolvedValue({
      id: '1935600000000000003',
      url: 'mango-file:1935600000000000003',
      previewUrl: 'https://example.com/existing.png',
      fileName: 'existing.png',
      fileSize: 512,
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('默认保留完整工具栏配置', async () => {
    const wrapper = mount(Editor);

    await wrapper.vm.$nextTick();
    const toolbar = wrapper.findComponent({ name: 'Toolbar' });

    expect(toolbar.props('defaultConfig').toolbarKeys).toContain('bold');
    expect(toolbar.props('defaultConfig').toolbarKeys).toContain('insertImage');
    expect(toolbar.props('defaultConfig').toolbarKeys).toContain('fullScreen');
  });

  it('simple 模式不传完整工具栏配置', async () => {
    const wrapper = mount(Editor, {
      props: {
        mode: 'simple',
      },
    });

    await wrapper.vm.$nextTick();
    const toolbar = wrapper.findComponent({ name: 'Toolbar' });

    expect(toolbar.props('defaultConfig')).toEqual({});
  });

  it('支持通过 toolbarKeys 自定义展示按钮', async () => {
    const toolbarKeys = ['bold', 'color', '|', 'numberedList', 'bulletedList', '|', 'uploadImage'];
    const wrapper = mount(Editor, {
      props: {
        toolbarKeys,
      },
    });

    await wrapper.vm.$nextTick();
    const toolbar = wrapper.findComponent({ name: 'Toolbar' });

    expect(toolbar.props('defaultConfig')).toEqual({ toolbarKeys });
  });

  it('图片上传默认写入可访问 URL', async () => {
    const insertFn = vi.fn();
    const wrapper = mount(Editor);
    const config = wrapper.findComponent({ name: 'Editor' }).props('defaultConfig');

    await config.MENU_CONF.uploadImage.customUpload(new File(['image'], 'image.png'), insertFn);

    expect(uploadImage).toHaveBeenCalled();
    expect(insertFn).toHaveBeenCalledWith(
      'https://example.com/image.png',
      'image.png',
      'https://example.com/image.png',
    );
  });

  it('imageValueType 为 id 时写入文件 ID', async () => {
    const insertFn = vi.fn();
    const wrapper = mount(Editor, {
      props: {
        imageValueType: 'id',
      },
    });
    const config = wrapper.findComponent({ name: 'Editor' }).props('defaultConfig');

    await config.MENU_CONF.uploadImage.customUpload(new File(['image'], 'image.png'), insertFn);

    expect(insertFn).toHaveBeenCalledWith('1935600000000000001', 'image.png', '1935600000000000001');
  });

  it('imageValueType 为 token 时编辑态写入预览地址，由出站序列化为 mango-file token', async () => {
    const insertFn = vi.fn();
    const wrapper = mount(Editor, {
      props: {
        imageValueType: 'token',
      },
    });
    const config = wrapper.findComponent({ name: 'Editor' }).props('defaultConfig');

    await config.MENU_CONF.uploadImage.customUpload(new File(['image'], 'image.png'), insertFn);

    expect(insertFn).toHaveBeenCalledWith('https://example.com/image.png', 'image.png', '');
  });

  it('pasteImageMode 默认模式不拦截浏览器粘贴', () => {
    const wrapper = mount(Editor, {
      props: { imageValueType: 'token' },
    });
    const config = wrapper.findComponent({ name: 'Editor' }).props('defaultConfig');
    const callback = vi.fn();
    const preventDefault = vi.fn();

    config.customPaste(
      fakeEditor,
      {
        clipboardData: {
          getData: () => '<p>text<img src="data:image/png;base64,aW1hZ2U="></p>',
          files: [],
        },
        preventDefault,
      },
      callback,
    );

    expect(callback).toHaveBeenCalledWith(true);
    expect(preventDefault).not.toHaveBeenCalled();
    expect(uploadImage).not.toHaveBeenCalled();
  });

  it('托管粘贴上传 File 和 Data URI，并复用已有 token', async () => {
    const wrapper = mount(Editor, {
      props: {
        imageValueType: 'token',
        pasteImageMode: 'upload',
      },
    });
    const config = wrapper.findComponent({ name: 'Editor' }).props('defaultConfig');
    const callback = vi.fn();
    const preventDefault = vi.fn();
    const clipboardFile = new File(['clipboard'], 'clipboard.png', { type: 'image/png' });

    config.customPaste(
      fakeEditor,
      {
        clipboardData: {
          getData: () =>
            '<p>保留文字<img src="data:image/png;base64,aW1hZ2U="><img src="mango-file:1935600000000000003"></p>',
          files: [clipboardFile],
        },
        preventDefault,
      },
      callback,
    );
    await flushPromises();

    expect(callback).toHaveBeenCalledWith(false);
    expect(preventDefault).toHaveBeenCalledOnce();
    expect(uploadImage).toHaveBeenCalledTimes(2);
    expect(getUploadedFileDetail).toHaveBeenCalledWith('1935600000000000003');
    expect(importRemoteImage).not.toHaveBeenCalled();
    const emitted = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string;
    expect(emitted).toContain('保留文字');
    expect(emitted.match(/mango-file:/g)).toHaveLength(3);
    expect(emitted).not.toContain('data:image');
    expect(emitted).not.toContain('example.com');
  });

  it('远程图片混合粘贴局部失败时保留非图片 HTML', async () => {
    vi.mocked(importRemoteImage)
      .mockResolvedValueOnce({
        id: '1935600000000000004',
        url: 'https://example.com/managed.png',
        previewUrl: 'https://example.com/managed.png',
        fileName: 'managed.png',
        fileSize: 1024,
      })
      .mockRejectedValueOnce(new Error('remote import failed'));
    const wrapper = mount(Editor, {
      props: {
        imageValueType: 'token',
        pasteImageMode: 'upload',
      },
    });
    const config = wrapper.findComponent({ name: 'Editor' }).props('defaultConfig');

    config.customPaste(
      fakeEditor,
      {
        clipboardData: {
          getData: () =>
            '<p>中文<a href="/detail">链接</a></p><ul><li>列表</li></ul><img src="https://public.example/ok.png"><img src="https://public.example/fail.png">',
          files: [],
        },
        preventDefault: vi.fn(),
      },
      vi.fn(),
    );
    await flushPromises();

    expect(importRemoteImage).toHaveBeenCalledTimes(2);
    const emitted = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string;
    expect(emitted).toContain('中文');
    expect(emitted).toContain('href="/detail"');
    expect(emitted).toContain('<li>列表</li>');
    expect(emitted).toContain('mango-file:1935600000000000004');
    expect(emitted).not.toContain('public.example');
    expect(wrapper.emitted('image-error')).toBeTruthy();
  });
});
