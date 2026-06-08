import { expect, test, type APIRequestContext } from '@playwright/test';
import { api as e2eApi } from '../support/api';

type ApiResponse<T> = {
  code?: number;
  success?: boolean;
  data: T;
};

type FileRecord = {
  id: string;
  url?: string;
  previewUrl?: string;
  downloadUrl?: string;
  directPreviewUrl?: string;
  directDownloadUrl?: string;
};

async function loginToken(request: APIRequestContext) {
  const response = await request.post(e2eApi('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: '1',
      tenantCode: 'default',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json() as ApiResponse<{ accessToken: string }>;
  expect(body.success || body.code === 200).toBeTruthy();
  return body.data.accessToken;
}

function authHeaders(token: string) {
  return {
    Authorization: `Bearer ${token}`,
    'X-Tenant-Id': '1',
    'X-Forwarded-Proto': 'https',
    'X-Forwarded-Host': 'files.example.com',
    'X-Forwarded-Port': '443',
    'X-Forwarded-Prefix': '/api',
  };
}

function expectExternalUrls(record: Pick<FileRecord, 'url' | 'previewUrl' | 'downloadUrl' | 'directPreviewUrl' | 'directDownloadUrl'>) {
  for (const value of [record.url, record.previewUrl, record.downloadUrl, record.directPreviewUrl, record.directDownloadUrl]) {
    if (!value) {
      continue;
    }
    expect(value).toMatch(/^https:\/\/files\.example\.com\/api\//);
  }
}

test('file upload/detail/preview responses return external proxy urls', async ({ request }) => {
  const token = await loginToken(request);
  const suffix = `${Date.now()}-${test.info().workerIndex}`;
  const formData = {
    file: {
      name: `issue-99-upload-${suffix}.txt`,
      mimeType: 'text/plain',
      buffer: Buffer.from(`issue-99-${suffix}`),
    },
    purpose: 'attachment',
    accessLevel: 'PRIVATE',
  };

  const uploadResponse = await request.post(e2eApi('/file/files'), {
    headers: authHeaders(token),
    multipart: formData,
  });
  expect(uploadResponse.status()).toBe(200);
  const uploadBody = await uploadResponse.json() as ApiResponse<FileRecord>;
  expect(uploadBody.success || uploadBody.code === 200).toBeTruthy();
  expectExternalUrls(uploadBody.data);

  const detailResponse = await request.get(e2eApi(`/file/files/detail?id=${uploadBody.data.id}`), {
    headers: authHeaders(token),
  });
  expect(detailResponse.status()).toBe(200);
  const detailBody = await detailResponse.json() as ApiResponse<FileRecord>;
  expectExternalUrls(detailBody.data);

  const previewResponse = await request.get(e2eApi(`/file/files/preview?id=${uploadBody.data.id}`), {
    headers: authHeaders(token),
  });
  expect(previewResponse.status()).toBe(200);
  const previewBody = await previewResponse.json() as ApiResponse<FileRecord>;
  expect(previewBody.success || previewBody.code === 200).toBeTruthy();
  expectExternalUrls(previewBody.data);
  expect(previewBody.data.downloadUrl).toBe(uploadBody.data.downloadUrl);

  const batchResponse = await request.post(e2eApi('/file/files/batch'), {
    headers: authHeaders(token),
    multipart: {
      files: {
        name: `issue-99-batch-${suffix}.txt`,
        mimeType: 'text/plain',
        buffer: Buffer.from(`issue-99-batch-${suffix}`),
      },
      purpose: 'attachment',
      accessLevel: 'PRIVATE',
    },
  });
  expect(batchResponse.status()).toBe(200);
  const batchBody = await batchResponse.json() as ApiResponse<FileRecord[]>;
  expect(batchBody.success || batchBody.code === 200).toBeTruthy();
  expect(batchBody.data).toHaveLength(1);
  expectExternalUrls(batchBody.data[0]);
});
