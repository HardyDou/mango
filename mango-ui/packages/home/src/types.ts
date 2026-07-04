import type { ApiId, PageResult } from '@mango/api-schema';

export interface HomePageVO {
  id?: ApiId;
  routeKey?: string;
  tenantId?: ApiId;
  userId?: ApiId;
  templateId?: ApiId;
  templateVersionId?: ApiId;
  name: string;
  layoutJson: string;
  sort: number;
  enabled: boolean;
  defaultPage: boolean;
  builtIn: boolean;
  sourceType?: 'USER' | 'PERSONAL_AUTH' | 'ORG_AUTH' | 'ROLE_AUTH' | 'SYSTEM' | string;
  sourceLabel?: string;
  sourceLabels?: string[];
  readOnly?: boolean;
  canCopy?: boolean;
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

export interface BatchDeleteHomePagesCommand {
  ids: ApiId[];
}

export interface SortHomePagesCommand {
  ids: ApiId[];
}

export interface SetDefaultHomePageCommand {
  homeId: string;
}

export interface ResolveHomePageQuery {
  homeId?: string;
}

export type HomeTemplateVersionStatus = 'DRAFT' | 'ACTIVE' | 'HISTORY';
export type HomeTemplateAuthorizationSubjectType = 'USER' | 'ORG' | 'ROLE';

export interface HomeTemplateVersionVO {
  id?: ApiId;
  templateId?: ApiId;
  versionNo?: number;
  status?: HomeTemplateVersionStatus | string;
  layoutJson?: string;
  publishedBy?: ApiId;
  publishedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface HomeTemplateVO {
  id?: ApiId;
  tenantId?: ApiId;
  name: string;
  enabled: boolean;
  activeVersionId?: ApiId;
  activeVersionNo?: number;
  activeLayoutJson?: string;
  draftVersionId?: ApiId;
  draftLayoutJson?: string;
  authorizationCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface HomeTemplateAuthorizationItem {
  subjectType: HomeTemplateAuthorizationSubjectType;
  subjectId?: ApiId;
  subjectCode?: string;
  subjectName?: string;
  defaultFlag?: boolean;
  sort?: number;
}

export interface HomeTemplateAuthorizationVO extends HomeTemplateAuthorizationItem {
  id?: ApiId;
  templateId?: ApiId;
  enabled?: boolean;
  createdAt?: string;
}

export interface HomeTemplateQuery {
  keyword?: string;
  enabled?: boolean;
}

export interface UserHomePageQuery {
  page: number;
  size: number;
  keyword?: string;
  userId?: ApiId;
  enabled?: boolean;
}

export interface CreateHomeTemplateCommand {
  name: string;
  layoutJson?: string;
}

export interface UpdateHomeTemplateDraftCommand {
  id: ApiId;
  name: string;
  layoutJson: string;
}

export interface UpdateHomeTemplateStatusCommand {
  id: ApiId;
  enabled: boolean;
}

export interface HomeTemplateIdCommand {
  id: ApiId;
}

export interface HomeTemplateAuthorizationQuery {
  templateId: ApiId;
}

export interface SaveHomeTemplateAuthorizationsCommand {
  templateId: ApiId;
  authorizations: HomeTemplateAuthorizationItem[];
}

export interface UserHomeViewQuery {
  userId: ApiId;
  memberId?: ApiId;
  orgId?: ApiId;
}

export type UserHomePageResult = PageResult<HomePageVO>;
