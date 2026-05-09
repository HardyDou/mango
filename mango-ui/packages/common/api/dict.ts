import { get } from '../utils/request';

export interface DictOption {
  label: string;
  value: string;
  type?: string;
  sort?: number;
  status?: number;
}

export function listDictOptions(typeCode: string): Promise<DictOption[]> {
  return get<any[]>('/system/dict/data/options', { params: { typeCode } })
    .then((list) => (list || []).map(fromBackendOption));
}

function fromBackendOption(item: any): DictOption {
  return {
    label: item.label ?? item.dictLabel ?? item.value ?? item.dictValue ?? '',
    value: String(item.value ?? item.dictValue ?? ''),
    type: item.type ?? item.dictType,
    sort: item.sort ?? 0,
    status: item.status ?? 1,
  };
}
