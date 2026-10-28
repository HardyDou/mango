import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { defineComponent, h, nextTick } from 'vue';

const listDictOptions = vi.fn();
const listDictTree = vi.fn();

vi.mock('../../../api/dict', () => ({
  listDictOptions: (...args: unknown[]) => listDictOptions(...args),
  listDictTree: (...args: unknown[]) => listDictTree(...args),
}));

const ElTagStub = defineComponent({
  name: 'ElTag',
  props: {
    type: { type: String, default: '' },
    effect: { type: String, default: 'light' },
    size: { type: String, default: 'default' },
  },
  setup(props, { slots }) {
    return () => h('span', { class: 'el-tag', 'data-type': props.type }, slots.default?.());
  },
});

const stubs = { 'el-tag': ElTagStub };

const treeData = [
  {
    label: '办公用品',
    value: 'office_supplies',
    children: [
      { label: '纸张', value: 'paper' },
      { label: '笔', value: 'pen' },
    ],
  },
  {
    label: '数码设备',
    value: 'digital_devices',
    children: [{ label: '电脑', value: 'computer' }],
  },
];

const flatData = [
  { label: '启用', value: '1' },
  { label: '禁用', value: '0' },
];

import DictDisplay from '../index.vue';

async function flush() {
  await nextTick();
  await nextTick();
  await nextTick();
}

describe('DictDisplay', () => {
  beforeEach(() => {
    listDictOptions.mockReset();
    listDictTree.mockReset();
    listDictOptions.mockResolvedValue(flatData);
    listDictTree.mockResolvedValue(treeData);
  });

  it('FLAT 单值默认展示 label', async () => {
    const wrapper = mount(DictDisplay, {
      props: { dictType: 'sys_normal_disable', value: '1' },
      global: { stubs },
    });
    await flush();

    expect(wrapper.text()).toBe('启用');
    expect(wrapper.find('.dict-display__empty').exists()).toBe(false);
  });

  it('FLAT 多值默认用顿号连接', async () => {
    const wrapper = mount(DictDisplay, {
      props: { dictType: 'sys_normal_disable', value: ['1', '0'] },
      global: { stubs },
    });
    await flush();

    expect(wrapper.text()).toBe('启用、禁用');
  });

  it('多值分隔符可配置', async () => {
    const wrapper = mount(DictDisplay, {
      props: { dictType: 'sys_normal_disable', value: ['1', '0'], separator: ' / ' },
      global: { stubs },
    });
    await flush();

    expect(wrapper.text()).toBe('启用 / 禁用');
  });

  it('TREE 单值默认展示末级 label', async () => {
    const wrapper = mount(DictDisplay, {
      props: { dictType: 'system_demo_tree', shape: 'TREE', value: 'paper' },
      global: { stubs },
    });
    await flush();

    expect(wrapper.text()).toBe('纸张');
  });

  it('TREE 可展示完整路径', async () => {
    const wrapper = mount(DictDisplay, {
      props: {
        dictType: 'system_demo_tree',
        shape: 'TREE',
        value: ['paper', 'computer'],
        displayFormat: 'path',
        displaySeparator: '-',
      },
      global: { stubs },
    });
    await flush();

    expect(wrapper.text()).toBe('办公用品-纸张、数码设备-电脑');
  });

  it('tag 模式按值渲染标签', async () => {
    const wrapper = mount(DictDisplay, {
      props: { dictType: 'sys_normal_disable', value: ['1', '0'], mode: 'tag', tagType: 'success' },
      global: { stubs },
    });
    await flush();

    const tags = wrapper.findAll('.el-tag');
    expect(tags).toHaveLength(2);
    expect(tags.map((tag) => tag.text())).toEqual(['启用', '禁用']);
    expect(tags[0].attributes('data-type')).toBe('success');
  });

  it('空值展示空占位文本', async () => {
    const wrapper = mount(DictDisplay, {
      props: { dictType: 'sys_normal_disable', value: undefined },
      global: { stubs },
    });
    await flush();

    expect(wrapper.text()).toBe('-');
  });

  it('未命中字典时回退展示原始值', async () => {
    const wrapper = mount(DictDisplay, {
      props: { dictType: 'sys_normal_disable', value: '9' },
      global: { stubs },
    });
    await flush();

    expect(wrapper.text()).toBe('9');
    expect(wrapper.find('.dict-display__item').attributes('data-known')).toBe('false');
  });

  it('字典加载失败时回退原始值并抛出 error', async () => {
    listDictOptions.mockRejectedValue(new Error('boom'));
    const wrapper = mount(DictDisplay, {
      props: { dictType: 'sys_normal_disable', value: ['1', '0'] },
      global: { stubs },
    });
    await flush();

    expect(wrapper.text()).toBe('1、0');
    expect(wrapper.emitted('error')).toBeTruthy();
  });

  it('group 过滤只显示组内值', async () => {
    listDictOptions.mockResolvedValue([
      { label: '值1', value: '1', groups: ['A'] },
      { label: '值2', value: '2', groups: ['A', 'B'] },
      { label: '值3', value: '3', groups: ['A', 'B'] },
      { label: '值4', value: '4', groups: ['B'] },
    ]);

    const wrapper = mount(DictDisplay, {
      props: { dictType: 'demo_group', value: ['1', '2', '3', '4'], group: 'A' },
      global: { stubs },
    });
    await flush();

    expect(wrapper.text()).toBe('值1、值2、值3');
  });

  it('未配置 group 时显示全部值，保持向后兼容', async () => {
    listDictOptions.mockResolvedValue([
      { label: '值1', value: '1', groups: ['A'] },
      { label: '值2', value: '2', groups: ['B'] },
      { label: '值3', value: '3', groups: [] },
    ]);

    const wrapper = mount(DictDisplay, {
      props: { dictType: 'demo_group', value: ['1', '2', '3'] },
      global: { stubs },
    });
    await flush();

    expect(wrapper.text()).toBe('值1、值2、值3');
  });

  it('include 与 exclude 在回显侧同样生效', async () => {
    listDictOptions.mockResolvedValue([
      { label: '值1', value: '1', groups: ['A'] },
      { label: '值2', value: '2', groups: ['A'] },
      { label: '值3', value: '3', groups: ['A'] },
    ]);

    const wrapper = mount(DictDisplay, {
      props: {
        dictType: 'demo_group',
        value: ['1', '2', '3'],
        group: 'A',
        includeValues: ['2', '3'],
        excludeValues: ['3'],
      },
      global: { stubs },
    });
    await flush();

    expect(wrapper.text()).toBe('值2');
  });

  it('暴露 getItems 与 reload(force)', async () => {
    const wrapper = mount(DictDisplay, {
      props: { dictType: 'system_demo_tree', shape: 'TREE', value: 'paper', displayFormat: 'path' },
      global: { stubs },
    });
    await flush();

    const items = (wrapper.vm as any).getItems();
    expect(items).toHaveLength(1);
    expect(items[0].path).toEqual(['办公用品', '纸张']);
    expect(items[0].known).toBe(true);

    listDictOptions.mockResolvedValue([{ label: '新值', value: '1' }]);
    await (wrapper.vm as any).reload(true);
    await flush();
    expect(listDictTree).toHaveBeenCalled();
  });
});
