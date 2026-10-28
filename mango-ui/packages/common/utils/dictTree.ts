import type { DictTreeOption } from '../api/dict';

export interface DictTreeIndexEntry {
  node: DictTreeOption;
  path: DictTreeOption[];
}

/**
 * 将树形字典展开为 value → 节点与根到当前节点路径的索引，避免多次递归查找。
 */
export function indexDictTree(
  nodes: DictTreeOption[],
  parents: DictTreeOption[] = [],
  index: Map<string, DictTreeIndexEntry> = new Map()
): Map<string, DictTreeIndexEntry> {
  for (const node of nodes) {
    const path = [...parents, node];
    index.set(String(node.value), { node, path });
    if (node.children?.length) {
      indexDictTree(node.children, path, index);
    }
  }
  return index;
}

export interface DictValueScopeOptions {
  /** 目标分组，支持单个或多个；未配置表示不按组过滤 */
  group?: string | string[];
  /** 白名单，命中才可见 */
  includeValues?: Array<string | number>;
  /** 黑名单，命中即排除 */
  excludeValues?: Array<string | number>;
}

function toGroupSet(group?: string | string[]): Set<string> | undefined {
  if (group === undefined) {
    return undefined;
  }
  const list = Array.isArray(group) ? group : [group];
  const normalized = list.map((item) => String(item).trim()).filter((item) => item.length > 0);
  return normalized.length > 0 ? new Set(normalized) : undefined;
}

function toValueSet(values?: Array<string | number>): Set<string> | undefined {
  if (!values || values.length === 0) {
    return undefined;
  }
  return new Set(values.map((item) => String(item)));
}

/**
 * 生成字典值可见性判断：先按分组过滤，再叠加 include 白名单与 exclude 黑名单。
 * - 未配置 group 时保留全部（含未分组项），保持旧行为。
 * - 配置 group 后只保留命中该分组的项，未分组项不命中。
 * - include 与 exclude 同层生效：include 限定范围，exclude 继续剔除。
 */
export function createDictValueFilter(options: DictValueScopeOptions): (item: {
  value: unknown;
  groups?: string[];
}) => boolean {
  const groups = toGroupSet(options.group);
  const include = toValueSet(options.includeValues);
  const exclude = toValueSet(options.excludeValues);
  return (item) => {
    const value = String(item.value);
    if (groups) {
      const itemGroups = item.groups ?? [];
      if (!itemGroups.some((group) => groups.has(String(group)))) {
        return false;
      }
    }
    if (include && !include.has(value)) {
      return false;
    }
    return !(exclude && exclude.has(value));
  };
}

export interface FilteredDictTree {
  nodes: DictTreeOption[];
  /** 自身命中过滤条件的值集合，祖先节点不在其中 */
  matched: Set<string>;
}

/**
 * 按过滤条件裁剪树形字典：命中节点保留，未命中的祖先只作为结构节点保留。
 */
export function filterDictTree(
  nodes: DictTreeOption[],
  allow: (item: { value: unknown; groups?: string[] }) => boolean
): FilteredDictTree {
  const matched = new Set<string>();
  const walk = (list: DictTreeOption[]): DictTreeOption[] => {
    const result: DictTreeOption[] = [];
    for (const node of list) {
      const children = node.children?.length ? walk(node.children) : [];
      const selfMatched = allow({ value: node.value, groups: node.groups });
      if (selfMatched) {
        matched.add(String(node.value));
      }
      if (selfMatched || children.length > 0) {
        result.push({ ...node, children: children.length > 0 ? children : undefined });
      }
    }
    return result;
  };
  return { nodes: walk(nodes), matched };
}
