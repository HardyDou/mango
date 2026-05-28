export { default as NoticeBell } from './components/NoticeBell.vue';
export { default as NoticeDetailDialog } from './components/NoticeDetailDialog.vue';
export { default as NoticeClientBell } from './client/NoticeClientBell.vue';
export { default as NoticeClientMessageCenter } from './client/NoticeClientMessageCenter.vue';
export { default as NoticeClientReceiveSetting } from './client/NoticeClientReceiveSetting.vue';
export type { NoticeClientBellRuntimeConfig } from './client/types';
export { getNoticeReminderSetting, saveNoticeReminderSetting } from './api/notice';
