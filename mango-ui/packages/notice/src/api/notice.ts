import { del, get, post, put } from '@mango/common';
import type {
  NoticeBusinessConfigVersion,
  NoticeBusinessType,
  NoticeChannelConfig,
  NoticeChannelTemplate,
  NoticeChannelType,
  NoticeReceivePreference,
  NoticeReceivePreferenceScopeType,
  NoticeReminderSetting,
  NoticeRecipientAccount,
  NoticeRecipientAccountType,
  NoticeRecipientAccountStatus,
  NoticeSendCommand,
  NoticeSendRecord,
  NoticeSendResult,
  NoticeSiteMessage,
  NoticeSiteMessagePageQuery,
  NoticeTask,
  NoticeUnreadCount,
  PageResult,
  PersonalConfig,
} from '../types/notice';

const NOTICE_REMINDER_CONFIG_SCOPE = {
  groupCode: 'notice',
  bizType: 'client_reminder',
  configKey: 'reminder_setting',
} as const;

export const defaultNoticeReminderSetting: NoticeReminderSetting = {
  popupEnabled: true,
  popupPlacement: 'top-right',
  voiceEnabled: true,
  voiceText: '您有新的系统消息，请及时查看',
  desktopNotificationEnabled: true,
};

export function sendNotice(data: NoticeSendCommand) {
  return post<NoticeSendResult>('/notice/send', data);
}

export function sendSiteNotice(data: NoticeSendCommand) {
  return post<NoticeSendResult>('/notice/site/messages', { channelTypes: ['SITE'], ...data });
}

export interface NoticeIdentityUser {
  userId?: string;
  username: string;
  nickname?: string;
  phone?: string;
  email?: string;
  status?: number;
}

export interface NoticeOrgNode {
  id: string;
  orgName: string;
  pid?: string;
  orgCode?: string;
  orgStatus?: string;
  children?: NoticeOrgNode[];
}

export interface NoticePost {
  id?: string;
  postName: string;
  postCode?: string;
  postStatus?: string;
}

export interface NoticeRole {
  roleId?: string;
  roleName: string;
  roleCode?: string;
  status?: number;
}

export function getIdentityUsers(keyword: string, params?: { pageNum?: number; pageSize?: number; status?: number }) {
  return get<PageResult<NoticeIdentityUser>>('/identity/users/page', {
    params: {
      page: params?.pageNum,
      size: params?.pageSize,
      status: params?.status,
      keyword: keyword || undefined,
    },
  }).then((data) => ({
    list: data.list || (data as unknown as { records?: NoticeIdentityUser[] }).records || [],
    total: data.total,
    page: data.page,
    size: data.size,
    pages: data.pages,
  }));
}

export function getNoticeOrgTree(params?: { parentId?: string; includeDisabled?: boolean }) {
  return get<NoticeOrgNode[]>('/org/tree', { params });
}

export function getNoticePosts(params?: { pageNum?: number; pageSize?: number; postStatus?: string }) {
  return get<PageResult<NoticePost>>('/post/page', {
    params: {
      page: params?.pageNum,
      size: params?.pageSize,
      postStatus: params?.postStatus,
    },
  }).then((data) => ({
    list: data.list || (data as unknown as { records?: NoticePost[] }).records || [],
    total: data.total,
    page: data.page,
    size: data.size,
    pages: data.pages,
  }));
}

export function getNoticeRoles() {
  return get<NoticeRole[]>('/authorization/roles');
}

export function getBusinessTypes(params?: Record<string, unknown>) {
  return get<PageResult<NoticeBusinessType>>('/notice/business-types', { params });
}

export function createBusinessType(data: Partial<NoticeBusinessType>) {
  return post<NoticeBusinessType>('/notice/business-types', data);
}

export function updateBusinessType(id: string, data: Partial<NoticeBusinessType>) {
  return put<NoticeBusinessType>(`/notice/business-types/${id}`, data);
}

export function deleteBusinessType(id: string) {
  return del<boolean>(`/notice/business-types/${id}`);
}

export function getBusinessConfigVersions(businessTypeId: string) {
  return get<NoticeBusinessConfigVersion[]>(`/notice/business-types/${businessTypeId}/config-versions`);
}

export function saveBusinessConfigDraft(businessTypeId: string, data: Partial<NoticeBusinessConfigVersion>) {
  return put<NoticeBusinessConfigVersion>(`/notice/business-types/${businessTypeId}/config-draft`, data);
}

export function publishBusinessConfigDraft(businessTypeId: string) {
  return post<boolean>(`/notice/business-types/${businessTypeId}/config-draft/publish`);
}

export function activateBusinessConfigVersion(businessTypeId: string, version: number) {
  return post<boolean>(`/notice/business-types/${businessTypeId}/config-versions/${version}/activate`);
}

export function getChannelTemplates(businessTypeId: string) {
  return get<NoticeChannelTemplate[]>(`/notice/business-types/${businessTypeId}/channel-templates`);
}

export function saveChannelTemplate(businessTypeId: string, channelType: NoticeChannelType, data: Partial<NoticeChannelTemplate>) {
  return put<NoticeChannelTemplate>(`/notice/business-types/${businessTypeId}/channel-templates/${channelType}`, {
    channelType,
    templateName: data.templateName,
    titleTemplate: data.titleTemplate,
    contentTemplate: data.contentTemplate,
    channelTemplateId: data.channelTemplateId,
    variableMapping: data.variableMapping,
    enabled: data.enabled,
    channelConfigId: data.channelConfigId,
  });
}

export function publishChannelTemplate(businessTypeId: string, channelType: NoticeChannelType) {
  return post<boolean>(`/notice/business-types/${businessTypeId}/channel-templates/${channelType}/publish`);
}

export function getChannelConfigs(params?: Record<string, unknown>, options?: { silentError?: boolean }) {
  return get<PageResult<NoticeChannelConfig>>('/notice/channels', { params, silentError: options?.silentError });
}

export function saveChannelConfig(data: Partial<NoticeChannelConfig>) {
  return post<NoticeChannelConfig>('/notice/channels', data);
}

export function deleteChannelConfig(id: string) {
  return del<boolean>('/notice/channels', { params: { id } });
}

export function getNoticeTasks(params?: Record<string, unknown>) {
  return get<PageResult<NoticeTask>>('/notice/tasks', { params });
}

export function getSendRecords(params?: Record<string, unknown>) {
  return get<PageResult<NoticeSendRecord>>('/notice/records', { params });
}

export function retrySendRecord(id: string) {
  return post<boolean>(`/notice/records/${id}/retry`);
}

export function retrySendRecords(ids: string[]) {
  return post<boolean>('/notice/records/retry-batch', { ids });
}

export function markSendRecordManualSuccess(id: string, reason: string) {
  return post<boolean>(`/notice/records/${id}/manual-success`, { reason });
}

export function markSendRecordsManualSuccess(ids: string[], reason: string) {
  return post<boolean>('/notice/records/manual-success-batch', { ids, reason });
}

export function ignoreSendRecord(id: string, reason: string) {
  return post<boolean>(`/notice/records/${id}/ignore`, { reason });
}

export function ignoreSendRecords(ids: string[], reason: string) {
  return post<boolean>('/notice/records/ignore-batch', { ids, reason });
}

export function getMySiteMessages(params?: NoticeSiteMessagePageQuery) {
  return get<PageResult<NoticeSiteMessage>>('/notice/site/my/messages', { params });
}

export function getMySiteMessageDetail(id: string) {
  return get<NoticeSiteMessage>(`/notice/site/my/messages/${id}`);
}

export function getNoticeSettings() {
  return get<Record<string, unknown>>('/notice/settings');
}

export function saveNoticeSettings(data: Record<string, unknown>) {
  return put<boolean>('/notice/settings', data);
}

export function getRecipientAccounts(params?: { userId?: string; accountType?: NoticeRecipientAccountType }) {
  return get<NoticeRecipientAccount[]>('/notice/recipient-accounts', { params });
}

export function saveRecipientAccount(data: Partial<NoticeRecipientAccount> & {
  accountType: NoticeRecipientAccountType;
  accountValue: string;
  verifiedStatus?: NoticeRecipientAccountStatus;
}) {
  return post<NoticeRecipientAccount>('/notice/recipient-accounts', data);
}

export function disableRecipientAccount(id: string, userId?: string) {
  return post<boolean>(`/notice/recipient-accounts/${id}/disable`, undefined, { params: { userId } });
}

export function setDefaultRecipientAccount(id: string, userId?: string) {
  return post<boolean>(`/notice/recipient-accounts/${id}/default`, undefined, { params: { userId } });
}

export function getReceivePreferences(params?: {
  userId?: string;
  scopeType?: NoticeReceivePreferenceScopeType;
  scopeValue?: string;
}) {
  return get<NoticeReceivePreference[]>('/notice/receive-preferences', { params });
}

export function saveReceivePreference(data: Partial<NoticeReceivePreference> & {
  scopeType: NoticeReceivePreferenceScopeType;
  enabled: boolean;
}) {
  return put<NoticeReceivePreference>('/notice/receive-preferences', data);
}

export function getPersonalConfigs(params?: {
  groupCode?: string;
  bizType?: string;
  configKey?: string;
}) {
  return get<Array<PersonalConfig<string>>>('/system/personal-configs', { params });
}

export function getPersonalConfigValue(params: {
  groupCode: string;
  bizType: string;
  configKey: string;
}) {
  return get<PersonalConfig<string> | null>('/system/personal-configs/value', { params });
}

export function savePersonalConfig(data: PersonalConfig<string>) {
  return post<PersonalConfig<string>>('/system/personal-configs', data);
}

export function normalizeNoticeReminderSetting(value?: unknown): NoticeReminderSetting {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return { ...defaultNoticeReminderSetting };
  }
  const record = value as Partial<NoticeReminderSetting> & {
    soundEnabled?: boolean;
    soundText?: string;
  };
  return {
    popupEnabled: typeof record.popupEnabled === 'boolean' ? record.popupEnabled : defaultNoticeReminderSetting.popupEnabled,
    popupPlacement: record.popupPlacement === 'bottom-right' ? 'bottom-right' : 'top-right',
    voiceEnabled: typeof record.voiceEnabled === 'boolean'
      ? record.voiceEnabled
      : typeof record.soundEnabled === 'boolean'
        ? record.soundEnabled
        : defaultNoticeReminderSetting.voiceEnabled,
    voiceText: typeof record.voiceText === 'string' && record.voiceText.trim()
      ? record.voiceText
      : typeof record.soundText === 'string' && record.soundText.trim()
        ? record.soundText
        : defaultNoticeReminderSetting.voiceText,
    desktopNotificationEnabled: typeof record.desktopNotificationEnabled === 'boolean'
      ? record.desktopNotificationEnabled
      : defaultNoticeReminderSetting.desktopNotificationEnabled,
  };
}

export async function getNoticeReminderSetting() {
  const config = await getPersonalConfigValue(NOTICE_REMINDER_CONFIG_SCOPE);
  if (!config?.configValue) {
    return { ...defaultNoticeReminderSetting };
  }
  try {
    return normalizeNoticeReminderSetting(JSON.parse(config.configValue));
  } catch {
    return { ...defaultNoticeReminderSetting };
  }
}

export function saveNoticeReminderSetting(setting: NoticeReminderSetting) {
  return savePersonalConfig({
    ...NOTICE_REMINDER_CONFIG_SCOPE,
    configValue: JSON.stringify(normalizeNoticeReminderSetting(setting)),
    valueType: 'JSON',
    configName: '通知提醒设置',
  });
}

export function getMyUnreadCount() {
  return get<NoticeUnreadCount>('/notice/site/my/unread-count');
}

export function markMySiteMessageRead(id: string) {
  return post<boolean>(`/notice/site/my/messages/${id}/read`);
}

export function markMySiteMessagesRead(ids: string[]) {
  return post<boolean>('/notice/site/my/messages/read-batch', { ids });
}

export function markAllMySiteMessagesRead() {
  return post<boolean>('/notice/site/my/messages/read-all');
}

export function deleteMySiteMessage(id: string) {
  return post<boolean>(`/notice/site/my/messages/${id}/delete`);
}

export const noticeApi = {
  sendNotice,
  sendSiteNotice,
  getBusinessTypes,
  createBusinessType,
  updateBusinessType,
  deleteBusinessType,
  getBusinessConfigVersions,
  saveBusinessConfigDraft,
  publishBusinessConfigDraft,
  activateBusinessConfigVersion,
  getChannelTemplates,
  saveChannelTemplate,
  publishChannelTemplate,
  getChannelConfigs,
  saveChannelConfig,
  deleteChannelConfig,
  getNoticeTasks,
  getSendRecords,
  retrySendRecord,
  retrySendRecords,
  markSendRecordManualSuccess,
  markSendRecordsManualSuccess,
  ignoreSendRecord,
  ignoreSendRecords,
  getMySiteMessages,
  getMySiteMessageDetail,
  getIdentityUsers,
  getNoticeOrgTree,
  getNoticePosts,
  getNoticeRoles,
  getNoticeSettings,
  saveNoticeSettings,
  getRecipientAccounts,
  saveRecipientAccount,
  disableRecipientAccount,
  setDefaultRecipientAccount,
  getReceivePreferences,
  saveReceivePreference,
  getPersonalConfigs,
  getPersonalConfigValue,
  savePersonalConfig,
  getNoticeReminderSetting,
  saveNoticeReminderSetting,
  getMyUnreadCount,
  markMySiteMessageRead,
  markMySiteMessagesRead,
  markAllMySiteMessagesRead,
  deleteMySiteMessage,
};
