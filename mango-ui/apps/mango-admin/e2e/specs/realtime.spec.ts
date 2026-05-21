import { expect, test, type Page } from '@playwright/test';

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

  await page.route('**/api/realtime/transports/negotiate**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        recommended: 'polling',
        transports: [
          { type: 'websocket', enabled: true, endpoint: '/realtime/transports/websocket', bidirectional: true },
          { type: 'sse', enabled: true, endpoint: '/realtime/transports/sse' },
          { type: 'polling', enabled: true, endpoint: '/realtime/transports/polling', longPolling: true },
        ],
      }),
    });
  });

  await page.route('**/api/realtime/transports/polling**', async (route) => {
    pollingCount += 1;
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(pollingCount === 1 ? [
        {
          id: 'server-1',
          version: '1.0',
          event: { domain: 'chat', name: 'message' },
          source: { platform: 'server' },
          context: { tenantId: 'default', userId: 1001 },
          metadata: { senderName: '服务端', roomName: '订单协作群' },
          payload: { type: 'text', text: 'hello from mocked polling' },
          timestamp: '2026-05-20T00:00:00Z',
        },
      ] : []),
    });
  });

  await page.route('**/api/realtime/messages/inbound/polling**', async (route) => {
    const payload = route.request().postDataJSON();
    lastInboundPayload = payload;
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
    await expect(page.locator('#message-format').getByRole('cell', { name: 'HEART', exact: true })).toBeVisible();
    await expect(page.locator('#message-format').getByRole('cell', { name: 'CUSTOM', exact: true })).toBeVisible();

    const negotiationResponse = page.waitForResponse((response) =>
      response.status() === 200 && response.url().includes('/api/realtime/transports/negotiate')
    );
    await page.getByRole('button', { name: '连接' }).click();
    await negotiationResponse;

    const chatPanel = page.getByTestId('realtime-chat-panel');
    await chatPanel.getByRole('tab', { name: '链接状态' }).click();
    await expect(chatPanel.getByText('连接成功')).toBeVisible({ timeout: 10000 });
    await expect(chatPanel.locator('.el-tag__content', { hasText: 'polling' })).toBeVisible({ timeout: 10000 });
    await expect(page.getByTestId('realtime-message-list').getByText('hello from mocked polling')).toBeVisible({ timeout: 10000 });

    await page.getByTestId('realtime-message-list').getByText('hello from mocked polling').click();
    await expect(page.getByRole('dialog', { name: '原始消息 JSON' })).toBeVisible();
    await expect(page.locator('.record-json')).toContainText('chat.message');
    await expect(page.locator('.record-json')).toContainText('"event"');
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
      event: { domain: 'chat', name: 'message' },
      payload: { type: 'text', text: 'hello inbound polling' },
      context: { tenantId: 'default', userId: 1001 },
      metadata: { senderName: '张三', department: '产品研发部' },
    });

    const publishResponse = page.waitForResponse((response) =>
      response.status() === 200 && response.url().includes('/api/realtime/messages/publish')
    );
    await page.getByRole('button', { name: '模拟服务端消息' }).click();
    await publishResponse;

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
