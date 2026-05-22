import { chromium, expect } from '@playwright/test';

const mode = process.env.REALTIME_E2E_MODE || 'auto';
const runId = `${mode}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`;
const profileName = `联调-${runId}`;
const roomId = `room-${runId}`;
const roomName = `E2E-${runId}`;
const inboundText = `hello inbound ${runId}`;
const serverMessageText = `服务端消息：${profileName} 已加入 ${roomName}`;
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({
  baseURL: process.env.REALTIME_E2E_BASE_URL || 'http://127.0.0.1:7791',
  viewport: { width: 1366, height: 900 },
});

async function login() {
  await page.goto('/#/login');
  await expect(page.locator('input[placeholder="用户名"]')).toBeVisible({ timeout: 10000 });
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.locator('input[placeholder="密码"]').blur();
  await page.waitForTimeout(800);

  const loginButton = page.getByRole('button', { name: '登 录' });
  await expect(loginButton).toBeEnabled({ timeout: 10000 });
  for (let attempt = 0; attempt < 3; attempt += 1) {
    await loginButton.click();
    try {
      await page.waitForURL('**/#/home', { timeout: 15000 });
      return;
    } catch (error) {
      if (attempt === 2) throw error;
      await page.waitForTimeout(1000);
      await expect(loginButton).toBeEnabled({ timeout: 10000 });
    }
  }
}

try {
  await login();
  await page.goto('/#/components/realtime');
  await expect(page.getByRole('heading', { name: 'RealtimeClient 实时通信组件', exact: true })).toBeVisible({ timeout: 10000 });
  await page.getByTestId('realtime-chat-panel').getByRole('tab', { name: '用户信息' }).click();
  await page.getByPlaceholder('用户姓名').fill(profileName);
  await page.getByPlaceholder('groupId').fill(roomId);
  await page.getByPlaceholder('群组名称').fill(roomName);
  await page.getByTestId('realtime-chat-panel').getByRole('tab', { name: '链接配置' }).click();
  if (mode !== 'auto') {
    await page.locator('.el-radio-button__inner', { hasText: mode === 'websocket' ? 'WS' : mode === 'sse' ? 'SSE' : 'Ajax' }).click();
  }

  await page.getByRole('button', { name: '连接' }).click();
  await page.getByTestId('realtime-chat-panel').getByRole('tab', { name: '链接状态' }).click();
  await expect(page.getByTestId('realtime-chat-panel').getByText('连接成功')).toBeVisible({ timeout: 15000 });

  await page.getByPlaceholder('输入业务消息内容').fill(inboundText);
  await page.getByRole('button', { name: '发送', exact: true }).click();
  await expect(page.getByTestId('realtime-message-list').locator('.message-content', { hasText: new RegExp(`^${escapeRegExp(inboundText)}$`) })).toBeVisible({ timeout: 10000 });
  await expect(page.getByTestId('realtime-message-list').locator('.message-content', { hasText: new RegExp(`^我收到你发送的消息“${escapeRegExp(inboundText)}”$`) })).toBeVisible({ timeout: 10000 });
  await expect(page.getByTestId('heartbeat-panel').getByText('客户端')).toBeVisible({ timeout: 35000 });
  await expect(page.getByTestId('heartbeat-panel').getByText('服务端')).toBeVisible({ timeout: 10000 });

  await page.getByRole('button', { name: '模拟服务端消息' }).click();
  await expect(page.getByTestId('realtime-message-list').locator('.message-content', { hasText: serverMessageText })).toHaveCount(1, { timeout: 10000 });

  await page.getByTestId('realtime-chat-panel').getByRole('tab', { name: '用户信息' }).click();
  await page.locator('.el-radio-button__inner', { hasText: '点对点' }).click();
  await page.getByTestId('realtime-chat-panel').getByRole('tab', { name: '链接状态' }).click();
  await page.getByRole('button', { name: '模拟服务端消息' }).click();
  await expect(page.getByTestId('realtime-message-list').locator('.message-content', { hasText: serverMessageText })).toHaveCount(2, { timeout: 10000 });

  await page.getByTestId('realtime-chat-panel').getByRole('tab', { name: '用户信息' }).click();
  await page.locator('.el-radio-button__inner', { hasText: '指定端' }).click();
  await page.getByTestId('realtime-chat-panel').getByRole('tab', { name: '链接状态' }).click();
  await page.getByRole('button', { name: '模拟服务端消息' }).click();
  await expect(page.getByTestId('realtime-message-list').locator('.message-content', { hasText: serverMessageText })).toHaveCount(3, { timeout: 10000 });

  await page.getByTestId('realtime-message-list').locator('.message-bubble').last().click();
  await expect(page.getByRole('dialog', { name: '原始消息 JSON' })).toBeVisible({ timeout: 5000 });
  const rawDialog = page.getByRole('dialog', { name: '原始消息 JSON' });
  await expect(rawDialog.getByText('"target"')).toBeVisible({ timeout: 5000 });
  await expect(rawDialog.getByText('"CLIENT"')).toBeVisible({ timeout: 5000 });

  const protocol = await page.getByTestId('realtime-chat-panel').locator('.el-tag__content').first().innerText();
  console.log(`${protocol}: connected, inbound ack, group/user/client delivery, heartbeat, raw JSON passed`);
  await page.keyboard.press('Escape');

  await page.getByRole('button', { name: '断开' }).click();
  await expect(page.getByTestId('realtime-chat-panel').getByText('已断开')).toBeVisible({ timeout: 5000 });
} finally {
  await browser.close();
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
