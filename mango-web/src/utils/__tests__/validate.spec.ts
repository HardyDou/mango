/**
 * validate 工具函数单元测试
 */

import { describe, it, expect } from 'vitest';
import {
  validatePhone,
  validateEmail,
  validateUrl,
  validateIP,
  validateIdCard,
  validatePassword,
  isEmpty,
  hasChinese,
  trim,
  validateNull,
} from '@/utils/validate';

describe('validate 工具函数', () => {
  describe('validatePhone', () => {
    it('应该正确验证有效手机号', () => {
      expect(validatePhone('13812345678')).toBe(true);
      expect(validatePhone('19912345678')).toBe(true);
      expect(validatePhone('18812345678')).toBe(true);
    });

    it('应该拒绝无效手机号', () => {
      expect(validatePhone('12345678901')).toBe(false); // 错误格式
      expect(validatePhone('1381234567')).toBe(false); // 长度不足
      expect(validatePhone('138123456789')).toBe(false); // 长度超限
      expect(validatePhone('abc12345678')).toBe(false); // 包含字母
    });
  });

  describe('validateEmail', () => {
    it('应该正确验证有效邮箱', () => {
      expect(validateEmail('test@example.com')).toBe(true);
      expect(validateEmail('user.name@domain.co.uk')).toBe(true);
    });

    it('应该拒绝无效邮箱', () => {
      expect(validateEmail('invalid')).toBe(false);
      expect(validateEmail('test@')).toBe(false);
      expect(validateEmail('@domain.com')).toBe(false);
    });
  });

  describe('validateUrl', () => {
    it('应该正确验证有效 URL', () => {
      expect(validateUrl('https://example.com')).toBe(true);
      expect(validateUrl('http://example.com')).toBe(true);
      expect(validateUrl('example.com')).toBe(true);
    });

    it('应该拒绝无效 URL', () => {
      expect(validateUrl('')).toBe(false);
    });
  });

  describe('validateIP', () => {
    it('应该正确验证有效 IP', () => {
      expect(validateIP('192.168.1.1')).toBe(true);
      expect(validateIP('127.0.0.1')).toBe(true);
      expect(validateIP('0.0.0.0')).toBe(true);
    });

    it('应该拒绝无效 IP', () => {
      expect(validateIP('256.1.1.1')).toBe(false);
      expect(validateIP('192.168.1')).toBe(false);
      expect(validateIP('abc.def.ghi.jkl')).toBe(false);
    });
  });

  describe('validateIdCard', () => {
    it('应该正确验证有效身份证', () => {
      expect(validateIdCard('123456789012345')).toBe(true); // 15位
      expect(validateIdCard('123456789012345678')).toBe(true); // 18位
      expect(validateIdCard('12345678901234567X')).toBe(true); // 18位含X
    });

    it('应该拒绝无效身份证', () => {
      expect(validateIdCard('123')).toBe(false); // 太短
      expect(validateIdCard('1234567890123456789')).toBe(false); // 太长
    });
  });

  describe('validatePassword', () => {
    it('应该验证密码长度', () => {
      expect(validatePassword('12345678')).toBe(true); // 8位
      expect(validatePassword('1234567890123456')).toBe(true); // 超过8位
      expect(validatePassword('1234567')).toBe(false); // 不足8位
    });

    it('应该支持自定义最小长度', () => {
      expect(validatePassword('12345', 5)).toBe(true);
      expect(validatePassword('1234', 5)).toBe(false);
    });
  });

  describe('isEmpty', () => {
    it('应该正确判断空值', () => {
      expect(isEmpty(null)).toBe(true);
      expect(isEmpty(undefined)).toBe(true);
      expect(isEmpty('')).toBe(true);
      expect(isEmpty([])).toBe(true);
      expect(isEmpty({})).toBe(true);
    });

    it('应该正确判断非空值', () => {
      expect(isEmpty('hello')).toBe(false);
      expect(isEmpty([1, 2, 3])).toBe(false);
      expect(isEmpty({ key: 'value' })).toBe(false);
      expect(isEmpty(0)).toBe(false);
      expect(isEmpty(false)).toBe(false);
    });
  });

  describe('hasChinese', () => {
    it('应该正确检测中文', () => {
      expect(hasChinese('你好')).toBe(true);
      expect(hasChinese('hello你好')).toBe(true);
    });

    it('应该正确检测非中文', () => {
      expect(hasChinese('hello')).toBe(false);
      expect(hasChinese('123456')).toBe(false);
    });
  });

  describe('trim', () => {
    it('应该去除空格', () => {
      expect(trim('  hello  ')).toBe('hello');
      expect(trim('hello world')).toBe('helloworld');
      expect(trim('  ')).toBe('');
    });
  });

  describe('validateNull 别名', () => {
    it('validateNull 应该是 isEmpty 的别名', () => {
      expect(validateNull(null)).toBe(true);
      expect(validateNull('')).toBe(true);
      expect(validateNull('test')).toBe(false);
    });
  });
});
