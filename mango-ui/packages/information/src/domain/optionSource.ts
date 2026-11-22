/**
 * 选项数据源：字段可以声明 `optionSource: {type: 'DICT', dictType: 'xxx'}`，
 * 此时可选值来自平台字典的启用项；存量定义没有 optionSource，按静态 options 处理。
 */
import { listDictOptions } from '@mango/common';
import type { FormCreateRule, InformationFieldOption } from './rules';

export const STATIC_OPTION_SOURCE = 'STATIC';
export const DICT_OPTION_SOURCE = 'DICT';

export interface InformationOptionSource {
  type: string;
  dictType?: string;
}

/** 读取字段的选项数据源；缺失或 STATIC 返回 undefined，表示沿用静态 options。 */
export function ruleOptionSource(rule: FormCreateRule | null | undefined): InformationOptionSource | undefined {
  const raw = rule?.optionSource;
  if (!raw || typeof raw !== 'object') {
    return undefined;
  }
  const record = raw as Record<string, unknown>;
  const type = typeof record.type === 'string' ? record.type.trim().toUpperCase() : '';
  if (!type || type === STATIC_OPTION_SOURCE) {
    return undefined;
  }
  const dictType = typeof record.dictType === 'string' ? record.dictType.trim() : '';
  return { type, dictType: dictType || undefined };
}

/** 遍历规则及其子表单/表格表单的行模板字段。 */
function eachRule(rules: FormCreateRule[], visit: (rule: FormCreateRule) => void): void {
  for (const rule of rules) {
    if (!rule || typeof rule !== 'object') {
      continue;
    }
    visit(rule);
    if (Array.isArray(rule.children)) {
      eachRule(rule.children, visit);
    }
    const nested = (rule.props as Record<string, unknown> | undefined)?.rule;
    if (Array.isArray(nested)) {
      eachRule(nested as FormCreateRule[], visit);
    }
  }
}

function cloneRules(rules: FormCreateRule[]): FormCreateRule[] {
  return rules.map((rule) => {
    const cloned: FormCreateRule = { ...rule };
    if (Array.isArray(rule.children)) {
      cloned.children = cloneRules(rule.children);
    }
    const props = rule.props as Record<string, unknown> | undefined;
    if (props && Array.isArray(props.rule)) {
      cloned.props = { ...props, rule: cloneRules(props.rule as FormCreateRule[]) };
    }
    return cloned;
  });
}

async function loadDictOptions(dictType: string): Promise<InformationFieldOption[]> {
  const list = await listDictOptions(dictType);
  return (list || [])
    .map((item) => ({ label: String(item.label ?? item.value ?? ''), value: String(item.value ?? '') }))
    .filter((item) => item.value !== '');
}

/**
 * 渲染前把 DICT 字段的 options 换成平台字典启用项；同一 dictType 只请求一次。
 *
 * 字典不存在或没有启用项时抛出异常，不静默退化为空选项：
 * 服务端会拒绝这类提交，页面需要把字典问题暴露给配置者。
 */
export async function resolveOptionSources(rules: FormCreateRule[] | undefined): Promise<FormCreateRule[]> {
  const source = rules || [];
  const dictTypes = new Set<string>();
  eachRule(source, (rule) => {
    const optionSource = ruleOptionSource(rule);
    if (optionSource?.type !== DICT_OPTION_SOURCE) {
      return;
    }
    if (!optionSource.dictType) {
      throw new Error(`字段 ${String(rule.title || rule.field || '')} 未选择字典类型`);
    }
    dictTypes.add(optionSource.dictType);
  });
  if (!dictTypes.size) {
    return source;
  }

  const optionCache = new Map<string, InformationFieldOption[]>();
  for (const dictType of dictTypes) {
    optionCache.set(dictType, await loadDictOptions(dictType));
  }

  const resolved = cloneRules(source);
  eachRule(resolved, (rule) => {
    const optionSource = ruleOptionSource(rule);
    if (optionSource?.type !== DICT_OPTION_SOURCE) {
      return;
    }
    const fieldName = String(rule.title || rule.field || '');
    const options = optionCache.get(optionSource.dictType as string) || [];
    if (!options.length) {
      throw new Error(`字段 ${fieldName} 绑定的字典 ${optionSource.dictType} 不存在或没有启用项`);
    }
    rule.options = options.map((option) => ({ ...option }));
  });
  return resolved;
}

/** 收敛数据源：DICT 字段不再保留静态 options，避免两套值域漂移。 */
export function normalizeOptionSourceRules(rules: FormCreateRule[]): FormCreateRule[] {
  eachRule(rules, (rule) => {
    if (ruleOptionSource(rule)?.type === DICT_OPTION_SOURCE) {
      rule.options = [];
    }
  });
  return rules;
}
