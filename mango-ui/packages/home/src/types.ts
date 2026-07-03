import type { ApiId } from '@mango/api-schema';

export interface HomePageVO {
  id?: ApiId;
  tenantId?: ApiId;
  userId?: ApiId;
  name: string;
  layoutJson: string;
  sort: number;
  enabled: boolean;
  defaultPage: boolean;
  builtIn: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateHomePageCommand {
  name: string;
  layoutJson?: string;
  setDefault?: boolean;
}

export interface RenameHomePageCommand {
  id: ApiId;
  name: string;
}

export interface SaveHomePageLayoutCommand {
  id: ApiId;
  layoutJson: string;
}

export interface HomePageIdCommand {
  id: ApiId;
}

export interface SortHomePagesCommand {
  ids: ApiId[];
}

export interface ResolveHomePageQuery {
  homeId?: ApiId;
}
