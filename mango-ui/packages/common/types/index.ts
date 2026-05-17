// Global type declarations for mango-ui

// Vite env
interface ImportMetaEnv {
  readonly VITE_ADMIN_PROXY_PATH: string;
  readonly VITE_PORT: string;
  readonly VITE_OPEN: string;
  readonly VITE_SM4_KEY: string;
  readonly VITE_APP_TITLE: string;
  readonly VITE_PUBLIC_PATH: string;
  readonly VITE_FILE_PREVIEW_PROVIDER_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// Element Plus global
declare module 'element-plus' {
  export const ElMessage: any;
  export const ElMessageBox: any;
}

// Route record
interface RouteRecordRaw {
  path?: string;
  name?: string;
  component?: any;
  redirect?: string;
  meta?: RouteMeta;
  children?: RouteRecordRaw[];
}

interface RouteMeta {
  isAuth?: boolean;
  title?: string;
  icon?: string;
  isLink?: string;
  isHide?: boolean;
  isFull?: boolean;
  isAffix?: boolean;
  isKeepAlive?: boolean;
  cacheName?: string;
}

export interface UserInfosState {
  userInfos: {
    username: string;
    nickname: string;
    photo: string;
    time: number;
    roles: string[];
    permissions: string[];
    authBtnList: string[];
    tenantId: string;
    tenantCode?: string;
    tenantName: string;
    realm: string;
    actorType: string;
    partyType: string;
    partyId: string | number;
    appCode: string;
  };
}
