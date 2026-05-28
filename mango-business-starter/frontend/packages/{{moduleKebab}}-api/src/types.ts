import type { ApiId } from '@mango/api-schema';

export interface Create{{aggregatePascal}}Command {
  name: string;
}

export interface {{aggregatePascal}}PageQuery {
  pageNo: number;
  pageSize: number;
  name?: string;
}

export interface {{aggregatePascal}}VO {
  id: ApiId;
  name: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
  pages: number;
}
