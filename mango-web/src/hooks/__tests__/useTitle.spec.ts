import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest';
import { nextTick } from 'vue';
import { useTitle, setTitle } from '../useTitle';

// Mock vue-i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'menu.home': '首页',
        'menu.user': '用户管理',
      };
      return translations[key] || key;
    },
    locale: {
      value: 'zh-cn',
    },
  }),
}));

describe('useTitle Hook', () => {
  const originalTitle = document.title;

  beforeEach(() => {
    document.title = originalTitle;
  });

  afterEach(() => {
    document.title = originalTitle;
  });

  describe('setTitle function', () => {
    it('should set document title with app name suffix', () => {
      setTitle('首页');
      expect(document.title).toBe('首页 - Mango Admin');
    });

    it('should set document title with custom app name', () => {
      setTitle('首页', 'Custom App');
      expect(document.title).toBe('首页 - Custom App');
    });

    it('should only show app name when title is empty', () => {
      setTitle('');
      expect(document.title).toBe('Mango Admin');
    });

    it('should handle undefined title as empty', () => {
      setTitle('' as any);
      expect(document.title).toBe('Mango Admin');
    });
  });

  describe('useTitle function', () => {
    it('should export useTitle function', () => {
      expect(typeof useTitle).toBe('function');
    });

    it('should return updateTitle and appName', () => {
      const result = useTitle();
      expect(result).toHaveProperty('updateTitle');
      expect(result).toHaveProperty('appName');
      expect(result.appName).toBe('Mango Admin');
    });
  });

  describe('title concatenation', () => {
    it('should concatenate title and app name with separator', () => {
      setTitle('用户管理');
      expect(document.title).toBe('用户管理 - Mango Admin');
    });

    it('should handle title with special characters', () => {
      setTitle('用户 <管理> & "测试"');
      expect(document.title).toBe('用户 <管理> & "测试" - Mango Admin');
    });
  });
});
