import { expect, test } from '@playwright/test';

async function login(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

function ok(data: unknown) {
  return JSON.stringify({ code: 200, success: true, data });
}

test.describe('通知中心 E2E', () => {
  test('登录后可以访问完整通知菜单、消息入口和系统消息主流程', async ({ page }) => {
    const businessTypes = [
      {
        id: '1',
        bizType: 'WORKFLOW_APPROVED',
        bizName: '审批通过通知',
        bizGroup: 'WORKFLOW',
        enabled: true,
        defaultPriority: 'NORMAL',
        paramsSchema: JSON.stringify({
          type: 'object',
          properties: {
            applyNo: { type: 'string', title: '申请单号', description: '审批单号' },
            occurredAt: { type: 'datetime', title: '发生时间', description: '业务发生时间' },
          },
          required: ['applyNo'],
        }),
      },
    ];
    const templates: Record<string, Array<Record<string, unknown>>> = {
      '1': [
        {
          id: '11',
          businessTypeId: '1',
          bizType: 'WORKFLOW_APPROVED',
          channelType: 'SITE',
          templateName: '系统消息模板',
          titleTemplate: '审批通过',
          contentTemplate: '审批单 {{applyNo}} 已通过',
          version: 1,
          versionStatus: 'ACTIVE',
          enabled: true,
        },
        {
          id: '12',
          businessTypeId: '1',
          bizType: 'WORKFLOW_APPROVED',
          channelType: 'SMS',
          templateName: '短信模板',
          titleTemplate: '审批通过',
          contentTemplate: '审批单 ${applyNo} 已通过',
          channelTemplateId: 'SMS_10001',
          variableMapping: JSON.stringify({ applyNo: 'thing1' }),
          version: 1,
          versionStatus: 'ACTIVE',
          enabled: true,
        },
      ],
    };
    const businessConfigVersions: Record<string, Array<Record<string, unknown>>> = {
      '1': [
        {
          id: '101',
          businessTypeId: '1',
          bizType: 'WORKFLOW_APPROVED',
          paramsSchema: businessTypes[0].paramsSchema,
          defaultPriority: 'NORMAL',
          idempotentStrategy: 'bizId',
          version: 1,
          versionStatus: 'ACTIVE',
        },
      ],
    };
    const messages = [
      {
        id: '1001',
        title: '测试系统消息',
        content: '这是一条系统消息内容',
        userId: '1',
        priority: 'NORMAL',
        readStatus: 'UNREAD',
        bizType: 'WORKFLOW_APPROVED',
        bizId: 'WF-1',
        createTime: '2026-05-25 17:10:00',
      },
    ];
    const realtimeMessage = {
      id: '2002',
      title: '实时系统消息',
      content: '这是一条实时推送消息',
      userId: '1',
      priority: 'NORMAL',
      readStatus: 'UNREAD',
      bizType: 'SYSTEM_NOTICE',
      bizId: 'RT-1',
      createTime: '2026-05-26 12:00:00',
    };
    let unreadCountOverride: number | undefined;
    const channelConfigs = [
      {
        id: '270501',
        channelType: 'SITE',
        providerCode: 'INTERNAL',
        configName: '默认系统消息通道',
        configJson: JSON.stringify({
          soundEnabled: true,
          soundText: '您有新的系统消息，请及时查看',
          popupEnabled: true,
          desktopNotificationEnabled: true,
        }),
        enabled: true,
        priority: 0,
        weight: 100,
      },
      { id: '1', channelType: 'EMAIL', providerCode: 'CUSTOM_SMTP', configName: '默认邮箱', enabled: true, priority: 0 },
    ];
    const sendRecords = [
      { id: '1', taskId: '1', recipientId: '1', bizType: 'WORKFLOW_APPROVED', bizId: 'WF-1', channelType: 'SITE', requestId: 'NR001', status: 'SUCCESS', renderedTitle: '测试系统消息', renderedContent: '这是一条系统消息内容', requestSnapshot: '{"bizType":"WORKFLOW_APPROVED","bizId":"WF-1","channelType":"SITE"}', responseSnapshot: '{"status":"SENT"}', providerMessageId: 'site-1001', retryCount: 0 },
      { id: '2', taskId: '2', recipientId: '1', bizType: 'WORKFLOW_APPROVED', bizId: 'WF-FAIL', channelType: 'SMS', requestId: 'NR_FAIL', status: 'FAILED', renderedTitle: '短信失败', renderedContent: '短信发送失败', requestSnapshot: '{"bizType":"WORKFLOW_APPROVED","bizId":"WF-FAIL","channelType":"SMS"}', responseSnapshot: '{"status":"FAILED"}', providerMessageId: 'sms-2001', failCode: 'PROVIDER_ERROR', failReason: '模板错误', retryCount: 3 },
    ];
    const identityUsers = [
      { userId: '1001', username: 'admin', nickname: '管理员', phone: '13800000000', email: 'admin@example.com', status: 1 },
      { userId: '1002', username: 'operator', nickname: '操作员', phone: '', email: 'operator@example.com', status: 1 },
    ];
    const orgTree = [
      { id: '2001', orgName: '芒果集团', pid: '0', orgStatus: '1', children: [{ id: '2002', orgName: '技术部', pid: '2001', orgStatus: '1', children: [] }] },
    ];
    const posts = [
      { id: '3001', postName: '产品经理', postCode: 'PM', postStatus: '1' },
      { id: '3002', postName: '研发工程师', postCode: 'DEV', postStatus: '1' },
    ];
    const roles = [
      { roleId: '4001', roleName: '系统管理员', roleCode: 'ADMIN', status: 1 },
      { roleId: '4002', roleName: '业务操作员', roleCode: 'OPERATOR', status: 1 },
    ];

    await page.addInitScript(() => {
      type NoticeTestWindow = Window & {
        __noticeAudioPlayCount: number;
        __noticeSpokenTexts: string[];
        __noticeDesktopNotifications: Array<{ title: string; body?: string }>;
      };
      const target = window as NoticeTestWindow;
      target.__noticeAudioPlayCount = 0;
      target.__noticeSpokenTexts = [];
      target.__noticeDesktopNotifications = [];

      class FakeAudio {
        constructor(public src: string) {}

        play() {
          target.__noticeAudioPlayCount += 1;
          return Promise.resolve();
        }
      }

      class FakeNotification {
        static permission = 'default';

        onclick?: () => void;

        constructor(public title: string, public options?: NotificationOptions) {
          target.__noticeDesktopNotifications.push({ title, body: options?.body });
        }

        static requestPermission() {
          FakeNotification.permission = 'granted';
          return Promise.resolve('granted');
        }
      }

      class FakeSpeechSynthesisUtterance {
        lang = '';
        rate = 0;
        pitch = 0;

        constructor(public text: string) {}
      }

      Object.defineProperty(window, 'Audio', { value: FakeAudio, configurable: true });
      Object.defineProperty(window, 'Notification', { value: FakeNotification, configurable: true });
      Object.defineProperty(window, 'SpeechSynthesisUtterance', { value: FakeSpeechSynthesisUtterance, configurable: true });
      Object.defineProperty(window, 'speechSynthesis', {
        value: {
          cancel: () => undefined,
          speak: (utterance: FakeSpeechSynthesisUtterance) => target.__noticeSpokenTexts.push(utterance.text),
        },
        configurable: true,
      });
    });

    await page.route('**/api/system/tenant/login-options**', async (route) => {
 await route.fulfill({ status:200, contentType: 'application/json', body: ok([{ tenantId: '1', tenantCode: 'mango', tenantName: '芒果集团' }]) });
 });
 await page.route('**/api/auth/login-institutions', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok([{ tenantId: '1', tenantCode: 'mango', tenantName: '芒果集团' }]) });
    });
    await page.route('**/api/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: ok({
          token: 'notice-e2e-token',
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
              'notice:business:view', 'notice:business:create', 'notice:business:edit', 'notice:business:publish', 'notice:business:delete',
              'notice:channel:view', 'notice:channel:create', 'notice:channel:delete', 'notice:task:create', 'notice:task:view', 'notice:record:view',
              'notice:site:view', 'notice:site:edit', 'notice:site:delete', 'notice:setting:view',
            ],
          },
        }),
      });
    });

    await page.route('**/api/authorization/menus/user**', async (route) => {
      const child = (menuId: string, menuName: string, path: string, component: string) => ({
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
        visible: 1,
        status: 1,
        children: [],
      });
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: ok([
          {
            menuId: '2900',
            parentId: '0',
            menuType: 1,
            menuName: '通知中心',
            menuCode: 'notice',
            path: '/notice',
            icon: 'Bell',
            redirect: '/notice/message-definition',
            moduleCode: 'mango-notice',
            pageType: 'LOCAL_ROUTE',
            visible: 1,
            status: 1,
            children: [
              child('2901', '消息配置', '/notice/message-definition', '@/views/notice/message-definition/index.vue'),
              child('2902', '发送任务', '/notice/send-message', '@/views/notice/send-message/index.vue'),
              child('2903', '渠道配置', '/notice/channel', '@/views/notice/channel/index.vue'),
              child('2904', '接收设置', '/notice/receive-setting', '@/views/notice/receive-setting/index.vue'),
              child('2905', '发送记录', '/notice/record', '@/views/notice/record/index.vue'),
              child('2906', '失败重试', '/notice/retry', '@/views/notice/retry/index.vue'),
              child('2907', '系统消息', '/notice/site-message', '@/views/notice/site-message/index.vue'),
            ],
          },
        ]),
      });
    });

    await page.route('**/api/notice/business-types**', async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      if (url.pathname.includes('/channel-templates')) {
        const parts = url.pathname.split('/');
        const businessTypeId = parts[parts.indexOf('business-types') + 1];
        const channelType = parts[parts.indexOf('channel-templates') + 1];
        if (request.method() === 'GET') {
          await route.fulfill({ status: 200, contentType: 'application/json', body: ok(templates[businessTypeId] || []) });
          return;
        }
        if (request.method() === 'PUT') {
          const body = request.postDataJSON();
          const saved = {
            id: `${businessTypeId}-${channelType}-${Date.now()}`,
            businessTypeId,
            bizType: businessTypes.find(item => item.id === businessTypeId)?.bizType,
            version: 2,
            versionStatus: 'DRAFT',
            enabled: true,
            ...body,
          };
          templates[businessTypeId] = [...(templates[businessTypeId] || []), saved];
          await route.fulfill({ status: 200, contentType: 'application/json', body: ok(saved) });
          return;
        }
        if (url.pathname.endsWith('/publish')) {
          await route.fulfill({ status: 200, contentType: 'application/json', body: ok(true) });
          return;
        }
        await route.fulfill({ status: 200, contentType: 'application/json', body: ok(true) });
        return;
      }
      if (url.pathname.includes('/config-versions')) {
        const parts = url.pathname.split('/');
        const businessTypeId = parts[parts.indexOf('business-types') + 1];
        await route.fulfill({ status: 200, contentType: 'application/json', body: ok(businessConfigVersions[businessTypeId] || []) });
        return;
      }
      if (url.pathname.includes('/config-draft')) {
        const parts = url.pathname.split('/');
        const businessTypeId = parts[parts.indexOf('business-types') + 1];
        if (request.method() === 'PUT') {
          const body = request.postDataJSON();
          const existing = businessConfigVersions[businessTypeId] || [];
          const draft = existing.find(item => item.versionStatus === 'DRAFT') || {
            id: `${businessTypeId}-config-draft`,
            businessTypeId,
            bizType: businessTypes.find(item => item.id === businessTypeId)?.bizType,
            version: existing.length + 1,
            versionStatus: 'DRAFT',
          };
          Object.assign(draft, body, { versionStatus: 'DRAFT' });
          businessConfigVersions[businessTypeId] = [...existing.filter(item => item.id !== draft.id), draft];
          await route.fulfill({ status: 200, contentType: 'application/json', body: ok(draft) });
          return;
        }
        if (request.method() === 'POST' && url.pathname.endsWith('/publish')) {
          const versions = businessConfigVersions[businessTypeId] || [];
          versions.forEach((item) => {
            if (item.versionStatus === 'ACTIVE') item.versionStatus = 'HISTORY';
            if (item.versionStatus === 'DRAFT') {
              item.versionStatus = 'ACTIVE';
              const business = businessTypes.find(type => type.id === businessTypeId);
              if (business) {
                business.paramsSchema = String(item.paramsSchema || '');
                business.defaultPriority = String(item.defaultPriority || 'NORMAL');
                business.idempotentStrategy = String(item.idempotentStrategy || '');
              }
            }
          });
          await route.fulfill({ status: 200, contentType: 'application/json', body: ok(true) });
          return;
        }
      }
      if (request.method() === 'GET') {
        await route.fulfill({ status: 200, contentType: 'application/json', body: ok({ list: businessTypes, total: businessTypes.length, page: 1, size: 10 }) });
        return;
      }
      if (request.method() === 'POST') {
        const body = request.postDataJSON();
        const created = {
          id: String(businessTypes.length + 1),
          enabled: true,
          defaultPriority: 'NORMAL',
          ...body,
        };
        businessTypes.unshift(created);
        await route.fulfill({ status: 200, contentType: 'application/json', body: ok(created) });
        return;
      }
      if (request.method() === 'PUT') {
        const body = request.postDataJSON();
        const id = url.pathname.split('/').pop();
        const index = businessTypes.findIndex(item => item.id === id);
        if (index >= 0) {
          businessTypes[index] = { ...businessTypes[index], ...body };
        }
        await route.fulfill({ status: 200, contentType: 'application/json', body: ok(businessTypes[index]) });
        return;
      }
      if (request.method() === 'DELETE') {
        const id = url.pathname.split('/').pop();
        const index = businessTypes.findIndex(item => item.id === id);
        if (index >= 0) businessTypes.splice(index, 1);
        delete businessConfigVersions[String(id)];
        delete templates[String(id)];
        await route.fulfill({ status: 200, contentType: 'application/json', body: ok(true) });
        return;
      }
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok(true) });
    });
    await page.route('**/api/notice/channels**', async (route) => {
      const request = route.request();
      if (request.method() === 'POST') {
        const body = request.postDataJSON();
        const created = { id: String(channelConfigs.length + 1), ...body };
        channelConfigs.unshift(created);
        await route.fulfill({ status: 200, contentType: 'application/json', body: ok(created) });
        return;
      }
      if (request.method() === 'DELETE') {
        const url = new URL(request.url());
        const id = url.searchParams.get('id');
        const index = channelConfigs.findIndex(item => item.id === id);
        if (index >= 0) channelConfigs.splice(index, 1);
        await route.fulfill({ status: 200, contentType: 'application/json', body: ok(true) });
        return;
      }
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok({ list: channelConfigs, total: channelConfigs.length, page: 1, size: 10 }) });
    });
    await page.route('**/api/notice/tasks**', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok({ list: [{
        id: '1',
        taskCode: 'NT001',
        bizType: 'WORKFLOW_APPROVED',
        bizGroup: 'WORKFLOW',
        bizName: '审批通过通知',
        paramsSnapshot: '{"applyNo":"APPLY-001"}',
        recipientTargetsSnapshot: '[{"targetType":"USER","targetId":"1001","targetName":"管理员"}]',
        channelTypes: 'SITE',
        status: 'SUCCESS',
        totalCount: 1,
        successCount: 1,
        failCount: 0,
      }], total: 1, page: 1, size: 10 }) });
    });
    await page.route('**/api/notice/records**', async (route) => {
      const url = new URL(route.request().url());
      const status = url.searchParams.get('status');
      const list = status ? sendRecords.filter(item => item.status === status) : sendRecords;
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok({ list, total: list.length, page: 1, size: 10 }) });
    });
    await page.route('**/api/notice/send', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok({ successCount: 0, failCount: 0 }) });
    });
    await page.route('**/api/identity/users/page**', async (route) => {
      const url = new URL(route.request().url());
      const keyword = url.searchParams.get('keyword') || '';
      const list = keyword
        ? identityUsers.filter(item => [item.username, item.nickname, item.phone, item.email].some(value => value.includes(keyword)))
        : identityUsers;
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok({ list, total: list.length, page: 1, size: 20 }) });
    });
    await page.route('**/api/org/tree**', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok(orgTree) });
    });
    await page.route('**/api/post/page**', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok({ list: posts, total: posts.length, page: 1, size: 20 }) });
    });
    await page.route('**/api/authorization/roles', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok(roles) });
    });
    await page.route('**/api/notice/settings**', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok({ soundEnabled: true, desktopEnabled: true, maxRetry: 3, retentionDays: 180 }) });
    });
    await page.route('**/api/notice/site/messages', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok({ successCount: 1, failCount: 0 }) });
    });
    await page.route('**/api/notice/site/my/unread-count**', async (route) => {
      const count = unreadCountOverride ?? messages.filter(item => item.readStatus === 'UNREAD').length;
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok({ count }) });
    });
    await page.route('**/api/notice/site/my/messages**', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({ status: 200, contentType: 'application/json', body: ok({ list: messages, total: messages.length, page: 1, size: 10 }) });
        return;
      }
      await route.continue();
    });
    await page.route('**/api/notice/site/my/messages/1001', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok(messages[0]) });
    });
    await page.route('**/api/notice/site/my/messages/2002', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok(realtimeMessage) });
    });
    await page.route('**/api/notice/site/my/messages/2002/read', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok(true) });
    });
    await page.route('**/api/notice/site/my/messages/1001/read', async (route) => {
      messages[0].readStatus = 'READ';
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok(true) });
    });
    await page.route('**/api/notice/site/my/messages/read-batch', async (route) => {
      const body = route.request().postDataJSON();
      messages.forEach((item) => {
        if (body.ids.includes(item.id)) {
          item.readStatus = 'READ';
        }
      });
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok(true) });
    });
    await page.route('**/api/notice/site/my/messages/1001/delete', async (route) => {
      messages.splice(0, 1);
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok(true) });
    });
    await page.route('**/api/notice/site/my/messages/read-all', async (route) => {
      messages.forEach(item => { item.readStatus = 'READ'; });
      await route.fulfill({ status: 200, contentType: 'application/json', body: ok(true) });
    });

    await login(page);
    const noticeBell = page.locator('.notice-bell');
    await expect(page.getByLabel('消息提醒')).toBeVisible();
    await expect(noticeBell.locator('.el-badge__content')).toHaveText('1');
    unreadCountOverride = 2;
    await page.evaluate(() => {
      window.dispatchEvent(new CustomEvent('mango-notice-message', {
        detail: { messageId: '2002', title: '实时系统消息' },
      }));
    });
    await expect(noticeBell.locator('.el-badge__content')).toHaveText('2');
    await expect(page.getByText('您有新消息了')).toBeVisible();
    await expect(page.getByText('实时系统消息')).toBeVisible();
    await expect.poll(async () => page.evaluate(() => (window as Window & { __noticeSpokenTexts?: string[] }).__noticeSpokenTexts?.[0] || '')).toBe('您有新的系统消息，请及时查看');
    await expect.poll(async () => page.evaluate(() => (window as Window & { __noticeDesktopNotifications?: Array<unknown> }).__noticeDesktopNotifications?.length ?? 0)).toBe(1);
    unreadCountOverride = undefined;
    await page.locator('.el-notification').click();
    const realtimeDetailDialog = page.getByRole('dialog', { name: '系统消息详情' });
    await expect(realtimeDetailDialog).toBeVisible();
    await expect(realtimeDetailDialog.getByText('这是一条实时推送消息')).toBeVisible();
    await expect(realtimeDetailDialog.getByText('RT-1')).toBeVisible();
    await realtimeDetailDialog.locator('.el-dialog__headerbtn').click();
    await expect(realtimeDetailDialog).toBeHidden();
    await page.getByLabel('消息提醒').click();
    await expect(page.getByText('测试系统消息')).toBeVisible();
    await page.keyboard.press('Escape');

    await page.getByRole('button', { name: '通知中心' }).click();
    await page.getByRole('menuitem', { name: '消息配置' }).click();
    await expect(page.getByText('WORKFLOW_APPROVED', { exact: true })).toBeVisible();

    await page.locator('.definition-main .list-toolbar').getByRole('button', { name: '新增' }).click();
    const maintainPage = page.locator('.notice-business-config-page');
    await expect(maintainPage.getByRole('heading', { name: '消息配置维护' })).toBeVisible();
    await expect(maintainPage).not.toContainText('默认优先级');
    await expect(maintainPage).not.toContainText('幂等策略');
    await maintainPage.locator('.el-form-item', { hasText: '业务域' }).locator('.el-select__wrapper').click();
    await page.locator('.el-select-dropdown__item:visible', { hasText: 'WORKFLOW' }).click();
    await maintainPage.locator('input[placeholder="order.shipped"]').fill('order.shipped');
    await maintainPage.locator('input[placeholder="出函成功"]').fill('订单发货通知');
    await maintainPage.locator('textarea[placeholder="用于说明该消息配置的业务场景"]').fill('订单发货后发送系统消息和短信');
    await maintainPage.getByRole('button', { name: '保存' }).click();
    await expect(page.getByText('order.shipped', { exact: true })).toBeVisible();
    await maintainPage.getByRole('button', { name: '返回' }).click();
    await expect(page.locator('tr', { hasText: 'order.shipped' })).toBeVisible();

    await page.locator('tr', { hasText: 'order.shipped' }).getByRole('button', { name: '编辑' }).click();
    await expect(maintainPage.getByRole('heading', { name: '消息配置维护' })).toBeVisible();
    const bizTypeInput = maintainPage.locator('input[disabled]').first();
    await expect(bizTypeInput).toHaveValue('order.shipped');
    await expect(bizTypeInput).toBeDisabled();

    const configPage = maintainPage;
    await expect(configPage.getByText('基础信息')).toBeVisible();
    await expect(configPage.getByText('order.shipped', { exact: true })).toBeVisible();
    await expect(configPage.getByText('参数设置')).toBeVisible();
    await expect(configPage.getByText('消息类型', { exact: true })).toBeVisible();
    await expect(configPage.getByRole('tab', { name: '系统消息' })).toBeVisible();
    await configPage.locator('input[placeholder="orderNo"]').fill('orderNo');
    await configPage.locator('input[placeholder="订单号"]').fill('订单号');
    await configPage.locator('.message-type-tabs .el-tabs__item', { hasText: '短信' }).click();
    await expect(configPage.getByRole('tab', { name: '短信' })).toBeVisible();
    await configPage.locator('.template-enabled-item .el-switch').click();
    await expect(configPage.locator('.template-enabled-item').getByText('启用', { exact: true })).toBeVisible();
    await configPage.locator('textarea[placeholder="请输入短信内容，例如：订单 {{orderNo}} 已发货"]').fill('订单 {{orderNo}} 已发货');
    await expect(page.getByText('订单号：{{orderNo}}')).toBeVisible();
    await page.getByRole('button', { name: '保存', exact: true }).click();
    await page.getByRole('button', { name: '发布新版本' }).click();
    await expect(page.getByText('已发布新版本')).toBeVisible();
    await page.getByRole('button', { name: '返回' }).click();
    await expect(page.getByText('order.shipped', { exact: true })).toBeVisible();
    await page.locator('tr', { hasText: 'order.shipped' }).getByRole('button', { name: '删除' }).click();
    await page.locator('.el-message-box').getByRole('button', { name: '取消' }).click();
    await expect(page.getByText('系统错误，请刷新页面')).toHaveCount(0);
    await expect(page.getByText('order.shipped', { exact: true })).toBeVisible();
    const deleteBusiness = page.waitForRequest(request => request.method() === 'DELETE' && request.url().includes('/api/notice/business-types/'));
    await page.locator('tr', { hasText: 'order.shipped' }).getByRole('button', { name: '删除' }).click();
    await page.locator('.el-message-box').getByRole('button', { name: '删除', exact: true }).click();
    const deleteBusinessRequest = await deleteBusiness;
    expect(deleteBusinessRequest.url()).toContain('/api/notice/business-types/2');
    await expect(page.getByText('order.shipped', { exact: true })).toHaveCount(0);

    await page.getByRole('menuitem', { name: '发送任务' }).click();
    await expect(page.getByRole('heading', { name: '发送任务' })).toBeVisible();
    await expect(page.locator('tr', { hasText: 'NT001' })).toBeVisible();
    await expect(page.locator('tr', { hasText: 'NT001' })).toContainText('WORKFLOW');
    await expect(page.locator('tr', { hasText: 'NT001' })).toContainText('审批通过通知');
    await expect(page.locator('tr', { hasText: 'NT001' })).not.toContainText('WORKFLOW_APPROVED');
    await expect(page.locator('tr', { hasText: 'NT001' })).not.toContainText('"applyNo":"APPLY-001"');
    await expect(page.locator('tr', { hasText: 'NT001' })).not.toContainText('"targetType":"USER"');
    await page.locator('tr', { hasText: 'NT001' }).getByRole('button', { name: '详情' }).click();
    const taskDetailDialog = page.getByRole('dialog', { name: '发送详情' });
    await expect(taskDetailDialog.getByText('WORKFLOW_APPROVED')).toBeVisible();
    await expect(taskDetailDialog.getByText('"targetType":"USER"')).toBeVisible();
    await taskDetailDialog.getByRole('tab', { name: '参数 JSON' }).click();
    await expect(taskDetailDialog.getByText('"applyNo":"APPLY-001"')).toBeVisible();
    await taskDetailDialog.locator('.el-dialog__headerbtn').click();
    await page.getByRole('button', { name: '新增任务' }).click();
    const sendDialog = page.getByRole('dialog', { name: '新增任务' });
    await expect(sendDialog).toBeVisible();
    await sendDialog.locator('.participant-add').click();
    const participantDialog = page.locator('.participant-dialog', { hasText: '选择对象' });
    await expect(participantDialog).toBeVisible();
    await participantDialog.locator('.participant-item', { hasText: '管理员' }).click();
    await participantDialog.getByRole('tab', { name: '角色' }).click();
    await participantDialog.locator('.participant-item', { hasText: '系统管理员' }).click();
    await participantDialog.getByRole('button', { name: '确认' }).click();
    await expect(sendDialog.getByText('用户：')).toBeVisible();
    await expect(sendDialog.getByText('角色：')).toBeVisible();
    await sendDialog.locator('.el-form-item', { hasText: '消息类型' }).locator('.el-select__wrapper').click();
    await page.locator('.el-select-dropdown__item:visible', { hasText: '审批通过通知' }).click();
    await expect(sendDialog.getByLabel('申请单号')).toBeVisible();
    await expect(sendDialog.getByLabel('发生时间')).toBeVisible();
    await sendDialog.getByLabel('申请单号').fill('APPLY-20260527');
    await sendDialog.getByLabel('发生时间').fill('2026-05-27 10:30:00');
    await sendDialog.getByRole('button', { name: '暂存' }).click();
    await expect(page.getByText('已暂存')).toBeVisible();
    const sendNoticeRequest = page.waitForRequest(request => request.method() === 'POST' && request.url().includes('/api/notice/send'));
    await sendDialog.getByRole('button', { name: '发送', exact: true }).click();
    const sendNoticeBody = (await sendNoticeRequest).postDataJSON();
    expect(sendNoticeBody.bizType).toBe('WORKFLOW_APPROVED');
    expect(sendNoticeBody.params).toEqual({ applyNo: 'APPLY-20260527', occurredAt: '2026-05-27 10:30:00' });
    expect(sendNoticeBody.recipientTargets).toEqual([
      { targetType: 'USER', targetId: '1001', targetName: '管理员' },
      { targetType: 'ROLE', targetId: '4001', targetName: '系统管理员 / ADMIN' },
    ]);
    expect(sendNoticeBody).not.toHaveProperty('bizId');
    expect(sendNoticeBody).not.toHaveProperty('userIds');
    expect(sendNoticeBody).not.toHaveProperty('recipients');
    expect(sendNoticeBody).not.toHaveProperty('channelTypes');
    await expect(sendDialog).toBeHidden();

    await page.getByRole('menuitem', { name: '渠道配置' }).click();
    await expect(page.getByText('默认系统消息通道')).toBeVisible();
    await expect(page.locator('tr', { hasText: '默认系统消息通道' }).getByRole('button', { name: '删除' })).toBeDisabled();
    await page.locator('tr', { hasText: '默认系统消息通道' }).getByRole('button', { name: '详情' }).click();
    const siteChannelDetailDialog = page.getByRole('dialog', { name: '渠道详情' });
    await expect(siteChannelDetailDialog.getByText('基础信息')).toBeVisible();
    await expect(siteChannelDetailDialog.getByText('渠道配置')).toBeVisible();
    await expect(siteChannelDetailDialog.getByText('渠道参数')).toBeVisible();
    await expect(siteChannelDetailDialog.getByRole('tab', { name: '表单形式' })).toBeVisible();
    await expect(siteChannelDetailDialog.getByRole('tab', { name: 'JSON 形式' })).toBeVisible();
    await expect(siteChannelDetailDialog.getByLabel('播报内容')).toHaveValue('您有新的系统消息，请及时查看');
    await siteChannelDetailDialog.getByRole('button', { name: '关闭' }).click();
    await page.locator('tr', { hasText: '默认系统消息通道' }).getByRole('button', { name: '编辑' }).click();
    const siteChannelDialog = page.getByRole('dialog', { name: '编辑渠道' });
    await expect(siteChannelDialog.getByLabel('播报内容')).toHaveValue('您有新的系统消息，请及时查看');
    await siteChannelDialog.getByRole('button', { name: '取消' }).click();
    await expect(page.getByText('默认邮箱')).toBeVisible();
    const saveSmsChannel = page.waitForRequest(request => request.method() === 'POST' && request.url().includes('/api/notice/channels'));
    await page.getByRole('button', { name: '新增' }).click();
    const channelDialog = page.getByRole('dialog', { name: '新增渠道' });
    await expect(channelDialog).toBeVisible();
    await channelDialog.locator('.el-select__wrapper').first().click();
    await page.locator('.el-select-dropdown__item:visible', { hasText: '短信' }).click();
    await channelDialog.getByLabel('通道名称').fill('阿里云短信');
    await channelDialog.getByLabel('AccessKey').fill('ak-test');
    await channelDialog.getByLabel('Secret').fill('sk-test');
    await channelDialog.getByLabel('签名').fill('芒果云');
    await channelDialog.getByLabel('接入地址').fill('dysmsapi.aliyuncs.com');
    await channelDialog.getByLabel('通知地址').fill('https://example.com/sms/callback');
    await channelDialog.getByRole('button', { name: '保存' }).click();
    const smsChannelRequest = await saveSmsChannel;
    const smsChannelBody = smsChannelRequest.postDataJSON();
    const smsChannelId = String(channelConfigs.find(item => item.configName === '阿里云短信')?.id || '');
    expect(smsChannelBody.channelType).toBe('SMS');
    expect(smsChannelBody.providerCode).toBe('ALIYUN_SMS');
    expect(JSON.parse(smsChannelBody.configJson)).toEqual({
      accessKeyId: 'ak-test',
      accessKeySecret: 'sk-test',
      signName: '芒果云',
      templatePlatform: '阿里云短信',
      endpoint: 'dysmsapi.aliyuncs.com',
      callbackUrl: 'https://example.com/sms/callback',
    });
    await expect(channelDialog).toBeHidden();
    await expect(page.locator('tr', { hasText: '阿里云短信' }).getByText('短信', { exact: true })).toBeVisible();
    const deleteSmsChannel = page.waitForRequest(request => request.method() === 'DELETE' && request.url().includes('/api/notice/channels'));
    await page.locator('tr', { hasText: '阿里云短信' }).getByRole('button', { name: '删除' }).click();
    await page.locator('.el-message-box').getByRole('button', { name: '删除', exact: true }).click();
    const deleteSmsChannelRequest = await deleteSmsChannel;
    expect(new URL(deleteSmsChannelRequest.url()).searchParams.get('id')).toBe(smsChannelId);
    await expect(page.getByText('阿里云短信')).toHaveCount(0);
    const saveEmailChannel = page.waitForRequest(request => request.method() === 'POST' && request.url().includes('/api/notice/channels'));
    await page.getByRole('button', { name: '新增' }).click();
    const emailChannelDialog = page.getByRole('dialog', { name: '新增渠道' });
    await expect(emailChannelDialog).toBeVisible();
    await emailChannelDialog.getByLabel('通道名称').fill('默认邮件账号');
    await emailChannelDialog.getByLabel('SMTP').fill('smtp.example.com');
    await emailChannelDialog.getByLabel('端口').fill('465');
    await emailChannelDialog.getByLabel('账号').fill('notice@example.com');
    await emailChannelDialog.getByLabel('密码').fill('secret');
    await emailChannelDialog.getByLabel('发件人').fill('notice@example.com');
    await emailChannelDialog.getByRole('button', { name: '保存' }).click();
    const emailChannelRequest = await saveEmailChannel;
    const emailChannelBody = emailChannelRequest.postDataJSON();
    expect(emailChannelBody.channelType).toBe('EMAIL');
    expect(JSON.parse(emailChannelBody.configJson)).toEqual({
      host: 'smtp.example.com',
      port: 465,
      username: 'notice@example.com',
      password: 'secret',
      from: 'notice@example.com',
      ssl: true,
    });
    await expect(emailChannelDialog).toBeHidden();
    await expect(page.getByText('默认邮件账号')).toBeVisible();
    await page.getByRole('button', { name: '新增' }).click();
    const aliyunEmailDialog = page.getByRole('dialog', { name: '新增渠道' });
    await expect(aliyunEmailDialog).toBeVisible();
    await aliyunEmailDialog.locator('.el-form-item', { hasText: '接入平台' }).locator('.el-select__wrapper').click();
    await page.locator('.el-select-dropdown__item:visible', { hasText: '阿里云邮件推送' }).click();
    await expect(aliyunEmailDialog.getByLabel('AccessKey')).toBeVisible();
    await expect(aliyunEmailDialog.getByLabel('区域')).toBeVisible();
    await expect(aliyunEmailDialog.getByLabel('Endpoint')).toBeVisible();
    await expect(aliyunEmailDialog.getByLabel('SMTP')).toHaveCount(0);
    await aliyunEmailDialog.getByRole('button', { name: '取消' }).click();
    await page.getByRole('menuitem', { name: '失败重试' }).click();
    const retryRow = page.locator('tr', { hasText: 'WF-FAIL' });
    await expect(retryRow.getByText('PROVIDER_ERROR')).toBeVisible();
    await expect(retryRow.getByText('模板错误')).toBeVisible();
    await page.getByRole('menuitem', { name: '发送记录' }).click();
    const recordRow = page.locator('tr', { hasText: 'NR001' });
    await expect(recordRow.getByText('SITE', { exact: true })).toBeVisible();
    await expect(recordRow.getByText('WORKFLOW_APPROVED', { exact: true })).toBeVisible();
    await expect(recordRow.getByText('WF-1', { exact: true })).toBeVisible();
    await expect(recordRow.getByText('NR001')).toBeVisible();
    await expect(recordRow.getByText('{"status":"SENT"}')).toBeVisible();
    await page.getByRole('menuitem', { name: '接收设置' }).click();
    await expect(page.getByText('提示音')).toBeVisible();

    await page.goto('/#/notice/site-message');
    await expect(page.locator('.notice-site-message-page__header').getByText('系统消息')).toBeVisible();
    await expect(page.getByLabel('我的消息').getByText('测试系统消息')).toBeVisible();
    await expect(page.getByLabel('我的消息').getByText('WF-1', { exact: true })).toBeVisible();
    await page.getByRole('tab', { name: '发送系统消息' }).click();
    const sendSiteRequest = page.waitForRequest(request => request.method() === 'POST' && request.url().includes('/api/notice/site/messages'));
    await page.getByLabel('接收用户ID').fill('1001');
    await page.getByLabel('业务类型').fill('SYSTEM_NOTICE');
    await page.getByLabel('业务ID').fill('BIZ-1001');
    await page.getByLabel('标题').fill('后台系统消息');
    await page.getByLabel('内容').fill('后台发送内容');
    await page.getByRole('button', { name: '发送' }).click();
    const sendSiteBody = (await sendSiteRequest).postDataJSON();
    expect(sendSiteBody.channelTypes).toEqual(['SITE']);
    expect(sendSiteBody.userId).toBe('1001');
    expect(sendSiteBody.title).toBe('后台系统消息');
    await expect(page.getByLabel('我的消息').getByText('测试系统消息')).toBeVisible();
    await expect(page.getByLabel('我的消息').getByText('未读', { exact: true })).toBeVisible();
    await page.getByRole('button', { name: '详情' }).click();
    const siteDetailDialog = page.getByRole('dialog', { name: '系统消息详情' });
    await expect(siteDetailDialog.getByText('这是一条系统消息内容')).toBeVisible();
    await expect(siteDetailDialog.getByText('WF-1', { exact: true })).toBeVisible();
    await page.keyboard.press('Escape');
    await page.getByRole('button', { name: '已读', exact: true }).click();
    await expect(page.getByText('已读', { exact: true })).toBeVisible();
    await page.locator('tr', { hasText: '测试系统消息' }).locator('.el-checkbox__input').click();
    await page.getByRole('button', { name: '批量已读' }).click();
    await page.getByRole('button', { name: '全部已读' }).click();
    await page.getByRole('button', { name: '删除' }).click();
    await page.getByRole('button', { name: 'OK' }).click();
    await expect(page.getByText('No Data')).toBeVisible();
  });
});
