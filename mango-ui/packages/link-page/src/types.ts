export interface LinkPageSearchEngine {
  code: string;
  label: string;
  searchUrl: string;
}

export interface LinkPageLoginInput {
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

export interface LinkPageProps {
  baseUrl?: string;
  /** Tenant context used only for anonymous public-link queries. */
  tenantId?: string | number;
  headers?: HeadersInit | (() => HeadersInit | Promise<HeadersInit>);
  credentials?: RequestCredentials;
  headline?: string;
  subtitle?: string;
  searchPlaceholder?: string;
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
  loginDefaults?: Omit<LinkPageLoginInput, 'username' | 'password'>;
  loginHandler?: (input: LinkPageLoginInput) => void | Promise<void>;
  logoutHandler?: () => void | Promise<void>;
  jumpEnabled?: boolean;
  searchEngines?: LinkPageSearchEngine[];
  defaultSearchEngine?: string;
}
