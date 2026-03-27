/**
 * 校验手机号
 */
export function validatePhone(phone: string): boolean {
  const reg = /^1[3-9]\d{9}$/;
  return reg.test(phone);
}

/**
 * 校验邮箱
 */
export function validateEmail(email: string): boolean {
  const reg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  return reg.test(email);
}

/**
 * 校验 URL
 */
export function validateUrl(url: string): boolean {
  const reg = /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([\/\w .-]*)*\/?$/;
  return reg.test(url);
}

/**
 * 校验 IP 地址
 */
export function validateIP(ip: string): boolean {
  const reg = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
  return reg.test(ip);
}

/**
 * 校验身份证
 */
export function validateIdCard(idCard: string): boolean {
  const reg = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/;
  return reg.test(idCard);
}

/**
 * 校验密码强度
 * @param password 密码
 * @param minLength 最小长度
 */
export function validatePassword(password: string, minLength: number = 8): boolean {
  if (password.length < minLength) return false;
  return true;
}

/**
 * 校验是否为空
 */
export function isEmpty(value: any): boolean {
  if (value === null || value === undefined || value === '') return true;
  if (Array.isArray(value) && value.length === 0) return true;
  if (typeof value === 'object' && Object.keys(value).length === 0) return true;
  return false;
}

/**
 * 校验是否包含中文
 */
export function hasChinese(str: string): boolean {
  const reg = /[\u4e00-\u9fa5]/;
  return reg.test(str);
}

/**
 * 去除字符串空格
 */
export function trim(str: string): string {
  return str.replace(/\s+/g, '');
}
