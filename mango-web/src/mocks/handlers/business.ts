/**
 * Business Mock Handlers - 租户、路由
 */

import { http, HttpResponse } from 'msw';
import type { SysTenant } from '@/api/admin/tenant';
import type { SysRoute, RouteQuery } from '@/api/admin/route';

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
  { id: 1, tenantName: '默认租户', tenantCode: 'default', contactName: '张三', contactPhone: '13800138000', contactEmail: 'zhangsan@example.com', expireTime: '2025-12-31', status: 1, createTime: '2024-01-01 10:00:00' },
  { id: 2, tenantName: '测试租户', tenantCode: 'test', contactName: '李四', contactPhone: '13800138001', contactEmail: 'lisi@example.com', expireTime: '2024-06-30', status: 1, createTime: '2024-01-15 10:00:00' },
  { id: 3, tenantName: '正式租户A', tenantCode: 'tenant_a', contactName: '王五', contactPhone: '13800138002', contactEmail: 'wangwu@example.com', expireTime: '2025-06-30', status: 1, createTime: '2024-02-01 10:00:00' },
  { id: 4, tenantName: '演示租户', tenantCode: 'demo', contactName: '赵六', contactPhone: '13800138003', contactEmail: 'zhaoliu@example.com', expireTime: '2024-03-01', status: 0, createTime: '2024-02-15 10:00:00' },
  { id: 5, tenantName: '内部租户', tenantCode: 'internal', contactName: '钱七', contactPhone: '13800138004', contactEmail: 'qianqi@example.com', expireTime: '2025-12-31', status: 1, createTime: '2024-03-01 10:00:00' },
];
let tenantIdCounter = Math.max(...mockTenants.map(t => t.id || 0)) + 1;

// ==================== 路由 Mock ====================
let mockRoutes: SysRoute[] = [
  { id: 1, parentId: 0, routeName: '首页', routePath: '/home', routeType: 1, component: '/layout/navBars/breadcrumb/home.vue', icon: 'home', isCache: 0, isAffix: 1, isVisible: 1, sort: 1, status: 1, description: '首页路由', createTime: '2024-01-01 10:00:00' },
  { id: 2, parentId: 0, routeName: '系统管理', routePath: '/system', routeType: 0, icon: 'setting', isCache: 0, isAffix: 0, isVisible: 1, sort: 2, status: 1, description: '系统管理父路由', createTime: '2024-01-01 10:01:00' },
  { id: 3, parentId: 2, routeName: '用户管理', routePath: '/system/user', routeType: 1, component: '/views/system/user/index.vue', icon: 'user', isCache: 0, isAffix: 0, isVisible: 1, sort: 1, status: 1, permission: 'system:user:list', description: '用户管理路由', createTime: '2024-01-01 10:02:00' },
  { id: 4, parentId: 2, routeName: '角色管理', routePath: '/system/role', routeType: 1, component: '/views/system/role/index.vue', icon: 'role', isCache: 0, isAffix: 0, isVisible: 1, sort: 2, status: 1, permission: 'system:role:list', description: '角色管理路由', createTime: '2024-01-01 10:03:00' },
  { id: 5, parentId: 2, routeName: '菜单管理', routePath: '/system/menu', routeType: 1, component: '/views/system/menu/index.vue', icon: 'menu', isCache: 0, isAffix: 0, isVisible: 1, sort: 3, status: 1, permission: 'system:menu:list', description: '菜单管理路由', createTime: '2024-01-01 10:04:00' },
  { id: 6, parentId: 0, routeName: '组件库', routePath: '/components', routeType: 0, icon: 'component', isCache: 0, isAffix: 0, isVisible: 1, sort: 3, status: 1, description: '组件库父路由', createTime: '2024-01-02 10:00:00' },
  { id: 7, parentId: 6, routeName: '表单设计', routePath: '/components/formcreate', routeType: 1, component: '/views/components/formcreate/index.vue', icon: 'form', isCache: 0, isAffix: 0, isVisible: 1, sort: 1, status: 1, description: '表单设计路由', createTime: '2024-01-02 10:01:00' },
];
let routeIdCounter = Math.max(...mockRoutes.map(r => r.id || 0)) + 1;

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

  // ==================== 路由管理 ====================

  // 分页查询
  http.get('/api/route/list', ({ request }) => {
    const url = new URL(request.url);
    const keyword = url.searchParams.get('keyword') || '';
    const routeType = url.searchParams.get('routeType');
    const status = url.searchParams.get('status');
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1');
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10');

    let filtered = mockRoutes;
    if (keyword) {
      filtered = filtered.filter(r => r.routeName.includes(keyword) || r.routePath.includes(keyword) || r.description?.includes(keyword));
    }
    if (routeType) {
      filtered = filtered.filter(r => r.routeType === parseInt(routeType));
    }
    if (status) {
      filtered = filtered.filter(r => r.status === parseInt(status));
    }

    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: paginate(filtered, pageNum, pageSize),
    });
  }),

  // 路由树
  http.get('/api/route/tree', () => {
    // 构建树形结构
    function buildTree(parentId: number): SysRoute[] {
      return mockRoutes
        .filter(r => r.parentId === parentId)
        .map(r => ({
          ...r,
          children: buildTree(r.id!),
        }));
    }
    return HttpResponse.json({ code: 200, success: true, message: 'success', data: buildTree(0) });
  }),

  // 详情
  http.get('/api/route/:id', ({ params }) => {
    const route = mockRoutes.find(r => r.id === parseInt(params.id as string));
    if (!route) {
      return HttpResponse.json({ code: 404, success: false, message: '路由不存在', data: null });
    }
    return HttpResponse.json({ code: 200, success: true, message: 'success', data: route });
  }),

  // 新增
  http.post('/api/route', async ({ request }) => {
    const body = await request.json() as Partial<SysRoute>;
    const newRoute: SysRoute = {
      id: routeIdCounter++,
      parentId: body.parentId ?? 0,
      routeName: body.routeName || '',
      routePath: body.routePath || '',
      routeType: body.routeType ?? 1,
      component: body.component,
      redirect: body.redirect,
      icon: body.icon,
      isCache: body.isCache ?? 0,
      isAffix: body.isAffix ?? 0,
      isVisible: body.isVisible ?? 1,
      sort: body.sort ?? 0,
      status: body.status ?? 1,
      permission: body.permission,
      description: body.description,
      createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    };
    mockRoutes.push(newRoute);
    return HttpResponse.json({ code: 200, success: true, message: '新增成功', data: newRoute });
  }),

  // 修改
  http.put('/api/route', async ({ request }) => {
    const body = await request.json() as Partial<SysRoute>;
    const index = mockRoutes.findIndex(r => r.id === body.id);
    if (index === -1) {
      return HttpResponse.json({ code: 404, success: false, message: '路由不存在', data: null });
    }
    mockRoutes[index] = { ...mockRoutes[index], ...body };
    return HttpResponse.json({ code: 200, success: true, message: '修改成功', data: mockRoutes[index] });
  }),

  // 删除
  http.delete('/api/route/:id', ({ params }) => {
    const index = mockRoutes.findIndex(r => r.id === parseInt(params.id as string));
    if (index === -1) {
      return HttpResponse.json({ code: 404, success: false, message: '路由不存在', data: null });
    }
    // 同时删除子路由
    function getChildIds(parentId: number): number[] {
      const ids: number[] = [parentId];
      mockRoutes.filter(r => r.parentId === parentId).forEach(r => {
        ids.push(...getChildIds(r.id!));
      });
      return ids;
    }
    const idsToDelete = getChildIds(parseInt(params.id as string));
    mockRoutes = mockRoutes.filter(r => !idsToDelete.includes(r.id!));
    return HttpResponse.json({ code: 200, success: true, message: '删除成功', data: null });
  }),

  // 排序
  http.put('/api/route/sort', async ({ request }) => {
    const body = await request.json() as { id: number; sort: number }[];
    body.forEach(item => {
      const route = mockRoutes.find(r => r.id === item.id);
      if (route) {
        route.sort = item.sort;
      }
    });
    return HttpResponse.json({ code: 200, success: true, message: '排序成功', data: null });
  }),
];
