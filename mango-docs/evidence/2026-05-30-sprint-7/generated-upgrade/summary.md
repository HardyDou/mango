# Mango Generated Project Upgrade E2E

- Generated project: /var/folders/v3/xy1sw5vj1czc1qkwhhh0g4v40000gn/T/mango-generated-upgrade-e2e-CYoFmw/upgrade-platform
- Frontend: http://127.0.0.1:5320
- Checks: package check, generated project template check, legacy compatibility install/typecheck/build, recommended install/typecheck/build, browser smoke screenshot
- Legacy compatibility path: @mango/*/capability and @mango/* packages still build through @mango/admin
- Recommended path: @mango/*-admin/capability and @mango/*-api / @mango/*-admin packages
- UI layout checks: Mango shell, header, nav, aside, runtime outlet, built-in menu, business menu, list page, search, actions, table, pagination, explicit API error state, no horizontal overflow
- Layout result: passed
- Evidence: legacy-compat-project.json, legacy-install.out, legacy-typecheck.out, legacy-build.out, recommended-project.json, recommended-install.out, recommended-typecheck.out, recommended-build.out, frontend-dev.out, layout-report.json, recommended-frontend.png

## Screenshot Review

- Screenshot: `recommended-frontend.png`
- Report: `layout-report.json`
- Result: passed
- Review: 页面为 Mango Admin Shell 原框架；顶部蓝色主导航、Mango 标识、右侧工具区和用户区、左侧业务菜单、标签页和业务列表页均可见。
- Menu: 顶部包含 `Guarantee Module`、`权限与组织管理`、`系统基础能力`、`模板中心`、`文件中心`、`通知中心`、`编号规则`、`工作日历`、`工作流`；左侧显示 `Letter管理`。
- Business page: `Letter管理` 列表页展示查询条件、查询/重置/刷新/清空筛选按钮、表格、空数据和分页。
- Data/error state: 本 E2E 不启动真实后端，列表接口返回 500 时页面显示明确错误提示和空态，未声明真实业务数据联调完成。
- Layout: 无横向溢出；搜索区、操作区、表格和分页区域无明显遮挡或重叠。
