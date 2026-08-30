import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const request = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

vi.mock('@mango/common/utils/request', () => request);

import { workflowApi } from '../workflow';

describe('workflow designer options', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads every candidate type through the workflow-owned endpoint', async () => {
    request.get.mockResolvedValue({
      users: [{ value: 'zhangsan', label: '张三' }],
      roles: [{ value: 21, label: '审批员' }],
      posts: [{ value: 31, label: '财务岗' }],
      organizations: [{ value: 41, label: '总部', children: [{ value: 42, label: '财务部' }] }],
      dictTypes: [{ value: 'expense_type', label: '费用类型' }],
    });

    const options = await workflowApi.designerOptions();

    expect(request.get).toHaveBeenCalledOnce();
    expect(request.get).toHaveBeenCalledWith('/workflow/definitions/designer-options');
    expect(options.roles[0]?.value).toBe('21');
    expect(options.organizations[0]?.children?.[0]).toEqual({
      value: '42',
      label: '财务部',
      children: undefined,
    });
  });

  it('does not couple definition designer components to platform-domain endpoints', () => {
    const definitionView = readFileSync(resolve(process.cwd(), 'src/views/workflow-definition/index.vue'), 'utf8');
    const participantSelector = readFileSync(resolve(
      process.cwd(),
      'src/views/workflow-definition/components/workflow-designer/WorkflowParticipantSelector.vue',
    ), 'utf8');
    const source = `${definitionView}\n${participantSelector}`;

    expect(source).not.toMatch(/\/(identity\/users\/page|authorization\/roles|post\/page|org\/tree|system\/dict\/type\/list)/);
  });

  it('loads template push tenants through the workflow-owned endpoint', async () => {
    request.get.mockResolvedValue([{ id: 1, tenantName: '默认机构', tenantCode: 'default' }]);

    const tenants = await workflowApi.tenants('默认');

    expect(request.get).toHaveBeenCalledWith('/workflow/templates/tenant-options', {
      params: { keyword: '默认' },
    });
    expect(tenants).toEqual([{
      id: '1',
      tenantName: '默认机构',
      tenantCode: 'default',
      status: undefined,
    }]);
  });

  it('does not load tenant options while mounting the template page', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/views/workflow-template/index.vue'), 'utf8');

    expect(source).not.toContain('/system/tenant/list');
    expect(source).not.toMatch(/Promise\.all\(\[[^\]]*loadTenants/);
    expect(source).toMatch(/openPushDialog[\s\S]*void loadTenants\(\)/);
  });
});
