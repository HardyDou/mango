import { expect, test, type APIRequestContext, type APIResponse, type Locator, type Page } from '@playwright/test';
import { createServer, type Server, type Socket } from 'node:net';
import { api as e2eApi } from '../support/api';
import { collectBrowserDiagnostics } from '../support/browser-diagnostics';
import {
  chooseElementPlusOption,
  chooseElementPlusRadio,
  openElementPlusSelect,
  setElementPlusSwitch,
} from '../support/element-plus';

type ApiHeaders = Record<string, string>;

type ChannelConfig = {
  id: string;
  configCode: string;
  configJson?: string;
  configName?: string;
  secretStatus?: string;
  secretValues?: Record<string, string>;
  lastSendStatus?: string;
  routeTagCodes?: string[];
};

type ChannelTemplate = {
  id: string;
  channelType: string;
  versionStatus: string;
  routeMode?: string;
  routeTagCode?: string;
  channelConfigId?: string;
};

type SendRecord = {
  id: string;
  status: string;
  failCode?: string;
  channelConfigId?: string;
  responseSnapshot?: string;
};

class SmtpCaptureServer {
  private server?: Server;
  private readonly capturedMessages: string[] = [];
  port = 0;

  get messages(): readonly string[] {
    return this.capturedMessages;
  }

  async start(): Promise<void> {
    this.server = createServer((socket) => this.handleConnection(socket));
    await new Promise<void>((resolve, reject) => {
      this.server?.once('error', reject);
      this.server?.listen(0, '127.0.0.1', () => resolve());
    });
    const address = this.server.address();
    if (!address || typeof address === 'string') {
      throw new Error('无法确定 SMTP 测试服务器端口');
    }
    this.port = address.port;
  }

  async stop(): Promise<void> {
    if (!this.server) return;
    await new Promise<void>((resolve, reject) => {
      this.server?.close((error) => (error ? reject(error) : resolve()));
    });
    this.server = undefined;
  }

  private handleConnection(socket: Socket): void {
    let buffer = '';
    let state: 'COMMAND' | 'AUTH_USER' | 'AUTH_PASSWORD' | 'DATA' = 'COMMAND';
    const dataLines: string[] = [];
    socket.setEncoding('utf8');
    socket.write('220 mango-e2e-smtp ESMTP ready\r\n');
    socket.on('data', (chunk: string) => {
      buffer += chunk;
      while (buffer.includes('\r\n')) {
        const separator = buffer.indexOf('\r\n');
        const line = buffer.slice(0, separator);
        buffer = buffer.slice(separator + 2);
        if (state === 'DATA') {
          if (line === '.') {
            this.capturedMessages.push(dataLines.join('\r\n'));
            dataLines.length = 0;
            state = 'COMMAND';
            socket.write('250 2.0.0 queued as MANGO-E2E\r\n');
          } else {
            dataLines.push(line.startsWith('..') ? line.slice(1) : line);
          }
          continue;
        }
        if (state === 'AUTH_USER') {
          state = 'AUTH_PASSWORD';
          socket.write('334 UGFzc3dvcmQ6\r\n');
          continue;
        }
        if (state === 'AUTH_PASSWORD') {
          state = 'COMMAND';
          socket.write('235 2.7.0 Authentication successful\r\n');
          continue;
        }
        const command = line.toUpperCase();
        if (command.startsWith('EHLO')) {
          socket.write('250-mango-e2e-smtp\r\n250 AUTH LOGIN\r\n');
        } else if (command === 'AUTH LOGIN') {
          state = 'AUTH_USER';
          socket.write('334 VXNlcm5hbWU6\r\n');
        } else if (command.startsWith('MAIL FROM:') || command.startsWith('RCPT TO:')) {
          socket.write('250 2.1.0 OK\r\n');
        } else if (command === 'DATA') {
          state = 'DATA';
          socket.write('354 End data with <CR><LF>.<CR><LF>\r\n');
        } else if (command === 'QUIT') {
          socket.write('221 2.0.0 Bye\r\n');
          socket.end();
        } else {
          socket.write('250 2.0.0 OK\r\n');
        }
      }
    });
  }
}

function smtpMessagesForCase(smtp: SmtpCaptureServer, caseNo: string): readonly string[] {
  const encodedSubject = Buffer.from(`ISSUE 641/642 ${caseNo}`, 'utf8').toString('base64');
  return smtp.messages.filter((message) => message.includes(`Subject: =?UTF-8?B?${encodedSubject}?=`));
}

async function apiData<T>(response: APIResponse, operation: string): Promise<T> {
  const body = await response.json();
  expect(response.status(), `${operation} HTTP 状态：${body.message || body.msg || body.code}`).toBe(200);
  expect(
    body.success || body.code === 200,
    `${operation} 业务结果：${body.message || body.msg || body.code}`,
  ).toBeTruthy();
  return body.data as T;
}

async function loginToken(request: APIRequestContext): Promise<string> {
  const response = await request.post(e2eApi('/auth/login'), {
    data: {
      username: 'admin',
      password: process.env.MANGO_E2E_ADMIN_PASSWORD || 'admin123',
      tenantId: '1',
      tenantCode: 'default',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  const data = await apiData<{ accessToken: string }>(response, '管理员登录');
  return data.accessToken;
}

function authHeaders(token: string): ApiHeaders {
  return {
    Authorization: `Bearer ${token}`,
    'TENANT-ID': '1',
    'X-Mango-Tenant-Id': '1',
  };
}

async function chooseTab(page: Page, name: string): Promise<void> {
  const tab = page.getByRole('tab', { name, exact: true });
  await tab.click();
  if ((await tab.getAttribute('aria-selected')) !== 'true') {
    await tab.focus();
    await tab.press('Enter');
  }
  await expect(tab).toHaveAttribute('aria-selected', 'true');
}

async function openBusinessEditor(page: Page, row: Locator, businessTypeId: string): Promise<void> {
  const encodedId = encodeURIComponent(businessTypeId);
  const loaded = Promise.all([
    page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes(`/api/notice/business-types/config-versions?businessTypeId=${encodedId}`),
    ),
    page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes(`/api/notice/business-types/channel-templates?businessTypeId=${encodedId}`),
    ),
    page.waitForResponse(
      (response) => response.request().method() === 'GET' && response.url().includes('/api/notice/channels?'),
    ),
  ]);
  await row.getByRole('button', { name: '编辑' }).click();
  await loaded;
  await expect(page.getByRole('heading', { name: '消息配置维护' })).toBeVisible();
  await expect(page.getByRole('tab', { name: '系统消息', exact: true })).toHaveAttribute('aria-selected', 'true');
}

async function loginPage(page: Page): Promise<void> {
  await page.goto('/#/login');
  await page.getByPlaceholder('用户名').fill('admin');
  await page.getByPlaceholder('密码').fill(process.env.MANGO_E2E_ADMIN_PASSWORD || 'admin123');
  await page.getByPlaceholder('密码').blur();
  await expect(page.locator('.tenant-select')).toContainText('芒果集团');
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.getByRole('button', { name: '登 录' }).click();
  await page.waitForURL('**/#/home', { timeout: 15_000 });
}

async function listChannels(request: APIRequestContext, headers: ApiHeaders): Promise<ChannelConfig[]> {
  const response = await request.get(e2eApi('/notice/channels?pageNum=1&pageSize=200'), { headers });
  const data = await apiData<{ list: ChannelConfig[] }>(response, '查询渠道账号');
  return data.list || [];
}

async function listTemplates(
  request: APIRequestContext,
  headers: ApiHeaders,
  businessTypeId: string,
): Promise<ChannelTemplate[]> {
  const response = await request.get(
    e2eApi(`/notice/business-types/channel-templates?businessTypeId=${encodeURIComponent(businessTypeId)}`),
    { headers },
  );
  return apiData<ChannelTemplate[]>(response, '查询渠道模板');
}

async function activeEmailTemplate(
  request: APIRequestContext,
  headers: ApiHeaders,
  businessTypeId: string,
): Promise<ChannelTemplate | undefined> {
  const templates = await listTemplates(request, headers, businessTypeId);
  return templates.find((item) => item.channelType === 'EMAIL' && item.versionStatus === 'ACTIVE');
}

async function emailRecords(
  request: APIRequestContext,
  headers: ApiHeaders,
  bizType: string,
  bizId: string,
): Promise<SendRecord[]> {
  const search = new URLSearchParams({ pageNum: '1', pageSize: '20', bizType, bizId, channelType: 'EMAIL' });
  const response = await request.get(e2eApi(`/notice/records?${search.toString()}`), { headers });
  const data = await apiData<{ list: SendRecord[] }>(response, '查询邮件发送记录');
  return data.list || [];
}

async function waitForEmailRecord(
  request: APIRequestContext,
  headers: ApiHeaders,
  bizType: string,
  bizId: string,
  expectedStatus: string,
): Promise<SendRecord> {
  await expect
    .poll(async () => (await emailRecords(request, headers, bizType, bizId))[0]?.status, {
      timeout: 30_000,
      intervals: [200, 500, 1_000],
    })
    .toBe(expectedStatus);
  const record = (await emailRecords(request, headers, bizType, bizId))[0];
  expect(record).toBeTruthy();
  return record;
}

async function publishEmailTemplate(
  request: APIRequestContext,
  headers: ApiHeaders,
  businessTypeId: string,
): Promise<void> {
  await apiData<boolean>(
    await request.post(
      e2eApi(
        `/notice/business-types/channel-templates/publish?businessTypeId=${encodeURIComponent(businessTypeId)}&channelType=EMAIL`,
      ),
      { headers },
    ),
    '发布邮件模板',
  );
}

async function sendEmail(
  request: APIRequestContext,
  headers: ApiHeaders,
  data: Record<string, unknown>,
): Promise<void> {
  await apiData(await request.post(e2eApi('/notice/send'), { headers, data }), `发送通知 ${String(data.bizId || '')}`);
}

test.describe('ISSUE 641/642 Notice 真实环境验收', () => {
  test.describe.configure({ mode: 'serial' });

  test('@p0 @notice File→Notice→SMTP 附件与三模式路由全链路', async ({ page, request }, testInfo) => {
    test.setTimeout(240_000);
    page.setDefaultTimeout(10_000);
    const smtp = new SmtpCaptureServer();
    await smtp.start();

    const unique = `${Date.now().toString(36)}${testInfo.workerIndex}`.toUpperCase();
    const goodCode = `E2E_642_GOOD_${unique}`;
    const badCode = `E2E_642_BAD_${unique}`;
    const goodName = `E2E 主邮件账号 ${unique}`;
    const badName = `E2E 故障邮件账号 ${unique}`;
    const primaryTagCode = `E2E_PRIMARY_${unique}`;
    const emptyTagCode = `E2E_EMPTY_${unique}`;
    const primaryTagName = `E2E 主路由 ${unique}`;
    const emptyTagName = `E2E 空路由 ${unique}`;
    const bizType = `E2E_NOTICE_641_642_${unique}`;
    const bizName = `ISSUE 641/642 真实验收 ${unique}`;
    const diagnostics = collectBrowserDiagnostics(page);
    const createdChannelIds: string[] = [];
    const createdTagIds: string[] = [];
    let businessTypeId = '';
    let fileId = '';
    const token = await loginToken(request);
    const headers = authHeaders(token);

    try {
      await loginPage(page);

      const channelsResponse = page.waitForResponse(
        (response) => response.url().includes('/api/notice/channels') && response.request().method() === 'GET',
      );
      await page.goto('/#/notice/channel');
      await channelsResponse;
      const channelSurface = page.locator('[data-surface="notice.channel.routing"]');
      await expect(channelSurface.getByRole('heading', { name: '渠道配置' })).toBeVisible();

      const resourceRow = channelSurface.getByRole('row').filter({ hasText: 'SITE_INTERNAL_TENANT_1' });
      await expect(resourceRow.getByText('Resource', { exact: true })).toBeVisible();
      await resourceRow.getByRole('button', { name: '编辑' }).click();
      const resourceDialog = page.getByRole('dialog', { name: '编辑渠道' });
      await expect(resourceDialog.getByText('该账号由 Resource 管理')).toBeVisible();
      await expect(resourceDialog.getByLabel('配置编码')).toBeDisabled();
      await expect(resourceDialog.getByLabel('渠道类型')).toBeDisabled();
      await expect(resourceDialog.getByLabel('通道名称')).toBeDisabled();
      await resourceDialog.getByRole('button', { name: '取消' }).click();

      await channelSurface.getByRole('button', { name: '路由标签' }).click();
      const routeManager = page.locator('[data-surface="notice.channel.reference-impact"]');
      for (const [tagCode, tagName] of [
        [primaryTagCode, primaryTagName],
        [emptyTagCode, emptyTagName],
      ]) {
        await routeManager.getByRole('button', { name: '新增标签' }).click();
        const editor = page.getByRole('dialog', { name: '新增路由标签' });
        await editor.getByLabel('标签编码').fill(tagCode);
        await editor.getByLabel('标签名称').fill(tagName);
        const saveTagResponse = page.waitForResponse(
          (response) =>
            response.url().includes('/api/notice/channel-route-tags') && response.request().method() === 'POST',
        );
        await editor.getByRole('button', { name: '保存' }).click();
        await apiData(await saveTagResponse, `保存路由标签 ${tagCode}`);
        await expect(routeManager.getByRole('row').filter({ hasText: tagCode })).toBeVisible();
      }
      const tags = await apiData<Array<{ id: string; tagCode: string }>>(
        await request.get(e2eApi('/notice/channel-route-tags?channelType=EMAIL'), { headers }),
        '回读路由标签',
      );
      createdTagIds.push(
        ...tags.filter((item) => [primaryTagCode, emptyTagCode].includes(item.tagCode)).map((item) => String(item.id)),
      );
      await routeManager.getByRole('button', { name: '关闭', exact: true }).click();

      await channelSurface.getByRole('button', { name: '新增', exact: true }).click();
      const channelDialog = page.getByRole('dialog', { name: '新增渠道' });
      await channelDialog.getByLabel('配置编码').fill(goodCode);
      await channelDialog.getByLabel('通道名称').fill(goodName);
      await channelDialog.getByLabel('优先级').fill('10');
      await openElementPlusSelect(channelDialog, '路由标签');
      await page.getByRole('option', { name: primaryTagName }).click();
      await channelDialog.getByLabel('SMTP').fill('127.0.0.1');
      await channelDialog.getByLabel('端口').fill(String(smtp.port));
      await channelDialog.getByLabel('账号').fill('mango-e2e');
      await channelDialog.getByLabel('密码').fill(`runtime-${unique}`);
      await channelDialog.getByLabel('发件人').fill('notice-e2e@mango.local');
      await setElementPlusSwitch(channelDialog, 'SSL', false);
      const saveChannelResponse = page.waitForResponse(
        (response) => response.url().includes('/api/notice/channels') && response.request().method() === 'POST',
      );
      await channelDialog.getByRole('button', { name: '保存' }).click();
      const savedChannel = await apiData<ChannelConfig>(await saveChannelResponse, '保存主邮件账号');
      createdChannelIds.push(String(savedChannel.id));

      const goodRow = channelSurface.getByRole('row').filter({ hasText: goodCode });
      await expect(goodRow.getByText('完整', { exact: true })).toHaveCount(2);
      await expect(goodRow.getByText(primaryTagCode, { exact: true })).toBeVisible();
      await goodRow.getByRole('button', { name: '编辑' }).click();
      const editChannelDialog = page.getByRole('dialog', { name: '编辑渠道' });
      await expect(editChannelDialog.getByLabel('配置编码')).toBeDisabled();
      await expect(editChannelDialog.getByLabel('密码')).toHaveValue('');
      await editChannelDialog.getByRole('button', { name: '取消' }).click();

      const persistedGood = (await listChannels(request, headers)).find((item) => item.configCode === goodCode);
      expect(persistedGood).toBeTruthy();
      expect(persistedGood?.secretStatus).toBe('COMPLETE');
      expect(persistedGood?.configJson).not.toContain('password');
      expect(persistedGood?.secretValues).toBeUndefined();

      await channelSurface.getByRole('button', { name: '路由标签' }).click();
      await expect(routeManager.getByRole('row').filter({ hasText: primaryTagCode })).toContainText(goodName);
      await expect(routeManager.getByRole('row').filter({ hasText: emptyTagCode })).toContainText('暂无');
      await routeManager.getByRole('button', { name: '关闭', exact: true }).click();

      const badChannel = await apiData<ChannelConfig>(
        await request.post(e2eApi('/notice/channels'), {
          headers,
          data: {
            configCode: badCode,
            channelType: 'EMAIL',
            providerCode: 'CUSTOM_SMTP',
            configName: badName,
            configJson: JSON.stringify({
              host: '127.0.0.1',
              port: 1,
              username: 'mango-e2e-bad',
              from: 'notice-e2e@mango.local',
              ssl: false,
              timeoutMillis: 500,
            }),
            secretValues: [{ key: 'password', value: `runtime-bad-${unique}` }],
            enabled: true,
            priority: 0,
            weight: 100,
            routeTagCodes: [],
          },
        }),
        '保存故障邮件账号',
      );
      createdChannelIds.push(String(badChannel.id));

      const business = await apiData<{ id: string }>(
        await request.post(e2eApi('/notice/business-types'), {
          headers,
          data: {
            bizType,
            bizName,
            bizGroup: 'NOTICE',
            domainCode: 'NOTICE',
            description: 'ISSUE 641/642 真实环境自动验收数据',
            paramsSchema: JSON.stringify({ type: 'object', properties: {} }),
            defaultPriority: 'NORMAL',
          },
        }),
        '创建验收消息配置',
      );
      businessTypeId = String(business.id);
      await apiData(
        await request.put(
          e2eApi(`/notice/business-types/config-draft?businessTypeId=${encodeURIComponent(businessTypeId)}`),
          {
            headers,
            data: {
              paramsSchema: JSON.stringify({ type: 'object', properties: {} }),
              defaultPriority: 'NORMAL',
              idempotentStrategy: 'bizId',
            },
          },
        ),
        '创建消息配置草稿',
      );
      await apiData(
        await request.put(
          e2eApi(`/notice/business-types/channel-templates?businessTypeId=${encodeURIComponent(businessTypeId)}`),
          {
            headers,
            data: {
              channelType: 'EMAIL',
              templateName: `E2E 邮件模板 ${unique}`,
              titleTemplate: 'ISSUE 641/642 {{caseNo}}',
              contentTemplate: '真实附件与路由验收 {{caseNo}}',
              enabled: true,
              routeMode: 'AUTO',
            },
          },
        ),
        '创建邮件模板草稿',
      );

      await page.goto('/#/notice/message-definition');
      const businessRow = page.getByRole('row').filter({ hasText: bizType });
      await expect(businessRow).toBeVisible({ timeout: 15_000 });
      await openBusinessEditor(page, businessRow, businessTypeId);
      await setElementPlusSwitch(page, '是否启用', false);
      await chooseTab(page, '邮件');
      const routeModeField = page.locator('[data-field="notice.channel.route-mode"]');
      await chooseElementPlusRadio(routeModeField, '指定账号');
      await openElementPlusSelect(page, '渠道账号');
      const goodChannelOption = page.getByRole('option', { name: goodName });
      await chooseElementPlusOption(goodChannelOption);
      await page.getByRole('button', { name: '保存', exact: true }).click();
      await expect
        .poll(async () => {
          const draft = (await listTemplates(request, headers, businessTypeId)).find(
            (item) => item.channelType === 'EMAIL' && item.versionStatus === 'DRAFT',
          );
          return { routeMode: draft?.routeMode, channelConfigId: draft?.channelConfigId };
        })
        .toEqual({ routeMode: 'EXACT', channelConfigId: String(savedChannel.id) });
      const exactDraft = (await listTemplates(request, headers, businessTypeId)).find(
        (item) => item.channelType === 'EMAIL' && item.versionStatus === 'DRAFT',
      );
      expect(exactDraft).toMatchObject({ routeMode: 'EXACT', channelConfigId: String(savedChannel.id) });
      expect(exactDraft?.routeTagCode).toBeFalsy();
      await page.getByRole('button', { name: '发布新版本' }).click();
      await expect(page.getByText('已发布新版本')).toBeVisible({ timeout: 15_000 });
      await expect
        .poll(async () => (await activeEmailTemplate(request, headers, businessTypeId))?.routeMode)
        .toBe('EXACT');

      await page.goto('/#/notice/channel');
      await goodRow.getByRole('button', { name: '删除' }).click();
      const protectedChannelDialog = page.getByRole('dialog', { name: '无法删除' });
      await expect(protectedChannelDialog).toContainText(bizType);
      await protectedChannelDialog.getByRole('button', { name: '确定' }).click();

      const attachmentName = `ISSUE-641-真实附件-${unique}.txt`;
      const attachmentText = `Mango ISSUE 641 real attachment ${unique}`;
      const uploaded = await apiData<Array<{ id: string; fileName: string; contentType: string }>>(
        await request.post(e2eApi('/file/files/batch'), {
          headers,
          multipart: {
            files: { name: attachmentName, mimeType: 'text/plain', buffer: Buffer.from(attachmentText, 'utf8') },
            purpose: 'attachment',
            accessLevel: 'PRIVATE',
            bizType,
            bizId: `FILE-${unique}`,
          },
        }),
        '上传真实附件',
      );
      fileId = String(uploaded[0].id);
      expect(uploaded[0].fileName).toBe(attachmentName);

      const exactBizId = `EXACT-ATTACHMENT-${unique}`;
      await sendEmail(request, headers, {
        bizType,
        bizId: exactBizId,
        params: { caseNo: exactBizId },
        attachmentFileIds: [fileId],
        channelTypes: ['EMAIL'],
        recipients: [{ recipientName: 'E2E 收件人', email: 'receiver@mango.local' }],
      });
      const exactRecord = await waitForEmailRecord(request, headers, bizType, exactBizId, 'SUCCESS');
      expect(exactRecord.channelConfigId).toBe(String(savedChannel.id));
      expect(exactRecord.responseSnapshot).toContain(`"fileId":${fileId}`);
      await expect.poll(() => smtpMessagesForCase(smtp, exactBizId).length).toBe(1);
      const attachmentMime = smtpMessagesForCase(smtp, exactBizId)[0];
      expect(attachmentMime).toContain('Content-Type: multipart/mixed');
      expect(attachmentMime).toContain("filename*=UTF-8''ISSUE-641-%E7%9C%9F%E5%AE%9E%E9%99%84%E4%BB%B6-");
      expect(attachmentMime).toContain(Buffer.from(attachmentText, 'utf8').toString('base64'));

      const missingBizId = `MISSING-ATTACHMENT-${unique}`;
      await sendEmail(request, headers, {
        bizType,
        bizId: missingBizId,
        params: { caseNo: missingBizId },
        attachmentFileIds: ['9223372036854775000'],
        channelTypes: ['EMAIL'],
        recipients: [{ recipientName: 'E2E 收件人', email: 'receiver@mango.local' }],
      });
      const missingRecord = await waitForEmailRecord(request, headers, bizType, missingBizId, 'FAILED');
      expect(missingRecord.failCode).toBe('ATTACHMENT_NOT_FOUND_OR_FORBIDDEN');
      expect(missingRecord.responseSnapshot).not.toContain('http');
      expect(smtpMessagesForCase(smtp, missingBizId)).toHaveLength(0);

      await page.goto('/#/notice/message-definition');
      await openBusinessEditor(page, page.getByRole('row').filter({ hasText: bizType }), businessTypeId);
      await chooseTab(page, '邮件');
      await chooseElementPlusRadio(routeModeField, '路由标签');
      await openElementPlusSelect(page, '路由标签');
      const primaryTagOption = page.getByRole('option', { name: new RegExp(primaryTagName) });
      await chooseElementPlusOption(primaryTagOption);
      await expect(page.getByText(`候选账号：${goodName}`)).toBeVisible();
      await page.getByRole('button', { name: '发布新版本' }).click();
      await expect(page.getByText('已发布新版本')).toBeVisible({ timeout: 15_000 });
      await expect
        .poll(async () => (await activeEmailTemplate(request, headers, businessTypeId))?.routeMode)
        .toBe('TAG');

      await page.goto('/#/notice/channel');
      await channelSurface.getByRole('button', { name: '路由标签' }).click();
      await routeManager
        .getByRole('row')
        .filter({ hasText: primaryTagCode })
        .getByRole('button', { name: '删除' })
        .click();
      const protectedTagDialog = page.getByRole('dialog', { name: '无法删除' });
      await expect(protectedTagDialog).toContainText(bizType);
      await protectedTagDialog.getByRole('button', { name: '确定' }).click();
      await routeManager.getByRole('button', { name: '关闭', exact: true }).click();

      const tagBizId = `TAG-SUCCESS-${unique}`;
      await sendEmail(request, headers, {
        bizType,
        bizId: tagBizId,
        params: { caseNo: tagBizId },
        channelTypes: ['EMAIL'],
        recipients: [{ recipientName: 'E2E 收件人', email: 'receiver@mango.local' }],
      });
      const tagRecord = await waitForEmailRecord(request, headers, bizType, tagBizId, 'SUCCESS');
      expect(tagRecord.channelConfigId).toBe(String(savedChannel.id));
      await expect.poll(() => smtpMessagesForCase(smtp, tagBizId).length).toBe(1);

      await apiData(
        await request.put(
          e2eApi(`/notice/business-types/channel-templates?businessTypeId=${encodeURIComponent(businessTypeId)}`),
          {
            headers,
            data: {
              channelType: 'EMAIL',
              templateName: `E2E 邮件模板 ${unique}`,
              titleTemplate: 'ISSUE 641/642 {{caseNo}}',
              contentTemplate: '真实附件与路由验收 {{caseNo}}',
              enabled: true,
              routeMode: 'TAG',
              routeTagCode: emptyTagCode,
            },
          },
        ),
        '切换空候选 TAG 草稿',
      );
      await publishEmailTemplate(request, headers, businessTypeId);

      await page.goto('/#/notice/message-definition');
      await openBusinessEditor(page, page.getByRole('row').filter({ hasText: bizType }), businessTypeId);
      await chooseTab(page, '邮件');
      await expect(page.getByText('候选账号：暂无')).toBeVisible();
      await page.getByRole('button', { name: '保存', exact: true }).click();
      await expect(page.getByText(new RegExp(`路由标签“${emptyTagName}”暂无可用候选账号`))).toBeVisible();

      const emptyTagBizId = `TAG-EMPTY-${unique}`;
      await sendEmail(request, headers, {
        bizType,
        bizId: emptyTagBizId,
        params: { caseNo: emptyTagBizId },
        channelTypes: ['EMAIL'],
        recipients: [{ recipientName: 'E2E 收件人', email: 'receiver@mango.local' }],
      });
      const emptyTagRecord = await waitForEmailRecord(request, headers, bizType, emptyTagBizId, 'FAILED');
      expect(emptyTagRecord.failCode).toBe('CHANNEL_ROUTE_TAG_UNAVAILABLE');
      expect(emptyTagRecord.channelConfigId).toBeFalsy();
      expect(smtpMessagesForCase(smtp, emptyTagBizId)).toHaveLength(0);

      await chooseElementPlusRadio(routeModeField, '自动轮换');
      await page.getByRole('button', { name: '发布新版本' }).click();
      await expect(page.getByText('已发布新版本')).toBeVisible({ timeout: 15_000 });
      const autoTemplate = await activeEmailTemplate(request, headers, businessTypeId);
      expect(autoTemplate).toMatchObject({ routeMode: 'AUTO' });
      expect(autoTemplate?.channelConfigId).toBeFalsy();
      expect(autoTemplate?.routeTagCode).toBeFalsy();

      const autoBizId = `AUTO-FAILOVER-${unique}`;
      await sendEmail(request, headers, {
        bizType,
        bizId: autoBizId,
        params: { caseNo: autoBizId },
        channelTypes: ['EMAIL'],
        recipients: [{ recipientName: 'E2E 收件人', email: 'receiver@mango.local' }],
      });
      const autoRecord = await waitForEmailRecord(request, headers, bizType, autoBizId, 'SUCCESS');
      expect(autoRecord.channelConfigId).toBe(String(savedChannel.id));
      await expect.poll(() => smtpMessagesForCase(smtp, autoBizId).length).toBe(1);
      const channelStates = await listChannels(request, headers);
      expect(channelStates.find((item) => item.configCode === badCode)?.lastSendStatus).toBe('FAILED');
      expect(channelStates.find((item) => item.configCode === goodCode)?.lastSendStatus).toBe('SUCCESS');

      expect(diagnostics, diagnostics.join('\n')).toEqual([]);
      await testInfo.attach('notice-routing-email-live', {
        body: await page.screenshot({ fullPage: true }),
        contentType: 'image/png',
      });
    } finally {
      if (businessTypeId) {
        await request
          .delete(e2eApi(`/notice/business-types?id=${encodeURIComponent(businessTypeId)}`), { headers })
          .catch(() => undefined);
      }
      if (fileId) {
        await request.post(e2eApi('/file/files/delete'), { headers, data: { ids: [fileId] } }).catch(() => undefined);
      }
      for (const id of createdChannelIds.reverse()) {
        await request
          .delete(e2eApi(`/notice/channels?id=${encodeURIComponent(id)}`), { headers })
          .catch(() => undefined);
      }
      for (const id of createdTagIds.reverse()) {
        await request
          .delete(e2eApi(`/notice/channel-route-tags?id=${encodeURIComponent(id)}`), { headers })
          .catch(() => undefined);
      }
      await smtp.stop();
    }
  });
});
