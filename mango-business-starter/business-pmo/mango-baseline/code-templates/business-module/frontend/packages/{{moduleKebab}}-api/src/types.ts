import type { ApiId } from '@mango/api-schema';

export interface Create{{aggregatePascal}}Command {
  name: string;
}

export interface Update{{aggregatePascal}}Command extends Create{{aggregatePascal}}Command {
  id: ApiId;
}

export interface DeleteCommand {
  id: ApiId;
}

export interface {{aggregatePascal}}PageQuery {
  page: number;
  size: number;
  name?: string;
}

export interface {{aggregatePascal}}VO {
  id: ApiId;
  name: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
  pages: number;
}
