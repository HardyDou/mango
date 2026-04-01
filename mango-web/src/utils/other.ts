/**
 * Utility Functions
 *
 * Core utilities migrated from pigx-ui for mango-web infrastructure parity.
 */

import { nextTick } from 'vue';
import { validateNull } from './validate';

/**
 * 树节点基础接口
 */
interface TreeNode {
  [key: string]: unknown;
  children?: TreeNode[];
}

/**
 * 图片懒加载数据项接口
 */
interface LazyImgItem {
  loading?: boolean;
  [key: string]: unknown;
}

/**
 * 图片懒加载
 * @param el dom 目标元素选择器
 * @param arr 列表数据
 * @description 使用 IntersectionObserver 实现图片懒加载
 */
export const lazyImg = (el: string, arr: LazyImgItem[]): void => {
  const io = new IntersectionObserver((res) => {
    res.forEach((v) => {
      if (v.isIntersecting) {
        const target = v.target as HTMLImageElement & { dataset: { img?: string; key?: string } };
        const { img, key } = target.dataset;
        if (img) target.src = img;
        target.onload = () => {
          io.unobserve(target);
          if (key !== undefined && arr[Number(key)]) {
            arr[Number(key)].loading = false;
          }
        };
      }
    });
  });
  nextTick(() => {
    document.querySelectorAll<HTMLImageElement>(el).forEach((img) => io.observe(img));
  });
};

/**
 * 列表结构转树结构
 * @param data 平铺的数组数据
 * @param id  ID 字段名
 * @param parentId 父级 ID 字段名
 * @param children 子级字段名
 * @param rootId 根节点父级 ID
 * @returns 树结构数据
 */
export function handleTree<T extends TreeNode = TreeNode>(
  data: T[],
  id = 'id',
  parentId = 'parentId',
  children = 'children',
  rootId?: string | number
): T[] {
  const safeId = id || 'id';
  const safeParentId = parentId || 'parentId';
  const safeChildren = children || 'children';

  // 计算 rootId（处理空数组情况）
  if (rootId === undefined) {
    const parentIds = data.map((item) => item[safeParentId]);
    const validParentIds = parentIds.filter((pid) => pid !== undefined && pid !== null);
    rootId = validParentIds.length > 0 ? Math.min(...validParentIds.map((p) => Number(p))) : 0;
  }

  // 对源数据深度克隆
  const cloneData: TreeNode[] = JSON.parse(JSON.stringify(data));

  // 循环所有项
  const treeData = cloneData.filter((father) => {
    const branchArr = cloneData.filter((child) => father[safeId] === child[safeParentId]);
    if (branchArr.length > 0) {
      father[safeChildren] = branchArr as TreeNode[];
    }
    return father[safeParentId] === rootId;
  });

  return (treeData.length > 0 ? treeData : data) as T[];
}

/**
 * 生成唯一 UUID
 * @returns RFC4122 compliant UUID
 */
export function generateUUID(): string {
  if (typeof crypto === 'object') {
    if (typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    if (typeof crypto.getRandomValues === 'function' && typeof Uint8Array === 'function') {
      const callback = (c: string): string => {
        const num = Number(c);
        return (num ^ (crypto.getRandomValues(new Uint8Array(1))[0] & (15 >> (num / 4)))).toString(16);
      };
      return '10000000-1000-4000-8000-100000000000'.replace(/[018]/g, callback);
    }
  }
  let timestamp = new Date().getTime();
  let performanceNow = (typeof performance !== 'undefined' && performance.now && performance.now() * 1000) || 0;
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    let random = Math.random() * 16;
    if (timestamp > 0) {
      random = (timestamp + random) % 16 | 0;
      timestamp = Math.floor(timestamp / 16);
    } else {
      random = (performanceNow + random) % 16 | 0;
      performanceNow = Math.floor(performanceNow / 16);
    }
    return (c === 'x' ? random : (random & 0x3) | 0x8).toString(16);
  });
}

/**
 * 获取 URL 参数
 * @param url 目标 URL
 * @param paraName 参数名
 * @returns 参数值
 */
export function getQueryString(url: string, paraName: string): string {
  const arrObj = url.split('?');
  if (arrObj.length > 1) {
    const arrPara = arrObj[1].split('&');
    for (const param of arrPara) {
      const [key, value] = param.split('=');
      if (key === paraName) {
        return value ?? ''; // 返回空字符串而非 undefined
      }
    }
  }
  return '';
}

/**
 * 自动适配不同的后端架构
 * 1. 微服务架构: 保持原路径
 * 2. 单体架构: 自动添加 /admin 前缀
 * @param originUrl 原始路径
 */
export function adaptationUrl(originUrl?: string): string {
  const isMicro = import.meta.env.VITE_IS_MICRO;
  if (validateNull(isMicro) || isMicro === 'true') {
    return originUrl || '';
  }

  // 特殊路径保持不变
  if (!originUrl) return '';

  if (originUrl.startsWith('/gen/') || originUrl.startsWith('/oauth/')) {
    return originUrl;
  }

  // 转为 /admin 路由前缀的请求
  if (originUrl.startsWith('/admin')) {
    return originUrl;
  }

  return `/admin/${originUrl.split('/').splice(2).join('/')}`;
}

/**
 * 获取不重复的 ID
 * @param length ID 的长度
 * @returns 不重复的 ID 字符串
 */
export function getNonDuplicateID(length = 8): string {
  let idStr = Date.now().toString(36);
  idStr += Math.random().toString(36).substring(3, length);
  return idStr;
}

/**
 * 判断数组对象中所有属性是否为空，为空则删除当前行对象
 * @param list 数组对象
 * @returns 删除空值后的数组对象
 */
export function handleEmpty<T extends Record<string, unknown>>(list: T[]): T[] {
  return list.filter((item) => {
    const values = Object.values(item);
    return !values.every((v) => v === '' || v === null || v === undefined);
  });
}

/**
 * 打开小窗口
 * @param url 目标 URL
 * @param title 窗口标题
 * @param w 窗口宽度
 * @param h 窗口高度
 */
export const openWindow = (url: string, title: string, w: number, h: number): Window | null => {
  const dualScreenLeft = window.screenLeft !== undefined ? window.screenLeft : screen.left;
  const dualScreenTop = window.screenTop !== undefined ? window.screenTop : screen.top;

  const width = window.innerWidth || document.documentElement.clientWidth || screen.width;
  const height = window.innerHeight || document.documentElement.clientHeight || screen.height;

  const left = width / 2 - w / 2 + dualScreenLeft;
  const top = height / 2 - h / 2 + dualScreenTop;

  return window.open(
    url,
    title,
    `toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=no, resizable=yes, copyhistory=no, width=${w}, height=${h}, top=${top}, left=${left}`
  );
};

/**
 * 判断是否是移动端
 */
export function isMobile(): boolean {
  return /phone|pad|pod|iPhone|iPod|ios|iPad|Android|Mobile|BlackBerry|IEMobile|MQQBrowser|JUC|Fennec|wOSBrowser|BrowserNG|WebOS|Symbian|Windows Phone/i.test(
    navigator.userAgent
  );
}

/**
 * 对象深克隆
 * @param obj 源对象
 * @returns 克隆后的对象
 */
export function deepClone<T>(obj: T): T {
  // 使用原生 structuredClone（现代浏览器/API）
  if (typeof structuredClone === 'function') {
    return structuredClone(obj);
  }

  // 降级方案：JSON 序列化（无法处理 undefined、Symbol、函数等）
  return JSON.parse(JSON.stringify(obj)) as T;
}

/**
 * 统一批量导出
 */
const other = {
  lazyImg,
  handleTree,
  generateUUID,
  getQueryString,
  adaptationUrl,
  getNonDuplicateID,
  handleEmpty,
  openWindow,
  isMobile,
  deepClone,
};

export default other;
