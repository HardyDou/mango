import { del, get, post, put } from '@mango/common/utils/request';
import type {
  BatchDeleteHomePagesCommand,
  CreateHomePageCommand,
  CreateHomeTemplateCommand,
  HomePageVO,
  HomePageIdCommand,
  HomeTemplateAuthorizationQuery,
  HomeTemplateAuthorizationVO,
  HomeTemplateIdCommand,
  HomeTemplateQuery,
  HomeTemplateVO,
  RenameHomePageCommand,
  ResolveHomePageQuery,
  SaveHomePageLayoutCommand,
  SaveHomeTemplateAuthorizationsCommand,
  SetDefaultHomePageCommand,
  SortHomePagesCommand,
  UpdateHomeTemplateDraftCommand,
  UpdateHomeTemplateStatusCommand,
  UserHomePageQuery,
  UserHomePageResult,
  UserHomeViewQuery,
} from '../types';

export const homePageApi = {
  listMyPages() {
    return get<HomePageVO[]>('/home/pages');
  },
  pageUserPages(query: UserHomePageQuery) {
    return get<UserHomePageResult>('/home/pages/user-pages', { params: query });
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
    const command: SetDefaultHomePageCommand = { homeId: id };
    return put<HomePageVO>('/home/pages/default', command);
  },
  delete(id: string) {
    const command: HomePageIdCommand = { id };
    return del<HomePageVO>('/home/pages', { data: command });
  },
  adminRename(id: string, command: Omit<RenameHomePageCommand, 'id'>) {
    return put<HomePageVO>('/home/pages/admin/name', { ...command, id });
  },
  adminSaveLayout(id: string, command: Omit<SaveHomePageLayoutCommand, 'id'>) {
    return put<HomePageVO>('/home/pages/admin/layout', { ...command, id });
  },
  adminDelete(id: string) {
    const command: HomePageIdCommand = { id };
    return del<void>('/home/pages/admin', { data: command });
  },
  adminBatchDelete(ids: BatchDeleteHomePagesCommand['ids']) {
    const command: BatchDeleteHomePagesCommand = { ids };
    return del<void>('/home/pages/admin/batch', { data: command });
  },
};

export const homeTemplateApi = {
  list(query: HomeTemplateQuery = {}) {
    return get<HomeTemplateVO[]>('/home/templates', { params: query });
  },
  detail(id: string) {
    return get<HomeTemplateVO>('/home/templates/detail', { params: { id } });
  },
  create(command: CreateHomeTemplateCommand) {
    return post<HomeTemplateVO>('/home/templates', command);
  },
  updateDraft(id: string, command: Omit<UpdateHomeTemplateDraftCommand, 'id'>) {
    return put<HomeTemplateVO>('/home/templates/draft', { ...command, id });
  },
  copy(id: string) {
    const command: HomeTemplateIdCommand = { id };
    return post<HomeTemplateVO>('/home/templates/copy', command);
  },
  publish(id: string) {
    const command: HomeTemplateIdCommand = { id };
    return put<HomeTemplateVO>('/home/templates/publish', command);
  },
  updateStatus(id: string, enabled: boolean) {
    const command: UpdateHomeTemplateStatusCommand = { id, enabled };
    return put<HomeTemplateVO>('/home/templates/status', command);
  },
  delete(id: string) {
    const command: HomeTemplateIdCommand = { id };
    return del<void>('/home/templates', { data: command });
  },
  listAuthorizations(query: HomeTemplateAuthorizationQuery) {
    return get<HomeTemplateAuthorizationVO[]>('/home/templates/authorizations', { params: query });
  },
  saveAuthorizations(command: SaveHomeTemplateAuthorizationsCommand) {
    return put<HomeTemplateAuthorizationVO[]>('/home/templates/authorizations', command);
  },
  resolveUserPages(query: UserHomeViewQuery) {
    return get<HomePageVO[]>('/home/templates/user-pages', { params: query });
  },
};
