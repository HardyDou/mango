import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { defineComponent, h, nextTick } from 'vue';

const listDictOptions = vi.fn();
const listDictTree = vi.fn();

vi.mock('../../../api/dict', () => ({
  listDictOptions: (...args: unknown[]) => listDictOptions(...args),
  listDictTree: (...args: unknown[]) => listDictTree(...args),
}));

const ElSelectStub = defineComponent({
  name: 'ElSelect',
  props: {
    modelValue: { default: undefined },
    multiple: { type: Boolean, default: false },
  },
  setup(props, { slots }) {
    return () => h('div', { class: 'el-select', 'data-multiple': String(props.multiple) }, slots.default?.());
  },
});

const ElOptionStub = defineComponent({
  name: 'ElOption',
  props: {
    label: { type: String, default: '' },
    value: { default: undefined },
  },
  setup(props) {
    return () => h('div', { class: 'el-option', 'data-value': String(props.value) }, props.label);
  },
});

const halfCheckedNodesMock: any[] = [];

const ElTreeSelectStub = defineComponent({
  name: 'ElTreeSelect',
  props: {
    modelValue: { default: undefined },
    data: { type: Array, default: () => [] },
    multiple: { type: Boolean, default: false },
    checkStrictly: { type: Boolean, default: false },
    displayFormat: { type: String, default: 'label' },
    displaySeparator: { type: String, default: ' / ' },
  },
  emits: ['update:modelValue', 'change', 'clear', 'check-change'],
  setup(props, { slots, expose }) {
    expose({
      getHalfCheckedNodes: () => halfCheckedNodesMock,
    });
    const flatten = (nodes: any[], parents: any[] = []): any[] =>
      nodes.flatMap((node) => {
        const path = [...parents, node];
        return [{ node, path }, ...flatten(node.children ?? [], path)];
      });

    return () => {
      const values = Array.isArray(props.modelValue) ? props.modelValue : [props.modelValue];
      const items = values
        .filter((value) => value !== undefined && value !== '')
        .map((value) => {
          const matched = flatten(props.data as any[]).find((item) => item.node.value === value);
          const label = matched ? matched.node.label : String(value);
          const text = matched && props.displayFormat === 'path'
            ? matched.path.map((node: any) => node.label).join(props.displaySeparator)
            : label;
          return { value, label, text };
        });

      return h(
        'div',
        {
          class: 'el-tree-select',
          'data-multiple': String(props.multiple),
          'data-check-strictly': String(props.checkStrictly),
        },
        items.map((item) =>
          h(
            'span',
            { class: 'selected-label' },
            slots.label ? slots.label({ label: item.label, value: item.value }) : item.text,
          ),
        ),
      );
    };
  },
});

const ElAlertStub = defineComponent({
  name: 'ElAlert',
  props: { title: { type: String, default: '' } },
  setup(props) {
    return () => h('div', { class: 'el-alert' }, props.title);
  },
});

const stubs = {
  'el-select': ElSelectStub,
  'el-option': ElOptionStub,
  'el-tree-select': ElTreeSelectStub,
  'el-alert': ElAlertStub,
};

const treeData = [
  {
    label: '办公用品',
    value: 'office_supplies',
    children: [
      { label: '纸张', value: 'paper', parentValue: 'office_supplies' },
      { label: '笔', value: 'pen', parentValue: 'office_supplies' },
    ],
  },
];

const flatData = [
  { label: '启用', value: '1' },
  { label: '禁用', value: '0' },
];

import DictSelect from '../index.vue';

async function flush() {
  await nextTick();
  await nextTick();
  await nextTick();
}

describe('DictSelect', () => {
  beforeEach(() => {
    listDictOptions.mockReset();
    listDictTree.mockReset();
    listDictOptions.mockResolvedValue(flatData);
    listDictTree.mockResolvedValue(treeData);
  });

  it('FLAT 形态加载普通字典并渲染选项', async () => {
    const wrapper = mount(DictSelect, {
      props: { dictType: 'sys_normal_disable', shape: 'FLAT', modelValue: '1' },
      global: { stubs },
    });
    await flush();

    expect(listDictOptions).toHaveBeenCalledWith('sys_normal_disable', false);
    expect(listDictTree).not.toHaveBeenCalled();
    expect(wrapper.find('.el-select').exists()).toBe(true);
    expect(wrapper.findAll('.el-option').map((item) => item.text())).toEqual(['启用', '禁用']);
  });

  it('TREE 形态加载树形字典并按 label 回显', async () => {
    const wrapper = mount(DictSelect, {
      props: { dictType: 'system_demo_tree', shape: 'TREE', modelValue: 'paper' },
      global: { stubs },
    });
    await flush();

    expect(listDictTree).toHaveBeenCalled();
    expect(wrapper.find('.el-tree-select').exists()).toBe(true);
    expect(wrapper.find('.selected-label').text()).toBe('纸张');
  });

  it('TREE 形态 displayFormat=path 时回显完整路径', async () => {
    const wrapper = mount(DictSelect, {
      props: {
        dictType: 'system_demo_tree',
        shape: 'TREE',
        modelValue: ['paper', 'pen'],
        multiple: true,
        displayFormat: 'path',
        displaySeparator: '-',
      },
      global: { stubs },
    });
    await flush();

    expect(wrapper.find('.el-tree-select').attributes('data-multiple')).toBe('true');
    expect(wrapper.findAll('.selected-label').map((item) => item.text())).toEqual([
      '办公用品-纸张',
      '办公用品-笔',
    ]);
  });

  it('leafOnly 默认禁用父节点，关闭后父节点可选', async () => {
    const wrapper = mount(DictSelect, {
      props: { dictType: 'system_demo_tree', shape: 'TREE', multiple: true },
      global: { stubs },
    });
    await flush();
    const firstNode = () => (wrapper.findComponent(ElTreeSelectStub).props('data') as any[])[0];
    expect(firstNode().disabled).toBe(true);

    await wrapper.setProps({ leafOnly: false });
    await flush();
    expect(firstNode().disabled).toBe(false);
  });

  it('checkStrictly 透传，支持父子不联动', async () => {
    const wrapper = mount(DictSelect, {
      props: { dictType: 'system_demo_tree', shape: 'TREE', multiple: true, checkStrictly: true },
      global: { stubs },
    });
    await flush();

    expect(wrapper.find('.el-tree-select').attributes('data-check-strictly')).toBe('true');
  });

  it('change 事件返回 value、节点和路径', async () => {
    const wrapper = mount(DictSelect, {
      props: { dictType: 'system_demo_tree', shape: 'TREE', modelValue: ['paper'] },
      global: { stubs },
    });
    await flush();

    wrapper.findComponent(ElTreeSelectStub).vm.$emit('change', ['paper']);
    await flush();

    const emitted = wrapper.emitted('change');
    expect(emitted).toBeTruthy();
    const [value, nodes, paths] = emitted![0] as [string[], any[], any[][]];
    expect(value).toEqual(['paper']);
    expect(nodes.map((node) => node.label)).toEqual(['纸张']);
    expect(paths[0].map((node) => node.label)).toEqual(['办公用品', '纸张']);
  });

  it('暴露 reload 强制刷新并更新选项', async () => {
    const wrapper = mount(DictSelect, {
      props: { dictType: 'system_demo_tree', shape: 'TREE' },
      global: { stubs },
    });
    await flush();

    listDictTree.mockResolvedValue([{ label: '新根节点', value: 'new_root', children: [] }]);
    await (wrapper.vm as any).reload(true);
    await flush();

    expect(listDictTree).toHaveBeenLastCalledWith('system_demo_tree', true);
    expect((wrapper.findComponent(ElTreeSelectStub).props('data') as any[])[0].label).toBe('新根节点');
  });

  it('字典加载失败时展示错误并抛出 error 事件', async () => {
    listDictTree.mockRejectedValue(new Error('字典加载失败'));
    const wrapper = mount(DictSelect, {
      props: { dictType: 'system_demo_tree', shape: 'TREE' },
      global: { stubs },
    });
    await flush();

    expect(wrapper.find('.el-alert').text()).toContain('字典加载失败');
    expect(wrapper.emitted('error')).toBeTruthy();
  });

  it('空结果时不渲染选项且不报错', async () => {
    listDictOptions.mockResolvedValue([]);
    const wrapper = mount(DictSelect, {
      props: { dictType: 'empty_dict', shape: 'FLAT' },
      global: { stubs },
    });
    await flush();

    expect(wrapper.findAll('.el-option')).toHaveLength(0);
    expect(wrapper.emitted('error')).toBeFalsy();
  });

  it('numberValue 开启时 FLAT 选项值为数字，与数字型 v-model 匹配', async () => {
    const wrapper = mount(DictSelect, {
      props: { dictType: 'sys_normal_disable', shape: 'FLAT', numberValue: true, modelValue: 1 },
      global: { stubs },
    });
    await flush();

    const options = wrapper.findAll('.el-option');
    expect(options.map((item) => item.attributes('data-value'))).toEqual(['1', '0']);
    expect((wrapper.findComponent(ElOptionStub).props('value') as number)).toBe(1);
  });

  it('check-change 只在半选状态真实变化时触发父节点事件', async () => {
    const wrapper = mount(DictSelect, {
      props: { dictType: 'system_demo_tree', shape: 'TREE', multiple: true },
      global: { stubs },
    });
    await flush();

    halfCheckedNodesMock.length = 0;
    halfCheckedNodesMock.push({ label: '办公用品', value: 'office_supplies', children: [] });
    wrapper.findComponent(ElTreeSelectStub).vm.$emit('check-change', { label: '笔', value: 'pen' }, true, false);
    await flush();

    const parentEvents = () =>
      (wrapper.emitted('check-change') ?? []).filter(
        (event) => (event[0] as any).value === 'office_supplies' && event[2] === true,
      );
    expect(parentEvents()).toHaveLength(1);

    // 半选状态未变化时不应重复通知
    wrapper.findComponent(ElTreeSelectStub).vm.$emit('check-change', { label: '纸张', value: 'paper' }, true, false);
    await flush();
    expect(parentEvents()).toHaveLength(1);

    // 退出半选时补发 indeterminate=false
    halfCheckedNodesMock.length = 0;
    wrapper.findComponent(ElTreeSelectStub).vm.$emit('check-change', { label: '纸张', value: 'paper' }, false, false);
    await flush();
    const cleared = (wrapper.emitted('check-change') ?? []).filter(
      (event) => (event[0] as any).value === 'office_supplies' && event[2] === false && event[1] === false,
    );
    expect(cleared.length).toBeGreaterThan(0);
  });

  it('clear 方法清空单选并触发 clear 事件', async () => {
    const wrapper = mount(DictSelect, {
      props: { dictType: 'system_demo_tree', shape: 'TREE', modelValue: 'paper' },
      global: { stubs },
    });
    await flush();

    (wrapper.vm as any).clear();
    await flush();

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual([undefined]);
    expect(wrapper.emitted('clear')).toBeTruthy();
  });

  it('多选 clear 方法返回空数组', async () => {
    const wrapper = mount(DictSelect, {
      props: { dictType: 'system_demo_tree', shape: 'TREE', multiple: true, modelValue: ['paper'] },
      global: { stubs },
    });
    await flush();

    (wrapper.vm as any).clear();
    await flush();

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual([[]]);
  });

  it('getSelectedNodes 和 getSelectedPaths 返回当前选择', async () => {
    const wrapper = mount(DictSelect, {
      props: { dictType: 'system_demo_tree', shape: 'TREE', multiple: true, modelValue: ['paper', 'pen'] },
      global: { stubs },
    });
    await flush();

    expect((wrapper.vm as any).getSelectedNodes().map((node: any) => node.label)).toEqual(['纸张', '笔']);
    const paths = (wrapper.vm as any).getSelectedPaths();
    expect(paths.map((path: any[]) => path.map((node) => node.label))).toEqual([
      ['办公用品', '纸张'],
      ['办公用品', '笔'],
    ]);
  });
});
