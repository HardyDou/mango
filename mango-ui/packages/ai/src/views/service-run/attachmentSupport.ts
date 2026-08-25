import type { AiMessageContentPartCommand, AiModality } from '@mango/ai-api';

const TEXT_FILE_EXTENSIONS = ['.txt', '.md', '.csv', '.json', '.xml'];
const AUDIO_FILE_EXTENSIONS = ['.mp3', '.wav'];
const MAX_ATTACHMENT_BYTES = 20 * 1024 * 1024;
const MAX_TOTAL_ATTACHMENT_BYTES = 40 * 1024 * 1024;

export interface AttachmentSupport {
  accept: string;
  labels: string[];
  enabled: boolean;
}

export type AttachmentFileType = Extract<AiMessageContentPartCommand['type'], 'IMAGE' | 'VIDEO' | 'AUDIO' | 'FILE'>;

export type AttachmentValidation =
  { accepted: true; partType: AttachmentFileType } | { accepted: false; message: string };

export function attachmentSupport(modalities: AiModality[] = []): AttachmentSupport {
  const values = new Set(modalities);
  const accept: string[] = [];
  const labels: string[] = [];
  if (values.has('IMAGE')) {
    accept.push('image/*');
    labels.push('图片');
  }
  if (values.has('AUDIO')) {
    accept.push('audio/mpeg', 'audio/wav', ...AUDIO_FILE_EXTENSIONS);
    labels.push('音频');
  }
  if (values.has('VIDEO')) {
    accept.push('video/*');
    labels.push('视频');
  }
  if (values.has('FILE')) {
    accept.push('application/pdf', '.pdf');
    labels.push('PDF');
  }
  if (values.has('TEXT') || values.has('FILE')) {
    accept.push('text/*', 'application/json', 'application/xml', ...TEXT_FILE_EXTENSIONS);
    labels.push('文本文件');
  }
  return { accept: [...new Set(accept)].join(','), labels: [...new Set(labels)], enabled: accept.length > 0 };
}

export function contentPartType(file: Pick<File, 'name' | 'type'>): AttachmentFileType | undefined {
  const contentType = file.type.toLowerCase();
  const extension = fileExtension(file.name);
  if (contentType.startsWith('image/')) return 'IMAGE';
  if (
    contentType === 'audio/mpeg' ||
    contentType === 'audio/mp3' ||
    contentType === 'audio/wav' ||
    AUDIO_FILE_EXTENSIONS.includes(extension)
  )
    return 'AUDIO';
  if (contentType.startsWith('video/')) return 'VIDEO';
  if (contentType === 'application/pdf' || extension === '.pdf') return 'FILE';
  if (
    contentType.startsWith('text/') ||
    contentType === 'application/json' ||
    contentType === 'application/xml' ||
    TEXT_FILE_EXTENSIONS.includes(extension)
  )
    return 'FILE';
  return undefined;
}

export function validateAttachment(
  file: File,
  modalities: AiModality[],
  currentTotalBytes: number,
): AttachmentValidation {
  const partType = contentPartType(file);
  if (!partType) return { accepted: false, message: `不支持文件“${file.name}”的格式` };
  if (file.size <= 0) return { accepted: false, message: `文件“${file.name}”内容为空` };
  if (file.size > MAX_ATTACHMENT_BYTES) return { accepted: false, message: `文件“${file.name}”超过20MB` };
  if (currentTotalBytes + file.size > MAX_TOTAL_ATTACHMENT_BYTES) {
    return { accepted: false, message: '单条消息附件总大小不能超过40MB' };
  }
  const required = requiredModality(file, partType);
  if (!modalities.includes(required)) {
    return { accepted: false, message: `当前模型不支持${modalityLabel(required)}输入，请更换模型或文件` };
  }
  return { accepted: true, partType };
}

function requiredModality(
  file: Pick<File, 'name' | 'type'>,
  partType: AiMessageContentPartCommand['type'],
): AiModality {
  if (partType === 'IMAGE') return 'IMAGE';
  if (partType === 'AUDIO') return 'AUDIO';
  if (partType === 'VIDEO') return 'VIDEO';
  return isTextFile(file) ? 'TEXT' : 'FILE';
}

function isTextFile(file: Pick<File, 'name' | 'type'>) {
  const contentType = file.type.toLowerCase();
  return (
    contentType.startsWith('text/') ||
    contentType === 'application/json' ||
    contentType === 'application/xml' ||
    TEXT_FILE_EXTENSIONS.includes(fileExtension(file.name))
  );
}

function modalityLabel(value: AiModality) {
  const labels: Record<AiModality, string> = {
    TEXT: '文本文件',
    IMAGE: '图片',
    AUDIO: '音频',
    VIDEO: '视频',
    FILE: '文件',
    VECTOR: '向量',
  };
  return labels[value];
}

function fileExtension(fileName: string) {
  const index = fileName.lastIndexOf('.');
  return index < 0 ? '' : fileName.slice(index).toLowerCase();
}
