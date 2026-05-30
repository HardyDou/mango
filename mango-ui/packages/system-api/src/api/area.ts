import { del, get, post, put } from '@mango/common/utils/request';

export interface SysArea {
  id?: string | number;
  pid: string | number;
  name: string;
  letter?: string;
  adcode?: string | number;
  location?: string;
  areaSort?: number;
  areaStatus?: string;
  areaType?: string;
  hot?: string;
  cityCode?: string;
  tenantId?: string | number;
  children?: SysArea[];
}

export const areaApi = {
  root: (type = 1) => get<SysArea[]>('/system/area/tree', { params: { type } }),
  children: (parentId: string | number) => get<SysArea[]>('/system/area/children', { params: { parentId } }),
  detail: (id: string | number) => get<SysArea>('/system/area/detail', { params: { id } }),
  create: (data: SysArea) => post<void>('/system/area', normalizeArea(data)),
  update: (data: SysArea) => put<void>('/system/area', normalizeArea(data)),
  delete: (id: string | number) => del<void>('/system/area', { params: { id } }),
};

function normalizeArea(data: SysArea): SysArea {
  return {
    id: data.id,
    pid: data.pid ?? 0,
    name: data.name,
    letter: data.letter,
    adcode: data.adcode,
    location: data.location,
    areaSort: data.areaSort ?? 0,
    areaStatus: data.areaStatus ?? '1',
    areaType: data.areaType ?? '1',
    hot: data.hot ?? '0',
    cityCode: data.cityCode,
    tenantId: data.tenantId ?? 1,
  };
}
