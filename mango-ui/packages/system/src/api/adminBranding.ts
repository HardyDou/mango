import { get, put } from '@mango/common/utils/request';

export interface AdminBranding {
  enabled: boolean;
  title: string;
  shortTitle: string;
  subtitle: string;
  loginTitle: string;
  loginSubtitle: string;
  logoFile: string;
  faviconFile: string;
  loginImageFile: string;
  footerCopyright: string;
  icp: string;
  contact: string;
}

export type SaveAdminBrandingCommand = AdminBranding;

export const adminBrandingApi = {
  get: () => get<AdminBranding>('/system/admin-branding'),
  getPublic: () => get<AdminBranding>('/system/admin-branding/public', {
    ignoreToken: true,
    silentError: true,
  }),
  save: (data: SaveAdminBrandingCommand) => put<boolean>('/system/admin-branding', data),
};
