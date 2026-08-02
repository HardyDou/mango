import { mount } from '@vue/test-utils';
import { defineComponent, h, nextTick } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { downloadUploadedFile } from '../../../api/upload';
import MangoAvatar from '../index.vue';

vi.mock('../../../api/upload', () => ({
  downloadUploadedFile: vi.fn(),
}));

const ElAvatarStub = defineComponent({
  name: 'ElAvatar',
  inheritAttrs: false,
  props: {
    src: { type: String, default: '' },
    size: { type: [String, Number], default: undefined },
    shape: { type: String, default: 'circle' },
    fit: { type: String, default: 'cover' },
  },
  emits: ['error'],
  setup(props, { attrs, slots }) {
    return () => h('div', { ...attrs, 'data-src': props.src }, slots.default?.());
  },
});

beforeEach(() => {
  vi.clearAllMocks();
  Object.defineProperty(URL, 'createObjectURL', {
    configurable: true,
    value: vi.fn(() => 'blob:mango-avatar-preview'),
  });
  Object.defineProperty(URL, 'revokeObjectURL', {
    configurable: true,
    value: vi.fn(),
  });
});

afterEach(() => {
  vi.restoreAllMocks();
});

function mountAvatar(source: string) {
  return mount(MangoAvatar, {
    props: { source },
    slots: { default: 'fallback' },
    global: { stubs: { ElAvatar: ElAvatarStub } },
  });
}

describe('MangoAvatar', () => {
  it('uses normal image addresses without requesting the file service', async () => {
    const wrapper = mountAvatar('https://cdn.example.com/avatar.png');
    await nextTick();

    expect(wrapper.attributes('data-src')).toBe('https://cdn.example.com/avatar.png');
    expect(downloadUploadedFile).not.toHaveBeenCalled();
  });

  it('downloads a managed file token and releases its object URL', async () => {
    vi.mocked(downloadUploadedFile).mockResolvedValue({
      data: new Blob(['avatar'], { type: 'image/png' }),
      headers: { 'content-type': 'image/png' },
    });
    const wrapper = mountAvatar('mango-file:2083696618563768322');
    await vi.waitFor(() => expect(wrapper.attributes('data-src')).toBe('blob:mango-avatar-preview'));

    expect(downloadUploadedFile).toHaveBeenCalledWith('2083696618563768322');
    wrapper.unmount();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mango-avatar-preview');
  });

  it('keeps the fallback visible for unsupported source values', async () => {
    const wrapper = mountAvatar('not-an-avatar-source');
    await nextTick();

    expect(wrapper.attributes('data-src')).toBe('');
    expect(wrapper.text()).toContain('fallback');
    expect(downloadUploadedFile).not.toHaveBeenCalled();
  });
});
