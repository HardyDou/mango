import { describe, expect, it } from 'vitest';
import { parseRules, rulesToFields, toDesignerRules, type FormCreateRule } from '../domain/rules';

/**
 * 回归：设计器的子表单/表格表单在加载时会直接读 props，
 * 缺失 props 会让设计器抛异常并留下空白画布（历史数据没有 props）。
 */
describe('toDesignerRules', () => {
  it('为缺少 props 的子表单补齐 props.rule 行内字段', () => {
    const stored: FormCreateRule[] = [
      {
        type: 'group',
        field: 'members',
        title: '成员明细',
        children: [
          { type: 'input', field: 'memberName', title: '成员姓名' },
          { type: 'inputNumber', field: 'memberAge', title: '年龄' },
        ],
      },
    ];

    const [group] = toDesignerRules(stored);
    expect(group.props).toBeDefined();
    expect((group.props as { rule: FormCreateRule[] }).rule).toHaveLength(2);
    expect((group.props as { rule: FormCreateRule[] }).rule[0].field).toBe('memberName');
  });

  it('为缺少 style 的表格表单列补齐设计器需要的列结构', () => {
    const stored: FormCreateRule[] = [
      {
        type: 'tableForm',
        field: 'rows',
        title: '明细表',
        children: [{ type: 'input', field: 'itemName', title: '项目' }],
      },
    ];

    const [table] = toDesignerRules(stored);
    const columns = (table.props as { columns: Array<Record<string, unknown>> }).columns;
    expect(columns).toHaveLength(1);
    expect(columns[0].style).toEqual({ width: '', color: '' });
    expect(columns[0].rule).toEqual([]);
    expect(table.children).toEqual([]);
  });

  it('保留已有的设计器结构且不改动普通控件', () => {
    const stored: FormCreateRule[] = [
      { type: 'input', field: 'name', title: '姓名', props: { placeholder: '请输入' } },
      {
        type: 'tableForm',
        field: 'rows',
        title: '明细表',
        props: {
          columns: [{ label: '数量', required: true, style: { width: '120', color: '' }, rule: [] }],
        },
      },
    ];

    const [name, table] = toDesignerRules(stored);
    expect(name.props).toEqual({ placeholder: '请输入' });
    const columns = (table.props as { columns: Array<Record<string, unknown>> }).columns;
    expect(columns[0].label).toBe('数量');
    expect(columns[0].required).toBe(true);
    expect((columns[0].style as Record<string, unknown>).width).toBe('120');
  });

  it('转换后的规则仍可被采集项解析识别（不破坏存储契约）', () => {
    const stored: FormCreateRule[] = [
      {
        type: 'group',
        field: 'members',
        title: '成员明细',
        children: [{ type: 'input', field: 'memberName', title: '成员姓名' }],
      },
    ];

    const fields = rulesToFields(parseRules(JSON.stringify(toDesignerRules(stored))));
    expect(fields).toHaveLength(1);
    expect(fields[0].fieldCode).toBe('members');
  });
});
