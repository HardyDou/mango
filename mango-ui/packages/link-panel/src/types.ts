export interface LinkPanelSearchEngine {
  code: string;
  label: string;
  searchUrl: string;
}

export interface LinkPanelLoginInput {
  username: string;
  password: string;
  tenantId?: string | number;
  tenantCode?: string;
  realm?: string;
  actorType?: string;
  partyType?: string;
  partyId?: string | number;
  appCode?: string;
}

export interface LinkPanelProps {
  baseUrl?: string;
  headers?: HeadersInit | (() => HeadersInit | Promise<HeadersInit>);
  credentials?: RequestCredentials;
  loginUrl?: string;
  title?: string;
  logoUrl?: string;
  logoText?: string;
  logoAlt?: string;
  authenticated?: boolean;
  userAvatarUrl?: string;
  userName?: string;
  userAccount?: string;
  userEmail?: string;
  userPhone?: string;
  userDepartment?: string;
  userRole?: string;
  loginDefaults?: Omit<LinkPanelLoginInput, 'username' | 'password'>;
  loginHandler?: (input: LinkPanelLoginInput) => void | Promise<void>;
  logoutHandler?: () => void | Promise<void>;
  searchEngines?: LinkPanelSearchEngine[];
  defaultSearchEngine?: string;
}
