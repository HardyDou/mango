export type NoticePriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type NoticeReadStatus = 'UNREAD' | 'READ';
export type NoticeChannelType = 'SITE' | 'SMS' | 'EMAIL' | 'WECHAT_OFFICIAL' | 'WECOM' | 'DINGTALK';
export type NoticeTaskStatus = 'WAITING' | 'SENDING' | 'PARTIAL_SUCCESS' | 'SUCCESS' | 'FAILED' | 'CANCELED';
export type NoticeSendStatus = 'PENDING' | 'SENDING' | 'SUCCESS' | 'FAILED' | 'RETRY_WAITING' | 'FINAL_FAILED' | 'CANCELED';
export type NoticeTemplateVersionStatus = 'DRAFT' | 'ACTIVE' | 'HISTORY';
export type NoticeSyncStatus = 'SYNCED' | 'PENDING_PUBLISH';
export type NoticeChannelConfigStatus = 'COMPLETE' | 'INCOMPLETE';
export type NoticeChannelSendHealthStatus = 'NONE' | 'SUCCESS' | 'FAILED';

export interface NoticeSiteMessage {
  id: string;
  title: string;
  content: string;
  userId: string;
  priority: NoticePriority;
  readStatus: NoticeReadStatus;
  readTime?: string;
  bizType?: string;
  bizId?: string;
  createTime?: string;
}

export interface NoticeSiteMessagePageQuery {
  pageNum?: number;
  pageSize?: number;
  unreadOnly?: boolean;
  bizType?: string;
}

export interface NoticeRecipientCommand {
  userId?: string;
  recipientName?: string;
  mobile?: string;
  email?: string;
  wechatOpenid?: string;
  wecomUserId?: string;
  dingtalkUserId?: string;
  externalId?: string;
}

export interface NoticeSendCommand {
  bizType: string;
  bizId?: string;
  params?: Record<string, unknown>;
  channelTypes?: NoticeChannelType[];
  recipients?: NoticeRecipientCommand[];
  userId?: string;
  userIds?: string[];
  title?: string;
  content?: string;
  priority?: NoticePriority;
  idempotentKey?: string;
}

export interface NoticeBusinessType {
  id: string;
  bizType: string;
  bizName: string;
  bizGroup?: string;
  description?: string;
  paramsSchema?: string;
  enabled: boolean;
  defaultPriority: NoticePriority;
  idempotentStrategy?: string;
  createdAt?: string;
  updatedAt?: string;
  syncStatus?: NoticeSyncStatus;
  syncReason?: string;
  activeVersion?: number;
  draftVersion?: number;
  lastPublishTime?: string;
  enabledChannels?: string;
}

export interface NoticeBusinessConfigVersion {
  id: string;
  businessTypeId: string;
  bizType: string;
  paramsSchema?: string;
  defaultPriority: NoticePriority;
  idempotentStrategy?: string;
  version: number;
  versionStatus: NoticeTemplateVersionStatus;
  publishTime?: string;
}

export interface NoticeChannelTemplate {
  id: string;
  businessTypeId: string;
  bizType: string;
  channelType: NoticeChannelType;
  templateName?: string;
  titleTemplate?: string;
  contentTemplate?: string;
  channelTemplateId?: string;
  variableMapping?: string;
  version: number;
  versionStatus: NoticeTemplateVersionStatus;
  enabled: boolean;
  channelConfigId?: string;
}

export interface NoticeChannelConfig {
  id: string;
  channelType: NoticeChannelType;
  providerCode?: string;
  configName?: string;
  configJson?: string;
  enabled: boolean;
  priority: number;
  weight: number;
  configStatus?: NoticeChannelConfigStatus;
  lastSendStatus?: NoticeChannelSendHealthStatus;
  lastSendTime?: string;
  lastFailureCode?: string;
  lastFailureReason?: string;
  rateLimitConfig?: string;
  updatedAt?: string;
}

export interface NoticeTask {
  id: string;
  taskCode: string;
  bizType: string;
  bizId?: string;
  channelTypes?: string;
  status: NoticeTaskStatus;
  totalCount: number;
  successCount: number;
  failCount: number;
  createdAt?: string;
}

export interface NoticeSendRecord {
  id: string;
  taskId: string;
  recipientId: string;
  bizType?: string;
  bizId?: string;
  channelType: NoticeChannelType;
  requestId?: string;
  status: NoticeSendStatus;
  renderedTitle?: string;
  renderedContent?: string;
  requestSnapshot?: string;
  responseSnapshot?: string;
  providerMessageId?: string;
  failCode?: string;
  failReason?: string;
  retryCount: number;
  sentAt?: string;
}

export interface NoticeSendResult {
  successCount: number;
  failCount: number;
}

export interface NoticeUnreadCount {
  count: number;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
  pages?: number;
}
