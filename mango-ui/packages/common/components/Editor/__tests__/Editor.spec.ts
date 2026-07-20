import { mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import Editor from '../index.vue';
import { uploadImage } from '../../../api/upload';

const { fakeEditor } = vi.hoisted(() => ({
  fakeEditor: {
    getHtml: vi.fn(() => '<p>content</p>'),
    getText: vi.fn(() => 'content'),
    setHtml: vi.fn(),
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
  uploadImage: vi.fn(),
}));

describe('Editor 组件单元测试', () => {
  beforeEach(() => {
    vi.mocked(uploadImage).mockResolvedValue({
      id: '1935600000000000001',
      url: 'https://example.com/image.png',
      fileName: 'image.png',
      fileSize: 1024,
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

  it('imageValueType 为 token 时写入 mango-file token', async () => {
    const insertFn = vi.fn();
    const wrapper = mount(Editor, {
      props: {
        imageValueType: 'token',
      },
    });
    const config = wrapper.findComponent({ name: 'Editor' }).props('defaultConfig');

    await config.MENU_CONF.uploadImage.customUpload(new File(['image'], 'image.png'), insertFn);

    expect(insertFn).toHaveBeenCalledWith(
      'mango-file:1935600000000000001',
      'image.png',
      'mango-file:1935600000000000001',
    );
  });
});
