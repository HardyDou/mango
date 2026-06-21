import { expect, test, type Page } from '@playwright/test';

const serverMessageText = 'hello from mocked polling';

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function loginByUi(page: Page) {
  await page.goto('/#/login');
  await expect(page.locator('input[placeholder="用户名"]')).toBeVisible({ timeout: 10000 });
  await page.locator('input[placeholder="用户名"]').fill('admin');
  await page.locator('input[placeholder="密码"]').fill('admin123');

  const loginButton = page.getByRole('button', { name: '登 录' });
  await expect(loginButton).toBeEnabled({ timeout: 10000 });
  for (let attempt = 0; attempt < 2; attempt += 1) {
    await loginButton.click();
    try {
      await page.waitForURL('**/#/home', { timeout: 15000 });
      return;
    } catch (error) {
      if (attempt === 1) throw error;
      await expect(loginButton).toBeEnabled({ timeout: 10000 });
    }
  }
}

async function mockRealtimeApi(page: Page) {
  let pollingCount = 0;
  let lastInboundPayload: Record<string, unknown> | null = null;
  let pendingServerMessage = false;

  await page.route('**/api/realtime/transports/negotiate**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        recommended: 'polling',
        transports: [
          { type: 'websocket', enabled: false, endpoint: '/realtime/transports/websocket', bidirectional: true },
          { type: 'sse', enabled: false, endpoint: '/realtime/transports/sse' },
          { type: 'polling', enabled: true, endpoint: '/realtime/transports/polling', longPolling: true },
        ],
      }),
    });
  });

  await page.route('**/api/realtime/transports/polling**', async (route) => {
    pollingCount += 1;
    const deadline = Date.now() + 5000;
    while (!pendingServerMessage && Date.now() < deadline) {
      await sleep(100);
    }
    const messages = pendingServerMessage ? [
      {
        id: `server-${pollingCount}`,
        version: '1.0',
        event: { domain: 'chat', name: 'message.delivered' },
        source: { platform: 'server' },
        context: { tenantId: 'default', userId: 1001 },
        metadata: { senderName: '服务端', roomName: '订单协作群' },
        payload: { type: 'text', text: serverMessageText },
        timestamp: '2026-05-20T00:00:00Z',
      },
    ] : [];
    pendingServerMessage = false;
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(messages),
    });
  });

  await page.route('**/api/realtime/messages/inbound/polling**', async (route) => {
    const payload = route.request().postDataJSON();
    lastInboundPayload = payload;
    if (payload.event?.name !== 'message.send') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: '',
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 'accepted-1',
        version: '1.0',
        event: { domain: 'chat', name: 'message.accepted' },
        source: { platform: 'server' },
        context: { tenantId: payload.context?.tenantId, userId: payload.context?.userId },
        metadata: { senderName: '服务端' },
        payload: { type: 'text', message: 'inbound accepted' },
        status: { code: 200, state: 'SUCCESS' },
        ack: { messageId: payload.id, accepted: true },
        timestamp: '2026-05-20T00:00:00Z',
      }),
    });
  });

  await page.route('**/api/realtime/messages/publish', async (route) => {
    const payload = route.request().postDataJSON();
    expect(payload.metadata?.senderName).toBe('系统通知');
    pendingServerMessage = true;
    await route.fulfill({ status: 200, contentType: 'application/json', body: '' });
  });

  return {
    getLastInboundPayload: () => lastInboundPayload,
  };
}

test.describe('Realtime 实时消息 E2E', () => {
  test('业务示例可协商到 Polling 并完成消息交互', async ({ page }) => {
    const realtimeMock = await mockRealtimeApi(page);
    await loginByUi(page);

    await page.goto('/#/components/realtime');
    await expect(page.getByRole('heading', { name: 'RealtimeClient 实时通信组件', exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: '聊天室业务用法' })).toBeVisible();
    await expect(page.getByRole('heading', { name: '支持模式' })).toBeVisible();
    await expect(page.getByRole('heading', { name: '提供方法' })).toBeVisible();
    await expect(page.getByRole('heading', { name: '配置项' })).toBeVisible();
    await expect(page.getByRole('heading', { name: '消息协议' })).toBeVisible();
    await expect(page.locator('#message-format').getByRole('cell', { name: 'heartbeat.ping', exact: true })).toBeVisible();
    await expect(page.locator('#message-format').getByRole('cell', { name: 'heartbeat.pong', exact: true })).toBeVisible();
    await expect(page.locator('#message-format').getByRole('cell', { name: 'message.send', exact: true })).toBeVisible();

    const negotiationResponse = page.waitForResponse((response) =>
      response.status() === 200 && response.url().includes('/api/realtime/transports/negotiate')
    );
    await page.getByRole('button', { name: '连接' }).click();
    await negotiationResponse;

    const chatPanel = page.getByTestId('realtime-chat-panel');
    await chatPanel.getByRole('tab', { name: '链接状态' }).click();
    await expect(chatPanel.getByText('连接成功')).toBeVisible({ timeout: 10000 });
    await expect(chatPanel.locator('.el-tag__content', { hasText: 'polling' })).toBeVisible({ timeout: 10000 });

    const publishResponse = page.waitForResponse((response) =>
      response.status() === 200 && response.url().includes('/api/realtime/messages/publish')
    );
    await page.getByRole('button', { name: '模拟服务端消息' }).click();
    await publishResponse;
    await expect(page.getByTestId('realtime-message-list').getByText(serverMessageText)).toBeVisible({ timeout: 30000 });

    await page.getByTestId('realtime-message-list').getByText(serverMessageText).click();
    await expect(page.getByRole('dialog', { name: '原始消息 JSON' })).toBeVisible();
    await expect(page.locator('.record-json')).toContainText('"event"');
    await expect(page.locator('.record-json')).toContainText('"domain": "chat"');
    await expect(page.locator('.record-json')).toContainText('"name": "message.delivered"');
    await page.keyboard.press('Escape');

    const inboundResponse = page.waitForResponse((response) =>
      response.status() === 200 && response.url().includes('/api/realtime/messages/inbound/polling')
    );
    await page.getByPlaceholder('输入业务消息内容').fill('hello inbound polling');
    await page.getByRole('button', { name: '发送', exact: true }).click();
    await inboundResponse;

    await expect(page.getByTestId('realtime-message-list').getByText('hello inbound polling')).toBeVisible();
    await expect(page.getByTestId('realtime-message-list').getByText('inbound accepted')).toBeVisible();
    expect(realtimeMock.getLastInboundPayload()).toMatchObject({
      event: { domain: 'chat', name: 'message.send' },
      payload: { type: 'text', text: 'hello inbound polling' },
      context: { tenantId: '1', userId: 1 },
      metadata: { senderName: 'Administrator', department: '产品研发部' },
    });

    await page.getByRole('button', { name: '断开' }).click();
    await expect(chatPanel.getByText('已断开')).toBeVisible();
  });

  test('移动端布局下示例区和消息区不水平溢出', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await mockRealtimeApi(page);
    await loginByUi(page);

    await page.goto('/#/components/realtime');
    await expect(page.getByRole('heading', { name: 'RealtimeClient 实时通信组件', exact: true })).toBeVisible();
    await expect(page.getByTestId('realtime-chat-panel')).toBeVisible();
    await expect(page.getByTestId('realtime-message-list')).toBeVisible();

    const viewportWidth = page.viewportSize()?.width || 390;
    for (const selector of ['.doc-layout', '.demo-block', '.realtime-shell', '.message-list']) {
      const box = await page.locator(selector).first().boundingBox();
      expect(box, `${selector} should be measurable`).toBeTruthy();
      expect(Math.ceil((box?.x || 0) + (box?.width || 0))).toBeLessThanOrEqual(viewportWidth);
    }
  });
});
