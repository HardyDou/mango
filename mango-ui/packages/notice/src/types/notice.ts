export type NoticePriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type NoticeReadStatus = 'UNREAD' | 'READ';
export type NoticeSiteMessageCategory = 'APPROVAL' | 'SYSTEM' | 'BUSINESS';
export type NoticeChannelType = 'SITE' | 'SMS' | 'EMAIL' | 'WECHAT_OFFICIAL' | 'WECOM' | 'DINGTALK';
export type NoticeTaskStatus = 'WAITING' | 'SENDING' | 'PARTIAL_SUCCESS' | 'SUCCESS' | 'FAILED' | 'CANCELED';
export type NoticeSendStatus =
  | 'PENDING'
  | 'SENDING'
  | 'SUCCESS'
  | 'FAILED'
  | 'RETRY_WAITING'
  | 'FINAL_FAILED'
  | 'MANUAL_SUCCESS'
  | 'IGNORED'
  | 'CANCELED';
export type NoticeTemplateVersionStatus = 'DRAFT' | 'ACTIVE' | 'HISTORY';
export type NoticeSyncStatus = 'SYNCED' | 'PENDING_PUBLISH';
export type NoticeChannelConfigStatus = 'COMPLETE' | 'INCOMPLETE';
export type NoticeChannelCapabilityMode = 'SEND' | 'RECEIVE' | 'BOTH';
export type NoticeChannelSendHealthStatus = 'NONE' | 'SUCCESS' | 'FAILED';
export type NoticeChannelRouteMode = 'EXACT' | 'TAG' | 'AUTO';
export type NoticeChannelSecretStatus = 'NOT_REQUIRED' | 'COMPLETE' | 'INCOMPLETE';
export type NoticeRecipientTargetType = 'USER' | 'ORG' | 'POST' | 'ROLE';
export type NoticeAnnouncementStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE';
export type NoticeAnnouncementTargetType = 'ALL' | 'ORG' | 'ROLE' | 'USER';
export type NoticeAnnouncementConfirmStatus = 'NOT_REQUIRED' | 'PENDING' | 'CONFIRMED';
export type NoticeRecipientAccountType = 'MOBILE' | 'EMAIL' | 'WECHAT' | 'WECOM' | 'DINGTALK' | 'FEISHU';
export type NoticeRecipientAccountStatus = 'UNBOUND' | 'PENDING_VERIFY' | 'VERIFIED' | 'DISABLED';
export type NoticeReceivePreferenceScopeType = 'GLOBAL' | 'BIZ_GROUP' | 'BIZ_TYPE';
export type NoticeSiteMessageTargetType = 'NONE' | 'ROUTE' | 'FLOW';
export type NoticeSiteMessageActionInteractionType = 'EVENT' | 'ROUTE';
export type NoticeSiteMessageActionStatus =
  'AVAILABLE' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'DISABLED' | 'EXPIRED';
export type NoticeSiteMessageActionRequestStatus = 'REQUESTED' | 'SUCCEEDED' | 'FAILED';

export interface NoticeSiteMessageTarget {
  targetType: NoticeSiteMessageTargetType;
  targetKey?: string;
  params?: Record<string, unknown>;
  openMode?: string;
}

export interface NoticeSiteMessageSubject {
  subjectType?: string;
  subjectId?: string;
  subjectName?: string;
}

export interface NoticeSiteMessageAction {
  id: string;
  actionCode: string;
  actionLabel: string;
  interactionType: NoticeSiteMessageActionInteractionType;
  eventType?: string;
  target?: NoticeSiteMessageTarget;
  confirmRequired?: boolean;
  inputSchema?: string;
  status: NoticeSiteMessageActionStatus;
  failureReason?: string;
  sortOrder?: number;
  expireTime?: string;
}

export interface NoticeSiteMessageActionRequest {
  requestId: string;
  messageId: string;
  actionCode: string;
  eventId?: string;
  status: NoticeSiteMessageActionRequestStatus;
  failCode?: string;
  failReason?: string;
  result?: Record<string, unknown>;
  createdAt?: string;
  finishedAt?: string;
}

export interface NoticeSiteMessage {
  id: string;
  title: string;
  content: string;
  userId: string;
  messageScene?: string;
  subject?: NoticeSiteMessageSubject;
  target?: NoticeSiteMessageTarget;
  data?: Record<string, unknown>;
  actions?: NoticeSiteMessageAction[];
  expireTime?: string;
  priority: NoticePriority;
  readStatus: NoticeReadStatus;
  readTime?: string;
  bizGroup?: string;
  bizName?: string;
  bizType?: string;
  bizId?: string;
  createTime?: string;
}

export interface NoticeSiteMessagePageQuery {
  pageNum?: number;
  pageSize?: number;
  unreadOnly?: boolean;
  category?: NoticeSiteMessageCategory;
  keyword?: string;
  bizGroup?: string;
  bizType?: string;
  priority?: NoticePriority;
  startTime?: string;
  endTime?: string;
}

export interface NoticeRecipientAccount {
  id: string;
  userId: string;
  accountType: NoticeRecipientAccountType;
  accountValue: string;
  displayName?: string;
  verifiedStatus: NoticeRecipientAccountStatus;
  defaultAccount: boolean;
  enabled: boolean;
  updatedAt?: string;
}

export interface NoticeReceivePreference {
  id: string;
  userId: string;
  scopeType: NoticeReceivePreferenceScopeType;
  scopeValue?: string;
  channelType?: NoticeChannelType;
  enabled: boolean;
  accountId?: string;
  updatedAt?: string;
}

export type NoticePopupPlacement = 'top-right' | 'bottom-right';
export type NoticeReminderMode = 'SOUND' | 'VOICE';
export type NoticeSoundType = 'IM' | 'SOFT' | 'DOUBLE' | 'NONE';

export interface NoticeReminderSetting {
  popupEnabled: boolean;
  popupPlacement: NoticePopupPlacement;
  voiceEnabled: boolean;
  reminderMode: NoticeReminderMode;
  voiceText: string;
  soundType: NoticeSoundType;
  desktopNotificationEnabled: boolean;
}

export interface PersonalConfig<T = string> {
  id?: string;
  tenantId?: string;
  userId?: string;
  groupCode: string;
  bizType: string;
  configKey: string;
  configValue: T;
  valueType?: string;
  configName?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
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

export interface NoticeRecipientTargetCommand {
  targetType: NoticeRecipientTargetType;
  targetId: string;
  targetName?: string;
}

export interface NoticeSendCommand {
  bizType: string;
  bizId?: string;
  params?: Record<string, unknown>;
  messageScene?: string;
  messageSubject?: NoticeSiteMessageSubject;
  messageTarget?: NoticeSiteMessageTarget;
  messageData?: Record<string, unknown>;
  messageActions?: Array<{
    actionCode: string;
    actionLabel: string;
    interactionType?: NoticeSiteMessageActionInteractionType;
    eventType?: string;
    target?: NoticeSiteMessageTarget;
    confirmRequired?: boolean;
    inputSchema?: string;
    sortOrder?: number;
    expireTime?: string;
  }>;
  messageExpireTime?: string;
  channelTypes?: NoticeChannelType[];
  recipients?: NoticeRecipientCommand[];
  recipientTargets?: NoticeRecipientTargetCommand[];
  userId?: string;
  userIds?: string[];
  title?: string;
  content?: string;
  priority?: NoticePriority;
  idempotentKey?: string;
}

export interface NoticeAnnouncementTargetCommand {
  targetType: NoticeAnnouncementTargetType;
  targetId?: string;
  targetName?: string;
  includeChildren?: boolean;
}

export interface SaveNoticeAnnouncementCommand {
  id?: string;
  title: string;
  content: string;
  validStartTime?: string;
  validEndTime?: string;
  pinned?: boolean;
  confirmRequired?: boolean;
  syncMessageEnabled?: boolean;
  targets?: NoticeAnnouncementTargetCommand[];
}

export interface PublishNoticeAnnouncementCommand {
  id?: string;
  validStartTime?: string;
  validEndTime?: string;
  pinned?: boolean;
  confirmRequired?: boolean;
  syncMessageEnabled?: boolean;
  targets?: NoticeAnnouncementTargetCommand[];
}

export interface NoticeAnnouncementTarget {
  id: string;
  announcementId: string;
  targetType: NoticeAnnouncementTargetType;
  targetId?: string;
  targetName?: string;
  includeChildren?: boolean;
}

export interface NoticeAnnouncementStats {
  announcementId: string;
  recipientCount: number;
  readCount: number;
  pendingConfirmCount: number;
  confirmedCount: number;
}

export interface NoticeAnnouncement {
  id: string;
  title: string;
  content: string;
  status: NoticeAnnouncementStatus;
  publishTime?: string;
  validStartTime?: string;
  validEndTime?: string;
  pinned?: boolean;
  confirmRequired?: boolean;
  syncMessageEnabled?: boolean;
  targets?: NoticeAnnouncementTarget[];
  stats?: NoticeAnnouncementStats;
  readStatus?: NoticeReadStatus;
  readTime?: string;
  confirmStatus?: NoticeAnnouncementConfirmStatus;
  confirmTime?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface NoticeBusinessType {
  id: string;
  bizType: string;
  bizName: string;
  bizGroup?: string;
  domainCode?: string;
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

export interface NoticeDomainOption {
  id?: string;
  domainCode: string;
  domainName: string;
  children?: NoticeDomainOption[];
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
  routeMode?: NoticeChannelRouteMode;
  routeTagCode?: string;
}

export interface NoticeChannelConfig {
  id: string;
  configCode: string;
  channelType: NoticeChannelType;
  capabilityMode: NoticeChannelCapabilityMode;
  providerCode?: string;
  configName?: string;
  configJson?: string;
  secretValues?: NoticeChannelSecretValue[];
  resourceId?: string;
  resourceVersion?: number;
  resourceModuleCode?: string;
  resourceSource?: 'MANUAL' | 'RESOURCE';
  secretStatus?: NoticeChannelSecretStatus;
  missingSecretKeys?: string[];
  routeTagCodes?: string[];
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

export interface NoticeChannelSecretValue {
  key: string;
  value: string;
}

export interface NoticeRouteTag {
  id: string;
  channelType: NoticeChannelType;
  tagCode: string;
  tagName: string;
  description?: string;
  candidateCount: number;
  candidateConfigNames: string[];
}

export interface NoticeChannelReferenceImpact {
  referenceCount: number;
  businessTemplateNames: string[];
}

export interface NoticeTask {
  id: string;
  taskCode: string;
  bizType: string;
  bizGroup?: string;
  bizName?: string;
  bizId?: string;
  paramsSnapshot?: string;
  recipientTargetsSnapshot?: string;
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
  userId?: string;
  recipientName?: string;
  recipientAccount?: string;
  bizType?: string;
  bizGroup?: string;
  bizName?: string;
  messageName?: string;
  bizId?: string;
  businessChannelTemplateId?: string;
  businessChannelTemplateName?: string;
  templateVersion?: number;
  channelType: NoticeChannelType;
  channelConfigId?: string;
  channelConfigName?: string;
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

export type NoticeInboundMessageStatus =
  'RECEIVED' | 'ATTACHMENT_PROCESSING' | 'READY_TO_BROADCAST' | 'BROADCASTED' | 'RETRYABLE_FAILED' | 'DEAD_LETTER';

export type NoticeInboundAttachmentStatus = 'PENDING' | 'PROCESSING' | 'SAVED' | 'RETRYABLE_FAILED' | 'DEAD_LETTER';

export interface NoticeInboundAttachment {
  id: string;
  fileId?: string;
  fileName: string;
  contentType?: string;
  fileSize: number;
  status: NoticeInboundAttachmentStatus;
  failureReason?: string;
}

export interface NoticeInboundMessage {
  id: string;
  channelConfigId: string;
  channelType: NoticeChannelType;
  providerCode?: string;
  messageId?: string;
  subject?: string;
  fromAddress?: string;
  toAddressesJson?: string;
  bodyText?: string;
  bodyHtml?: string;
  status: NoticeInboundMessageStatus;
  eventId: string;
  failureCode?: string;
  failureReason?: string;
  attemptCount: number;
  receivedAt: string;
  processedAt?: string;
  attachments?: NoticeInboundAttachment[];
}

export interface NoticeSendResult {
  successCount: number;
  failCount: number;
}

export interface NoticeUnreadCount {
  count: number;
}

export interface NoticeUnreadCategoryCount {
  category: NoticeSiteMessageCategory;
  count: number;
}

export interface NoticeUnreadCategoryStats {
  total: number;
  categories: NoticeUnreadCategoryCount[];
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
  pages?: number;
}
