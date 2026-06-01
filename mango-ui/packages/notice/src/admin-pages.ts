import { registerModulePages } from '@mango/admin-pages/core';

let registered = false;

export function registerMangoNoticeAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-notice',
    pages: {
      'notice/business-config/index': () => import('./admin').then(m => m.NoticeBusinessConfigView),
      'notice/message-definition/index': () => import('./admin').then(m => m.NoticeMessageDefinitionView),
      'notice/send-message/index': () => import('./admin').then(m => m.NoticeSendMessageView),
      'notice/channel/index': () => import('./admin').then(m => m.NoticeChannelView),
      'notice/task/index': () => import('./admin').then(m => m.NoticeTaskView),
      'notice/record/index': () => import('./admin').then(m => m.NoticeRecordView),
      'notice/site-message/index': () => import('./admin').then(m => m.NoticeSiteMessageView),
      'notice/site/messages/index': () => import('./admin').then(m => m.NoticeSiteMessageView),
      'notice/setting/index': () => import('./admin').then(m => m.NoticeSettingView),
      'notice/receive-setting/index': () => import('./admin').then(m => m.NoticeReceiveSettingView),
      'notice/retry/index': () => import('./admin').then(m => m.NoticeRetryView),
    },
  });
}
