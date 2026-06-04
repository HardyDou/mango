import { get, post } from '@mango/common';
import type {
  Create{{aggregatePascal}}Command,
  DeleteCommand,
  PageResult,
  {{aggregatePascal}}PageQuery,
  Update{{aggregatePascal}}Command,
  {{aggregatePascal}}VO,
} from './types';

const basePath = '/{{moduleKebab}}/{{aggregateKebab}}s';

export function create{{aggregatePascal}}(command: Create{{aggregatePascal}}Command) {
  return post<string>(`${basePath}/create`, command);
}

export function update{{aggregatePascal}}(command: Update{{aggregatePascal}}Command) {
  return post<boolean>(`${basePath}/update`, command);
}

export function delete{{aggregatePascal}}(command: DeleteCommand) {
  return post<boolean>(`${basePath}/delete`, command);
}

export function page{{aggregatePascal}}(query: {{aggregatePascal}}PageQuery) {
  return get<PageResult<{{aggregatePascal}}VO>>(`${basePath}/page`, { params: query });
}

export function get{{aggregatePascal}}Detail(id: string) {
  return get<{{aggregatePascal}}VO>(`${basePath}/detail`, { params: { id } });
}
