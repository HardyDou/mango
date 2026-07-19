import type { HttpClient } from '@mango/api-schema';
import type { Create{{aggregatePascal}}Command } from './types';
import type { DeleteCommand } from './types';
import type { PageResult } from './types';
import type { {{aggregatePascal}}PageQuery } from './types';
import type { Update{{aggregatePascal}}Command } from './types';
import type { {{aggregatePascal}}VO } from './types';

const basePath = '/{{moduleKebab}}/{{aggregateKebab}}s';

export interface {{aggregatePascal}}Api {
  create(command: Create{{aggregatePascal}}Command, signal?: AbortSignal): Promise<string>;
  update(command: Update{{aggregatePascal}}Command, signal?: AbortSignal): Promise<boolean>;
  delete(command: DeleteCommand, signal?: AbortSignal): Promise<boolean>;
  page(query: {{aggregatePascal}}PageQuery, signal?: AbortSignal): Promise<PageResult<{{aggregatePascal}}VO>>;
  detail(id: string, signal?: AbortSignal): Promise<{{aggregatePascal}}VO>;
}

export function create{{aggregatePascal}}Api(client: HttpClient): {{aggregatePascal}}Api {
  return {
    create(command, signal) {
      return client.request<string, Create{{aggregatePascal}}Command>({
        method: 'POST',
        url: `${basePath}/create`,
        body: command,
        signal,
      });
    },
    update(command, signal) {
      return client.request<boolean, Update{{aggregatePascal}}Command>({
        method: 'POST',
        url: `${basePath}/update`,
        body: command,
        signal,
      });
    },
    delete(command, signal) {
      return client.request<boolean, DeleteCommand>({
        method: 'POST',
        url: `${basePath}/delete`,
        body: command,
        signal,
      });
    },
    page(query, signal) {
      return client.request<PageResult<{{aggregatePascal}}VO>>({
        method: 'GET',
        url: `${basePath}/page`,
        query: { ...query },
        signal,
      });
    },
    detail(id, signal) {
      return client.request<{{aggregatePascal}}VO>({
        method: 'GET',
        url: `${basePath}/detail`,
        query: { id },
        signal,
      });
    },
  };
}
