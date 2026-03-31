/**
 * Array Operation Utilities
 *
 * Common array manipulation functions for mango-web.
 */

/**
 * 数组去重
 * @param arr 目标数组
 * @returns 去重后的数组
 */
export function unique<T>(arr: T[]): T[] {
  return Array.from(new Set(arr));
}

/**
 * 数组分组
 * @param arr 目标数组
 * @param key 分组 key
 * @returns 分组后的对象
 */
export function groupBy<T>(arr: T[], key: keyof T): Record<string, T[]> {
  return arr.reduce((result, item) => {
    const groupKey = String(item[key]);
    if (!result[groupKey]) {
      result[groupKey] = [];
    }
    result[groupKey].push(item);
    return result;
  }, {} as Record<string, T[]>);
}

/**
 * 数组按 key 排序
 * @param arr 目标数组
 * @param key 排序字段
 * @param asc 是否升序（默认升序）
 * @returns 排序后的数组
 */
export function sortBy<T>(arr: T[], key: keyof T, asc = true): T[] {
  return [...arr].sort((a, b) => {
    const valA = a[key];
    const valB = b[key];
    if (valA < valB) return asc ? -1 : 1;
    if (valA > valB) return asc ? 1 : -1;
    return 0;
  });
}

/**
 * 数组合并（并集）
 * @param arr1 数组1
 * @param arr2 数组2
 * @returns 合并后的数组
 */
export function union<T>(arr1: T[], arr2: T[]): T[] {
  return unique([...arr1, ...arr2]);
}

/**
 * 数组交集
 * @param arr1 数组1
 * @param arr2 数组2
 * @returns 交集数组
 */
export function intersection<T>(arr1: T[], arr2: T[]): T[] {
  return arr1.filter((item) => arr2.includes(item));
}

/**
 * 数组差集
 * @param arr1 数组1
 * @param arr2 数组2
 * @returns 差集数组
 */
export function difference<T>(arr1: T[], arr2: T[]): T[] {
  return arr1.filter((item) => !arr2.includes(item));
}

/**
 * 扁平化数组
 * @param arr 多维数组
 * @returns 扁平化后的一维数组
 */
export function flatten<T>(arr: any[]): T[] {
  return arr.reduce<T[]>((result, item) => {
    return result.concat(Array.isArray(item) ? flatten<T>(item) : item);
  }, []);
}

/**
 * 数组分块
 * @param arr 目标数组
 * @param size 每块大小
 * @returns 分块后的数组
 */
export function chunk<T>(arr: T[], size: number): T[][] {
  const result: T[][] = [];
  for (let i = 0; i < arr.length; i += size) {
    result.push(arr.slice(i, i + size));
  }
  return result;
}

/**
 * 获取数组最后 n 个元素
 * @param arr 目标数组
 * @param n 元素个数
 * @returns 最后 n 个元素
 */
export function takeRight<T>(arr: T[], n = 1): T[] {
  if (n >= arr.length) return arr;
  return arr.slice(-n);
}

/**
 * 数组过滤空值
 * @param arr 目标数组
 * @returns 过滤空值后的数组
 */
export function filterEmpty<T>(arr: (T | null | undefined | '' | 0)[]): T[] {
  return arr.filter((item) => item !== null && item !== undefined && item !== '' && item !== 0) as T[];
}

/**
 * 统一批量导出
 */
const arrayOperation = {
  unique,
  groupBy,
  sortBy,
  union,
  intersection,
  difference,
  flatten,
  chunk,
  takeRight,
  filterEmpty,
};

export default arrayOperation;
