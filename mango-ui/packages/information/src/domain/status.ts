import type { InformationFormStatus, InformationSubmissionStatus } from '../api/information';

type Tone = 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'neutral';

const FORM_STATUS_LABELS: Record<InformationFormStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '启用',
  DISABLED: '停用',
};

const FORM_STATUS_TONES: Record<InformationFormStatus, Tone> = {
  DRAFT: 'info',
  PUBLISHED: 'success',
  DISABLED: 'neutral',
};

const SUBMISSION_STATUS_LABELS: Record<InformationSubmissionStatus, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
};

const SUBMISSION_STATUS_TONES: Record<InformationSubmissionStatus, Tone> = {
  PENDING: 'warning',
  PROCESSING: 'warning',
  COMPLETED: 'success',
  CLOSED: 'neutral',
};

export function formStatusLabel(status?: string): string {
  return FORM_STATUS_LABELS[(status || '') as InformationFormStatus] || status || '-';
}

export function formStatusTone(status?: string): Tone {
  return FORM_STATUS_TONES[(status || '') as InformationFormStatus] || 'neutral';
}

export function submissionStatusLabel(status?: string): string {
  return SUBMISSION_STATUS_LABELS[(status || '') as InformationSubmissionStatus] || status || '-';
}

export function submissionStatusTone(status?: string): Tone {
  return SUBMISSION_STATUS_TONES[(status || '') as InformationSubmissionStatus] || 'neutral';
}

/** 列表和详情只要展示到日即可得到与原型一致的紧凑列宽。 */
export function formatDate(value?: string): string {
  if (!value) {
    return '-';
  }
  return String(value).slice(0, 10);
}

export function formatDateTime(value?: string): string {
  if (!value) {
    return '-';
  }
  return String(value).replace('T', ' ').slice(0, 19);
}

export function formatCount(value?: number | string): string {
  const number = Number(value ?? 0);
  if (Number.isNaN(number)) {
    return '0';
  }
  return number.toLocaleString('zh-CN');
}
