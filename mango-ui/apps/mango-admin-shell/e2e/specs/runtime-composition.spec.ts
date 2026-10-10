import { expect, test, type Page } from '@playwright/test';
import { login } from '../support/login';
import { cmsPage, waitCmsReady } from '../support/element-plus';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const runtimeConfigPath =
  process.env.PLAYWRIGHT_RUNTIME_CONFIG_PATH || resolve(__dirname, '../../runtime-config.dev.json');
const distRuntimeConfigPath = resolve(__dirname, '../../dist/runtime-config.json');
const shellOrigin = new URL(process.env.PLAYWRIGHT_BASE_URL || 'http://a.mango.io:5176').origin;
const rbacEntry = process.env.PLAYWRIGHT_RBAC_ENTRY || resolvePeerEntry('b.mango.io', 5181, 4181);
const workflowEntry = process.env.PLAYWRIGHT_WORKFLOW_ENTRY || resolvePeerEntry('c.mango.io', 5182, 4182);
const cmsEntry = process.env.PLAYWRIGHT_CMS_ENTRY || resolvePeerEntry('e.mango.io', 5184, 4184);
const brokenRbacEntry = rbacEntry.replace(/:\d+\//, ':5999/');
const failClosedRuntimeConfig = new URL(shellOrigin).port === '4176';
let originalRuntimeConfig = '';
let originalDistRuntimeConfig = '';

type MangoRuntimeWindow = Window & {
  __MANGO_RUNTIME_EVENT_BUS__?: { emit(event: string): void };
  __MANGO_ACTIVE_MICRO_APP__?: { entryUrl?: string };
  __MANGO_MICRO_APP_EVENTS__?: Array<{ entryUrl?: string }>;
  __MANGO_RUNTIME_CONFIG_DIAGNOSTICS__?: Array<{
    moduleCode?: string;
    field?: string;
    level?: string;
  }>;
};

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

  test('@p0 @runtime hybrid profile loads RBAC and Workflow from remote micro apps', async ({ page }) => {
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
    await expectBusinessSmoke(page, 'rbac', 'micro');

    // Keep the RBAC role tab open while switching to another micro app. Returning
    // to it must restore rendered content instead of an empty cached host.
    await page.goto('/#/system/role');
    await expectRuntime(page, {
      moduleCode: 'mango-authorization',
      runtimeCode: 'mango-admin-rbac-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: new URL(rbacEntry).host,
    });
    await expect(page.getByRole('button', { name: '新增角色', exact: true })).toBeVisible();

    await page.goto('/#/workflow/start-process');
    await page.waitForURL('**/#/workflow/start-process', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-workflow',
      runtimeCode: 'mango-admin-workflow-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: new URL(workflowEntry).host,
    });
    await expect(page.locator('[data-page="workflow.start-process"]')).toContainText('已发布流程');
    await expectRemoteResource(page, new URL(workflowEntry).host);
    await expectBusinessSmoke(page, 'workflow', 'micro');

    await page.goto('/#/system/role');
    await expectRuntime(page, {
      moduleCode: 'mango-authorization',
      runtimeCode: 'mango-admin-rbac-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: new URL(rbacEntry).host,
    });
    await expect(page.getByRole('button', { name: '新增角色', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '角色名称', exact: true })).toBeVisible();

    await page.goto('/#/system/menu-package');
    await page.waitForURL('**/#/system/menu-package**', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-authorization',
      runtimeCode: 'mango-admin-rbac-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: new URL(rbacEntry).host,
    });
    await expect(page.getByText('新增套餐')).toBeVisible();
    await expectBusinessSmoke(page, 'rbac', 'micro');

    await page.goto('/#/cms/sites');
    await page.waitForURL('**/#/cms/sites**', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-cms',
      runtimeCode: 'mango-admin-cms-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: new URL(cmsEntry).host,
    });
    await waitCmsReady(page);
    await expect(cmsPage(page).getByRole('columnheader', { name: '站点', exact: true })).toBeVisible();
    await expectRemoteResource(page, new URL(cmsEntry).host);
    await expectBusinessSmoke(page, 'cms', 'micro');
  });

  test('@p0 @runtime monolith profile renders modules locally without loading remote apps', async ({ page }) => {
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
    await expectBusinessSmoke(page, 'rbac', 'local');

    await page.goto('/#/workflow/start-process');
    await page.waitForURL('**/#/workflow/start-process', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-workflow',
      runtimeCode: 'mango-admin-workflow-local',
      pageType: 'LOCAL_ROUTE',
    });
    await expect(page.locator('[data-page="workflow.start-process"]')).toContainText('已发布流程');
    await expectBusinessSmoke(page, 'workflow', 'local');

    await page.goto('/#/cms/sites');
    await page.waitForURL('**/#/cms/sites**', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-cms',
      runtimeCode: 'mango-admin-cms-local',
      pageType: 'LOCAL_ROUTE',
    });
    await waitCmsReady(page);
    await expect(cmsPage(page).getByRole('columnheader', { name: '站点', exact: true })).toBeVisible();
    await expectBusinessSmoke(page, 'cms', 'local');

    const remoteResources = await remoteRuntimeResources(page);
    expect(remoteResources).toEqual([]);
  });

  test('@p1 @runtime broken remote app shows an actionable runtime error', async ({ page }) => {
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

  test('@p1 @runtime missing remote entry does not fall back to another micro app', async ({ page }) => {
    writeRuntimeConfig(missingEntryHybridConfig);
    await login(page);

    await page.goto('/#/workflow/start-process');
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

  test('@p1 @runtime invalid runtime mode falls back to local rendering with diagnostics', async ({ page }) => {
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

  test('@p0 @runtime micro app unauthorized event is handled by the shell', async ({ page }) => {
    writeRuntimeConfig(hybridConfig);
    await login(page);

    await page.evaluate(() => {
      const eventBus = (window as MangoRuntimeWindow).__MANGO_RUNTIME_EVENT_BUS__;
      if (!eventBus) throw new Error('Shell runtime event bus is not registered');
      eventBus.emit('unauthorized');
    });

    await page.waitForURL('**/#/login**', { timeout: 10000 });
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

async function expectRuntime(
  page: Page,
  expected: {
    moduleCode: string;
    runtimeCode: string;
    pageType: string;
    entryIncludes?: string;
  },
) {
  await expect
    .poll(async () => {
      return page.locator('[data-mango-runtime-module]').evaluate((el) => ({
        moduleCode: (el as HTMLElement).dataset.mangoRuntimeModule,
        runtimeCode: (el as HTMLElement).dataset.mangoRuntimeCode,
        pageType: (el as HTMLElement).dataset.mangoRuntimePageType,
        entry: (el as HTMLElement).dataset.mangoRuntimeEntry,
      }));
    })
    .toMatchObject({
      moduleCode: expected.moduleCode,
      runtimeCode: expected.runtimeCode,
      pageType: expected.pageType,
    });

  if (expected.entryIncludes) {
    const entry = await page
      .locator('[data-mango-runtime-module]')
      .evaluate((el) => (el as HTMLElement).dataset.mangoRuntimeEntry || '');
    expect(entry).toContain(expected.entryIncludes);
  }
}

async function expectRemoteResource(page: Page, urlPart: string) {
  await expect
    .poll(async () => {
      const resources = await remoteRuntimeResources(page);
      if (resources.some((url) => url.includes(urlPart))) {
        return true;
      }
      const runtimeEvidence = await page.evaluate((part) => {
        const runtimeWindow = window as MangoRuntimeWindow;
        const active = runtimeWindow.__MANGO_ACTIVE_MICRO_APP__;
        const events = runtimeWindow.__MANGO_MICRO_APP_EVENTS__ || [];
        return Boolean(active?.entryUrl?.includes(part) || events.some((event) => event.entryUrl?.includes(part)));
      }, urlPart);
      return runtimeEvidence;
    })
    .toBeTruthy();
}

async function expectBusinessSmoke(page: Page, module: 'rbac' | 'workflow' | 'cms', mode: 'micro' | 'local') {
  const runtime = {
    moduleCode: module === 'rbac' ? 'mango-authorization' : `mango-${module}`,
    runtimeCode: `mango-admin-${module}-${mode === 'micro' ? 'app' : 'local'}`,
    pageType: mode === 'micro' ? 'MICRO_ROUTE' : 'LOCAL_ROUTE',
  };
  if (module === 'rbac') {
    await page.goto('/#/system/role');
    await expectRuntime(page, runtime);
    await expect(page.getByRole('button', { name: '新增角色', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '角色名称', exact: true })).toBeVisible();

    await page.goto('/#/system/menu');
    await expectRuntime(page, runtime);
    await expect(page.getByRole('button', { name: '新增菜单', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '菜单名称', exact: true })).toBeVisible();
    return;
  }

  if (module === 'cms') {
    for (const [path, column] of [
      ['content-categories', '分类名称'],
      ['ad-deliveries', '投放'],
    ]) {
      await page.goto(`/#/cms/${path}`);
      await expectRuntime(page, runtime);
      await waitCmsReady(page);
      await expect(cmsPage(page).getByRole('columnheader', { name: column, exact: true })).toBeVisible();
      await expect(cmsPage(page).getByRole('button', { name: '新增', exact: true })).toBeVisible();
    }
    return;
  }

  for (const taskMode of ['initiated', 'done']) {
    await page.goto(`/#/workflow/task/${taskMode}`);
    await expectRuntime(page, runtime);
    await expect(page.getByPlaceholder('搜索流程/任务名称', { exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '任务名称', exact: true })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: '业务单号', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: '查询', exact: true })).toBeVisible();
  }
}

async function expectRuntimeDiagnostic(
  page: Page,
  expected: {
    moduleCode: string;
    field: string;
    level: string;
  },
) {
  await expect
    .poll(async () => {
      return page.evaluate((item) => {
        const diagnostics = (window as MangoRuntimeWindow).__MANGO_RUNTIME_CONFIG_DIAGNOSTICS__ || [];
        return diagnostics.some(
          (diagnostic) =>
            diagnostic.moduleCode === item.moduleCode &&
            diagnostic.field === item.field &&
            diagnostic.level === item.level,
        );
      }, expected);
    })
    .toBeTruthy();
}

async function remoteRuntimeResources(page: Page) {
  return page.evaluate(
    (origins) =>
      performance
        .getEntriesByType('resource')
        .map((entry) => entry.name)
        .filter((url) => origins.includes(new URL(url).origin)),
    [rbacEntry, workflowEntry, cmsEntry].map((entry) => new URL(entry).origin),
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
