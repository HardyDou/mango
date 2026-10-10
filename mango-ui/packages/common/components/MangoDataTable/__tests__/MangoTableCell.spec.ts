import { mount } from '@vue/test-utils';
import { defineComponent, h, nextTick } from 'vue';
import { describe, expect, it } from 'vitest';
import MangoTableCell from '../MangoTableCell.vue';
import type { MangoTableColumn } from '../types';

interface TestRow {
  id: string;
  profile: { name: string };
  status: string;
  empty?: string;
  active?: boolean;
}

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: { type: [String, Number], default: '' },
    disabled: Boolean,
  },
  emits: ['focus', 'update:modelValue', 'change', 'blur'],
  setup(props) {
    return () => h('input', { value: props.modelValue, disabled: props.disabled });
  },
});

const ElSwitchStub = defineComponent({
  name: 'ElSwitch',
  props: {
    modelValue: { type: Boolean, default: false },
    disabled: Boolean,
    loading: Boolean,
  },
  emits: ['update:modelValue', 'change'],
  setup(props) {
    return () =>
      h('button', {
        type: 'button',
        role: 'switch',
        'aria-checked': props.modelValue,
        disabled: props.disabled,
      });
  },
});

function mountCell(row: TestRow, column: MangoTableColumn<TestRow>) {
  return mount(MangoTableCell<TestRow>, {
    props: { row, rowIndex: 2, column },
    global: {
      stubs: {
        ElInput: ElInputStub,
        ElSwitch: ElSwitchStub,
      },
    },
  });
}

describe('MangoTableCell', () => {
  it('resolves nested values and passes a complete formatter context', () => {
    const row: TestRow = {
      id: '1',
      profile: { name: 'Mango' },
      status: 'ENABLED',
    };
    const wrapper = mountCell(row, {
      field: 'profile.name',
      label: '名称',
      formatter: ({ value, rowIndex }) => `${value}-${rowIndex}`,
    });

    expect(wrapper.text()).toBe('Mango-2');
  });

  it('uses consumer-provided option labels and semantic tones for status cells', () => {
    const row: TestRow = {
      id: '1',
      profile: { name: 'Mango' },
      status: 'ENABLED',
    };
    const wrapper = mountCell(row, {
      field: 'status',
      label: '状态',
      type: 'status',
      options: [{ label: '已启用', value: 'ENABLED', tone: 'success' }],
    });

    expect(wrapper.text()).toBe('已启用');
    expect(wrapper.find('.mango-status-text--success').exists()).toBe(true);
  });

  it('emits one change with the previous value after an editable cell is committed', async () => {
    const row: TestRow = {
      id: '1',
      profile: { name: '旧名称' },
      status: 'ENABLED',
    };
    const wrapper = mountCell(row, {
      field: 'profile.name',
      label: '名称',
      type: 'input',
    });
    const input = wrapper.getComponent(ElInputStub);

    input.vm.$emit('focus');
    input.vm.$emit('update:modelValue', '新名称');
    await nextTick();
    input.vm.$emit('change', '新名称');
    input.vm.$emit('blur');

    expect(row.profile.name).toBe('新名称');
    expect(wrapper.emitted('change')).toHaveLength(1);
    expect(wrapper.emitted('change')?.[0]?.[0]).toMatchObject({
      field: 'profile.name',
      value: '新名称',
      previousValue: '旧名称',
      rowIndex: 2,
    });
  });

  it('renders the configured empty text for nullish or empty values', () => {
    const row: TestRow = {
      id: '1',
      profile: { name: 'Mango' },
      status: 'ENABLED',
      empty: '',
    };
    const wrapper = mountCell(row, {
      field: 'empty',
      label: '空值',
      emptyText: '无数据',
    });

    expect(wrapper.text()).toBe('无数据');
  });

  it('emits a change with the previous boolean value after a switch is toggled', async () => {
    const row: TestRow = {
      id: '1',
      profile: { name: 'Mango' },
      status: 'ENABLED',
      active: false,
    };
    const wrapper = mountCell(row, {
      field: 'active',
      label: '激活',
      type: 'switch',
    });
    const switchComponent = wrapper.getComponent(ElSwitchStub);

    switchComponent.vm.$emit('update:modelValue', true);
    await nextTick();
    switchComponent.vm.$emit('change', true);

    expect(row.active).toBe(true);
    expect(wrapper.emitted('change')).toHaveLength(1);
    expect(wrapper.emitted('change')?.[0]?.[0]).toMatchObject({
      field: 'active',
      value: true,
      previousValue: false,
      rowIndex: 2,
    });
  });

  it('supports 1 and 0 as truthy switch values', async () => {
    const row: TestRow = {
      id: '1',
      profile: { name: 'Mango' },
      status: 'ENABLED',
      active: 1,
    };
    const wrapper = mountCell(row, {
      field: 'active',
      label: '激活',
      type: 'switch',
    });
    const switchComponent = wrapper.getComponent(ElSwitchStub);

    switchComponent.vm.$emit('update:modelValue', false);
    await nextTick();
    switchComponent.vm.$emit('change', false);

    expect(row.active).toBe(0);
    expect(wrapper.emitted('change')).toHaveLength(1);
    expect(wrapper.emitted('change')?.[0]?.[0]).toMatchObject({
      field: 'active',
      value: 0,
      previousValue: 1,
      rowIndex: 2,
    });
  });
});
