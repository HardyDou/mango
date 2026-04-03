/**
 * System Mock Handlers - 系统配置、参数、日志
 */

import { http, HttpResponse } from 'msw';
import type { SysParam, SysParamQuery } from '@/api/admin/param';
import type { SysConfig, SysConfigQuery } from '@/api/admin/config';
import type { SysLoginLog, LoginLogQuery, SysOperationLog, OperationLogQuery } from '@/api/admin/log';

// ==================== 通用分页 ====================
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

// ==================== 系统参数 Mock ====================
let mockParams: SysParam[] = [
  { id: 1, paramKey: 'sys.user.initPassword', paramValue: '123456', paramType: 1, description: '用户初始密码', status: 1, createTime: '2024-01-01 10:00:00' },
  { id: 2, paramKey: 'sys.account.captchaEnabled', paramValue: 'true', paramType: 1, description: '是否开启验证码', status: 1, createTime: '2024-01-02 10:00:00' },
  { id: 3, paramKey: 'sys.account.registerEnabled', paramValue: 'false', paramType: 1, description: '是否开启注册', status: 1, createTime: '2024-01-03 10:00:00' },
  { id: 4, paramKey: 'sys.upload.maxSize', paramValue: '10485760', paramType: 2, description: '文件上传大小限制(字节)', status: 1, createTime: '2024-01-04 10:00:00' },
  { id: 5, paramKey: 'sys.upload.allowedTypes', paramValue: 'jpg,png,pdf,doc', paramType: 2, description: '允许上传的文件类型', status: 1, createTime: '2024-01-05 10:00:00' },
  { id: 6, paramKey: 'sys.log.expireDays', paramValue: '30', paramType: 1, description: '日志保留天数', status: 1, createTime: '2024-01-06 10:00:00' },
  { id: 7, paramKey: 'sys.session.timeout', paramValue: '3600', paramType: 1, description: '会话超时时间(秒)', status: 0, createTime: '2024-01-07 10:00:00' },
];
let paramIdCounter = Math.max(...mockParams.map(p => p.id || 0)) + 1;

// ==================== 系统配置 Mock ====================
let mockConfigs: SysConfig[] = [
  { id: 1, configKey: 'sys.index.slogan', configValue: '欢迎使用 Mango 管理平台', configGroup: 'index', description: '首页标语', status: 1, createTime: '2024-01-01 10:00:00' },
  { id: 2, configKey: 'sys.index.title', configValue: 'Mango Admin', configGroup: 'index', description: '网站标题', status: 1, createTime: '2024-01-01 10:01:00' },
  { id: 3, configKey: 'sys.copyright', configValue: '© 2024 Mango Inc.', configGroup: 'footer', description: '版权信息', status: 1, createTime: '2024-01-02 10:00:00' },
  { id: 4, configKey: 'sys.logo.url', configValue: '/logo.png', configGroup: 'theme', description: 'Logo地址', status: 1, createTime: '2024-01-02 10:01:00' },
  { id: 5, configKey: 'sys.theme.default', configValue: 'light', configGroup: 'theme', description: '默认主题', status: 1, createTime: '2024-01-03 10:00:00' },
  { id: 6, configKey: 'sys.security.tokenExpire', configValue: '86400', configGroup: 'security', description: 'Token过期时间(秒)', status: 1, createTime: '2024-01-04 10:00:00' },
  { id: 7, configKey: 'sys.security.maxRetry', configValue: '5', configGroup: 'security', description: '最大登录失败次数', status: 1, createTime: '2024-01-05 10:00:00' },
];
let configIdCounter = Math.max(...mockConfigs.map(c => c.id || 0)) + 1;

// ==================== 登录日志 Mock ====================
let mockLoginLogs: SysLoginLog[] = [
  { id: 1, username: 'admin', ip: '127.0.0.1', location: '本机', loginTime: '2024-03-15 10:00:00', status: 1, msg: '登录成功' },
  { id: 2, username: 'admin', ip: '192.168.1.100', location: '局域网', loginTime: '2024-03-15 09:30:00', status: 1, msg: '登录成功' },
  { id: 3, username: 'user01', ip: '10.0.0.1', location: '北京市', loginTime: '2024-03-15 09:00:00', status: 0, msg: '密码错误' },
  { id: 4, username: 'admin', ip: '127.0.0.1', location: '本机', loginTime: '2024-03-14 18:00:00', status: 1, msg: '登录成功' },
  { id: 5, username: 'test', ip: '172.16.0.1', location: '上海市', loginTime: '2024-03-14 15:30:00', status: 0, msg: '用户不存在' },
  { id: 6, username: 'admin', ip: '127.0.0.1', location: '本机', loginTime: '2024-03-14 10:00:00', status: 1, msg: '登录成功' },
];

// ==================== 操作日志 Mock ====================
let mockOperationLogs: SysOperationLog[] = [
  { id: 1, username: 'admin', operation: '用户管理', requestMethod: 'POST', requestUrl: '/system/user', requestParams: '{"username":"test","mobile":"13800138000"}', operateTime: '2024-03-15 10:30:00', costTime: 120, ip: '127.0.0.1', status: 1 },
  { id: 2, username: 'admin', operation: '角色授权', requestMethod: 'PUT', requestUrl: '/system/role/1/permissions', requestParams: '{"permissions":["system:user:list"]}', operateTime: '2024-03-15 10:00:00', costTime: 80, ip: '127.0.0.1', status: 1 },
  { id: 3, username: 'admin', operation: '字典配置', requestMethod: 'POST', requestUrl: '/system/dict/type', requestParams: '{"name":"订单状态","code":"order_status"}', operateTime: '2024-03-15 09:30:00', costTime: 95, ip: '127.0.0.1', status: 1 },
  { id: 4, username: 'admin', operation: '系统参数', requestMethod: 'PUT', requestUrl: '/system/param/1', requestParams: '{"paramValue":"false"}', operateTime: '2024-03-15 09:00:00', costTime: 60, ip: '127.0.0.1', status: 1 },
  { id: 5, username: 'admin', operation: '菜单查询', requestMethod: 'GET', requestUrl: '/auth/menu/list', operateTime: '2024-03-14 18:00:00', costTime: 25, ip: '127.0.0.1', status: 1 },
  { id: 6, username: 'user01', operation: '个人中心', requestMethod: 'PUT', requestUrl: '/auth/info', requestParams: '{"nickname":"新昵称"}', operateTime: '2024-03-14 15:00:00', costTime: 45, ip: '10.0.0.1', status: 1 },
];

export const systemHandlers = [
  // ==================== 系统参数 ====================

  // 分页查询
  http.get('/api/system/param/list', ({ request }) => {
    const url = new URL(request.url);
    const keyword = url.searchParams.get('keyword') || '';
    const paramType = url.searchParams.get('paramType');
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1');
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10');

    let filtered = mockParams;
    if (keyword) {
      filtered = filtered.filter(p => p.paramKey.includes(keyword) || p.description?.includes(keyword));
    }
    if (paramType) {
      filtered = filtered.filter(p => p.paramType === parseInt(paramType));
    }

    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: paginate(filtered, pageNum, pageSize),
    });
  }),

  // 详情
  http.get('/api/system/param/:id', ({ params }) => {
    const param = mockParams.find(p => p.id === parseInt(params.id as string));
    if (!param) {
      return HttpResponse.json({ code: 404, success: false, message: '参数不存在', data: null });
    }
    return HttpResponse.json({ code: 200, success: true, message: 'success', data: param });
  }),

  // 新增
  http.post('/api/system/param', async ({ request }) => {
    const body = await request.json() as Partial<SysParam>;
    const newParam: SysParam = {
      id: paramIdCounter++,
      paramKey: body.paramKey || '',
      paramValue: body.paramValue || '',
      paramType: body.paramType ?? 1,
      description: body.description,
      status: body.status ?? 1,
      createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    };
    mockParams.push(newParam);
    return HttpResponse.json({ code: 200, success: true, message: '新增成功', data: newParam });
  }),

  // 修改
  http.put('/api/system/param', async ({ request }) => {
    const body = await request.json() as Partial<SysParam>;
    const index = mockParams.findIndex(p => p.id === body.id);
    if (index === -1) {
      return HttpResponse.json({ code: 404, success: false, message: '参数不存在', data: null });
    }
    mockParams[index] = { ...mockParams[index], ...body };
    return HttpResponse.json({ code: 200, success: true, message: '修改成功', data: mockParams[index] });
  }),

  // 删除
  http.delete('/api/system/param/:id', ({ params }) => {
    const index = mockParams.findIndex(p => p.id === parseInt(params.id as string));
    if (index === -1) {
      return HttpResponse.json({ code: 404, success: false, message: '参数不存在', data: null });
    }
    mockParams.splice(index, 1);
    return HttpResponse.json({ code: 200, success: true, message: '删除成功', data: null });
  }),

  // 修改参数值
  http.put('/api/system/param/value/:id', async ({ params, request }) => {
    const body = await request.json() as { paramValue: string };
    const param = mockParams.find(p => p.id === parseInt(params.id as string));
    if (!param) {
      return HttpResponse.json({ code: 404, success: false, message: '参数不存在', data: null });
    }
    param.paramValue = body.paramValue;
    return HttpResponse.json({ code: 200, success: true, message: '修改成功', data: null });
  }),

  // ==================== 系统配置 ====================

  // 分页查询
  http.get('/api/system/config/list', ({ request }) => {
    const url = new URL(request.url);
    const keyword = url.searchParams.get('keyword') || '';
    const configGroup = url.searchParams.get('configGroup');
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1');
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10');

    let filtered = mockConfigs;
    if (keyword) {
      filtered = filtered.filter(c => c.configKey.includes(keyword) || c.description?.includes(keyword));
    }
    if (configGroup) {
      filtered = filtered.filter(c => c.configGroup === configGroup);
    }

    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: paginate(filtered, pageNum, pageSize),
    });
  }),

  // 详情
  http.get('/api/system/config/:id', ({ params }) => {
    const config = mockConfigs.find(c => c.id === parseInt(params.id as string));
    if (!config) {
      return HttpResponse.json({ code: 404, success: false, message: '配置不存在', data: null });
    }
    return HttpResponse.json({ code: 200, success: true, message: 'success', data: config });
  }),

  // 新增
  http.post('/api/system/config', async ({ request }) => {
    const body = await request.json() as Partial<SysConfig>;
    const newConfig: SysConfig = {
      id: configIdCounter++,
      configKey: body.configKey || '',
      configValue: body.configValue || '',
      configGroup: body.configGroup || 'default',
      description: body.description,
      status: body.status ?? 1,
      createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    };
    mockConfigs.push(newConfig);
    return HttpResponse.json({ code: 200, success: true, message: '新增成功', data: newConfig });
  }),

  // 修改
  http.put('/api/system/config', async ({ request }) => {
    const body = await request.json() as Partial<SysConfig>;
    const index = mockConfigs.findIndex(c => c.id === body.id);
    if (index === -1) {
      return HttpResponse.json({ code: 404, success: false, message: '配置不存在', data: null });
    }
    mockConfigs[index] = { ...mockConfigs[index], ...body };
    return HttpResponse.json({ code: 200, success: true, message: '修改成功', data: mockConfigs[index] });
  }),

  // 删除
  http.delete('/api/system/config/:id', ({ params }) => {
    const index = mockConfigs.findIndex(c => c.id === parseInt(params.id as string));
    if (index === -1) {
      return HttpResponse.json({ code: 404, success: false, message: '配置不存在', data: null });
    }
    mockConfigs.splice(index, 1);
    return HttpResponse.json({ code: 200, success: true, message: '删除成功', data: null });
  }),

  // 按分组获取配置
  http.get('/api/system/config/group/:group', ({ params }) => {
    const configs = mockConfigs.filter(c => c.configGroup === params.group);
    return HttpResponse.json({ code: 200, success: true, message: 'success', data: configs });
  }),

  // 获取所有分组
  http.get('/api/system/config/groups', () => {
    const groups = [...new Set(mockConfigs.map(c => c.configGroup))];
    return HttpResponse.json({ code: 200, success: true, message: 'success', data: groups });
  }),

  // ==================== 登录日志 ====================

  // 分页查询
  http.get('/api/system/log/login/list', ({ request }) => {
    const url = new URL(request.url);
    const keyword = url.searchParams.get('keyword') || '';
    const status = url.searchParams.get('status');
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1');
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10');

    let filtered = mockLoginLogs;
    if (keyword) {
      filtered = filtered.filter(l => l.username.includes(keyword) || l.ip.includes(keyword));
    }
    if (status) {
      filtered = filtered.filter(l => l.status === parseInt(status));
    }

    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: paginate(filtered, pageNum, pageSize),
    });
  }),

  // 统计（必须在 :id 前面，否则会被 :id 匹配）
  http.get('/api/system/log/login/statistics', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: {
        totalCount: mockLoginLogs.length,
        successCount: mockLoginLogs.filter(l => l.status === 1).length,
        failCount: mockLoginLogs.filter(l => l.status === 0).length,
        todayCount: mockLoginLogs.filter(l => l.loginTime.startsWith('2024-03-15')).length,
      },
    });
  }),

  // 清理（必须在 :id 前面）
  http.delete('/api/system/log/login/clean', () => {
    return HttpResponse.json({ code: 200, success: true, message: '清理成功', data: null });
  }),

  // 详情
  http.get('/api/system/log/login/:id', ({ params }) => {
    const log = mockLoginLogs.find(l => l.id === parseInt(params.id as string));
    if (!log) {
      return HttpResponse.json({ code: 404, success: false, message: '日志不存在', data: null });
    }
    return HttpResponse.json({ code: 200, success: true, message: 'success', data: log });
  }),

  // ==================== 操作日志 ====================

  // 分页查询
  http.get('/api/system/log/operation/list', ({ request }) => {
    const url = new URL(request.url);
    const keyword = url.searchParams.get('keyword') || '';
    const username = url.searchParams.get('username');
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1');
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10');

    let filtered = mockOperationLogs;
    if (keyword) {
      filtered = filtered.filter(l => l.operation.includes(keyword) || l.requestUrl.includes(keyword));
    }
    if (username) {
      filtered = filtered.filter(l => l.username === username);
    }

    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: paginate(filtered, pageNum, pageSize),
    });
  }),

  // 清理（必须在 :id 前面）
  http.delete('/api/system/log/operation/clean', () => {
    return HttpResponse.json({ code: 200, success: true, message: '清理成功', data: null });
  }),

  // 导出（必须在 :id 前面）
  http.get('/api/system/log/operation/export', () => {
    return HttpResponse.json({ code: 200, success: true, message: '导出成功', data: null });
  }),

  // 详情
  http.get('/api/system/log/operation/:id', ({ params }) => {
    const log = mockOperationLogs.find(l => l.id === parseInt(params.id as string));
    if (!log) {
      return HttpResponse.json({ code: 404, success: false, message: '日志不存在', data: null });
    }
    return HttpResponse.json({ code: 200, success: true, message: 'success', data: log });
  }),
];
