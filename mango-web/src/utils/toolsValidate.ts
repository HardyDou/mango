/**
 * Tools Validation Utilities
 *
 * Common validation functions for mango-web.
 */

/**
 * 验证 URL
 * @param url 目标 URL
 * @returns 是否为有效 URL
 */
export function verifyUrl(url: string): boolean {
  return /^https?:\/\/(www\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)$/.test(url);
}

/**
 * 验证手机号
 * @param phone 目标手机号
 * @returns 是否为有效手机号
 */
export function verifyMobile(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone);
}

/**
 * 验证邮箱
 * @param email 目标邮箱
 * @returns 是否为有效邮箱
 */
export function verifyEmail(email: string): boolean {
  return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email);
}

/**
 * 验证身份证
 * @param idCard 目标身份证号
 * @returns 是否为有效身份证号
 */
export function verifyIdCard(idCard: string): boolean {
  return /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(idCard);
}

/**
 * 验证银行卡
 * @param bankCard 目标银行卡号
 * @returns 是否为有效银行卡号
 */
export function verifyBankCard(bankCard: string): boolean {
  return /^[1-9]\d{9,29}$/.test(bankCard);
}

/**
 * 验证中文姓名
 * @param name 目标姓名
 * @returns 是否为有效中文姓名
 */
export function verifyChineseName(name: string): boolean {
  return /^[\u4e00-\u9fa5]{2,10}$/.test(name);
}

/**
 * 验证密码强度
 * @param password 目标密码
 * @param minLength 最小长度
 * @returns 是否满足最小长度要求
 */
export function verifyPassword(password: string, minLength = 8): boolean {
  return password.length >= minLength;
}

/**
 * 验证 IP 地址
 * @param ip 目标 IP 地址
 * @returns 是否为有效 IP 地址
 */
export function verifyIP(ip: string): boolean {
  return /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/.test(ip);
}

/**
 * 验证端口号
 * @param port 目标端口号
 * @returns 是否为有效端口号
 */
export function verifyPort(port: string | number): boolean {
  const portNum = typeof port === 'string' ? parseInt(port, 10) : port;
  return Number.isInteger(portNum) && portNum >= 1 && portNum <= 65535;
}

/**
 * 统一批量导出
 */
const toolsValidate = {
  verifyUrl,
  verifyMobile,
  verifyEmail,
  verifyIdCard,
  verifyBankCard,
  verifyChineseName,
  verifyPassword,
  verifyIP,
  verifyPort,
};

export default toolsValidate;
