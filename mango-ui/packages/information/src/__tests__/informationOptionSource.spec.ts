import { describe, expect, it, vi, beforeEach } from 'vitest';
import type { FormCreateRule } from '../domain/rules';
import {
  DICT_OPTION_SOURCE,
  normalizeOptionSourceRules,
  resolveOptionSources,
  ruleOptionSource,
} from '../domain/optionSource';

const dictOptions = vi.fn();

vi.mock('@mango/common', () => ({
  listDictOptions: (typeCode: string) => dictOptions(typeCode),
}));

beforeEach(() => {
  dictOptions.mockReset();
});

describe('ruleOptionSource', () => {
  it('老表单没有 optionSource 时按静态选项处理', () => {
    expect(ruleOptionSource({ type: 'select', field: 'level', options: [{ label: '普通', value: 'normal' }] })).toBeUndefined();
    expect(ruleOptionSource(undefined)).toBeUndefined();
    expect(ruleOptionSource({ type: 'select', field: 'level', optionSource: { type: 'STATIC' } })).toBeUndefined();
  });

  it('识别字典数据源并保留字典类型编码', () => {
    expect(ruleOptionSource({ type: 'select', field: 'level', optionSource: { type: 'dict', dictType: ' customer_level ' } }))
      .toEqual({ type: DICT_OPTION_SOURCE, dictType: 'customer_level' });
  });
});

describe('resolveOptionSources', () => {
  it('静态选项表单不请求字典，规则原样返回', async () => {
    const rules: FormCreateRule[] = [
      { type: 'select', field: 'level', options: [{ label: '普通', value: 'normal' }] },
    ];

    const resolved = await resolveOptionSources(rules);

    expect(dictOptions).not.toHaveBeenCalled();
    expect(resolved).toBe(rules);
  });

  it('字典字段的 options 换成字典启用项，且同一字典只请求一次', async () => {
    dictOptions.mockResolvedValue([
      { label: '普通', value: 'normal' },
      { label: '重要', value: 'vip' },
    ]);
    const rules: FormCreateRule[] = [
      {
        type: 'select',
        field: 'level',
        title: '客户等级',
        options: [{ label: '已废弃', value: 'legacy' }],
        optionSource: { type: DICT_OPTION_SOURCE, dictType: 'customer_level' },
      },
      {
        type: 'radio',
        field: 'backupLevel',
        title: '备用等级',
        optionSource: { type: DICT_OPTION_SOURCE, dictType: 'customer_level' },
      },
    ];

    const resolved = await resolveOptionSources(rules);

    expect(dictOptions).toHaveBeenCalledTimes(1);
    expect(dictOptions).toHaveBeenCalledWith('customer_level');
    expect(resolved[0].options).toEqual([
      { label: '普通', value: 'normal' },
      { label: '重要', value: 'vip' },
    ]);
    expect(resolved[1].options).toHaveLength(2);
    // 不改写调用方数据
    expect(rules[0].options).toEqual([{ label: '已废弃', value: 'legacy' }]);
  });

  it('字典不存在或没有启用项时拒绝，而不是静默放行', async () => {
    dictOptions.mockResolvedValue([]);
    const rules: FormCreateRule[] = [
      { type: 'select', field: 'level', title: '客户等级', optionSource: { type: 'DICT', dictType: 'missing_dict' } },
    ];

    await expect(resolveOptionSources(rules)).rejects.toThrow(
      '字段 客户等级 绑定的字典 missing_dict 不存在或没有启用项',
    );
  });

  it('DICT 字段没有填写字典类型时拒绝', async () => {
    const rules: FormCreateRule[] = [
      { type: 'select', field: 'level', title: '客户等级', optionSource: { type: 'DICT' } },
    ];

    await expect(resolveOptionSources(rules)).rejects.toThrow('字段 客户等级 未选择字典类型');
    expect(dictOptions).not.toHaveBeenCalled();
  });
});

describe('normalizeOptionSourceRules', () => {
  it('把 DICT 字段的静态 options 置空，静态字段保持不变', () => {
    const rules: FormCreateRule[] = [
      {
        type: 'select',
        field: 'level',
        options: [{ label: '普通', value: 'normal' }],
        optionSource: { type: 'DICT', dictType: 'customer_level' },
      },
      { type: 'select', field: 'scale', options: [{ label: '小型', value: 's' }] },
    ];

    normalizeOptionSourceRules(rules);

    expect(rules[0].options).toEqual([]);
    expect(rules[1].options).toEqual([{ label: '小型', value: 's' }]);
  });
});
