import type { NoticeSiteMessage, NoticeSiteMessageAction } from '../types/notice';

export type NoticeStatusTagType = 'primary' | 'success' | 'warning' | 'info' | 'danger';

export interface NoticeDisplayField {
  key: string;
  label: string;
  value: string;
}

export interface NoticeMessagePresentation {
  typeLabel: string;
  statusLabel: string;
  statusType: NoticeStatusTagType;
  subject: string;
  summary: string;
  fields: NoticeDisplayField[];
  relativeTime: string;
  primaryAction?: NoticeSiteMessageAction;
  primaryActionLabel?: string;
}

const WORKFLOW_STATUS: Record<string, { label: string; type: NoticeStatusTagType }> = {
  'workflow.task.assigned': { label: '待审批', type: 'warning' },
  'workflow.task.claimable': { label: '待领取', type: 'warning' },
  'workflow.task.cc': { label: '待查阅', type: 'primary' },
  'workflow.task.rejected': { label: '已驳回', type: 'danger' },
  'workflow.process.completed': { label: '已完成', type: 'success' },
  'workflow.process.rejected': { label: '已拒绝', type: 'danger' },
  'workflow.process.ended': { label: '已结束', type: 'info' },
};

const VALUE_FIELDS = [
  ['businessKey', '申请单'],
  ['applyCode', '申请编号'],
  ['applicantName', '申请人'],
  ['taskName', '当前节点'],
  ['currentTaskNames', '当前节点'],
  ['assigneeName', '当前处理人'],
  ['currentAssigneeNames', '当前处理人'],
  ['comment', '审批意见'],
  ['reason', '处理原因'],
  ['orderNo', '订单号'],
  ['payOrderNo', '支付单号'],
  ['refundOrderNo', '退款单号'],
  ['amount', '金额'],
  ['statusName', '状态'],
] as const;

export function presentNoticeMessage(message: NoticeSiteMessage, now = Date.now()): NoticeMessagePresentation {
  const data = message.data || {};
  const bizType = message.bizType || message.messageScene || '';
  const status = resolveStatus(bizType, data);
  const primaryAction = visibleNoticeActions(message)[0];
  return {
    typeLabel: message.bizName || resolveTypeLabel(bizType),
    statusLabel: status.label,
    statusType: status.type,
    subject: firstText(
      message.subject?.subjectName,
      data.processName,
      data.definitionName,
      data.applyTitle,
      message.bizName,
      message.title,
      '系统消息',
    ),
    summary: firstText(message.content, '暂无详情'),
    fields: buildDisplayFields(message),
    relativeTime: formatNoticeRelativeTime(message.createTime, now),
    primaryAction,
    primaryActionLabel: primaryAction ? resolveNoticeActionLabel(message, primaryAction) : undefined,
  };
}

export function visibleNoticeActions(message: NoticeSiteMessage): NoticeSiteMessageAction[] {
  return (message.actions || [])
    .filter((action) => action.status !== 'DISABLED' && !isNoticeActionDisabled(action))
    .sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0));
}

export function isNoticeActionDisabled(action: NoticeSiteMessageAction) {
  if (action.interactionType === 'EVENT') {
    return !['AVAILABLE', 'FAILED'].includes(action.status);
  }
  return ['DISABLED', 'EXPIRED'].includes(action.status);
}

export function resolveNoticeActionLabel(message: NoticeSiteMessage, action: NoticeSiteMessageAction) {
  const bizType = message.bizType || message.messageScene || '';
  if (bizType === 'workflow.task.assigned') {
    return isClaimable(message.data) ? '去领取' : '去审批';
  }
  if (bizType === 'workflow.task.claimable') return '去领取';
  if (bizType === 'workflow.task.cc') return '查看抄送';
  if (bizType === 'workflow.task.rejected') return '查看驳回详情';
  if (['workflow.process.completed', 'workflow.process.rejected', 'workflow.process.ended'].includes(bizType)) {
    return '查看申请';
  }
  if (/refund/i.test(bizType)) return '查看退款';
  if (/payment|pay\./i.test(bizType)) return '查看订单';
  if (/password/i.test(bizType)) return '修改密码';
  if (/account|profile|login/i.test(bizType)) return '查看资料';
  return firstText(action.actionLabel, '查看详情');
}

export function formatNoticeRelativeTime(value?: string, now = Date.now()) {
  if (!value) return '-';
  const timestamp = Date.parse(value.includes('T') ? value : value.replace(' ', 'T'));
  if (!Number.isFinite(timestamp)) return value;
  const diff = Math.max(0, now - timestamp);
  if (diff < 60_000) return '刚刚';
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`;
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`;
  if (diff < 604_800_000) return `${Math.floor(diff / 86_400_000)} 天前`;
  return value;
}

export function noticeDesktopSummary(message: NoticeSiteMessage) {
  const presentation = presentNoticeMessage(message);
  const fieldSummary = presentation.fields
    .slice(0, 2)
    .map((field) => `${field.label}：${field.value}`)
    .join('；');
  return [presentation.statusLabel, presentation.subject, fieldSummary || presentation.summary]
    .filter(Boolean)
    .join(' · ')
    .slice(0, 160);
}

function buildDisplayFields(message: NoticeSiteMessage): NoticeDisplayField[] {
  const data = message.data || {};
  const fields: NoticeDisplayField[] = [];
  const seenLabels = new Set<string>();
  const add = (key: string, label: string, raw: unknown) => {
    const value = displayValue(raw);
    if (!value || seenLabels.has(label)) return;
    fields.push({ key, label, value });
    seenLabels.add(label);
  };

  VALUE_FIELDS.forEach(([key, label]) => add(key, label, data[key]));
  if (fields.length === 0) {
    add('bizId', '业务编号', message.bizId);
  }
  return fields.slice(0, 6);
}

function resolveStatus(bizType: string, data: Record<string, unknown>) {
  if (bizType === 'workflow.task.assigned' && isClaimable(data)) {
    return { label: '待领取', type: 'warning' as const };
  }
  if (WORKFLOW_STATUS[bizType]) return WORKFLOW_STATUS[bizType];
  const explicit = firstText(data.applyStatusName, data.statusName);
  if (explicit) return { label: explicit, type: statusType(explicit) };
  if (/fail|failed|reject|error/i.test(bizType)) return { label: '处理失败', type: 'danger' as const };
  if (/success|completed/i.test(bizType)) return { label: '处理成功', type: 'success' as const };
  return { label: '通知', type: 'info' as const };
}

function resolveTypeLabel(bizType: string) {
  if (WORKFLOW_STATUS[bizType]) {
    return bizType.startsWith('workflow.task') ? '工作流任务' : '工作流进度';
  }
  if (/refund/i.test(bizType)) return '退款通知';
  if (/payment|pay\./i.test(bizType)) return '支付通知';
  if (/account|profile|login|password/i.test(bizType)) return '账号通知';
  return bizType || '系统通知';
}

function statusType(status: string): NoticeStatusTagType {
  if (/完成|成功|通过|正常/.test(status)) return 'success';
  if (/失败|拒绝|驳回|异常/.test(status)) return 'danger';
  if (/待|处理中|进行中/.test(status)) return 'warning';
  return 'info';
}

function isClaimable(data?: Record<string, unknown>) {
  if (!data) return false;
  const claimStatus = String(data.claimStatus || '').toUpperCase();
  const assigneeId = displayValue(data.assigneeId || data.assignee);
  return (
    !assigneeId && (claimStatus === 'CLAIMABLE' || hasValues(data.candidateUsers) || hasValues(data.candidateGroups))
  );
}

function hasValues(value: unknown) {
  return Array.isArray(value) ? value.length > 0 : Boolean(value);
}

function displayValue(value: unknown): string {
  if (value === undefined || value === null || value === '') return '';
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') return String(value);
  if (Array.isArray(value)) return value.map(displayValue).filter(Boolean).join('、');
  return '';
}

function firstText(...values: unknown[]) {
  for (const value of values) {
    const text = displayValue(value).trim();
    if (text) return text;
  }
  return '';
}
