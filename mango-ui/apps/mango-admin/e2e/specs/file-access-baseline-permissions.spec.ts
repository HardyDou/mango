import { expect, test, type APIRequestContext, type APIResponse } from '@playwright/test';
import { api } from '../support/api';

type ApiBody<T> = {
  code?: number;
  success?: boolean;
  data?: T;
  msg?: string;
};

type LoginData = {
  accessToken?: string;
  token?: string;
  passwordResetRequired?: boolean;
  loginAction?: string;
  passwordResetTicket?: string;
};

type FileRecord = {
  id: string | number;
  fileName: string;
  previewUrl?: string;
  downloadUrl?: string;
};

type FilePreview = {
  id: string | number;
  fileName: string;
  previewUrl?: string;
  downloadUrl?: string;
  directAccess?: boolean;
  directPreviewUrl?: string;
  directDownloadUrl?: string;
  directPreviewExpireSeconds?: number;
  directDownloadExpireSeconds?: number;
};

type FilePreviewLink = {
  fileId: string | number;
  previewUrl: string;
  previewToken: string;
  expireSeconds: number;
};

type FileSettings = {
  accessMode?: string;
  accessTokenEnabled?: boolean;
  publicReadRequiresToken?: boolean;
  accessTokenExpireSeconds?: number;
  previewExpireSeconds?: number;
  maxSize?: number;
  defaultAccessLevel?: string;
  duplicateNameStrategy?: string;
};

type IdentityUser = {
  userId?: string | number;
  memberId?: string | number;
  username: string;
};

const ordinaryPassword = 'E2E@123456';

test.describe('文件访问基线权限矩阵 @p0 @file @permission', () => {
  test('匿名、普通登录用户和管理员的文件基础能力边界符合基线', async ({ request, playwright }) => {
    test.setTimeout(90_000);

    const unique = `${Date.now()}-${test.info().workerIndex}`;
    const username = `E2E_FILE_BASE_${unique}`;
    const adminToken = await loginToken(request, 'admin', 'admin123');
    let ordinaryToken = '';
    const uploadedFileIds: string[] = [];

    try {
      await cleanupUser(request, adminToken, username);
      const changedPassword = `E2E@${unique}Aa1`;
      await createTempUser(request, adminToken, username);
      ordinaryToken = await loginToken(request, username, ordinaryPassword, changedPassword);

      const anonymousRequest = await playwright.request.newContext();
      await expectDenied(anonymousRequest.post(api('/file/files'), {
        multipart: {
          file: {
            name: `anonymous-${unique}.txt`,
            mimeType: 'text/plain',
            buffer: Buffer.from(`anonymous-${unique}`),
          },
          purpose: 'attachment',
          accessLevel: 'PRIVATE',
        },
      }), '匿名用户不能上传文件');
      await anonymousRequest.dispose();

      const settingsResponse = await request.get(api('/file/settings'), {
        headers: authHeaders(ordinaryToken),
      });
      const settings = await expectBusinessOk<FileSettings>(settingsResponse, '普通登录用户读取文件设置失败');
      expect(settings.accessMode).toBe('DIRECT');
      expect(settings.accessTokenEnabled).toBe(true);
      expect(settings.publicReadRequiresToken).toBe(true);
      expect(Number(settings.accessTokenExpireSeconds)).toBe(86400);
      expect(Number(settings.previewExpireSeconds)).toBe(86400);

      const uploadResponse = await request.post(api('/file/files'), {
        headers: authHeaders(ordinaryToken),
        multipart: {
          file: {
            name: `mango-file-access-baseline-${unique}.txt`,
            mimeType: 'text/plain',
            buffer: Buffer.from(`file access baseline ${unique}`),
          },
          purpose: 'attachment',
          accessLevel: 'PRIVATE',
          bizType: 'mango-file-access-baseline',
          bizId: unique,
        },
      });
      const uploaded = await expectBusinessOk<FileRecord>(uploadResponse, '普通登录用户上传文件失败');
      uploadedFileIds.push(String(uploaded.id));
      expect(uploaded.fileName).toContain('mango-file-access-baseline');
      expectRuntimeUrl(uploaded.previewUrl, '上传响应 previewUrl');
      expectRuntimeUrl(uploaded.downloadUrl, '上传响应 downloadUrl');

      const detail = await expectBusinessOk<FileRecord>(
        await request.get(api(`/file/files/detail?id=${encodeURIComponent(String(uploaded.id))}`), {
          headers: authHeaders(ordinaryToken),
        }),
        '普通登录用户读取文件详情失败',
      );
      expect(String(detail.id)).toBe(String(uploaded.id));
      expectRuntimeUrl(detail.previewUrl, '详情响应 previewUrl');
      expectRuntimeUrl(detail.downloadUrl, '详情响应 downloadUrl');

      const preview = await expectBusinessOk<FilePreview>(
        await request.get(api(`/file/files/preview?id=${encodeURIComponent(String(uploaded.id))}`), {
          headers: authHeaders(ordinaryToken),
        }),
        '普通登录用户读取文件预览元数据失败',
      );
      expect(String(preview.id)).toBe(String(uploaded.id));
      expect(preview.directAccess).toBe(true);
      expect(Number(preview.directPreviewExpireSeconds)).toBe(86400);
      expect(Number(preview.directDownloadExpireSeconds)).toBe(86400);
      expectRuntimeUrl(preview.previewUrl, '预览响应 previewUrl');
      expectRuntimeUrl(preview.downloadUrl, '预览响应 downloadUrl');
      expectRuntimeUrl(preview.directPreviewUrl, '预览响应 directPreviewUrl');
      expectRuntimeUrl(preview.directDownloadUrl, '预览响应 directDownloadUrl');

      const previewContentResponse = await request.get(
        api(`/file/files/preview-content?id=${encodeURIComponent(String(uploaded.id))}`),
        { headers: authHeaders(ordinaryToken) },
      );
      expect(previewContentResponse.status()).toBe(200);
      expect(await previewContentResponse.text()).toBe(`file access baseline ${unique}`);

      const previewLink = await expectBusinessOk<FilePreviewLink>(
        await request.get(api(`/file-preview/files/preview-link?fileId=${encodeURIComponent(String(uploaded.id))}`), {
          headers: authHeaders(ordinaryToken),
        }),
        '普通登录用户创建统一文件预览链接失败',
      );
      expect(String(previewLink.fileId)).toBe(String(uploaded.id));
      expect(previewLink.previewUrl).toContain('/file-preview/files/preview-entry?token=');
      expect(previewLink.previewToken).toBeTruthy();
      expect(Number(previewLink.expireSeconds)).toBeGreaterThan(0);

      const previewPageResponse = await request.get(
        new URL(previewLink.previewUrl, 'http://127.0.0.1:30003').toString(),
      );
      expect(previewPageResponse.status()).toBe(200);
      expect(previewPageResponse.headers()['content-type']).toContain('text/html');

      const anonymousPreviewRequest = await playwright.request.newContext();
      await expectDenied(
        anonymousPreviewRequest.get(api(`/file-preview/files/preview-link?fileId=${encodeURIComponent(String(uploaded.id))}`)),
        '匿名用户不能创建统一文件预览链接',
      );
      await anonymousPreviewRequest.dispose();

      const downloadResponse = await request.get(api(`/file/files/download?id=${encodeURIComponent(String(uploaded.id))}`), {
        headers: authHeaders(ordinaryToken),
      });
      expect(downloadResponse.status()).toBe(200);
      expect((await downloadResponse.body()).byteLength).toBeGreaterThan(0);

      const packageResponse = await request.post(api('/file/files/package'), {
        headers: authHeaders(ordinaryToken),
        data: {
          fileName: `mango-file-access-baseline-${unique}.zip`,
          purpose: 'attachment',
          accessLevel: 'PRIVATE',
          bizType: 'mango-file-access-baseline',
          bizId: `${unique}-package`,
          entries: [
            {
              fileId: String(uploaded.id),
              path: '${fileName}',
            },
          ],
        },
      });
      const packaged = await expectBusinessOk<FileRecord>(packageResponse, '普通登录用户打包文件失败');
      uploadedFileIds.push(String(packaged.id));
      expect(packaged.fileName).toContain('.zip');
      expectRuntimeUrl(packaged.previewUrl, '打包响应 previewUrl');
      expectRuntimeUrl(packaged.downloadUrl, '打包响应 downloadUrl');

      await expectDenied(request.get(api('/file/files/page?page=1&size=10'), {
        headers: authHeaders(ordinaryToken),
      }), '普通登录用户不能访问文件管理列表');
      await expectDenied(request.delete(api(`/file/files?id=${encodeURIComponent(String(uploaded.id))}&reason=e2e-denied`), {
        headers: authHeaders(ordinaryToken),
      }), '普通登录用户不能归档文件');
      await expectDenied(request.post(api('/file/files/delete'), {
        headers: authHeaders(ordinaryToken),
        data: { ids: [String(uploaded.id)] },
      }), '普通登录用户不能删除文件');
      await expectDenied(request.put(api('/file/settings'), {
        headers: authHeaders(ordinaryToken),
        data: settingsSavePayload(settings),
      }), '普通登录用户不能保存文件设置');
    } finally {
      for (const id of uploadedFileIds) {
        await request.post(api('/file/files/delete'), {
          headers: authHeaders(adminToken),
          data: { ids: [String(id)] },
        }).catch(() => undefined);
      }
      await cleanupUser(request, adminToken, username);
    }
  });
});

async function loginToken(request: APIRequestContext, username: string, password: string, changedPassword?: string) {
  const response = await request.post(api('/auth/login'), {
    data: {
      username,
      password,
      tenantId: '1',
      tenantCode: 'default',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  const data = await expectBusinessOk<LoginData>(response, `登录失败: ${username}`);
  if ((data.passwordResetRequired || data.loginAction === 'CHANGE_PASSWORD') && data.passwordResetTicket) {
    expect(changedPassword, `账号 ${username} 要求首次改密，但测试未提供新密码`).toBeTruthy();
    const changeResponse = await request.post(api('/auth/password/change-required'), {
      data: {
        passwordResetTicket: data.passwordResetTicket,
        newPassword: changedPassword,
        confirmPassword: changedPassword,
      },
    });
    const changed = await expectBusinessOk<LoginData>(changeResponse, `首次改密失败: ${username}`);
    const changedToken = changed.accessToken || changed.token;
    expect(changedToken, `首次改密后未返回 token: ${username}`).toBeTruthy();
    return changedToken!;
  }
  const token = data.accessToken || data.token;
  expect(token, `登录未返回 token: ${username}`).toBeTruthy();
  return token!;
}

async function createTempUser(request: APIRequestContext, adminToken: string, username: string) {
  const response = await request.post(api('/identity/users'), {
    headers: authHeaders(adminToken),
    data: {
      username,
      password: ordinaryPassword,
      nickname: username,
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      partyId: 1,
      email: `${username.toLowerCase()}@example.com`,
      phone: '13800000009',
      status: 1,
      remark: 'E2E文件访问基线临时用户',
    },
  });
  await expectBusinessOk<unknown>(response, `创建临时用户失败: ${username}`);
}

async function cleanupUser(request: APIRequestContext, adminToken: string, username: string) {
  const response = await request.get(api('/identity/users/page'), {
    headers: authHeaders(adminToken),
    params: { page: 1, size: 20, username },
  });
  if (!response.ok()) {
    return;
  }
  const body = await response.json() as ApiBody<{ records?: IdentityUser[]; list?: IdentityUser[] }>;
  const records = body.data?.records || body.data?.list || [];
  for (const user of records.filter((item) => item.username === username)) {
    const userId = user.userId || user.memberId;
    if (!userId) {
      continue;
    }
    await request.delete(api(`/identity/users?userId=${encodeURIComponent(String(userId))}`), {
      headers: authHeaders(adminToken),
    }).catch(() => undefined);
  }
}

function authHeaders(token: string) {
  return {
    Authorization: `Bearer ${token}`,
    'TENANT-ID': '1',
    'X-Mango-Tenant-Id': '1',
    'X-Tenant-Id': '1',
  };
}

async function expectBusinessOk<T>(response: APIResponse, message: string) {
  const body = await response.json() as ApiBody<T>;
  expect(response.status(), `${message}: ${JSON.stringify(body)}`).toBe(200);
  expect(body.success || body.code === 200, `${message}: ${body.msg || JSON.stringify(body)}`).toBeTruthy();
  return body.data as T;
}

async function expectDenied(responsePromise: Promise<APIResponse>, message: string) {
  const response = await responsePromise;
  if (response.status() === 401 || response.status() === 403) {
    return;
  }
  let body: ApiBody<unknown> | undefined;
  try {
    body = await response.json() as ApiBody<unknown>;
  } catch {
    body = undefined;
  }
  expect(
    body?.success === false || body?.code === 401 || body?.code === 403,
    `${message}: HTTP ${response.status()} ${JSON.stringify(body)}`,
  ).toBeTruthy();
}

function expectRuntimeUrl(value: string | undefined, label: string) {
  expect(value, `${label} 不能为空`).toBeTruthy();
  expect(value || '', `${label} 不能退回后端下载接口`).not.toContain('/api/file/files/download');
  const expires = readQueryParam(value || '', 'X-Amz-Expires');
  if (expires) {
    expect(expires, `${label} 预签名有效期`).toBe('86400');
  }
}

function readQueryParam(value: string, name: string) {
  try {
    const parsed = new URL(value, 'http://127.0.0.1');
    return parsed.searchParams.get(name);
  } catch {
    return null;
  }
}

function settingsSavePayload(settings: FileSettings) {
  return {
    maxSize: settings.maxSize || 104857600,
    allowedExtensions: ['txt', 'png', 'jpg', 'pdf'],
    blockedExtensions: ['exe', 'bat', 'cmd', 'sh', 'jar'],
    defaultAccessLevel: settings.defaultAccessLevel || 'PRIVATE',
    duplicateNameStrategy: settings.duplicateNameStrategy || 'AUTO_RENAME',
    duplicateCheckDirectoryScoped: true,
    objectNameStrategy: 'DATE_UUID',
    instantUploadEnabled: true,
    instantUploadScope: 'TENANT',
    contentTypeCheckEnabled: false,
    allowedContentTypes: [],
    blockedContentTypes: [],
    directUploadEnabled: false,
    directUploadExpireSeconds: 600,
    accessTokenEnabled: true,
    publicReadRequiresToken: true,
    accessMode: 'DIRECT',
    accessTokenExpireSeconds: 86400,
    previewProviderUrl: '/file-preview/files/preview',
    previewExpireSeconds: 86400,
    previewExternalExtensions: ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'],
    archiveRetainEnabled: true,
    archiveRetainDays: 180,
    archiveRestoreEnabled: false,
    physicalDeleteEnabled: false,
  };
}
