import { del, get, post, put } from '@mango/common/utils/request';

export type ContactType = 'PHONE' | 'EMAIL';

export interface CurrentUserProfile {
  userId: string | number;
  username: string;
  nickname?: string;
  avatar?: string;
  phone?: string;
  email?: string;
  realName?: string;
  documentType?: string;
  documentNumber?: string;
  verificationStatus: string;
  verificationSource?: string;
}

export interface UpdateCurrentUserProfilePayload {
  nickname?: string;
  avatar?: string;
  realName?: string;
  documentType?: string;
  documentNumber?: string;
}

export interface ContactCaptchaTicket {
  key: string;
  target: string;
  expiresInSeconds: number;
}

export interface ExternalIdentityBinding {
  id: string | number;
  userId: string | number;
  appCode: string;
  provider: ExternalAuthProvider;
  corpId?: string;
  externalUserId?: string;
  displayName?: string;
  bindSource?: string;
  bindStatus?: string;
  bindTime?: string;
  lastLoginTime?: string;
}

export type ExternalAuthProvider = 'WECOM' | 'DINGTALK';

export function getCurrentUserProfile() {
  return get<CurrentUserProfile>('/identity/me/profile');
}

export function updateCurrentUserProfile(data: UpdateCurrentUserProfilePayload) {
  return put<CurrentUserProfile>('/identity/me/profile', data);
}

export function sendCurrentContactCaptcha(data: { contactType: ContactType; target: string }) {
  return post<ContactCaptchaTicket>('/identity/me/contact-captcha', data);
}

export function updateCurrentUserContact(data: {
  contactType: ContactType;
  target: string;
  currentPassword: string;
  captchaKey: string;
  captchaCode: string;
}) {
  return put<CurrentUserProfile>('/identity/me/contact', data);
}

export function listCurrentExternalIdentities() {
  return get<ExternalIdentityBinding[]>('/identity/me/external-identities');
}

export function unbindCurrentExternalIdentity(data: { bindingId: string | number; currentPassword: string }) {
  return del<boolean>('/identity/me/external-identities', { data });
}
