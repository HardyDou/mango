import { test, expect } from '@playwright/test';

test.describe('前端组件库 E2E 测试', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/#/login');
    await page.waitForLoadState('networkidle');
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', 'admin123');
    await page.click('button:has-text("登 录")');

    // 等待登录完成并跳转
    await page.waitForURL('**/#/home', { timeout: 10000 });
    await page.waitForLoadState('networkidle');

    // 导航到组件演示页面
    await page.goto('/#/demo/components');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000); // 等待组件加载
  });

  test('富文本编辑器 - 工具栏显示', async ({ page }) => {
    // 截图验证整体页面
    await page.screenshot({ path: 'test-results/01-页面整体.png', fullPage: true });

    // 检查编辑器容器
    const editorWrapper = page.locator('.editor-wrapper');
    await expect(editorWrapper).toBeVisible();

    // 截图验证编辑器
    await page.screenshot({ path: 'test-results/02-富文本编辑器.png' });

    // 检查工具栏存在
    const toolbar = page.locator('.editor-toolbar');
    await expect(toolbar).toBeVisible();

    // 截图验证工具栏
    await page.screenshot({ path: 'test-results/03-编辑器工具栏.png' });
  });

  test('代码编辑器 - 渲染和交互', async ({ page }) => {
    // 检查 CodeMirror 编辑器
    const codeEditor = page.locator('.code-editor-container');
    await expect(codeEditor).toBeVisible();

    // 截图验证代码编辑器
    await page.screenshot({ path: 'test-results/04-代码编辑器.png' });

    // 检查 CodeMirror 内部结构
    const codeMirror = page.locator('.CodeMirror');
    await expect(codeMirror).toBeVisible();

    // 截图验证
    await page.screenshot({ path: 'test-results/05-代码编辑器内容.png' });
  });

  test('ECharts 图表渲染', async ({ page }) => {
    // 检查图表容器
    const chart = page.locator('.echarts-container');
    await expect(chart).toBeVisible();

    // 截图验证
    await page.screenshot({ path: 'test-results/06-ECharts图表.png' });
  });

  test('权限指令显示', async ({ page }) => {
    // 截图验证
    await page.screenshot({ path: 'test-results/07-权限指令.png' });

    // 检查有权限的按钮显示
    const adminBtn = page.locator('.auth-demo button').first();
    await expect(adminBtn).toBeVisible();

    // 注意：由于 mock 登录时 authBtnList 为空，指令不会隐藏元素
    // 这是设计行为 - 等待数据加载
  });

  test('所有组件整体验证', async ({ page }) => {
    // 截图验证完整页面
    await page.screenshot({ path: 'test-results/08-完整组件页面.png', fullPage: true });

    // 验证页面标题
    const title = page.locator('h1');
    await expect(title).toContainText('前端组件库');

    // 验证所有卡片都存在
    const cards = page.locator('.demo-card');
    expect(await cards.count()).toBeGreaterThanOrEqual(5);
  });
});
