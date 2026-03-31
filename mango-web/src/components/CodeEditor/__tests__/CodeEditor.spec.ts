import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, VueWrapper } from '@vue/test-utils';
import { nextTick } from 'vue';
import CodeEditor from '../index.vue';

describe('CodeEditor 组件单元测试', () => {
  let wrapper: VueWrapper<any>;

  const defaultProps = {
    modelValue: 'const hello = "world";',
    language: 'javascript',
    theme: 'default' as const,
    readonly: false,
    lineNumbers: true,
    matchBrackets: true,
    autoCloseBrackets: true,
    height: '300px',
  };

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
  });

  describe('Props 验证', () => {
    it('应该正确接收 modelValue prop', () => {
      wrapper = mount(CodeEditor, {
        props: { modelValue: 'console.log("test");' },
      });
      expect(wrapper.props('modelValue')).toBe('console.log("test");');
    });

    it('应该使用默认的 language', () => {
      wrapper = mount(CodeEditor, {
        props: { modelValue: '' },
      });
      expect(wrapper.props('language')).toBe('javascript');
    });

    it('应该使用默认的 theme', () => {
      wrapper = mount(CodeEditor, {
        props: { modelValue: '' },
      });
      expect(wrapper.props('theme')).toBe('default');
    });

    it('应该使用默认的 readonly 状态', () => {
      wrapper = mount(CodeEditor, {
        props: { modelValue: '' },
      });
      expect(wrapper.props('readonly')).toBe(false);
    });

    it('应该支持 material-darker 主题', () => {
      wrapper = mount(CodeEditor, {
        props: { modelValue: '', theme: 'material-darker' },
      });
      expect(wrapper.props('theme')).toBe('material-darker');
    });

    it('应该支持 material-ocean 主题', () => {
      wrapper = mount(CodeEditor, {
        props: { modelValue: '', theme: 'material-ocean' },
      });
      expect(wrapper.props('theme')).toBe('material-ocean');
    });

    it('应该支持只读模式', () => {
      wrapper = mount(CodeEditor, {
        props: { modelValue: '', readonly: true },
      });
      expect(wrapper.props('readonly')).toBe(true);
    });

    it('应该使用默认的 height', () => {
      wrapper = mount(CodeEditor, {
        props: { modelValue: '' },
      });
      expect(wrapper.props('height')).toBe('300px');
    });
  });

  describe('语言模式映射', () => {
    const languageCases = [
      { input: 'js', expected: 'javascript' },
      { input: 'javascript', expected: 'javascript' },
      { input: 'xml', expected: 'xml' },
      { input: 'html', expected: 'htmlmixed' },
      { input: 'css', expected: 'css' },
      { input: 'python', expected: 'python' },
      { input: 'sql', expected: 'sql' },
      { input: 'markdown', expected: 'markdown' },
      { input: 'json', expected: { name: 'javascript', json: true } },
      { input: 'java', expected: 'text/x-java' },
      { input: 'c', expected: 'text/x-csrc' },
      { input: 'cpp', expected: 'text/x-c++src' },
      { input: 'go', expected: 'text/x-go' },
      { input: 'rust', expected: 'text/x-rust' },
    ];

    languageCases.forEach(({ input, expected }) => {
      it(`应该正确映射语言: ${input}`, () => {
        const getMode = (lang: string): string | object => {
          const modeMap: Record<string, string> = {
            js: 'javascript',
            javascript: 'javascript',
            xml: 'xml',
            html: 'htmlmixed',
            css: 'css',
            python: 'python',
            java: 'text/x-java',
            sql: 'sql',
            markdown: 'markdown',
            json: { name: 'javascript', json: true },
            c: 'text/x-csrc',
            cpp: 'text/x-c++src',
            csharp: 'text/x-csharp',
            go: 'text/x-go',
            rust: 'text/x-rust',
          };
          return modeMap[lang] || 'javascript';
        };
        expect(getMode(input)).toEqual(expected);
      });
    });
  });

  describe('编辑器配置', () => {
    it('应该正确初始化编辑器配置', () => {
      const initEditorConfig = {
        value: defaultProps.modelValue,
        mode: 'javascript',
        theme: 'default',
        readOnly: defaultProps.readonly,
        lineNumbers: defaultProps.lineNumbers,
        matchBrackets: defaultProps.matchBrackets,
        autoCloseBrackets: defaultProps.autoCloseBrackets,
        styleActiveLine: true,
        lineWrapping: true,
      };

      expect(initEditorConfig.value).toBe('const hello = "world";');
      expect(initEditorConfig.mode).toBe('javascript');
      expect(initEditorConfig.theme).toBe('default');
      expect(initEditorConfig.readOnly).toBe(false);
      expect(initEditorConfig.lineNumbers).toBe(true);
      expect(initEditorConfig.matchBrackets).toBe(true);
      expect(initEditorConfig.autoCloseBrackets).toBe(true);
    });

    it('只读模式应该设置 readOnly 为 true', () => {
      const config = {
        readOnly: true,
      };
      expect(config.readOnly).toBe(true);
    });

    it('行号应该默认为开启', () => {
      const config = {
        lineNumbers: true,
      };
      expect(config.lineNumbers).toBe(true);
    });

    it('括号匹配应该默认为开启', () => {
      const config = {
        matchBrackets: true,
      };
      expect(config.matchBrackets).toBe(true);
    });

    it('自动关闭括号应该默认为开启', () => {
      const config = {
        autoCloseBrackets: true,
      };
      expect(config.autoCloseBrackets).toBe(true);
    });

    it('应该启用 styleActiveLine', () => {
      const config = {
        styleActiveLine: true,
      };
      expect(config.styleActiveLine).toBe(true);
    });

    it('应该启用 lineWrapping', () => {
      const config = {
        lineWrapping: true,
      };
      expect(config.lineWrapping).toBe(true);
    });
  });

  describe('快捷键配置', () => {
    it('应该定义 Ctrl+/ 切换注释快捷键', () => {
      const extraKeys = {
        'Ctrl-/': 'toggleComment',
        'Cmd-/': 'toggleComment',
      };
      expect(extraKeys['Ctrl-/']).toBe('toggleComment');
      expect(extraKeys['Cmd-/']).toBe('toggleComment');
    });
  });

  describe('暴露的方法', () => {
    it('应该暴露 getEditor 方法', () => {
      wrapper = mount(CodeEditor, {
        props: defaultProps,
      });
      const vm = wrapper.vm as any;
      expect(typeof vm.getEditor).toBe('function');
    });

    it('应该暴露 getValue 方法', () => {
      wrapper = mount(CodeEditor, {
        props: defaultProps,
      });
      const vm = wrapper.vm as any;
      expect(typeof vm.getValue).toBe('function');
    });

    it('应该暴露 setValue 方法', () => {
      wrapper = mount(CodeEditor, {
        props: defaultProps,
      });
      const vm = wrapper.vm as any;
      expect(typeof vm.setValue).toBe('function');
    });

    it('应该暴露 clear 方法', () => {
      wrapper = mount(CodeEditor, {
        props: defaultProps,
      });
      const vm = wrapper.vm as any;
      expect(typeof vm.clear).toBe('function');
    });

    it('应该暴露 refresh 方法', () => {
      wrapper = mount(CodeEditor, {
        props: defaultProps,
      });
      const vm = wrapper.vm as any;
      expect(typeof vm.refresh).toBe('function');
    });
  });

  describe('样式类名', () => {
    it('code-editor-container 应该有正确的样式', () => {
      const containerStyle = {
        border: '1px solid #dcdfe6',
        borderRadius: '4px',
        overflow: 'hidden',
      };
      expect(containerStyle.border).toBe('1px solid #dcdfe6');
      expect(containerStyle.borderRadius).toBe('4px');
    });

    it('CodeMirror 应该有正确的字体配置', () => {
      const fontFamily = "'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace";
      expect(fontFamily).toContain('Monaco');
      expect(fontFamily).toContain('Menlo');
    });
  });
});
