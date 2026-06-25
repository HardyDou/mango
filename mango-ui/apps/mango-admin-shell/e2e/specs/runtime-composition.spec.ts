import { expect, test, type Page } from '@playwright/test';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const runtimeConfigPath = resolve(__dirname, '../../public/runtime-config.json');
const distRuntimeConfigPath = resolve(__dirname, '../../dist/runtime-config.json');
const shellOrigin = new URL(process.env.PLAYWRIGHT_BASE_URL || 'http://a.mango.io:5176').origin;
const rbacEntry = process.env.PLAYWRIGHT_RBAC_ENTRY || resolvePeerEntry('b.mango.io', 5181, 4181);
const workflowEntry = process.env.PLAYWRIGHT_WORKFLOW_ENTRY || resolvePeerEntry('c.mango.io', 5182, 4182);
const cmsEntry = process.env.PLAYWRIGHT_CMS_ENTRY || resolvePeerEntry('e.mango.io', 5184, 4184);
const brokenRbacEntry = rbacEntry.replace(/:\d+\//, ':5999/');
const failClosedRuntimeConfig = new URL(shellOrigin).port === '4176';
let originalRuntimeConfig = '';
let originalDistRuntimeConfig = '';

const hybridConfig = {
  profile: 'hybrid',
  modules: {
    'mango-authorization': {
      mode: 'micro',
      runtimeCode: 'mango-admin-rbac-app',
      entry: rbacEntry,
    },
    'mango-system': {
      mode: 'local',
      runtimeCode: 'mango-admin-system-local',
    },
    'mango-workflow': {
      mode: 'micro',
      runtimeCode: 'mango-admin-workflow-app',
      entry: workflowEntry,
    },
    'mango-cms': {
      mode: 'micro',
      runtimeCode: 'mango-admin-cms-app',
      entry: cmsEntry,
    },
  },
};

const monolithConfig = {
  profile: 'monolith',
  modules: {
    'mango-authorization': {
      mode: 'local',
      runtimeCode: 'mango-admin-rbac-local',
    },
    'mango-system': {
      mode: 'local',
      runtimeCode: 'mango-admin-system-local',
    },
    'mango-workflow': {
      mode: 'local',
      runtimeCode: 'mango-admin-workflow-local',
    },
    'mango-cms': {
      mode: 'local',
      runtimeCode: 'mango-admin-cms-local',
    },
  },
};

const brokenHybridConfig = {
  profile: 'hybrid',
  modules: {
    'mango-authorization': {
      mode: 'micro',
      runtimeCode: 'mango-admin-rbac-app',
      entry: brokenRbacEntry,
      timeoutMs: 1000,
    },
    'mango-system': {
      mode: 'local',
      runtimeCode: 'mango-admin-system-local',
    },
    'mango-workflow': {
      mode: 'local',
      runtimeCode: 'mango-admin-workflow-local',
    },
  },
};

const missingEntryHybridConfig = {
  profile: 'hybrid',
  modules: {
    'mango-authorization': {
      mode: 'micro',
      runtimeCode: 'mango-admin-rbac-app',
      entry: rbacEntry,
    },
    'mango-system': {
      mode: 'local',
      runtimeCode: 'mango-admin-system-local',
    },
    'mango-workflow': {
      mode: 'micro',
      runtimeCode: 'mango-admin-workflow-app',
    },
  },
};

const invalidModeConfig = {
  profile: 'hybrid',
  modules: {
    'mango-authorization': {
      mode: 'remote',
      runtimeCode: 'mango-admin-rbac-app',
      entry: rbacEntry,
    },
    'mango-system': {
      mode: 'local',
      runtimeCode: 'mango-admin-system-local',
    },
    'mango-workflow': {
      mode: 'local',
      runtimeCode: 'mango-admin-workflow-local',
    },
  },
};

test.describe.serial('Shell runtime composition', () => {
  test.beforeAll(() => {
    originalRuntimeConfig = readFileSync(runtimeConfigPath, 'utf-8');
    if (existsSync(distRuntimeConfigPath)) {
      originalDistRuntimeConfig = readFileSync(distRuntimeConfigPath, 'utf-8');
    }
  });

  test.afterAll(() => {
    if (originalRuntimeConfig) {
      writeFileSync(runtimeConfigPath, originalRuntimeConfig);
    }
    if (originalDistRuntimeConfig && existsSync(distRuntimeConfigPath)) {
      writeFileSync(distRuntimeConfigPath, originalDistRuntimeConfig);
    }
  });

  test('hybrid profile loads RBAC and Workflow from remote micro apps', async ({ page }) => {
    writeRuntimeConfig(hybridConfig);
    await login(page);

    await page.goto('/#/system/menu-package');
    await page.waitForURL('**/#/system/menu-package**', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-authorization',
      runtimeCode: 'mango-admin-rbac-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: new URL(rbacEntry).host,
    });
    await expect(page.getByText('新增套餐')).toBeVisible();
    await expectRemoteResource(page, new URL(rbacEntry).host);
    await expectBusinessSmoke(page, 'rbac');

    await page.getByRole('button', { name: /审批中心/ }).click({ force: true });
    await page.waitForURL('**/#/workflow/start-process', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-workflow',
      runtimeCode: 'mango-admin-workflow-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: new URL(workflowEntry).host,
    });
    await expect(page.locator('main')).toContainText('发起流程');
    await expectRemoteResource(page, new URL(workflowEntry).host);
    await expectBusinessSmoke(page, 'workflow');

    await page.goto('/#/system/menu-package');
    await page.waitForURL('**/#/system/menu-package**', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-authorization',
      runtimeCode: 'mango-admin-rbac-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: new URL(rbacEntry).host,
    });
    await expect(page.getByText('新增套餐')).toBeVisible();
    await expectBusinessSmoke(page, 'rbac');

    await page.goto('/#/cms/sites');
    await page.waitForURL('**/#/cms/sites**', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-cms',
      runtimeCode: 'mango-admin-cms-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: new URL(cmsEntry).host,
    });
    await expect(page.locator('main')).toContainText('站点管理');
    await expectRemoteResource(page, new URL(cmsEntry).host);
    await expectBusinessSmoke(page, 'cms');
  });

  test('monolith profile renders modules locally without loading remote apps', async ({ page }) => {
    writeRuntimeConfig(monolithConfig);
    await login(page);

    await page.goto('/#/system/menu-package');
    await page.waitForURL('**/#/system/menu-package**', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-authorization',
      runtimeCode: 'mango-admin-rbac-local',
      pageType: 'LOCAL_ROUTE',
    });
    await expect(page.getByText('新增套餐')).toBeVisible();
    await expectBusinessSmoke(page, 'rbac');

    await page.getByRole('button', { name: /审批中心/ }).click({ force: true });
    await page.waitForURL('**/#/workflow/start-process', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-workflow',
      runtimeCode: 'mango-admin-workflow-local',
      pageType: 'LOCAL_ROUTE',
    });
    await expect(page.locator('main')).toContainText('发起流程');
    await expectBusinessSmoke(page, 'workflow');

    await page.goto('/#/cms/sites');
    await page.waitForURL('**/#/cms/sites**', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-cms',
      runtimeCode: 'mango-admin-cms-local',
      pageType: 'LOCAL_ROUTE',
    });
    await expect(page.locator('main')).toContainText('站点管理');
    await expectBusinessSmoke(page, 'cms');

    const remoteResources = await remoteRuntimeResources(page);
    expect(remoteResources).toEqual([]);
  });

  test('broken remote app shows an actionable runtime error', async ({ page }) => {
    writeRuntimeConfig(brokenHybridConfig);
    await login(page);

    await page.goto('/#/system/menu-package');
    await page.waitForURL('**/#/system/menu-package**', { timeout: 10000 });
    if (failClosedRuntimeConfig) {
      await expect(page.getByText('运行配置加载失败')).toBeVisible();
      await expectRuntimeDiagnostic(page, {
        moduleCode: 'mango-authorization',
        field: 'entry',
        level: 'error',
      });
      return;
    }

    await expectRuntime(page, {
      moduleCode: 'mango-authorization',
      runtimeCode: 'mango-admin-rbac-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: new URL(brokenRbacEntry).host,
    });
    await expect(page.getByText('页面加载失败')).toBeVisible();
    await expect(page.getByText(/运行单元：mango-admin-rbac-app/)).toBeVisible();
    await expect(page.getByText(new RegExp(`入口地址：${escapeRegExp(brokenRbacEntry)}`))).toBeVisible();
    await expect(page.getByRole('button', { name: '重试' })).toBeVisible();
  });

  test('missing remote entry does not fall back to another micro app', async ({ page }) => {
    writeRuntimeConfig(missingEntryHybridConfig);
    await login(page);

    await page.getByRole('button', { name: /审批中心/ }).click({ force: true });
    await page.waitForURL('**/#/workflow/start-process', { timeout: 10000 });
    if (failClosedRuntimeConfig) {
      await expect(page.getByText('运行配置加载失败')).toBeVisible();
      await expect(page.locator('main')).not.toContainText('新增套餐');
      return;
    }
    await expectRuntime(page, {
      moduleCode: 'mango-workflow',
      runtimeCode: 'mango-admin-workflow-app',
      pageType: 'MICRO_ROUTE',
    });
    await expect(page.getByText('缺少微应用运行配置：mango-admin-workflow-app')).toBeVisible();
    await expect(page.getByText(/Micro module 'mango-workflow' is missing entry/)).toBeVisible();
    await expectRuntimeDiagnostic(page, {
      moduleCode: 'mango-workflow',
      field: 'entry',
      level: 'error',
    });
    await expect(page.locator('main')).not.toContainText('新增套餐');
  });

  test('invalid runtime mode falls back to local rendering with diagnostics', async ({ page }) => {
    writeRuntimeConfig(invalidModeConfig);
    await login(page);

    await page.goto('/#/system/menu-package');
    await page.waitForURL('**/#/system/menu-package**', { timeout: 10000 });
    if (failClosedRuntimeConfig) {
      await expect(page.getByText('运行配置加载失败')).toBeVisible();
      const remoteResources = await remoteRuntimeResources(page);
      expect(remoteResources).toEqual([]);
      return;
    }

    await expectRuntime(page, {
      moduleCode: 'mango-authorization',
      runtimeCode: 'mango-admin-rbac-app',
      pageType: 'LOCAL_ROUTE',
    });
    await expect(page.getByText('新增套餐')).toBeVisible();
    await expectRuntimeDiagnostic(page, {
      moduleCode: 'mango-authorization',
      field: 'mode',
      level: 'error',
    });
    const remoteResources = await remoteRuntimeResources(page);
    expect(remoteResources).toEqual([]);
  });

  test('micro app unauthorized event is handled by the shell', async ({ page }) => {
    writeRuntimeConfig(hybridConfig);
    await login(page);

    await page.evaluate(() => {
      (window as any).__MANGO_RUNTIME_EVENT_BUS__.emit('unauthorized');
    });

    await page.waitForURL('**/#/login', { timeout: 10000 });
    await expect(page.getByPlaceholder('用户名')).toBeVisible();
    const token = await page.evaluate(() => sessionStorage.getItem('MANGO_TOKEN'));
    expect(token).toBeNull();
  });
});

function writeRuntimeConfig(config: unknown) {
  const content = `${JSON.stringify(config, null, 2)}\n`;
  writeFileSync(runtimeConfigPath, content);
  if (existsSync(distRuntimeConfigPath)) {
    writeFileSync(distRuntimeConfigPath, content);
  }
}

async function login(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.getByPlaceholder('密码').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.getByRole('button', { name: /^登\s*录$/ }).click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
  await expect(page.locator('.shell-runtime-content')).toBeVisible();
}

async function expectRuntime(
  page: Page,
  expected: {
    moduleCode: string;
    runtimeCode: string;
    pageType: string;
    entryIncludes?: string;
  }
) {
  await expect.poll(async () => {
    return page.locator('.shell-runtime-content').evaluate((el) => ({
      moduleCode: (el as HTMLElement).dataset.mangoRuntimeModule,
      runtimeCode: (el as HTMLElement).dataset.mangoRuntimeCode,
      pageType: (el as HTMLElement).dataset.mangoRuntimePageType,
      entry: (el as HTMLElement).dataset.mangoRuntimeEntry,
    }));
  }).toMatchObject({
    moduleCode: expected.moduleCode,
    runtimeCode: expected.runtimeCode,
    pageType: expected.pageType,
  });

  if (expected.entryIncludes) {
    const entry = await page.locator('.shell-runtime-content').evaluate((el) =>
      (el as HTMLElement).dataset.mangoRuntimeEntry || ''
    );
    expect(entry).toContain(expected.entryIncludes);
  }
}

async function expectRemoteResource(page: Page, urlPart: string) {
  await expect.poll(async () => {
    const resources = await remoteRuntimeResources(page);
    if (resources.some((url) => url.includes(urlPart))) {
      return true;
    }
    const runtimeEvidence = await page.evaluate((part) => {
      const active = (window as any).__MANGO_ACTIVE_MICRO_APP__;
      const events = ((window as any).__MANGO_MICRO_APP_EVENTS__ || []) as Array<{ entryUrl?: string }>;
      return Boolean(active?.entryUrl?.includes(part) || events.some((event) => event.entryUrl?.includes(part)));
    }, urlPart);
    return runtimeEvidence;
  }).toBeTruthy();
}

async function expectBusinessSmoke(page: Page, module: 'rbac' | 'workflow' | 'cms') {
  if (module === 'rbac') {
    await page.goto('/#/system/role');
    await expectRuntime(page, {
      moduleCode: 'mango-authorization',
      runtimeCode: await currentRuntimeCode(page),
      pageType: await currentPageType(page),
    });
    await expect(page.getByText('新增角色')).toBeVisible();
    await expect(page.getByText('角色名称')).toBeVisible();

    await page.goto('/#/system/menu');
    await expect(page.getByRole('button', { name: '新增菜单' })).toBeVisible();
    await expect(page.getByText('菜单名称')).toBeVisible();
    return;
  }

  if (module === 'cms') {
    await page.goto('/#/cms/content-categories');
    await expectRuntime(page, {
      moduleCode: 'mango-cms',
      runtimeCode: await currentRuntimeCode(page),
      pageType: await currentPageType(page),
    });
    await expect(page.locator('main')).toContainText('内容分类');
    await expect(page.getByRole('button', { name: '新增' })).toBeVisible();

    await page.goto('/#/cms/ad-deliveries');
    await expect(page.locator('main')).toContainText('广告投放管理');
    await expect(page.getByRole('button', { name: '新增' })).toBeVisible();
    return;
  }

  await page.goto('/#/workflow/task/initiated');
  await expectRuntime(page, {
    moduleCode: 'mango-workflow',
    runtimeCode: await currentRuntimeCode(page),
    pageType: await currentPageType(page),
  });
  await expect(page.locator('main')).toContainText('我的申请');
  await expect(page.locator('main')).toContainText('当前用户发起的流程实例');

  await page.goto('/#/workflow/task/done');
  await expect(page.locator('main')).toContainText('我的已办');
  await expect(page.locator('main')).toContainText('当前用户已经处理完成的流程任务');
}

async function currentRuntimeCode(page: Page) {
  return page.locator('.shell-runtime-content').evaluate((el) =>
    (el as HTMLElement).dataset.mangoRuntimeCode || ''
  );
}

async function currentPageType(page: Page) {
  return page.locator('.shell-runtime-content').evaluate((el) =>
    (el as HTMLElement).dataset.mangoRuntimePageType || ''
  );
}

async function expectRuntimeDiagnostic(
  page: Page,
  expected: {
    moduleCode: string;
    field: string;
    level: string;
  }
) {
  await expect.poll(async () => {
    return page.evaluate((item) => {
      const diagnostics = ((window as any).__MANGO_RUNTIME_CONFIG_DIAGNOSTICS__ || []) as Array<{
        moduleCode?: string;
        field?: string;
        level?: string;
      }>;
      return diagnostics.some(diagnostic =>
        diagnostic.moduleCode === item.moduleCode
        && diagnostic.field === item.field
        && diagnostic.level === item.level
      );
    }, expected);
  }).toBeTruthy();
}

async function remoteRuntimeResources(page: Page) {
  return page.evaluate(() =>
    performance
      .getEntriesByType('resource')
      .map((entry) => entry.name)
      .filter((url) =>
        url.includes('b.mango.io:5181')
        || url.includes('c.mango.io:5182')
        || url.includes('e.mango.io:5184')
        || url.includes('b.mango.io:4181')
        || url.includes('c.mango.io:4182')
        || url.includes('e.mango.io:4184')
      )
  );
}

function resolvePeerEntry(hostname: string, devPort: number, previewPort: number) {
  const shellUrl = new URL(shellOrigin);
  const port = shellUrl.port === '4176' ? previewPort : devPort;
  return `${shellUrl.protocol}//${hostname}:${port}/`;
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

async function closeTag(page: Page, title: string) {
  const tag = page.locator('.tags-view-item', { hasText: title }).last();
  await expect(tag).toBeVisible();
  await tag.locator('.close-icon').click();
}
