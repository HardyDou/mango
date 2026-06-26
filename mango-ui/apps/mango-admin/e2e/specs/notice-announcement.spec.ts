import { expect, test, type Page, type Route } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

type AnnouncementStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE';
type ConfirmStatus = 'NOT_REQUIRED' | 'PENDING' | 'CONFIRMED';
type ReadStatus = 'UNREAD' | 'READ';
type TargetType = 'ALL' | 'ORG' | 'ROLE' | 'USER';

interface AnnouncementTarget {
  targetType: TargetType;
  targetId?: string;
  targetName?: string;
  includeChildren?: boolean;
}

interface Announcement {
  id: string;
  title: string;
  content: string;
  status: AnnouncementStatus;
  pinned: boolean;
  confirmRequired: boolean;
  syncMessageEnabled: boolean;
  publishTime?: string;
  updatedAt: string;
  validStartTime?: string;
  validEndTime?: string;
  targets: AnnouncementTarget[];
  readStatus?: ReadStatus;
  confirmStatus?: ConfirmStatus;
  stats: {
    recipientCount: number;
    readCount: number;
    pendingConfirmCount: number;
    confirmedCount: number;
  };
}

interface SiteMessage {
  id: string;
  title: string;
  content: string;
  userId: string;
  priority: 'NORMAL';
  readStatus: ReadStatus;
  bizGroup: string;
  bizName: string;
  bizType: string;
  bizId: string;
  createTime: string;
}

interface TestState {
  announcements: Announcement[];
  messages: SiteMessage[];
  publishRequests: Array<{ id: string; targets?: AnnouncementTarget[] }>;
}

const evidenceDir = resolve(__dirname, '../../../../../mango-docs/evidence/2026-06-26-issue-260-notice-announcement');

function ok(data: unknown) {
  return JSON.stringify({ code: 200, success: true, data });
}

async function fulfillJson(route: Route, data: unknown) {
  await route.fulfill({ status: 200, contentType: 'application/json', body: ok(data) });
}

async function login(page: Page) {
  await page.goto('/#/login');
  const loginData = await page.evaluate(async () => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'admin',
        password: 'admin123',
        tenantId: '1',
        tenantCode: 'default',
        realm: 'INTERNAL',
        actorType: 'INTERNAL_USER',
        partyType: 'INTERNAL_ORG',
        appCode: 'internal-admin',
      }),
    });
    const body = await response.json();
    if (!response.ok || !(body.success || body.code === 200) || !body.data?.accessToken) {
      throw new Error(`登录失败：${JSON.stringify(body)}`);
    }
    return body.data;
  });
  await page.evaluate((data) => {
    const userInfo = {
      ...data,
      tenantId: data.tenantId || '1',
      tenantCode: data.tenantCode || 'default',
      tenantName: data.tenantName || '芒果集团',
      realm: data.realm || 'INTERNAL',
      actorType: data.actorType || 'INTERNAL_USER',
      partyType: data.partyType || 'INTERNAL_ORG',
      partyId: data.partyId || '1',
      appCode: data.appCode || 'internal-admin',
    };
    sessionStorage.setItem('MANGO_TOKEN', data.accessToken);
    sessionStorage.setItem('MANGO_REFRESH_TOKEN', data.refreshToken || '');
    sessionStorage.setItem('MANGO_TOKEN_EXPIRES_AT', String(Date.now() + Number(data.expiresIn || 7200) * 1000));
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
    sessionStorage.setItem('tenantId', String(userInfo.tenantId));
    document.cookie = `MANGO_TOKEN=${encodeURIComponent(data.accessToken)}; path=/; SameSite=Lax`;
  }, loginData);
  const menuResponsePromise = page.waitForResponse(response =>
    response.url().includes('/api/authorization/menus/user') && response.status() === 200
  );
  await page.goto('/#/home');
  await menuResponsePromise;
  await expect(page.getByRole('button', { name: '通知中心' })).toBeVisible({ timeout: 10000 });
}

function createState(): TestState {
  const announcement: Announcement = {
    id: 'ann-100',
    title: '端午值班安排',
    content: '端午期间请按排班表完成值班确认。',
    status: 'PUBLISHED',
    pinned: true,
    confirmRequired: true,
    syncMessageEnabled: true,
    publishTime: '2026-06-26 09:00:00',
    updatedAt: '2026-06-26 09:00:00',
    targets: [{ targetType: 'ORG', targetId: '2002', targetName: '技术部', includeChildren: false }],
    readStatus: 'UNREAD',
    confirmStatus: 'PENDING',
    stats: { recipientCount: 1, readCount: 0, pendingConfirmCount: 1, confirmedCount: 0 },
  };
  return {
    announcements: [announcement],
    messages: [
      {
        id: 'msg-ann-100',
        title: '公告：端午值班安排',
        content: '端午期间请按排班表完成值班确认。',
        userId: '1',
        priority: 'NORMAL',
        readStatus: 'UNREAD',
        bizGroup: 'SYSTEM',
        bizName: '通知公告',
        bizType: 'notice.announcement.published',
        bizId: 'ann-100',
        createTime: '2026-06-26 09:00:01',
      },
    ],
    publishRequests: [],
  };
}

function targetRecipientCount(targets: AnnouncementTarget[]) {
  if (targets.some(target => target.targetType === 'ALL')) {
    return 3;
  }
  const resolvedUsers = new Set<string>();
  for (const target of targets) {
    if (target.targetType === 'USER' && target.targetId) {
      resolvedUsers.add(target.targetId);
    }
    if (target.targetType === 'ORG') {
      resolvedUsers.add('1001');
      resolvedUsers.add('1002');
    }
    if (target.targetType === 'ROLE' && target.targetId === '4001') {
      resolvedUsers.add('1001');
    }
  }
  return resolvedUsers.size;
}

function visibleAnnouncements(state: TestState) {
  return state.announcements
    .filter(item => item.status === 'PUBLISHED')
    .map(item => ({ ...item }));
}

async function setupRoutes(page: Page, state: TestState) {
  const identityUsers = [
    { userId: '1001', username: 'admin', nickname: '管理员', phone: '13800000000', email: 'admin@example.com', status: 1 },
    { userId: '1002', username: 'operator', nickname: '操作员', phone: '13800000001', email: 'operator@example.com', status: 1 },
    { userId: '1003', username: 'auditor', nickname: '审计员', phone: '13800000002', email: 'auditor@example.com', status: 1 },
  ];
  const orgTree = [
    {
      id: '2001',
      orgName: '芒果集团',
      pid: '0',
      orgStatus: '1',
      children: [{ id: '2002', orgName: '技术部', pid: '2001', orgStatus: '1', children: [] }],
    },
  ];
  const roles = [
    { roleId: '4001', roleName: '系统管理员', roleCode: 'ADMIN', status: 1 },
    { roleId: '4002', roleName: '业务操作员', roleCode: 'OPERATOR', status: 1 },
  ];
  const child = (menuId: string, menuName: string, path: string, component: string, visible = 1) => ({
    menuId,
    parentId: '2900',
    menuType: 2,
    menuName,
    menuCode: path.replace('/notice/', 'notice:'),
    path,
    icon: 'Message',
    component,
    moduleCode: 'mango-notice',
    pageType: 'LOCAL_ROUTE',
    visible,
    status: 1,
    children: [],
  });

  await page.route('**/api/system/tenant/login-options**', route =>
    fulfillJson(route, [{ tenantId: '1', tenantCode: 'mango', tenantName: '芒果集团' }])
  );
  await page.route('**/api/auth/login-institutions', route =>
    fulfillJson(route, [{ tenantId: '1', tenantCode: 'mango', tenantName: '芒果集团' }])
  );
  await page.route('**/api/auth/login', route =>
    fulfillJson(route, {
      accessToken: 'notice-announcement-e2e-token',
      tokenType: 'Bearer',
      expiresIn: '7200',
      refreshToken: 'notice-announcement-e2e-refresh-token',
      userId: '1',
      memberId: '1001',
      username: 'admin',
      nickname: 'Admin',
      tenantId: '1',
      tenantCode: 'mango',
      tenantName: '芒果集团',
      appCode: 'internal-admin',
      roles: ['admin'],
      permissions: [
        'notice:announcement:view',
        'notice:announcement:create',
        'notice:announcement:edit',
        'notice:announcement:publish',
        'notice:announcement:offline',
        'notice:announcement-user:view',
        'notice:announcement-user:confirm',
        'notice:site:view',
        'notice:site:edit',
        'notice:site:delete',
      ],
      token: 'notice-announcement-e2e-token',
      userInfo: {
        userId: '1',
        username: 'admin',
        nickname: 'Admin',
        tenantId: '1',
        tenantCode: 'mango',
        tenantName: '芒果集团',
        appCode: 'internal-admin',
        roles: ['admin'],
        permissions: [
          'notice:announcement:view',
          'notice:announcement:create',
          'notice:announcement:edit',
          'notice:announcement:publish',
          'notice:announcement:offline',
          'notice:announcement-user:view',
          'notice:announcement-user:confirm',
          'notice:site:view',
          'notice:site:edit',
          'notice:site:delete',
        ],
      },
    })
  );
  await page.route('**/api/auth/info', route =>
    fulfillJson(route, {
      userId: '1',
      memberId: '1001',
      username: 'admin',
      nickname: 'Admin',
      tenantId: '1',
      tenantCode: 'mango',
      tenantName: '芒果集团',
      appCode: 'internal-admin',
      roles: ['admin'],
      permissions: [
        'notice:announcement:view',
        'notice:announcement:create',
        'notice:announcement:edit',
        'notice:announcement:publish',
        'notice:announcement:offline',
        'notice:announcement-user:view',
        'notice:announcement-user:confirm',
        'notice:site:view',
        'notice:site:edit',
        'notice:site:delete',
      ],
    })
  );
  await page.route('**/api/authorization/menus/user**', route =>
    fulfillJson(route, [
      {
        menuId: '2900',
        parentId: '0',
        menuType: 1,
        menuName: '通知中心',
        menuCode: 'notice',
        path: '/notice',
        icon: 'Bell',
        redirect: '/notice/announcement',
        moduleCode: 'mango-notice',
        pageType: 'LOCAL_ROUTE',
        visible: 1,
        status: 1,
        children: [
          child('2907', '公告管理', '/notice/announcement', 'notice/announcement/index'),
          child('2910', '接收设置', '/notice/receive-setting', 'notice/receive-setting/index', 0),
        ],
      },
      {
        menuId: '2920',
        parentId: '0',
        menuType: 1,
        menuName: '消息中心',
        menuCode: 'message-center',
        path: '/message-center',
        icon: 'Message',
        redirect: '/message-center/site-message',
        moduleCode: 'mango-notice',
        pageType: 'LOCAL_ROUTE',
        visible: 1,
        status: 1,
        children: [
          { ...child('2921', '我的消息', '/message-center/site-message', 'notice/site-message/index'), parentId: '2920' },
          { ...child('2922', '公告', '/message-center/announcement', 'notice/announcement-user/index'), parentId: '2920' },
        ],
      },
    ])
  );
  await page.route('**/api/grid-layout/personal**', route => fulfillJson(route, null));
  await page.route('**/api/calendar/lunar/day**', route =>
    fulfillJson(route, { date: '2026-06-26', lunarText: '五月十二', festival: '' })
  );
  await page.route('**/api/calendar/workdays/day**', route =>
    fulfillJson(route, { date: '2026-06-26', workday: true, holiday: false })
  );
  await page.route('**/api/calendar/workdays/month/summary**', route =>
    fulfillJson(route, { workdayCount: 22, holidayCount: 8, adjustedWorkdayCount: 0 })
  );
  await page.route('**/api/workflow/tasks/my/summary**', route =>
    fulfillJson(route, { total: 0, pending: 0, completed: 0 })
  );
  await page.route('**/api/workflow/tasks/todo/summary**', route =>
    fulfillJson(route, { total: 0, pending: 0 })
  );
  await page.route('**/api/workflow/business-applies/my/summary**', route =>
    fulfillJson(route, { total: 0, draft: 0, running: 0, completed: 0 })
  );
  await page.route('**/api/realtime/transports/negotiate**', route =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        recommended: 'polling',
        order: ['polling'],
        transports: [{ type: 'polling', enabled: true, available: true }],
      }),
    })
  );
  await page.route('**/api/realtime/transports/polling**', route =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) })
  );
  await page.route('**/api/system/personal-configs/value**', route =>
    fulfillJson(route, {
      groupCode: 'notice',
      bizType: 'client_reminder',
      configKey: 'reminder_setting',
      configValue: JSON.stringify({ popupEnabled: false, voiceEnabled: false, desktopNotificationEnabled: false }),
      valueType: 'JSON',
    })
  );
  await page.route('**/api/domain/domains/enabled-tree**', route =>
    fulfillJson(route, [{ id: 'system', domainCode: 'SYSTEM', domainName: '系统平台', children: [] }])
  );
  await page.route('**/identity/users/page**', route => {
    const url = new URL(route.request().url());
    const keyword = url.searchParams.get('keyword') || '';
    const list = keyword
      ? identityUsers.filter(item => [item.username, item.nickname, item.phone, item.email].some(value => value.includes(keyword)))
      : identityUsers;
    return fulfillJson(route, { list, total: list.length, page: 1, size: 20 });
  });
  await page.route('**/org/tree**', route => fulfillJson(route, orgTree));
  await page.route('**/authorization/roles**', route => fulfillJson(route, roles));
  await page.route('**/notice/site/my/unread-count**', route =>
    fulfillJson(route, { count: state.messages.filter(item => item.readStatus === 'UNREAD').length })
  );
  await page.route('**/notice/site/my/messages/read-batch', route => fulfillJson(route, true));
  await page.route('**/notice/site/my/messages/read-all', route => {
    state.messages.forEach(item => { item.readStatus = 'READ'; });
    return fulfillJson(route, true);
  });
  await page.route('**/notice/site/my/messages/*/delete', route => fulfillJson(route, true));
  await page.route('**/notice/site/my/messages/*/read', route => {
    const id = route.request().url().match(/messages\/([^/]+)\/read/)?.[1];
    const message = state.messages.find(item => item.id === id);
    if (message) {
      message.readStatus = 'READ';
    }
    return fulfillJson(route, true);
  });
  await page.route('**/notice/site/my/messages/*', route => {
    const id = route.request().url().split('/').pop();
    return fulfillJson(route, state.messages.find(item => item.id === id) || null);
  });
  await page.route('**/notice/site/my/messages**', route => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    if (path.endsWith('/read')) {
      const id = path.match(/messages\/([^/]+)\/read/)?.[1];
      const message = state.messages.find(item => item.id === id);
      if (message) {
        message.readStatus = 'READ';
      }
      return fulfillJson(route, true);
    }
    if (path.endsWith('/delete')) {
      return fulfillJson(route, true);
    }
    if (/\/notice\/site\/my\/messages\/[^/]+$/.test(path)) {
      const id = path.split('/').pop();
      return fulfillJson(route, state.messages.find(item => item.id === id) || null);
    }
    if (route.request().method() !== 'GET') {
      return fulfillJson(route, true);
    }
    const unreadOnly = url.searchParams.get('unreadOnly') === 'true';
    const list = unreadOnly ? state.messages.filter(item => item.readStatus === 'UNREAD') : state.messages;
    return fulfillJson(route, { list, total: list.length, page: 1, size: 10 });
  });
  await page.route('**/notice/announcements/detail**', route => {
    const id = new URL(route.request().url()).searchParams.get('id') || '';
    return fulfillJson(route, state.announcements.find(item => item.id === id) || null);
  });
  await page.route('**/notice/announcements/stats**', route => {
    const id = new URL(route.request().url()).searchParams.get('id') || '';
    return fulfillJson(route, state.announcements.find(item => item.id === id)?.stats || null);
  });
  await page.route('**/notice/announcements/publish**', async route => {
    const body = route.request().postDataJSON() as { id: string; targets?: AnnouncementTarget[] };
    state.publishRequests.push(body);
    const announcement = state.announcements.find(item => item.id === body.id);
    if (announcement) {
      announcement.status = 'PUBLISHED';
      announcement.publishTime = '2026-06-26 10:00:00';
      announcement.targets = body.targets?.length ? body.targets : announcement.targets;
      const count = targetRecipientCount(announcement.targets);
      announcement.stats = {
        recipientCount: count,
        readCount: 0,
        pendingConfirmCount: announcement.confirmRequired ? count : 0,
        confirmedCount: 0,
      };
      announcement.readStatus = 'UNREAD';
      announcement.confirmStatus = announcement.confirmRequired ? 'PENDING' : 'NOT_REQUIRED';
      if (announcement.syncMessageEnabled && !state.messages.some(item => item.bizId === announcement.id)) {
        state.messages.unshift({
          id: `msg-${announcement.id}`,
          title: `公告：${announcement.title}`,
          content: announcement.content,
          userId: '1',
          priority: 'NORMAL',
          readStatus: 'UNREAD',
          bizGroup: 'SYSTEM',
          bizName: '通知公告',
          bizType: 'notice.announcement.published',
          bizId: announcement.id,
          createTime: '2026-06-26 10:00:01',
        });
      }
    }
    await fulfillJson(route, true);
  });
  await page.route('**/notice/announcements/offline**', route => {
    const body = route.request().postDataJSON() as { id: string };
    const announcement = state.announcements.find(item => item.id === body.id);
    if (announcement) {
      announcement.status = 'OFFLINE';
      announcement.updatedAt = '2026-06-26 10:10:00';
    }
    return fulfillJson(route, true);
  });
  await page.route('**/notice/announcements**', route => {
    const requestUrl = new URL(route.request().url());
    const path = requestUrl.pathname;
    if (path.endsWith('/detail')) {
      const id = requestUrl.searchParams.get('id') || '';
      return fulfillJson(route, state.announcements.find(item => item.id === id) || null);
    }
    if (path.endsWith('/stats')) {
      const id = requestUrl.searchParams.get('id') || '';
      return fulfillJson(route, state.announcements.find(item => item.id === id)?.stats || null);
    }
    if (path.endsWith('/publish')) {
      const body = route.request().postDataJSON() as { id: string; targets?: AnnouncementTarget[] };
      state.publishRequests.push(body);
      const announcement = state.announcements.find(item => item.id === body.id);
      if (announcement) {
        announcement.status = 'PUBLISHED';
        announcement.publishTime = '2026-06-26 10:00:00';
        announcement.targets = body.targets?.length ? body.targets : announcement.targets;
        const count = targetRecipientCount(announcement.targets);
        announcement.stats = {
          recipientCount: count,
          readCount: 0,
          pendingConfirmCount: announcement.confirmRequired ? count : 0,
          confirmedCount: 0,
        };
        announcement.readStatus = 'UNREAD';
        announcement.confirmStatus = announcement.confirmRequired ? 'PENDING' : 'NOT_REQUIRED';
        if (announcement.syncMessageEnabled && !state.messages.some(item => item.bizId === announcement.id)) {
          state.messages.unshift({
            id: `msg-${announcement.id}`,
            title: `公告：${announcement.title}`,
            content: announcement.content,
            userId: '1',
            priority: 'NORMAL',
            readStatus: 'UNREAD',
            bizGroup: 'SYSTEM',
            bizName: '通知公告',
            bizType: 'notice.announcement.published',
            bizId: announcement.id,
            createTime: '2026-06-26 10:00:01',
          });
        }
      }
      return fulfillJson(route, true);
    }
    if (path.endsWith('/offline')) {
      const body = route.request().postDataJSON() as { id: string };
      const announcement = state.announcements.find(item => item.id === body.id);
      if (announcement) {
        announcement.status = 'OFFLINE';
        announcement.updatedAt = '2026-06-26 10:10:00';
      }
      return fulfillJson(route, true);
    }
    const method = route.request().method();
    if (method === 'POST') {
      const body = route.request().postDataJSON() as Partial<Announcement>;
      const announcement: Announcement = {
        id: `ann-${state.announcements.length + 200}`,
        title: body.title || '',
        content: body.content || '',
        status: 'DRAFT',
        pinned: Boolean(body.pinned),
        confirmRequired: Boolean(body.confirmRequired),
        syncMessageEnabled: body.syncMessageEnabled !== false,
        updatedAt: '2026-06-26 10:00:00',
        validStartTime: body.validStartTime,
        validEndTime: body.validEndTime,
        targets: body.targets || [],
        stats: { recipientCount: 0, readCount: 0, pendingConfirmCount: 0, confirmedCount: 0 },
      };
      state.announcements.unshift(announcement);
      return fulfillJson(route, announcement);
    }
    if (method === 'PUT') {
      const body = route.request().postDataJSON() as Partial<Announcement> & { id: string };
      const announcement = state.announcements.find(item => item.id === body.id);
      if (announcement) {
        Object.assign(announcement, {
          title: body.title,
          content: body.content,
          pinned: Boolean(body.pinned),
          confirmRequired: Boolean(body.confirmRequired),
          syncMessageEnabled: body.syncMessageEnabled !== false,
          targets: body.targets || [],
          validStartTime: body.validStartTime,
          validEndTime: body.validEndTime,
          updatedAt: '2026-06-26 10:05:00',
        });
      }
      return fulfillJson(route, announcement);
    }
    const status = requestUrl.searchParams.get('status');
    const keyword = requestUrl.searchParams.get('keyword') || '';
    const list = state.announcements.filter(item =>
      (!status || item.status === status) &&
      (!keyword || item.title.includes(keyword) || item.content.includes(keyword))
    );
    return fulfillJson(route, { list, total: list.length, page: 1, size: 50 });
  });
  await page.route('**/notice/site/my/announcements/detail**', route => {
    const id = new URL(route.request().url()).searchParams.get('id') || '';
    const announcement = state.announcements.find(item => item.id === id);
    if (announcement) {
      announcement.readStatus = 'READ';
      announcement.stats.readCount = Math.min(announcement.stats.recipientCount, announcement.stats.readCount + 1);
    }
    return fulfillJson(route, announcement || null);
  });
  await page.route('**/notice/site/my/announcements/confirm**', route => {
    const body = route.request().postDataJSON() as { id: string };
    const announcement = state.announcements.find(item => item.id === body.id);
    if (announcement) {
      announcement.readStatus = 'READ';
      announcement.confirmStatus = 'CONFIRMED';
      announcement.stats.pendingConfirmCount = Math.max(0, announcement.stats.pendingConfirmCount - 1);
      announcement.stats.confirmedCount += 1;
    }
    return fulfillJson(route, true);
  });
  await page.route('**/notice/site/my/announcements**', route => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    if (path.endsWith('/detail')) {
      const id = url.searchParams.get('id') || '';
      const announcement = state.announcements.find(item => item.id === id);
      if (announcement) {
        announcement.readStatus = 'READ';
        announcement.stats.readCount = Math.min(announcement.stats.recipientCount, announcement.stats.readCount + 1);
      }
      return fulfillJson(route, announcement || null);
    }
    if (path.endsWith('/confirm')) {
      const body = route.request().postDataJSON() as { id: string };
      const announcement = state.announcements.find(item => item.id === body.id);
      if (announcement) {
        announcement.readStatus = 'READ';
        announcement.confirmStatus = 'CONFIRMED';
        announcement.stats.pendingConfirmCount = Math.max(0, announcement.stats.pendingConfirmCount - 1);
        announcement.stats.confirmedCount += 1;
      }
      return fulfillJson(route, true);
    }
    const unreadOnly = url.searchParams.get('unreadOnly') === 'true';
    const pendingConfirmOnly = url.searchParams.get('pendingConfirmOnly') === 'true';
    const keyword = url.searchParams.get('keyword') || '';
    const list = visibleAnnouncements(state).filter(item =>
      (!unreadOnly || item.readStatus === 'UNREAD') &&
      (!pendingConfirmOnly || item.confirmStatus === 'PENDING') &&
      (!keyword || item.title.includes(keyword) || item.content.includes(keyword))
    );
    return fulfillJson(route, { list, total: list.length, page: 1, size: 50 });
  });
}

async function assertNoRuntimeErrors(page: Page, action: () => Promise<void>) {
  const consoleErrors: string[] = [];
  const failedRequests: string[] = [];
  page.on('console', message => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text());
    }
  });
  page.on('response', response => {
    if ((response.url().includes('/api/') || response.url().includes('/notice/')) && response.status() >= 400) {
      failedRequests.push(`${response.status()} ${response.url()}`);
    }
  });
  await action();
  expect(consoleErrors).toEqual([]);
  expect(failedRequests).toEqual([]);
}

async function chooseMixedTargets(page: Page) {
  const dialog = page.getByRole('dialog', { name: '新增公告' });
  await dialog.locator('.participant-add').click();
  const participantDialog = page.locator('.participant-dialog', { hasText: '选择对象' });
  await expect(participantDialog).toBeVisible();
  await participantDialog.locator('.participant-item', { hasText: '管理员' }).click();
  await participantDialog.getByRole('tab', { name: '部门范围' }).click();
  await participantDialog.locator('.el-tree-node', { hasText: '技术部' }).locator('.el-checkbox').first().click();
  await participantDialog.getByRole('tab', { name: '角色' }).click();
  await participantDialog.locator('.participant-item', { hasText: '系统管理员' }).click();
  await participantDialog.getByRole('button', { name: '确认' }).click();
  await expect(dialog.getByText('用户：')).toBeVisible();
  await expect(dialog.getByText('部门范围：')).toBeVisible();
  await expect(dialog.getByText('角色：')).toBeVisible();
}

async function expectMessage(page: Page, text: string) {
  await expect(page.locator('.el-message__content', { hasText: text }).last()).toBeVisible();
}

test.describe('通知中心公告 E2E', () => {
  test('覆盖公告管理、发布对象、用户公告确认和消息中心跳转', async ({ page }) => {
    const state = createState();
    mkdirSync(evidenceDir, { recursive: true });
    await setupRoutes(page, state);

    await assertNoRuntimeErrors(page, async () => {
      await login(page);

      await page.goto('/#/notice/announcement');
      await expect(page.getByRole('heading', { name: '公告管理' })).toBeVisible();
      await expect(page.locator('tr', { hasText: '端午值班安排' })).toContainText('已发布');
      await expect(page.locator('tr', { hasText: '端午值班安排' })).toContainText('1/0/0');

      await page.getByRole('button', { name: '新增公告' }).click();
      const createDialog = page.getByRole('dialog', { name: '新增公告' });
      await expect(createDialog).toBeVisible();
      await createDialog.getByRole('button', { name: '保存并发布' }).click();
      await expect(page.getByText('请输入公告标题')).toBeVisible();
      await expect(page.getByText('请选择发布对象')).toBeVisible();
      await createDialog.locator('.el-form-item', { hasText: '标题' }).locator('input').fill('系统升级通知');
      await createDialog.locator('.el-form-item', { hasText: '内容' }).locator('textarea').fill('今晚 22:00 至 23:00 系统升级，请提前保存数据。');
      await createDialog.getByText('需要确认').click();
      await chooseMixedTargets(page);
      await page.screenshot({ path: resolve(evidenceDir, 'announcement-admin-mixed-targets.png'), fullPage: true });
      await createDialog.getByRole('button', { name: '保存草稿' }).click();
      await expectMessage(page, '公告已保存');
      await expect(page.locator('tr', { hasText: '系统升级通知' })).toContainText('草稿');

      await page.locator('tr', { hasText: '系统升级通知' }).getByRole('button', { name: '编辑' }).click();
      const editDialog = page.getByRole('dialog', { name: '编辑公告' });
      await editDialog.locator('.el-form-item', { hasText: '内容' }).locator('textarea').fill('今晚 22:30 至 23:30 系统升级，请提前保存数据。');
      await editDialog.getByRole('button', { name: '保存草稿' }).click();
      await expectMessage(page, '公告已保存');

      await page.locator('tr', { hasText: '系统升级通知' }).getByRole('button', { name: '发布' }).click();
      await page.getByRole('dialog', { name: '发布公告' }).getByRole('button', { name: '确定' }).click();
      await expectMessage(page, '公告已发布');
      expect(state.publishRequests.at(-1)).toEqual({ id: 'ann-201' });
      await expect(page.locator('tr', { hasText: '系统升级通知' })).toContainText('已发布');
      await expect(page.locator('tr', { hasText: '系统升级通知' })).toContainText('2/0/0');

      await page.locator('tr', { hasText: '系统升级通知' }).getByRole('button', { name: '详情' }).click();
      const detailDialog = page.getByRole('dialog', { name: '公告详情' });
      await expect(detailDialog.getByText('接收人数')).toBeVisible();
      await expect(detailDialog.locator('.el-descriptions__cell', { hasText: '接收人数' })
        .locator('xpath=following-sibling::*[1]')).toHaveText('2');
      await detailDialog.locator('.el-dialog__headerbtn').click();

      await page.getByRole('button', { name: '新增公告' }).click();
      const allDialog = page.getByRole('dialog', { name: '新增公告' });
      await allDialog.locator('.el-form-item', { hasText: '标题' }).locator('input').fill('全员安全提醒');
      await allDialog.locator('.el-form-item', { hasText: '内容' }).locator('textarea').fill('请所有同事完成本周安全培训。');
      await allDialog.getByText('全员').click();
      await expect(allDialog.getByText('请选择用户、部门、角色或岗位')).toHaveCount(0);
      await allDialog.getByRole('button', { name: '保存并发布' }).click();
      await expectMessage(page, '公告已发布');
      expect(state.announcements[0].targets).toEqual([{ targetType: 'ALL', targetName: '全员' }]);
      await expect(page.locator('tr', { hasText: '全员安全提醒' })).toContainText('已发布');
      await expect(page.locator('tr', { hasText: '全员安全提醒' })).toContainText('3/0/0');

      await page.locator('tr', { hasText: '全员安全提醒' }).getByRole('button', { name: '下线' }).click();
      await page.getByRole('dialog', { name: '下线公告' }).getByRole('button', { name: '确定' }).click();
      await expectMessage(page, '公告已下线');
      await expect(page.locator('tr', { hasText: '全员安全提醒' })).toContainText('已下线');

      await page.goto('/#/message-center/announcement');
      await expect(page.getByRole('heading', { name: '公告' })).toBeVisible();
      await expect(page.locator('tr', { hasText: '端午值班安排' })).toContainText('待确认');
      await page.locator('tr', { hasText: '端午值班安排' }).getByRole('button', { name: '查看' }).click();
      const userDetailDialog = page.getByRole('dialog', { name: '公告详情' });
      await expect(userDetailDialog.getByText('端午期间请按排班表完成值班确认。')).toBeVisible();
      await userDetailDialog.getByRole('button', { name: '确认已读' }).click();
      await expectMessage(page, '已确认');
      await page.screenshot({ path: resolve(evidenceDir, 'announcement-user-confirmed.png'), fullPage: true });
      await userDetailDialog.getByRole('button', { name: '关闭', exact: true }).click();
      await expect(page.locator('tr', { hasText: '端午值班安排' })).toContainText('已确认');

      const userPageActions = page.locator('.notice-announcement-user-page .page-actions');
      await userPageActions.getByText('未读').click();
      await expect(page.locator('tr', { hasText: '端午值班安排' })).toHaveCount(0);
      await userPageActions.getByText('未读').click();
      await userPageActions.getByText('待确认').click();
      await expect(page.locator('tr', { hasText: '端午值班安排' })).toHaveCount(0);
      await userPageActions.getByText('待确认').click();

      await page.goto('/#/message-center/site-message');
      await expect(page.locator('.notice-site-message-page__title', { hasText: '我的消息' })).toBeVisible();
      await expect(page.locator('tr', { hasText: '公告：端午值班安排' })).toContainText('未读');
      await page.locator('tr', { hasText: '公告：端午值班安排' }).getByRole('button', { name: '详情' }).click();
      await page.waitForURL('**/#/message-center/announcement?id=ann-100');
      await expect(page.getByRole('dialog', { name: '公告详情' })).toBeVisible();
      await expect(page.getByRole('dialog', { name: '公告详情' }).getByText('端午值班安排')).toBeVisible();
      await page.screenshot({ path: resolve(evidenceDir, 'site-message-announcement-jump.png'), fullPage: true });
      expect(state.messages.find(item => item.id === 'msg-ann-100')?.readStatus).toBe('READ');
    });
  });
});
