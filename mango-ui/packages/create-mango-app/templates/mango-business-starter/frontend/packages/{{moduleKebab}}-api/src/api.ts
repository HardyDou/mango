import { get, post } from '@mango/common/utils/request';
import type {
  Create{{aggregatePascal}}Command,
  PageResult,
  {{aggregatePascal}}PageQuery,
  {{aggregatePascal}}VO,
} from './types';

const basePath = '/{{moduleKebab}}/{{aggregateKebab}}s';

export function create{{aggregatePascal}}(command: Create{{aggregatePascal}}Command) {
  return post<{{aggregatePascal}}VO>(basePath, command);
}

export function page{{aggregatePascal}}(query: {{aggregatePascal}}PageQuery) {
  return get<PageResult<{{aggregatePascal}}VO>>(basePath, { params: query });
}

export function get{{aggregatePascal}}Detail(id: string) {
  return get<{{aggregatePascal}}VO>(`${basePath}/detail`, { params: { id } });
}
