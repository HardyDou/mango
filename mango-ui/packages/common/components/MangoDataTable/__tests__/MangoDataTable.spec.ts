/* eslint-disable vue/one-component-per-file */

import { readFileSync } from 'node:fs';
import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { describe, expect, it } from 'vitest';
import MangoDataTable from '../index.vue';

interface TestRow {
  id: string;
  name: string;
}

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: {
    data: { type: Array, default: () => [] },
    rowKey: { type: [String, Function], default: undefined },
    border: Boolean,
    headerCellStyle: { type: Object, default: undefined },
  },
  emits: ['selection-change'],
  setup(_props, { expose, slots }) {
    expose({
      clearSelection: () => undefined,
      toggleRowSelection: () => undefined,
      toggleRowExpansion: () => undefined,
    });
    return () => h('div', { class: 'el-table' }, slots.default?.());
  },
});

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: {
    type: { type: String, default: '' },
    label: { type: String, default: '' },
    index: { type: Function, default: undefined },
  },
  setup(props) {
    return () =>
      h('div', {
        class: 'el-table-column',
        'data-column-type': props.type,
        'data-column-label': props.label,
        'data-first-index': props.index?.(0),
      });
  },
});

const PaginationStub = defineComponent({
  name: 'MangoPaginationStub',
  props: {
    page: { type: Number, default: 1 },
    limit: { type: Number, default: 20 },
    total: { type: Number, default: 0 },
  },
  emits: ['pagination'],
  setup(props) {
    return () =>
      h('nav', {
        class: 'pagination-stub',
        'data-page': props.page,
        'data-limit': props.limit,
        'data-total': props.total,
      });
  },
});

const ElSegmentedStub = defineComponent({
  name: 'ElSegmented',
  props: {
    modelValue: { type: String, default: '' },
    options: { type: Array, default: () => [] },
  },
  emits: ['change'],
  setup(props) {
    return () => h('div', { class: 'el-segmented', 'data-value': props.modelValue });
  },
});

const ElResultStub = defineComponent({
  name: 'ElResult',
  props: {
    title: { type: String, default: '' },
    subTitle: { type: String, default: '' },
  },
  setup(props, { slots }) {
    return () => h('section', { class: 'el-result' }, [props.title, props.subTitle, slots.extra?.()]);
  },
});

const ElButtonStub = defineComponent({
  name: 'ElButton',
  emits: ['click'],
  setup(_props, { emit, slots }) {
    return () => h('button', { type: 'button', onClick: () => emit('click') }, slots.default?.());
  },
});

const global = {
  directives: {
    loading: () => undefined,
  },
  stubs: {
    ElTable: ElTableStub,
    ElTableColumn: ElTableColumnStub,
    Pagination: PaginationStub,
    ElSegmented: ElSegmentedStub,
    ElResult: ElResultStub,
    ElButton: ElButtonStub,
    ElTooltip: true,
    ElIcon: true,
    ElDropdown: true,
    ElDropdownMenu: true,
    ElDropdownItem: true,
  },
};

const rows: TestRow[] = [{ id: '1', name: 'Mango' }];
const columns = [{ field: 'name', label: '名称', expandable: true }];

function mountTable(props: Record<string, unknown> = {}) {
  return mount(MangoDataTable<TestRow>, {
    props: {
      rows,
      columns,
      rowKey: 'id',
      ...props,
    },
    global,
  });
}

describe('MangoDataTable', () => {
  it('switches card presentation through one component implementation', () => {
    const card = mountTable({ card: true });
    const plain = mountTable({ card: false });

    expect(card.classes()).toContain('mango-data-table');
    expect(card.classes()).not.toContain('mango-data-table--plain');
    expect(plain.classes()).toContain('mango-data-table--plain');
  });

  it('uses themed table header colors with legacy-theme fallbacks and explicit overrides', () => {
    const themed = mountTable();
    const overridden = mountTable({
      headerCellStyle: {
        backgroundColor: '#ffffff',
        color: '#123456',
      },
    });

    expect(themed.getComponent(ElTableStub).props('headerCellStyle')).toMatchObject({
      backgroundColor: 'var(--mango-table-header-bg, #eef1f5)',
      color: 'var(--mango-table-header-text, #000000)',
      fontWeight: 600,
    });
    expect(overridden.getComponent(ElTableStub).props('headerCellStyle')).toMatchObject({
      backgroundColor: '#ffffff',
      color: '#123456',
    });
  });

  it('keeps the default header colors and declares semantic variables in every public theme profile', () => {
    const defaultThemeFiles = [
      '../../../theme/index.css',
      '../../../theme/light.scss',
      '../../../theme/admin-standard.css',
    ];
    const themeFiles = [...defaultThemeFiles, '../../../theme/dark.scss', '../../../theme/admin-compact.css'];

    defaultThemeFiles.forEach((themeFile) => {
      const source = readFileSync(new URL(themeFile, import.meta.url), 'utf8');
      expect(source).toContain('--mango-table-header-bg: #eef1f5;');
      expect(source).toContain('--mango-table-header-text: #000;');
    });

    themeFiles.forEach((themeFile) => {
      const source = readFileSync(new URL(themeFile, import.meta.url), 'utf8');
      expect(source).toContain('--mango-table-header-bg');
      expect(source).toContain('--mango-table-header-text');
    });
  });

  it('emits selection and pagination changes through the public event contract', () => {
    const wrapper = mountTable({
      showSelection: true,
      pagination: { page: 2, limit: 20, total: 45 },
    });

    wrapper.getComponent(ElTableStub).vm.$emit('selection-change', rows);
    wrapper.getComponent(PaginationStub).vm.$emit('pagination', { page: 3, limit: 20 });

    expect(wrapper.emitted('selection-change')).toEqual([[rows]]);
    expect(wrapper.emitted('page-change')).toEqual([[{ page: 3, limit: 20 }]]);
    expect(wrapper.get('.pagination-stub').attributes()).toMatchObject({
      'data-page': '2',
      'data-limit': '20',
      'data-total': '45',
    });
    expect(wrapper.get('[data-column-type="selection"]').exists()).toBe(true);
    expect(wrapper.get('[data-column-type="index"]').attributes('data-first-index')).toBe('21');
  });

  it('keeps controlled display mode in sync and emits both v-model and change events', () => {
    const wrapper = mountTable({ mode: 'flat', showModeSwitch: true });
    wrapper.getComponent(ElSegmentedStub).vm.$emit('change', 'expand');

    expect(wrapper.emitted('update:mode')).toEqual([['expand']]);
    expect(wrapper.emitted('mode-change')).toEqual([['expand']]);
  });

  it('shows an explicit error state and only exposes retry when enabled', async () => {
    const wrapper = mountTable({
      error: '网络不可用',
      retryable: true,
      errorTitle: '加载失败',
    });

    expect(wrapper.findComponent(ElTableStub).exists()).toBe(false);
    expect(wrapper.text()).toContain('加载失败');
    expect(wrapper.text()).toContain('网络不可用');
    await wrapper.get('button').trigger('click');
    expect(wrapper.emitted('retry')).toHaveLength(1);
  });
});
