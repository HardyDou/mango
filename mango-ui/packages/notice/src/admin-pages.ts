import { defineAsyncComponent } from 'vue';
import { Bell, Message, Reading } from '@element-plus/icons-vue';
import { registerModulePages } from '@mango/admin-pages/core';
import { noticeMessageCenterWidgets } from './widgets/message-center';

let registered = false;

const noticeFeatureRegistration = {
  businessDomainCode: 'NOTICE',
  businessDomainName: '通知管理',
  groupName: '通知管理',
  widgets: noticeMessageCenterWidgets,
  profileSections: [
    {
      key: 'notice-site-message',
      label: '我的消息',
      group: '消息中心',
      icon: Message,
      component: defineAsyncComponent(() => import('./views/site-message/index.vue')),
    },
    {
      key: 'notice-announcement-user',
      label: '系统公告',
      group: '消息中心',
      icon: Reading,
      component: defineAsyncComponent(() => import('./views/announcement-user/index.vue')),
    },
    {
      key: 'notice-receive-setting',
      label: '通知设置',
      group: '消息中心',
      icon: Bell,
      component: defineAsyncComponent(() => import('./views/receive-setting/index.vue')),
    },
  ],
};

export function registerMangoNoticeAdminPages() {
  if (!registered) {
    registered = true;
    registerModulePages({
      moduleCode: 'mango-notice',
      pages: {
        'notice/business-config/index': () => import('./admin').then((m) => m.NoticeBusinessConfigView),
        'notice/message-definition/index': () => import('./admin').then((m) => m.NoticeMessageDefinitionView),
        'notice/send-message/index': () => import('./admin').then((m) => m.NoticeSendMessageView),
        'notice/announcement/index': () => import('./admin').then((m) => m.NoticeAnnouncementView),
        'notice/announcement-user/index': () => import('./admin').then((m) => m.NoticeAnnouncementUserView),
        'notice/channel/index': () => import('./admin').then((m) => m.NoticeChannelView),
        'notice/task/index': () => import('./admin').then((m) => m.NoticeTaskView),
        'notice/record/index': () => import('./admin').then((m) => m.NoticeRecordView),
        'notice/inbound/index': () => import('./admin').then((m) => m.NoticeInboundView),
        'notice/site-message/index': () => import('./admin').then((m) => m.NoticeSiteMessageView),
        'notice/site/messages/index': () => import('./admin').then((m) => m.NoticeSiteMessageView),
        'notice/setting/index': () => import('./admin').then((m) => m.NoticeSettingView),
        'notice/receive-setting/index': () => import('./admin').then((m) => m.NoticeReceiveSettingView),
        'notice/retry/index': () => import('./admin').then((m) => m.NoticeRetryView),
      },
      routes: [
        {
          path: '/notice/setting',
          component: 'notice/setting/index',
          menuName: '全局设置',
          menuCode: 'notice:setting',
          visible: 0,
        },
        {
          path: '/notice/site-message',
          component: 'notice/site-message/index',
          menuName: '我的消息（兼容入口）',
          visible: 0,
        },
        {
          path: '/message-center/receive-setting',
          component: 'notice/receive-setting/index',
          menuName: '通知设置',
          menuCode: 'notice:receive-setting',
          visible: 0,
        },
        {
          path: '/notice/receive-setting',
          component: 'notice/receive-setting/index',
          menuName: '通知设置（兼容入口）',
          visible: 0,
        },
      ],
    });
  }

  return noticeFeatureRegistration;
}
