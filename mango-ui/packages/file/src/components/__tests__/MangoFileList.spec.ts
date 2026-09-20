import { shallowMount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { describe, expect, it } from 'vitest';
import MangoFileList from '../MangoFileList.vue';

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: {
    data: { type: Array, default: () => [] },
    spanMethod: { type: Function, default: undefined },
    headerCellStyle: { type: Object, default: undefined },
  },
  setup(_props, { slots }) {
    return () => h('table', slots.default?.());
  },
});

const baseRows = [
  { key: '1', group: '申请资料', name: '营业执照', files: [] },
  { key: '2', group: '申请资料', name: '法人证件', files: [] },
  { key: '3', group: '签约资料', name: '合同', files: [] },
];
const columns = [
  { key: 'group', label: '资料分组', prop: 'group', mergeAdjacent: true },
  { key: 'name', label: '资料名称', prop: 'name' },
  { key: 'files', label: '文件', prop: 'files', type: 'files' as const },
];

function mountList(props: Record<string, unknown> = {}) {
  return shallowMount(MangoFileList, {
    props: { rows: baseRows, columns, ...props } as never,
    global: {
      stubs: {
        ElTable: ElTableStub,
        ElTableColumn: true,
        ElButton: true,
        ElEmpty: true,
        MangoFilePreviewDialog: true,
      },
    },
  });
}

describe('MangoFileList', () => {
  it('只合并相邻且合并键相同的资料行', () => {
    const wrapper = mountList();
    const table = wrapper.findComponent(ElTableStub);
    const spanMethod = table.props('spanMethod') as (context: {
      row: (typeof baseRows)[number];
      rowIndex: number;
      columnIndex: number;
      column: Record<string, never>;
    }) => [number, number];

    expect(spanMethod({ row: baseRows[0], rowIndex: 0, columnIndex: 0, column: {} })).toEqual([2, 1]);
    expect(spanMethod({ row: baseRows[1], rowIndex: 1, columnIndex: 0, column: {} })).toEqual([0, 0]);
    expect(spanMethod({ row: baseRows[2], rowIndex: 2, columnIndex: 0, column: {} })).toEqual([1, 1]);
  });

  it('允许关闭相邻行合并', () => {
    const wrapper = mountList({ mergeAdjacentRows: false });
    const spanMethod = wrapper.findComponent(ElTableStub).props('spanMethod') as (context: {
      row: (typeof baseRows)[number];
      rowIndex: number;
      columnIndex: number;
      column: Record<string, never>;
    }) => [number, number];

    expect(spanMethod({ row: baseRows[0], rowIndex: 0, columnIndex: 0, column: {} })).toEqual([1, 1]);
  });

  it('表头默认使用 Mango 主题变量', () => {
    const wrapper = mountList();
    expect(wrapper.findComponent(ElTableStub).props('headerCellStyle')).toMatchObject({
      color: 'var(--mango-table-header-text)',
      backgroundColor: 'var(--mango-table-header-bg)',
    });
  });

  it('拒绝重复的行 Key', () => {
    expect(() => mountList({ rows: [baseRows[0], { ...baseRows[1], key: '1' }] })).toThrow('行 key 1 重复');
  });
});
