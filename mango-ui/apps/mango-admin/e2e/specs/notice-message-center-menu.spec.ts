import { expect, test, type Page, type Route } from '@playwright/test';
import { collectBrowserDiagnostics } from '../support/browser-diagnostics';
import { openUnlabelledElementPlusCombobox, setElementPlusCheckbox } from '../support/element-plus';

interface NoticeTestState {
  preferences: Array<{ scopeType: string; scopeValue: string; channelType: string; enabled: boolean }>;
  reminderSetting: Record<string, unknown>;
}

function ok(data: unknown) {
  return JSON.stringify({ code: 200, success: true, data });
}

async function fulfillJson(route: Route, data: unknown) {
  await route.fulfill({ status: 200, contentType: 'application/json', body: ok(data) });
}

function createState(): NoticeTestState {
  return {
    preferences: [{ scopeType: 'BIZ_TYPE', scopeValue: 'LEAVE_APPROVED', channelType: 'EMAIL', enabled: false }],
    reminderSetting: {
      popupEnabled: false,
      popupPlacement: 'top-right',
      voiceEnabled: false,
      reminderMode: 'SOUND',
      voiceText: '您有新的系统消息，请及时查看',
      desktopNotificationEnabled: false,
    },
  };
}

async function setupRoutes(page: Page, state: NoticeTestState) {
  const messages = [
    {
      id: 'msg-001',
      title: '请假申请已通过',
      content: '你的请假申请已审批通过。',
      userId: '1',
      priority: 'NORMAL',
      readStatus: 'UNREAD',
      bizGroup: 'WORKFLOW',
      bizName: '请假审批',
      bizType: 'LEAVE_APPROVED',
      bizId: 'leave-001',
      category: 'APPROVAL',
      createTime: '2026-08-02 10:00:00',
    },
  ];
  const announcements = [
    {
      id: 'announcement-001',
      title: '服务维护公告',
      content: '本周日 02:00 至 04:00 进行维护。',
      status: 'PUBLISHED',
      pinned: true,
      confirmRequired: true,
      readStatus: 'UNREAD',
      confirmStatus: 'PENDING',
      publishTime: '2026-08-02 09:00:00',
    },
  ];

  await page.route('**://*/api/**', (route) => fulfillJson(route, []));
  await page.route('**://*/api/realtime/transports/negotiate**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        recommended: 'polling',
        order: ['polling'],
        transports: [{ type: 'polling', enabled: true }],
      }),
    }),
  );
  await page.route('**://*/api/realtime/transports/polling**', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
  );
  await page.route('**://*/api/system/tenant/login-options**', (route) =>
    fulfillJson(route, [{ tenantId: '1', tenantCode: 'mango', tenantName: '芒果集团' }]),
  );
  await page.route('**://*/api/auth/login-institutions**', (route) =>
    fulfillJson(route, [{ tenantId: '1', tenantCode: 'mango', tenantName: '芒果集团' }]),
  );
  await page.route('**://*/api/auth/login', (route) =>
    fulfillJson(route, {
      accessToken: 'notice-profile-e2e-token',
      tokenType: 'Bearer',
      expiresIn: '7200',
      refreshToken: 'notice-profile-e2e-refresh-token',
      userId: '1',
      memberId: '1',
      username: 'admin',
      nickname: '管理员',
      tenantId: '1',
      tenantCode: 'mango',
      tenantName: '芒果集团',
      appCode: 'internal-admin',
      roles: ['admin'],
      permissions: ['notice:site:view', 'notice:announcement-user:view', 'notice:receive-setting:view'],
    }),
  );
  await page.route('**://*/api/auth/info**', (route) =>
    fulfillJson(route, {
      userId: '1',
      username: 'admin',
      nickname: '管理员',
      tenantId: '1',
      tenantCode: 'mango',
      appCode: 'internal-admin',
      roles: ['admin'],
      permissions: ['notice:site:view', 'notice:announcement-user:view', 'notice:receive-setting:view'],
    }),
  );
  await page.route('**://*/api/authorization/menus/user**', (route) =>
    fulfillJson(route, [
      {
        menuId: '2900',
        parentId: '0',
        menuType: 1,
        menuName: '通知管理',
        menuCode: 'notice',
        path: '/notice',
        icon: 'Bell',
        redirect: '/notice/announcement',
        moduleCode: 'mango-notice',
        pageType: 'LOCAL_ROUTE',
        visible: 1,
        status: 1,
        children: [],
      },
    ]),
  );
  await page.route('**://*/api/identity/me/profile**', (route) =>
    fulfillJson(route, {
      userId: '1',
      username: 'admin',
      nickname: '管理员',
      verificationStatus: 'UNVERIFIED',
    }),
  );
  await page.route('**://*/api/notice/site/my/messages**', (route) => {
    const keyword = new URL(route.request().url()).searchParams.get('keyword') || '';
    return fulfillJson(route, {
      list: messages.filter((message) => !keyword || `${message.title}${message.content}`.includes(keyword)),
      total: messages.length,
      pageNum: 1,
      pageSize: 10,
    });
  });
  await page.route('**://*/api/notice/site/my/announcements**', (route) => {
    const pathname = new URL(route.request().url()).pathname;
    if (pathname.endsWith('/detail')) return fulfillJson(route, announcements[0]);
    if (pathname.endsWith('/confirm')) {
      announcements[0].confirmStatus = 'CONFIRMED';
      return fulfillJson(route, true);
    }
    return fulfillJson(route, { list: announcements, total: announcements.length, pageNum: 1, pageSize: 10 });
  });
  await page.route('**://*/api/domain/domains/enabled-tree**', (route) =>
    fulfillJson(route, [{ id: 'workflow', domainCode: 'WORKFLOW', domainName: '流程管理', children: [] }]),
  );
  await page.route('**://*/api/notice/business-types**', (route) =>
    fulfillJson(route, {
      list: [
        {
          id: 'business-001',
          bizType: 'LEAVE_APPROVED',
          bizName: '请假审批通过',
          bizGroup: 'WORKFLOW',
          enabled: true,
        },
      ],
      total: 1,
      pageNum: 1,
      pageSize: 200,
    }),
  );
  await page.route('**://*/api/notice/site/business-types**', (route) =>
    fulfillJson(route, {
      list: [
        {
          id: 'business-001',
          bizType: 'LEAVE_APPROVED',
          bizName: '请假审批通过',
          bizGroup: 'WORKFLOW',
          enabled: true,
        },
      ],
      total: 1,
      pageNum: 1,
      pageSize: 200,
    }),
  );
  await page.route('**://*/api/system/log/login/my/list**', (route) =>
    fulfillJson(route, {
      list: [
        {
          id: 'login-log-001',
          userId: '1',
          username: 'admin',
          loginType: 'PASSWORD',
          ip: '127.0.0.1',
          location: '本机',
          browser: 'Mozilla/5.0 Chrome/140.0.0.0 Safari/537.36',
          os: 'macOS',
          status: 1,
          msg: '成功',
          loginTime: '2026-08-02T13:20:10',
        },
      ],
      total: 1,
      page: 1,
      size: 10,
    }),
  );
  await page.route('**://*/api/notice/receive-preferences**', async (route) => {
    if (route.request().method() === 'GET') {
      await fulfillJson(route, state.preferences);
      return;
    }
    const payload = route.request().postDataJSON() as {
      scopeType: string;
      scopeValue: string;
      channelType: string;
      enabled: boolean;
    };
    const index = state.preferences.findIndex(
      (item) =>
        item.scopeType === payload.scopeType &&
        item.scopeValue === payload.scopeValue &&
        item.channelType === payload.channelType,
    );
    if (index >= 0) state.preferences[index] = payload;
    else state.preferences.push(payload);
    await fulfillJson(route, payload);
  });
  await page.route('**://*/api/system/personal-configs/value**', (route) =>
    fulfillJson(route, {
      groupCode: 'notice',
      bizType: 'client_reminder',
      configKey: 'reminder_setting',
      configValue: JSON.stringify(state.reminderSetting),
      valueType: 'JSON',
    }),
  );
  await page.route('**://*/api/system/personal-configs', async (route) => {
    const payload = route.request().postDataJSON() as { configValue: string };
    state.reminderSetting = JSON.parse(payload.configValue) as Record<string, unknown>;
    await fulfillJson(route, payload);
  });
}

async function login(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('请输入用户名').fill('admin');
  await page.getByPlaceholder('请输入密码').fill('admin123');
  await page.getByPlaceholder('请输入密码').blur();
  await openUnlabelledElementPlusCombobox(page);
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.getByRole('button', { name: '登录', exact: true }).click();
  await page.waitForURL('**/#/home');
}

test('@p0 @notice 个人中心消息中心提供三项入口与最新列表交互', async ({ page }, testInfo) => {
  const state = createState();
  const diagnostics = collectBrowserDiagnostics(page);
  await setupRoutes(page, state);
  await login(page);

  await page.goto('/#/profile?tab=notice-site-message');
  await expect(page.locator('[data-page="account.profile"]')).toBeVisible();
  const profileContainerBox = await page.locator('[data-page="account.profile"]').boundingBox();
  const layoutContentBox = await page.locator('.layout-main-content').boundingBox();
  expect(
    Math.abs(
      (profileContainerBox?.x ?? 0) +
        (profileContainerBox?.width ?? 0) / 2 -
        ((layoutContentBox?.x ?? 0) + (layoutContentBox?.width ?? 0) / 2),
    ),
  ).toBeLessThanOrEqual(1);
  await expect(page.getByText('消息中心', { exact: true })).toBeVisible();
  await expect(page.locator('[data-action="switch-notice-site-message"]')).toContainText('我的消息');
  await expect(page.locator('[data-action="switch-notice-announcement-user"]')).toContainText('系统公告');
  await expect(page.locator('[data-action="switch-notice-receive-setting"]')).toContainText('通知设置');
  await expect(page.getByText('基础设置', { exact: true })).toBeVisible();
  await expect(page.getByText('安全设置', { exact: true })).toBeVisible();
  await expect(page.getByText('账号关联', { exact: true })).toBeVisible();
  await expect(page.locator('[data-action="switch-login-log"]')).toContainText('登录日志');
  const profileSidebar = page.locator('[data-surface="profile.navigation"]');
  await expect(profileSidebar.getByText('个人中心', { exact: true })).toHaveCount(0);
  await expect(profileSidebar.getByText('账户设置', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: '消息中心', exact: true })).toHaveCount(0);
  const messageCenterItem = await page.locator('[data-action="switch-notice-site-message"]').boundingBox();
  const profileItem = await page.locator('[data-action="switch-profile"]').boundingBox();
  expect(messageCenterItem?.y).toBeLessThan(profileItem?.y ?? 0);
  const profileContent = page.locator('[data-state="notice-site-message"]');
  const profileContentBox = await profileContent.boundingBox();
  const messagesPageBox = await page.locator('[data-page="notice.site-message"]').boundingBox();
  expect(Math.abs((messagesPageBox?.x ?? 0) - (profileContentBox?.x ?? 0) - 16)).toBeLessThanOrEqual(1);
  await expect(profileContent.getByText('账户设置', { exact: true })).toHaveCount(0);
  await expect(profileContent.getByText('查看和处理发送给当前账号的消息。', { exact: true })).toHaveCount(0);

  const messagesPage = page.locator('[data-page="notice.site-message"]');
  await expect(messagesPage).toBeVisible();
  await expect(messagesPage.locator('.mango-search-panel[data-surface="search"]')).toBeVisible();
  await expect(messagesPage.locator('.mango-list-panel[data-surface="list"]')).toBeVisible();
  await expect(messagesPage.locator('[data-surface="notice.site-message.table"]')).toBeVisible();
  await expect(messagesPage.getByText('请假申请已通过', { exact: true })).toBeVisible();
  await messagesPage.getByPlaceholder('标题/内容').fill('请假');
  await messagesPage.getByRole('button', { name: '查询', exact: true }).click();
  await expect(messagesPage.getByText('请假申请已通过', { exact: true })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('profile-notice-site-message.png'), fullPage: true });

  await page.locator('[data-action="switch-notice-announcement-user"]').click();
  const announcementsPage = page.locator('[data-page="notice.announcement-user"]');
  await expect(announcementsPage).toBeVisible();
  await expect(announcementsPage.locator('.mango-search-panel[data-surface="search"]')).toBeVisible();
  await expect(announcementsPage.locator('.mango-list-panel[data-surface="list"]')).toBeVisible();
  await expect(announcementsPage.locator('[data-surface="notice.announcement-user.table"]')).toBeVisible();
  await announcementsPage.getByRole('button', { name: '查看', exact: true }).click();
  await expect(page.getByRole('dialog', { name: '公告详情' })).toContainText('服务维护公告');
  await page.getByRole('button', { name: '确认已读', exact: true }).click();
  await expect(page.getByRole('dialog', { name: '公告详情' })).toContainText('已确认');
  await page.getByRole('dialog', { name: '公告详情' }).getByRole('button', { name: '关闭', exact: true }).click();
  await expect(page.getByRole('dialog', { name: '公告详情' })).toBeHidden();
  await expect(page.getByRole('alert').filter({ hasText: '已确认' })).toBeHidden();
  await page.screenshot({ path: testInfo.outputPath('profile-notice-announcement.png'), fullPage: true });

  await page.locator('[data-action="switch-notice-receive-setting"]').click();
  const settingsPage = page.locator('[data-page="notice.receive-setting"]');
  await expect(settingsPage).toBeVisible();
  await expect(settingsPage.locator('[data-surface="notice.reminder-setting"]')).toBeVisible();
  const reminderControls = settingsPage.locator('[data-surface="notice.reminder-controls"]');
  const reminderSecondary = settingsPage.locator('[data-surface="notice.reminder-secondary"]');
  const reminderActions = settingsPage.locator('[data-surface="notice.reminder-actions"]');
  await expect(settingsPage.locator('[data-field="notice-voice-text"]')).toHaveCount(0);
  await settingsPage.locator('[data-field="notice-voice-enabled"]').click();
  const voiceText = settingsPage.locator('[data-field="notice-voice-text"]');
  await expect(voiceText).toBeVisible();
  await expect(voiceText).toBeDisabled();
  await settingsPage.locator('[data-field="notice-reminder-mode"]').click();
  await page.getByRole('option', { name: '语音播报', exact: true }).click();
  await expect(voiceText).toBeEnabled();
  await voiceText.fill('您有一条新的待办消息');
  const controlsBox = await reminderControls.boundingBox();
  const secondaryBox = await reminderSecondary.boundingBox();
  const actionsBox = await reminderActions.boundingBox();
  expect(secondaryBox?.y).toBeGreaterThan(controlsBox?.y ?? 0);
  expect(actionsBox?.y).toBeGreaterThan(controlsBox?.y ?? 0);
  expect(Math.abs((secondaryBox?.x ?? 0) - (controlsBox?.x ?? 0))).toBeLessThanOrEqual(1);
  expect(
    Math.abs((secondaryBox?.x ?? 0) + (secondaryBox?.width ?? 0) - ((controlsBox?.x ?? 0) + (controlsBox?.width ?? 0))),
  ).toBeLessThanOrEqual(1);
  await reminderActions.getByRole('button', { name: '保存设置', exact: true }).click();
  await expect.poll(() => state.reminderSetting.voiceText).toBe('您有一条新的待办消息');
  expect(state.reminderSetting.voiceEnabled).toBe(true);
  expect(state.reminderSetting.reminderMode).toBe('VOICE');
  await expect(settingsPage.getByText('流程管理', { exact: true })).toBeVisible();
  await expect(settingsPage).not.toContainText('流程管理（WORKFLOW）');
  await expect(settingsPage.getByText('请假审批通过', { exact: true })).toBeVisible();
  await expect(settingsPage.getByRole('checkbox', { name: '站内信', exact: true })).toBeChecked();
  await expect(settingsPage.getByRole('checkbox', { name: '短信', exact: true })).toBeChecked();
  await expect(settingsPage.getByRole('checkbox', { name: '企业微信', exact: true })).toBeChecked();
  await expect(settingsPage.getByRole('checkbox')).toHaveCount(4);
  const emailChannel = settingsPage.getByRole('checkbox', { name: '邮件', exact: true });
  await expect(emailChannel).not.toBeChecked();
  await setElementPlusCheckbox(settingsPage, '邮件', true);
  await expect(emailChannel).toBeChecked();
  expect(state.preferences).toContainEqual({
    scopeType: 'BIZ_TYPE',
    scopeValue: 'LEAVE_APPROVED',
    channelType: 'EMAIL',
    enabled: true,
  });
  await expect(settingsPage.getByText('绑定账号', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('alert').filter({ hasText: '提醒设置已保存' })).toBeHidden();
  await expect(page.getByRole('alert').filter({ hasText: '消息接收设置已保存' })).toBeHidden();
  await page.screenshot({ path: testInfo.outputPath('profile-notice-receive-setting.png'), fullPage: true });

  await page.locator('[data-action="switch-login-log"]').click();
  const loginLogPage = page.locator('[data-page="system.personal-login-log"]');
  await expect(loginLogPage).toBeVisible();
  await expect(loginLogPage.locator('.mango-search-panel[data-surface="search"]')).toBeVisible();
  await expect(loginLogPage.locator('.mango-list-panel[data-surface="list"]')).toBeVisible();
  await expect(loginLogPage.locator('[data-surface="system.personal-login-log.table"]')).toBeVisible();
  await expect(loginLogPage.getByText('2026-08-02 13:20:10', { exact: true })).toBeVisible();
  await expect(loginLogPage.getByText('127.0.0.1', { exact: true })).toBeVisible();
  await expect(loginLogPage.getByText('本机', { exact: true })).toBeVisible();
  await expect(loginLogPage.getByText('Mozilla/5.0 Chrome/140.0.0.0 Safari/537.36', { exact: true })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath('profile-login-log.png'), fullPage: true });

  await page.goto('/#/notice/receive-setting');
  await expect(page.locator('[data-page="notice.receive-setting"]')).toBeVisible();
  expect(diagnostics).toEqual([]);
});
