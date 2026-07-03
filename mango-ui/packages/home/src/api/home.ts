import { del, get, post, put } from '@mango/common/utils/request';
import type {
  CreateHomePageCommand,
  HomePageVO,
  HomePageIdCommand,
  RenameHomePageCommand,
  ResolveHomePageQuery,
  SaveHomePageLayoutCommand,
  SortHomePagesCommand,
} from '../types';

export const homePageApi = {
  listMyPages() {
    return get<HomePageVO[]>('/home/pages');
  },
  resolve(query: ResolveHomePageQuery = {}) {
    return get<HomePageVO>('/home/pages/resolve', { params: query });
  },
  create(command: CreateHomePageCommand) {
    return post<HomePageVO>('/home/pages', command);
  },
  rename(id: string, command: Omit<RenameHomePageCommand, 'id'>) {
    return put<HomePageVO>('/home/pages/name', { ...command, id });
  },
  duplicate(id: string) {
    const command: HomePageIdCommand = { id };
    return post<HomePageVO>('/home/pages/duplicate', command);
  },
  saveLayout(id: string, command: Omit<SaveHomePageLayoutCommand, 'id'>) {
    return put<HomePageVO>('/home/pages/layout', { ...command, id });
  },
  sort(command: SortHomePagesCommand) {
    return put<HomePageVO[]>('/home/pages/sort', command);
  },
  setDefault(id: string) {
    const command: HomePageIdCommand = { id };
    return put<HomePageVO>('/home/pages/default', command);
  },
  delete(id: string) {
    const command: HomePageIdCommand = { id };
    return del<HomePageVO>('/home/pages', { data: command });
  },
};
