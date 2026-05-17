import { expect, test, type Page } from '@playwright/test';
import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const runtimeConfigPath = resolve(__dirname, '../../public/runtime-config.json');
let originalRuntimeConfig = '';

const hybridConfig = {
  profile: 'hybrid',
  modules: {
    'mango-authorization': {
      mode: 'micro',
      runtimeCode: 'mango-admin-rbac-app',
      entry: 'http://b.mango.io:5181/',
    },
    'mango-system': {
      mode: 'local',
      runtimeCode: 'mango-admin-system-local',
    },
    'mango-workflow': {
      mode: 'micro',
      runtimeCode: 'mango-admin-workflow-app',
      entry: 'http://c.mango.io:5182/',
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
  },
};

test.describe.serial('Shell runtime composition', () => {
  test.beforeAll(() => {
    originalRuntimeConfig = readFileSync(runtimeConfigPath, 'utf-8');
  });

  test.afterAll(() => {
    if (originalRuntimeConfig) {
      writeFileSync(runtimeConfigPath, originalRuntimeConfig);
    }
  });

  test('hybrid profile loads RBAC and Workflow from remote micro apps', async ({ page }) => {
    writeRuntimeConfig(hybridConfig);
    await login(page);

    await expectRuntime(page, {
      moduleCode: 'mango-authorization',
      runtimeCode: 'mango-admin-rbac-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: 'b.mango.io:5181',
    });
    await expect(page.getByText('新增套餐')).toBeVisible();
    await expectRemoteResource(page, 'b.mango.io:5181');

    await page.getByRole('button', { name: /协同办公/ }).click({ force: true });
    await page.waitForURL('**/#/workflow/task/todo', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-workflow',
      runtimeCode: 'mango-admin-workflow-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: 'c.mango.io:5182',
    });
    await expect(page.locator('main')).toContainText('需要当前用户处理的流程任务');
    await expectRemoteResource(page, 'c.mango.io:5182');
  });

  test('monolith profile renders modules locally without loading remote apps', async ({ page }) => {
    writeRuntimeConfig(monolithConfig);
    await login(page);

    await expectRuntime(page, {
      moduleCode: 'mango-authorization',
      runtimeCode: 'mango-admin-rbac-local',
      pageType: 'LOCAL_ROUTE',
    });
    await expect(page.getByText('新增套餐')).toBeVisible();

    await page.getByRole('button', { name: /协同办公/ }).click({ force: true });
    await page.waitForURL('**/#/workflow/task/todo', { timeout: 10000 });
    await expectRuntime(page, {
      moduleCode: 'mango-workflow',
      runtimeCode: 'mango-admin-workflow-local',
      pageType: 'LOCAL_ROUTE',
    });
    await expect(page.locator('main')).toContainText('需要当前用户处理的流程任务');

    const remoteResources = await remoteRuntimeResources(page);
    expect(remoteResources).toEqual([]);
  });
});

function writeRuntimeConfig(config: unknown) {
  writeFileSync(runtimeConfigPath, `${JSON.stringify(config, null, 2)}\n`);
}

async function login(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('admin123');
  await page.getByRole('button', { name: /登\s*录/ }).click();
  await expect(page.getByRole('button', { name: /系统管理/ })).toBeVisible();
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

async function remoteRuntimeResources(page: Page) {
  return page.evaluate(() =>
    performance
      .getEntriesByType('resource')
      .map((entry) => entry.name)
      .filter((url) => url.includes('b.mango.io:5181') || url.includes('c.mango.io:5182'))
  );
}
