import { get } from '../utils/request';

export interface DictOption {
  label: string;
  value: string;
  type?: string;
  sort?: number;
  status?: number;
  /** 分组标记；空数组表示未分组 */
  groups?: string[];
}

export interface DictTreeOption extends DictOption {
  parentValue?: string;
  children?: DictTreeOption[];
}

interface BackendDictTreeOption {
  label?: string;
  dictLabel?: string;
  value?: string;
  dictValue?: string;
  parentValue?: string;
  groups?: string[] | string;
  children?: BackendDictTreeOption[];
  type?: string;
  dictType?: string;
  sort?: number;
  status?: number;
}

const optionCache = new Map<string, DictOption[]>();
const optionLoadingCache = new Map<string, Promise<DictOption[]>>();

/**
 * 查询平铺字典选项；默认使用会话内缓存，多个组件使用同一字典时不会重复请求。
 * 传入 force=true 时忽略缓存并重新请求。
 */
export async function listDictOptions(typeCode: string, force = false): Promise<DictOption[]> {
  if (!force && optionCache.has(typeCode)) {
    return optionCache.get(typeCode) ?? [];
  }
  if (!force && optionLoadingCache.has(typeCode)) {
    return optionLoadingCache.get(typeCode)!;
  }
  const request = get<any[]>('/system/dict/data/options', { params: { typeCode } })
    .then((list) => (list || []).map(fromBackendOption));
  optionLoadingCache.set(typeCode, request);
  try {
    const data = await request;
    optionCache.set(typeCode, data);
    return data;
  } finally {
    optionLoadingCache.delete(typeCode);
  }
}

export function clearDictOptionCache(typeCode?: string) {
  if (typeCode) {
    optionCache.delete(typeCode);
    return;
  }
  optionCache.clear();
}

const treeCache = new Map<string, DictTreeOption[]>();
const treeLoadingCache = new Map<string, Promise<DictTreeOption[]>>();

/**
 * 查询树形字典；默认使用会话内缓存，多个组件使用同一字典时不会重复请求。
 * 传入 force=true 时忽略缓存并重新请求。
 */
export async function listDictTree(typeCode: string, force = false): Promise<DictTreeOption[]> {
  if (!force && treeCache.has(typeCode)) {
    return treeCache.get(typeCode) ?? [];
  }
  if (!force && treeLoadingCache.has(typeCode)) {
    return treeLoadingCache.get(typeCode)!;
  }
  const request = get<BackendDictTreeOption[]>('/system/dict/data/tree', { params: { typeCode } })
    .then((list) => (list || []).map(fromBackendTreeOption));
  treeLoadingCache.set(typeCode, request);
  try {
    const data = await request;
    treeCache.set(typeCode, data);
    return data;
  } finally {
    treeLoadingCache.delete(typeCode);
  }
}

export function clearDictTreeCache(typeCode?: string) {
  if (typeCode) {
    treeCache.delete(typeCode);
    return;
  }
  treeCache.clear();
}

function fromBackendOption(item: BackendDictTreeOption): DictOption {
  return {
    label: item.label ?? item.dictLabel ?? item.value ?? item.dictValue ?? '',
    value: String(item.value ?? item.dictValue ?? ''),
    type: item.type ?? item.dictType,
    sort: item.sort ?? 0,
    status: item.status ?? 1,
    groups: normalizeGroups(item.groups),
  };
}

function normalizeGroups(groups?: string[] | string): string[] {
  if (!groups) {
    return [];
  }
  const list = Array.isArray(groups) ? groups : String(groups).split(',');
  return list
    .map((group) => String(group).trim())
    .filter((group, index, all) => group.length > 0 && all.indexOf(group) === index);
}

function fromBackendTreeOption(item: BackendDictTreeOption): DictTreeOption {
  return {
    ...fromBackendOption(item),
    parentValue: item.parentValue,
    children: item.children?.map(fromBackendTreeOption),
  };
}
