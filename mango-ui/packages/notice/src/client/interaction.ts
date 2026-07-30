import type { NoticeSiteMessage, NoticeSiteMessageAction } from '../types/notice';

export interface NoticeInteractionPayload {
  message: NoticeSiteMessage;
  action?: NoticeSiteMessageAction;
  targetKey?: string;
  targetType?: 'ROUTE' | 'FLOW';
  params?: Record<string, unknown>;
  onComplete?: (success: boolean) => void;
}

export function buildNoticeActionInput(
  message: NoticeSiteMessage,
  action?: NoticeSiteMessageAction,
): Record<string, unknown> {
  return {
    ...(message.target?.params || {}),
    ...(action?.target?.params || {}),
    bizType: message.bizType,
    bizId: message.bizId,
    bizGroup: message.bizGroup,
    bizName: message.bizName,
    messageScene: message.messageScene,
    messageId: message.id,
    actionCode: action?.actionCode,
    subject: message.subject || {},
    data: message.data || {},
  };
}

export function buildNoticeInteraction(
  message: NoticeSiteMessage,
  action?: NoticeSiteMessageAction,
): NoticeInteractionPayload {
  return {
    message,
    action,
    targetKey: action?.target?.targetKey || message.target?.targetKey,
    targetType: (action?.target?.targetType || message.target?.targetType) === 'FLOW' ? 'FLOW' : 'ROUTE',
    params: buildNoticeActionInput(message, action),
  };
}
