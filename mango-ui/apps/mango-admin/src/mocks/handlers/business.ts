/**
 * Business Mock Handlers - 租户
 */

import { http, HttpResponse } from 'msw';
import type { SysTenant } from '@/api/admin/tenant';

function paginate<T>(list: T[], pageNum: number, pageSize: number) {
  const start = (pageNum - 1) * pageSize;
  const end = start + pageSize;
  return {
    list: list.slice(start, end),
    total: list.length,
    pageNum,
    pageSize,
  };
}

// ==================== 租户 Mock ====================
let mockTenants: SysTenant[] = [
  { id: 1, tenantName: '芒果集团', tenantCode: 'default', contactName: '平台管理员', contactPhone: '13800000000', contactEmail: 'admin@mango.com', expireTime: '2026-12-31', status: 1, createTime: '2026-01-01 10:00:00' },
  { id: 2, tenantName: 'A公司', tenantCode: 'company_a', contactName: 'A公司管理员', contactPhone: '13800000001', contactEmail: 'admin@company-a.com', expireTime: '2026-12-31', status: 1, createTime: '2026-01-02 10:00:00' },
  { id: 3, tenantName: 'B公司', tenantCode: 'company_b', contactName: 'B公司管理员', contactPhone: '13800000002', contactEmail: 'admin@company-b.com', expireTime: '2026-12-31', status: 1, createTime: '2026-01-03 10:00:00' },
  { id: 4, tenantName: 'C公司', tenantCode: 'company_c', contactName: 'C公司管理员', contactPhone: '13800000003', contactEmail: 'admin@company-c.com', expireTime: '2026-12-31', status: 1, createTime: '2026-01-04 10:00:00' },
];
let tenantIdCounter = Math.max(...mockTenants.map(t => t.id || 0)) + 1;

export const businessHandlers = [
  // ==================== 租户管理 ====================

  // 分页查询
  http.get('/api/tenant/list', ({ request }) => {
    const url = new URL(request.url);
    const keyword = url.searchParams.get('keyword') || '';
    const status = url.searchParams.get('status');
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1');
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10');

    let filtered = mockTenants;
    if (keyword) {
      filtered = filtered.filter(t => t.tenantName.includes(keyword) || t.tenantCode.includes(keyword) || t.contactName?.includes(keyword));
    }
    if (status) {
      filtered = filtered.filter(t => t.status === parseInt(status));
    }

    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: paginate(filtered, pageNum, pageSize),
    });
  }),

  // 详情
  http.get('/api/tenant/:id', ({ params }) => {
    const tenant = mockTenants.find(t => t.id === parseInt(params.id as string));
    if (!tenant) {
      return HttpResponse.json({ code: 404, success: false, message: '租户不存在', data: null });
    }
    return HttpResponse.json({ code: 200, success: true, message: 'success', data: tenant });
  }),

  // 新增
  http.post('/api/tenant', async ({ request }) => {
    const body = await request.json() as Partial<SysTenant>;
    const newTenant: SysTenant = {
      id: tenantIdCounter++,
      tenantName: body.tenantName || '',
      tenantCode: body.tenantCode || '',
      contactName: body.contactName,
      contactPhone: body.contactPhone,
      contactEmail: body.contactEmail,
      expireTime: body.expireTime,
      status: body.status ?? 1,
      createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    };
    mockTenants.push(newTenant);
    return HttpResponse.json({ code: 200, success: true, message: '新增成功', data: newTenant });
  }),

  // 修改
  http.put('/api/tenant', async ({ request }) => {
    const body = await request.json() as Partial<SysTenant>;
    const index = mockTenants.findIndex(t => t.id === body.id);
    if (index === -1) {
      return HttpResponse.json({ code: 404, success: false, message: '租户不存在', data: null });
    }
    mockTenants[index] = { ...mockTenants[index], ...body };
    return HttpResponse.json({ code: 200, success: true, message: '修改成功', data: mockTenants[index] });
  }),

  // 删除
  http.delete('/api/tenant/:id', ({ params }) => {
    const index = mockTenants.findIndex(t => t.id === parseInt(params.id as string));
    if (index === -1) {
      return HttpResponse.json({ code: 404, success: false, message: '租户不存在', data: null });
    }
    mockTenants.splice(index, 1);
    return HttpResponse.json({ code: 200, success: true, message: '删除成功', data: null });
  }),

  // 修改状态
  http.put('/api/tenant/status/:id', async ({ params, request }) => {
    const body = await request.json() as { status: number };
    const tenant = mockTenants.find(t => t.id === parseInt(params.id as string));
    if (!tenant) {
      return HttpResponse.json({ code: 404, success: false, message: '租户不存在', data: null });
    }
    tenant.status = body.status;
    return HttpResponse.json({ code: 200, success: true, message: '状态修改成功', data: null });
  }),
];
