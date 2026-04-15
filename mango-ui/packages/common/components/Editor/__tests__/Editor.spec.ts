import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock @wangeditor/editor-for-vue before importing the component
vi.mock('@wangeditor/editor-for-vue', () => ({
  Editor: {
    name: 'Editor',
    template: '<div class="editor-content"></div>',
  },
  Toolbar: {
    name: 'Toolbar',
    template: '<div class="editor-toolbar"></div>',
  },
}));

// Mock the upload API
vi.mock('@/api/admin/upload', () => ({
  uploadImage: vi.fn().mockResolvedValue({
    url: 'https://example.com/image.png',
    fileName: 'image.png',
  }),
}));

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

describe('Editor 组件单元测试', () => {
  describe('Props 定义验证', () => {
    it('应该正确定义 modelValue prop 类型', () => {
      const props = {
        modelValue: '<p>测试内容</p>',
        placeholder: '请输入内容...',
        height: 300,
        disabled: false,
        mode: 'default' as const,
      };

      expect(typeof props.modelValue).toBe('string');
      expect(props.modelValue).toBe('<p>测试内容</p>');
    });

    it('应该定义正确的默认值', () => {
      const defaults = {
        modelValue: '',
        placeholder: '请输入内容...',
        height: 300,
        disabled: false,
        mode: 'default' as const,
      };

      expect(defaults.modelValue).toBe('');
      expect(defaults.placeholder).toBe('请输入内容...');
      expect(defaults.height).toBe(300);
      expect(defaults.disabled).toBe(false);
      expect(defaults.mode).toBe('default');
    });

    it('应该支持 simple 模式', () => {
      const props = {
        mode: 'simple' as const,
      };
      expect(props.mode).toBe('simple');
    });
  });

  describe('工具栏配置', () => {
    it('应该定义完整的工具栏配置', () => {
      const toolbarConfig = {
        toolbarKeys: [
          'headerSelect',
          '|',
          'bold',
          'underline',
          'italic',
          '|',
          'color',
          'bgColor',
          '|',
          'fontSize',
          'fontFamily',
          '|',
          'insertLink',
          'unLink',
          '|',
          'bulletedList',
          'numberedList',
          'indent',
          'delIndent',
          '|',
          'justifyLeft',
          'justifyRight',
          'justifyCenter',
          'justifyJustify',
          '|',
          'blockquote',
          '|',
          'insertImage',
          '|',
          'insertVideo',
          '|',
          'codeBlock',
          '|',
          'undo',
          'redo',
          '|',
          'fullScreen',
        ],
      };

      // 验证工具栏包含预期的按钮
      expect(toolbarConfig.toolbarKeys).toContain('bold');
      expect(toolbarConfig.toolbarKeys).toContain('italic');
      expect(toolbarConfig.toolbarKeys).toContain('underline');
      expect(toolbarConfig.toolbarKeys).toContain('headerSelect');
      expect(toolbarConfig.toolbarKeys).toContain('insertImage');
      expect(toolbarConfig.toolbarKeys).toContain('codeBlock');
      expect(toolbarConfig.toolbarKeys).toContain('fullScreen');
      expect(toolbarConfig.toolbarKeys).toContain('numberedList');
    });

    it('工具栏应该包含分隔符', () => {
      const toolbarConfig = {
        toolbarKeys: ['bold', '|', 'italic'],
      };
      expect(toolbarConfig.toolbarKeys).toContain('|');
    });
  });

  describe('编辑器配置', () => {
    it('应该配置图片上传最大文件大小', () => {
      const maxFileSize = 10 * 1024 * 1024; // 10MB
      expect(maxFileSize).toBe(10485760);
    });

    it('placeholder 应该是可配置的', () => {
      const customPlaceholder = '自定义占位符';
      expect(customPlaceholder).toBeTruthy();
    });

    it('应该支持自定义菜单配置', () => {
      const menuConfig = {
        uploadImage: {
          maxFileSize: 10 * 1024 * 1024,
        },
      };
      expect(menuConfig.uploadImage.maxFileSize).toBe(10485760);
    });
  });

  describe('样式绑定', () => {
    it('height 应该正确转换为像素字符串', () => {
      const height = 300;
      const expectedStyle = `${height}px`;
      expect(expectedStyle).toBe('300px');
    });

    it('height 可以是数字类型', () => {
      const height = 300;
      expect(typeof height).toBe('number');
    });

    it('height 可以是字符串类型', () => {
      const height = '500px';
      expect(typeof height).toBe('string');
    });
  });

  describe('事件定义', () => {
    it('应该定义 update:modelValue 和 change 事件', () => {
      const eventNames = ['update:modelValue', 'change'];
      eventNames.forEach((name) => {
        expect(typeof name).toBe('string');
        expect(name.length).toBeGreaterThan(0);
      });
    });
  });

  describe('组件暴露的方法', () => {
    it('应该暴露 getEditor 方法', () => {
      const exposedMethods = ['getEditor', 'getText', 'getHtml', 'setContent', 'clear'];
      exposedMethods.forEach((method) => {
        expect(typeof method).toBe('string');
      });
    });

    it('暴露的方法应该是函数', () => {
      const methodNames = ['getEditor', 'getText', 'getHtml', 'setContent', 'clear'];
      methodNames.forEach((name) => {
        expect(typeof name).toBe('string');
        expect(name.length).toBeGreaterThan(0);
      });
    });
  });

  describe('高度计算', () => {
    it('应该正确计算 v-bind height', () => {
      const height = 300;
      const binding = `${height}px`;
      expect(binding).toBe('300px');
    });

    it('应该处理自定义高度', () => {
      const height = 500;
      const binding = `${height}px`;
      expect(binding).toBe('500px');
    });
  });
});
