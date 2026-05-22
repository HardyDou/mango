import { chromium, expect } from '@playwright/test';

const baseURL = process.env.REALTIME_E2E_BASE_URL || 'http://127.0.0.1:7791';
const roomId = `room-e2e-${Date.now().toString(36)}`;
const messageText = `group fanout ${Date.now()}`;

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  baseURL,
  viewport: { width: 1366, height: 900 },
});
const diagnostics = [];

function collectDiagnostics(page, name) {
  page.on('console', message => {
    if (message.type() === 'error') diagnostics.push(`[${name}] console: ${message.text()}`);
  });
  page.on('pageerror', error => diagnostics.push(`[${name}] pageerror: ${error.message}`));
  page.on('requestfailed', request => {
    const url = request.url();
    if (url.includes('/realtime/')) diagnostics.push(`[${name}] request failed: ${url} ${request.failure()?.errorText || ''}`);
  });
}

async function login(page) {
  await page.goto('/#/login');
  await expect(page.locator('input[placeholder="用户名"]')).toBeVisible({ timeout: 10000 });
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.locator('input[placeholder="密码"]').blur();
  await page.waitForTimeout(800);

  const loginButton = page.getByRole('button', { name: '登 录' });
  await expect(loginButton).toBeEnabled({ timeout: 10000 });
  await loginButton.click();
  await page.waitForURL('**/#/home', { timeout: 15000 });
}

async function openRealtimePage(page, profile) {
  await page.goto('/#/components/realtime');
  await expect(page.getByRole('heading', { name: 'RealtimeClient 实时通信组件', exact: true })).toBeVisible({ timeout: 10000 });

  const panel = page.getByTestId('realtime-chat-panel');
  await panel.getByRole('tab', { name: '用户信息' }).click();
  await page.getByPlaceholder('用户姓名').fill(profile.name);
  await panel.locator('.profile-form .el-input-number input').fill(String(profile.userId));
  await page.getByPlaceholder('groupId').fill(roomId);
  await page.getByPlaceholder('群组名称').fill('E2E 群组');

  await panel.getByRole('tab', { name: '链接配置' }).click();
  await page.locator('.el-radio-button__inner', { hasText: 'WS' }).click();
  await page.getByRole('button', { name: '连接' }).click();

  await panel.getByRole('tab', { name: '链接状态' }).click();
  await expect(panel.getByText('连接成功')).toBeVisible({ timeout: 15000 });
  await expect(panel.locator('.el-tag__content', { hasText: 'websocket' })).toBeVisible({ timeout: 10000 });
}

try {
  const pageA = await context.newPage();
  collectDiagnostics(pageA, 'A');
  await login(pageA);

  const pageB = await context.newPage();
  collectDiagnostics(pageB, 'B');
  await login(pageB);
  await openRealtimePage(pageA, { name: '张三', userId: 1001, clientId: `e2e-a-${Date.now().toString(36)}` });
  await openRealtimePage(pageB, { name: '李四', userId: 1002, clientId: `e2e-b-${Date.now().toString(36)}` });

  await expect(pageA.getByTestId('realtime-room-members').getByText('张三')).toBeVisible({ timeout: 10000 });
  await expect(pageA.getByTestId('realtime-room-members').getByText('李四')).toBeVisible({ timeout: 10000 });
  await expect(pageB.getByTestId('realtime-room-members').getByText('张三')).toBeVisible({ timeout: 10000 });
  await expect(pageB.getByTestId('realtime-room-members').getByText('李四')).toBeVisible({ timeout: 10000 });
  await expect(pageB.getByTestId('realtime-room-members').getByText('2 在线 / 2 进入过')).toBeVisible({ timeout: 10000 });

  await expect(pageA.getByTestId('realtime-chat-panel').getByText('连接成功')).toBeVisible({ timeout: 5000 });
  await pageA.getByPlaceholder('输入业务消息内容').fill(messageText);
  const sendButton = pageA.getByRole('button', { name: '发送', exact: true });
  await expect(sendButton).toBeEnabled({ timeout: 5000 });
  await sendButton.click();

  try {
    await expect(pageA.getByTestId('realtime-message-list').locator('.message-content', { hasText: messageText })).toBeVisible({ timeout: 10000 });
    await expect(pageA.getByTestId('realtime-message-list').locator('.message-content', { hasText: `我收到你发送的消息“${messageText}”` })).toBeVisible({ timeout: 10000 });
    await expect(pageB.getByTestId('realtime-message-list').locator('.message-content', { hasText: messageText })).toBeVisible({ timeout: 10000 });
  } catch (error) {
    const aText = await pageA.getByTestId('realtime-message-list').innerText().catch(innerError => String(innerError));
    const bText = await pageB.getByTestId('realtime-message-list').innerText().catch(innerError => String(innerError));
    throw new Error(`${error.message}\nA messages:\n${aText}\nB messages:\n${bText}\nDiagnostics:\n${diagnostics.join('\n')}`);
  }

  const sender = await pageB.getByTestId('realtime-message-list').locator('.message-meta', { hasText: '张三' }).count();
  if (sender < 1) {
    throw new Error('Group receiver did not render sender metadata');
  }

  console.log(`websocket group fanout passed: ${roomId}`);
} finally {
  await browser.close();
}
