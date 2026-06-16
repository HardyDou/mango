import { del, get, put } from '@mango/common/utils/request';
import type { GridLayoutPersonalVO, GridLayoutValue, SaveGridLayoutPersonalCommand } from '../types';

export const gridLayoutPersonalApi = {
  getPersonal(pageCode: string) {
    return get<GridLayoutPersonalVO | null>('/grid-layout/personal', { params: { pageCode } });
  },
  savePersonal(command: SaveGridLayoutPersonalCommand) {
    return put<GridLayoutPersonalVO>('/grid-layout/personal', command);
  },
  resetPersonal(pageCode: string) {
    return del<boolean>('/grid-layout/personal', { params: { pageCode } });
  },
};

export function parseGridLayoutValue(pageCode: string, layoutJson?: string | null): GridLayoutValue | null {
  if (!layoutJson) {
    return null;
  }
  try {
    const value = JSON.parse(layoutJson) as GridLayoutValue;
    return {
      schemaVersion: 1,
      pageCode: value.pageCode || pageCode,
      items: Array.isArray(value.items) ? value.items : [],
    };
  } catch {
    return null;
  }
}

export function stringifyGridLayoutValue(value: GridLayoutValue): string {
  return JSON.stringify({
    schemaVersion: 1,
    pageCode: value.pageCode,
    items: value.items,
  });
}
