/**
 * Dict Mock Handlers - 字典管理
 */

import { http, HttpResponse } from 'msw';
import { mockDictTypes, mockDictData } from '../data';
import type { DictType, DictData, DictTypeQuery, DictDataQuery } from '@/api/admin/dict';

// 模拟分页结果
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

// 内存存储（支持增删改）
let dictTypes = [...mockDictTypes];
let dictData = [...mockDictData];
let typeIdCounter = Math.max(...dictTypes.map(t => t.id || 0), 0) + 1;
let dataIdCounter = Math.max(...dictData.map(d => d.id || 0), 0) + 1;

export const dictHandlers = [
  // ==================== 字典类型 ====================

  // 分页查询字典类型
  http.get('/api/system/dict/type/list', ({ request }) => {
    const url = new URL(request.url);
    const keyword = url.searchParams.get('keyword') || '';
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1');
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10');

    // 过滤
    let filtered = dictTypes;
    if (keyword) {
      filtered = dictTypes.filter(
        t => t.name!.includes(keyword) || t.code!.includes(keyword)
      );
    }

    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: paginate(filtered, pageNum, pageSize),
    });
  }),

  // 获取字典类型详情
  http.get('/api/system/dict/type/:id', ({ params }) => {
    const id = parseInt(params.id as string);
    const type = dictTypes.find(t => t.id === id);

    if (!type) {
      return HttpResponse.json({
        code: 404,
        success: false,
        message: '字典类型不存在',
        data: null,
      });
    }

    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: type,
    });
  }),

  // 新增字典类型
  http.post('/api/system/dict/type', async ({ request }) => {
    const body = await request.json() as Partial<DictType>;
    const newType: DictType = {
      id: typeIdCounter++,
      name: body.name || '',
      code: body.code || '',
      description: body.description,
      sort: body.sort || 0,
      status: body.status ?? 1,
      createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    };
    dictTypes.push(newType);

    return HttpResponse.json({
      code: 200,
      success: true,
      message: '新增成功',
      data: newType,
    });
  }),

  // 修改字典类型
  http.put('/api/system/dict/type', async ({ request }) => {
    const body = await request.json() as Partial<DictType>;
    const index = dictTypes.findIndex(t => t.id === body.id);

    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        success: false,
        message: '字典类型不存在',
        data: null,
      });
    }

    dictTypes[index] = { ...dictTypes[index], ...body };
    return HttpResponse.json({
      code: 200,
      success: true,
      message: '修改成功',
      data: dictTypes[index],
    });
  }),

  // 删除字典类型
  http.delete('/api/system/dict/type/:id', ({ params }) => {
    const id = parseInt(params.id as string);
    const index = dictTypes.findIndex(t => t.id === id);

    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        success: false,
        message: '字典类型不存在',
        data: null,
      });
    }

    dictTypes.splice(index, 1);
    // 同时删除关联的字典数据
    dictData = dictData.filter(d => d.typeId !== id);

    return HttpResponse.json({
      code: 200,
      success: true,
      message: '删除成功',
      data: null,
    });
  }),

  // ==================== 字典数据 ====================

  // 分页查询字典数据
  http.get('/api/system/dict/data/list', ({ request }) => {
    const url = new URL(request.url);
    const keyword = url.searchParams.get('keyword') || '';
    const typeId = url.searchParams.get('typeId');
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1');
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10');

    // 过滤
    let filtered = dictData;
    if (typeId) {
      filtered = filtered.filter(d => d.typeId === parseInt(typeId));
    }
    if (keyword) {
      filtered = filtered.filter(
        d => d.label.includes(keyword) || d.value.includes(keyword)
      );
    }

    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: paginate(filtered, pageNum, pageSize),
    });
  }),

  // 获取字典数据详情
  http.get('/api/system/dict/data/:id', ({ params }) => {
    const id = parseInt(params.id as string);
    const item = dictData.find(d => d.id === id);

    if (!item) {
      return HttpResponse.json({
        code: 404,
        success: false,
        message: '字典数据不存在',
        data: null,
      });
    }

    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: item,
    });
  }),

  // 新增字典数据
  http.post('/api/system/dict/data', async ({ request }) => {
    const body = await request.json() as Partial<DictData>;
    const newData: DictData = {
      id: dataIdCounter++,
      typeId: body.typeId || 0,
      label: body.label || '',
      value: body.value || '',
      sort: body.sort || 0,
      status: body.status ?? 1,
      extra: body.extra,
      createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    };
    dictData.push(newData);

    return HttpResponse.json({
      code: 200,
      success: true,
      message: '新增成功',
      data: newData,
    });
  }),

  // 修改字典数据
  http.put('/api/system/dict/data', async ({ request }) => {
    const body = await request.json() as Partial<DictData>;
    const index = dictData.findIndex(d => d.id === body.id);

    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        success: false,
        message: '字典数据不存在',
        data: null,
      });
    }

    dictData[index] = { ...dictData[index], ...body };
    return HttpResponse.json({
      code: 200,
      success: true,
      message: '修改成功',
      data: dictData[index],
    });
  }),

  // 删除字典数据
  http.delete('/api/system/dict/data/:id', ({ params }) => {
    const id = parseInt(params.id as string);
    const index = dictData.findIndex(d => d.id === id);

    if (index === -1) {
      return HttpResponse.json({
        code: 404,
        success: false,
        message: '字典数据不存在',
        data: null,
      });
    }

    dictData.splice(index, 1);
    return HttpResponse.json({
      code: 200,
      success: true,
      message: '删除成功',
      data: null,
    });
  }),

  // 获取指定类型的字典数据（用于下拉选项）
  http.get('/api/system/dict/data/options', ({ request }) => {
    const url = new URL(request.url);
    const typeCode = url.searchParams.get('typeCode') || '';

    // 通过类型编码查找类型ID
    const type = dictTypes.find(t => t.code === typeCode);
    if (!type) {
      return HttpResponse.json({
        code: 200,
        success: true,
        message: 'success',
        data: [],
      });
    }

    const options = dictData.filter(d => d.typeId === type.id && d.status === 1);
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: options,
    });
  }),
];
