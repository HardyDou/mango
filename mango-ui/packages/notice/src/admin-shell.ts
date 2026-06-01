import { NoticeBell, getNoticeReminderSetting } from './client';
import { registerMangoNoticeBellProvider } from '@mango/admin-pages/notice';

export function registerMangoNoticeAdminShell() {
  registerMangoNoticeBellProvider({
    component: NoticeBell,
    getReminderSetting: getNoticeReminderSetting,
  });
}
