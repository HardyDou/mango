import { expect, test, type APIResponse, type Page } from '@playwright/test';

test.setTimeout(60 * 1000);

async function login(page: Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.locator('input[placeholder="密码"]').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function apiHeaders(page: Page) {
  return page.evaluate(() => {
    const token = sessionStorage.getItem('MANGO_TOKEN') || '';
    const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}');
    const tenantId = String(userInfo?.tenantId || '1');
    return {
      Authorization: token ? `Bearer ${token}` : '',
      'TENANT-ID': tenantId,
      'X-Mango-Tenant-Id': tenantId,
    };
  });
}

async function expectBusinessOk(response: APIResponse) {
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.success || body.code === 200).toBeTruthy();
  return body;
}

async function cleanupCalendar(page: Page, headers: Record<string, string>, calendarCode: string) {
  const listResponse = await page.request.get('/api/calendar/admin/calendars/page', {
    headers,
    params: { keyword: calendarCode, page: '1', size: '20' },
  });
  const listBody = await expectBusinessOk(listResponse);
  const calendars = listBody.data?.list || [];
  for (const calendar of calendars) {
    if (calendar.calendarCode === calendarCode) {
      await page.request.delete('/api/calendar/admin/calendars', {
        headers,
        params: { id: String(calendar.id) },
      }).catch(() => undefined);
    }
  }
}

function collectVisibleMenuNames(menus: any[]): string[] {
  return menus.flatMap((menu) => [
    menu.menuType !== 3 && menu.visible !== 0 ? menu.menuName : undefined,
    ...collectVisibleMenuNames(menu.children || []),
  ]).filter(Boolean);
}

test.describe('日历管理 E2E', () => {
  test('数据管理入口展示 2026 中国标准日历并提供工作日计算', async ({ page }) => {
    const menuResponsePromise = page.waitForResponse((response) => {
      const url = response.url();
      return response.status() === 200
        && url.includes('/api/authorization/menus/user')
        && url.includes('fmt=tree');
    });

    await login(page);

    const menuBody = await (await menuResponsePromise).json();
    const visibleMenus = collectVisibleMenuNames(menuBody.data || []);
    expect(visibleMenus).toContain('数据管理');
    expect(visibleMenus).toContain('日历管理');
    expect(visibleMenus).toContain('发号器管理');
    expect(visibleMenus).toContain('支付管理');

    await page.getByRole('button', { name: '数据管理' }).click();
    await page.waitForURL('**/#/data/calendar', { timeout: 10000 });
    await expect(page.getByRole('heading', { name: '日历管理' })).toBeVisible();

    const calendarPageResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/calendar/admin/calendars/page') && response.status() === 200
    );
    await page.getByPlaceholder('编码/名称').fill('CN_STANDARD');
    await page.locator('.calendar-side').getByRole('button', { name: '查询' }).click();
    await calendarPageResponsePromise;
    await page.locator('.calendar-side .el-table__row', { hasText: 'CN_STANDARD' }).first().click();
    await expect(page.getByText('中国标准工作日历 / CN_STANDARD')).toBeVisible({ timeout: 10000 });

    const headers = await apiHeaders(page);
    for (const year of [2025, 2026]) {
      const refreshResponse = await page.request.put('/api/calendar/admin/years/lunar', {
        headers,
        data: { calendarCode: 'CN_STANDARD', year },
      });
      await expectBusinessOk(refreshResponse);
    }

    await expect(page.getByRole('tab', { name: '年度' })).toBeVisible();
    const yearRow = page.locator('.calendar-main .el-table__row', { hasText: '2026' }).filter({ hasText: '中国标准工作日历' }).first();
    await expect(yearRow).toContainText('365');
    await expect(yearRow).toContainText('248');
    await expect(yearRow).toContainText('117');
    await expect(yearRow).toContainText('33');
    await expect(yearRow).toContainText('6');

    await yearRow.getByRole('button', { name: '查看日期' }).click();
    await expect(page.getByRole('row', { name: /2026-01-01.*周四.*法定节假日.*否.*元旦/ })).toBeVisible();
    await expect(page.getByRole('row', { name: /2026-01-04.*周日.*调休补班.*是.*元旦调休上班/ })).toBeVisible();
    await expect(page.getByRole('row', { name: /2026-01-01.*冬月十三.*乙巳年.*蛇年/ })).toBeVisible();

    const countResponse = await page.request.get('/api/calendar/workdays/count', {
      headers,
      params: {
        calendarCode: 'CN_STANDARD',
        startDate: '2026-01-01',
        endDate: '2026-01-10',
        includeStart: 'true',
        includeEnd: 'true',
      },
    });
    expect(countResponse.status()).toBe(200);
    const countBody = await countResponse.json();
    expect(countBody.data).toBe(6);

    const addResponse = await page.request.get('/api/calendar/workdays/add', {
      headers,
      params: {
        calendarCode: 'CN_STANDARD',
        sourceDate: '2026-02-13',
        amount: '1',
      },
    });
    expect(addResponse.status()).toBe(200);
    const addBody = await addResponse.json();
    expect(addBody.data).toBe('2026-02-14');

    const lunarResponse = await page.request.get('/api/calendar/lunar/day', {
      headers,
      params: { date: '2025-01-29' },
    });
    const lunarBody = await expectBusinessOk(lunarResponse);
    expect(lunarBody.data.lunarText).toBe('正月初一');
    expect(lunarBody.data.ganzhiYear).toBe('乙巳');
    expect(lunarBody.data.zodiac).toBe('蛇');

    const lunarToSolarResponse = await page.request.get('/api/calendar/lunar/to-solar', {
      headers,
      params: { lunarYear: '2025', lunarMonth: '1', lunarDay: '1', leapMonth: 'false' },
    });
    const lunarToSolarBody = await expectBusinessOk(lunarToSolarResponse);
    expect(lunarToSolarBody.data).toBe('2025-01-29');

    const solarTermsResponse = await page.request.get('/api/calendar/lunar/solar-terms', {
      headers,
      params: { year: '2025' },
    });
    const solarTermsBody = await expectBusinessOk(solarTermsResponse);
    expect(solarTermsBody.data).toContainEqual(expect.objectContaining({ name: '清明', date: '2025-04-04' }));

    await page.getByRole('button', { name: '工具' }).click();
    await expect(page.locator('.el-drawer__header', { hasText: '日历工具' })).toBeVisible();
    await expect(page.locator('.tool-calendar')).toContainText('中国标准工作日历（CN_STANDARD）');
    await page.getByRole('button', { name: '区间工作日数' }).click();
    await expect(page.locator('.el-dialog__header', { hasText: '区间工作日数' })).toBeVisible();
    await expect(page.getByRole('textbox', { name: '日历' })).toHaveValue('中国标准工作日历（CN_STANDARD）');
    await page.locator('.el-dialog').getByRole('button', { name: '确认' }).click();
    await expect(page.locator('.tool-result')).toContainText('工作日数量');
    await page.locator('.el-dialog').getByRole('button', { name: '关闭' }).click();

    await page.getByRole('button', { name: '农历查询' }).click();
    await expect(page.locator('.el-dialog__header', { hasText: '农历查询' })).toBeVisible();
    await page.locator('.el-dialog').getByRole('button', { name: '确认' }).click();
    await expect(page.locator('.tool-result')).toContainText('农历');
    await page.locator('.el-dialog').getByRole('button', { name: '关闭' }).click();

    await page.getByRole('button', { name: '农历转公历' }).click();
    await expect(page.locator('.el-dialog__header', { hasText: '农历转公历' })).toBeVisible();
    await page.locator('.el-dialog').getByRole('button', { name: '确认' }).click();
    await expect(page.locator('.tool-result')).toContainText('公历日期');
    await page.locator('.el-dialog').getByRole('button', { name: '关闭' }).click();

    await page.getByRole('button', { name: '节气查询' }).click();
    await expect(page.locator('.el-dialog__header', { hasText: '节气查询' })).toBeVisible();
    await page.locator('.el-dialog').getByRole('button', { name: '确认' }).click();
    await expect(page.locator('.tool-result')).toContainText('清明');
  });

  test('日历、年度、日期删除按钮完成删除链路', async ({ page }) => {
    await login(page);
    const headers = await apiHeaders(page);
    const calendarCode = `E2E_DELETE_${Date.now()}`;

    await cleanupCalendar(page, headers, calendarCode);

    try {
      const createResponse = await page.request.post('/api/calendar/admin/calendars', {
        headers,
        data: {
          calendarCode,
          calendarName: '删除验证日历',
        },
      });
      await expectBusinessOk(createResponse);

      const initResponse = await page.request.post('/api/calendar/admin/years/init', {
        headers,
        data: {
          calendarCode,
          year: 2099,
          overwrite: false,
        },
      });
      await expectBusinessOk(initResponse);

      await page.goto('/#/data/calendar');
      await expect(page.getByRole('heading', { name: '日历管理' })).toBeVisible();
      await page.getByPlaceholder('编码/名称').fill(calendarCode);
      await page.locator('.calendar-side').getByRole('button', { name: '查询' }).click();
      const calendarRow = page.locator('.calendar-side .el-table__row', { hasText: calendarCode }).first();
      await expect(calendarRow).toBeVisible({ timeout: 10000 });
      await calendarRow.click();
      await expect(page.getByText(`删除验证日历 / ${calendarCode}`)).toBeVisible();

      const yearRow = page.locator('.calendar-main .el-table__row', { hasText: '2099' }).filter({ hasText: '删除验证日历' }).first();
      await expect(yearRow).toBeVisible();
      await yearRow.getByRole('button', { name: '查看日期' }).click();

      const dayRow = page.locator('.calendar-main .el-table__row', { hasText: '2099-01-01' }).first();
      await expect(dayRow).toBeVisible();
      const deleteDayResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/calendar/admin/days') &&
        response.request().method() === 'DELETE' &&
        response.status() === 200
      );
      await dayRow.getByRole('button', { name: '删除' }).click();
      await page.locator('.el-message-box').getByRole('button', { name: '删除' }).click();
      await expectBusinessOk(await deleteDayResponsePromise);

      await page.getByRole('tab', { name: '年度' }).click();
      const refreshedYearRow = page.locator('.calendar-main .el-table__row', { hasText: '2099' }).filter({ hasText: '删除验证日历' }).first();
      await expect(refreshedYearRow).toBeVisible();
      const deleteYearResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/calendar/admin/years') &&
        response.request().method() === 'DELETE' &&
        response.status() === 200
      );
      await refreshedYearRow.getByRole('button', { name: '删除' }).click();
      await page.locator('.el-message-box').getByRole('button', { name: '删除' }).click();
      await expectBusinessOk(await deleteYearResponsePromise);

      const deleteCalendarResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/calendar/admin/calendars') &&
        response.request().method() === 'DELETE' &&
        response.status() === 200
      );
      await calendarRow.getByLabel('删除日历').click();
      await page.locator('.el-message-box').getByRole('button', { name: '删除' }).click();
      await expectBusinessOk(await deleteCalendarResponsePromise);

      const listResponse = await page.request.get('/api/calendar/admin/calendars/page', {
        headers,
        params: { keyword: calendarCode, page: '1', size: '20' },
      });
      const listBody = await expectBusinessOk(listResponse);
      expect((listBody.data?.list || []).some((item: any) => item.calendarCode === calendarCode)).toBe(false);
    } finally {
      await cleanupCalendar(page, headers, calendarCode);
    }
  });
});
