/**
 * Message API - 消息推送接口
 *
 * Backend API prefix: /message
 * Response: {code, msg, data, success}
 */

import { get, post } from '@mango/common';

// ==================== 类型定义 ====================

/** 消息类型 */
export type MessageType = 'notification' | 'alert' | 'system';

/** 消息优先级 */
export type MessagePriority = 'low' | 'normal' | 'high' | 'urgent';

/** 消息 */
export interface Message {
  id?: number;
  /** 消息标题 */
  title: string;
  /** 消息内容 */
  content: string;
  /** 消息类型 */
  type?: MessageType;
  /** 优先级 */
  priority?: MessagePriority;
  /** 目标用户ID（可选，null 表示广播） */
  targetUserId?: number;
  /** 目标用户ID列表（批量发送） */
  targetUserIds?: number[];
  /** 过期时间（可选） */
  expireTime?: string;
  /** 扩展数据（JSON 字符串） */
  extraData?: string;
  /** 创建时间 */
  createTime?: string;
}

/** 消息查询参数 */
export interface MessageQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  type?: MessageType;
  priority?: MessagePriority;
  startTime?: string;
  endTime?: string;
  unreadOnly?: boolean;
}

/** 分页结果 */
export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

/** 消息发送响应 */
export interface MessageSendResponse {
  messageId: number;
  successCount: number;
  failCount: number;
}

// ==================== 消息发送 API ====================

/**
 * 发送消息给指定用户
 * @param data 消息内容
 */
export function sendMessage(data: Message) {
  return post<MessageSendResponse>('/message/send', data);
}

/**
 * 广播消息（所有在线用户）
 * @param data 消息内容
 */
export function broadcastMessage(data: Message) {
  return post<MessageSendResponse>('/message/broadcast', data);
}

// ==================== 消息查询 API ====================

/**
 * 分页查询当前用户的消息
 * @param params 查询参数
 */
export function getMessageList(params?: MessageQuery) {
  return get<PageResult<Message>>('/message/list', { params });
}

/**
 * 获取消息详情
 * @param id 消息ID
 */
export function getMessageDetail(id: number) {
  return get<Message>(`/message/${id}`);
}

/**
 * 获取未读消息数量
 */
export function getUnreadCount() {
  return get<{ count: number }>('/message/unread/count');
}

/**
 * 标记消息为已读
 * @param id 消息ID
 */
export function markAsRead(id: number) {
  return post<void>(`/message/${id}/read`);
}

/**
 * 批量标记消息为已读
 * @param ids 消息ID列表
 */
export function markAllAsRead(ids: number[]) {
  return post<void>('/message/read/batch', { ids });
}

/**
 * 删除消息
 * @param id 消息ID
 */
export function deleteMessage(id: number) {
  return post<void>(`/message/${id}/delete`);
}

// ==================== 导出便捷方法 ====================

export const messageApi = {
  // 发送消息
  send: sendMessage,
  broadcast: broadcastMessage,

  // 查询消息
  list: getMessageList,
  detail: getMessageDetail,
  unreadCount: getUnreadCount,

  // 操作
  markAsRead,
  markAllAsRead,
  delete: deleteMessage,
};
