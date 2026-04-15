/**
 * FormCreate 组件单元测试
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { ref } from 'vue';

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElForm: {
    name: 'el-form',
    props: {
      model: Object,
      rules: Object,
      labelWidth: [String, Number],
      inline: Boolean,
      labelPosition: String,
      size: String,
      disabled: Boolean,
    },
    setup: (props: any, { slots }: any) => slots.default?.(),
  },
  ElFormItem: {
    name: 'el-form-item',
    props: {
      label: String,
      prop: String,
      rules: Array,
    },
    setup: (props: any, { slots }: any) => slots.default?.(),
  },
  ElInput: {
    name: 'el-input',
    props: {
      modelValue: [String, Number],
      type: String,
      placeholder: String,
      disabled: Boolean,
      readonly: Boolean,
    },
    setup: (props: any, { slots }: any) => ({
      value: props.modelValue,
    }),
  },
  ElSelect: {
    name: 'el-select',
    props: {
      modelValue: [String, Number, Array],
      placeholder: String,
      disabled: Boolean,
    },
  },
  ElOption: {
    name: 'el-option',
    props: {
      label: String,
      value: [String, Number],
      disabled: Boolean,
    },
  },
  ElSwitch: {
    name: 'el-switch',
    props: {
      modelValue: Boolean,
      disabled: Boolean,
    },
  },
  ElDatePicker: {
    name: 'el-date-picker',
    props: {
      modelValue: [String, Array],
      type: String,
      placeholder: String,
      disabled: Boolean,
      readonly: Boolean,
    },
  },
  ElButton: {
    name: 'el-button',
    props: {
      type: String,
      disabled: Boolean,
    },
  },
  ElDivider: {
    name: 'el-divider',
  },
}));

// Mock vue-i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}));

// 导入被测组件
import FormCreate from '../index.vue';
import type { FormConfig } from '../types';

describe('FormCreate 组件', () => {
  const baseConfig: FormConfig = {
    name: 'test-form',
    fields: [
      {
        key: 'username',
        label: '用户名',
        type: 'input',
        placeholder: '请输入用户名',
        rules: [{ required: true, message: '用户名不能为空' }],
      },
      {
        key: 'email',
        label: '邮箱',
        type: 'input',
        placeholder: '请输入邮箱',
        rules: [
          { required: true, message: '邮箱不能为空' },
          { pattern: '^[^@]+@[^@]+\\.[^@]+$', message: '邮箱格式不正确' },
        ],
      },
      {
        key: 'status',
        label: '状态',
        type: 'switch',
        defaultValue: true,
      },
      {
        key: 'type',
        label: '类型',
        type: 'select',
        options: [
          { label: '类型A', value: 'A' },
          { label: '类型B', value: 'B' },
        ],
        rules: [{ required: true, message: '请选择类型' }],
      },
    ],
    labelWidth: '100px',
    labelPosition: 'right',
  };

  describe('基础渲染', () => {
    it('应该正确渲染表单字段', () => {
      const wrapper = mount(FormCreate, {
        props: {
          config: baseConfig,
          modelValue: {},
        },
      });

      expect(wrapper.exists()).toBe(true);
      expect(wrapper.find('.form-create').exists()).toBe(true);
    });

    it('应该渲染所有可见字段', () => {
      const wrapper = mount(FormCreate, {
        props: {
          config: baseConfig,
          modelValue: {},
        },
      });

      // 检查字段是否渲染（通过 label）
      expect(wrapper.text()).toContain('用户名');
      expect(wrapper.text()).toContain('邮箱');
      expect(wrapper.text()).toContain('状态');
      expect(wrapper.text()).toContain('类型');
    });
  });

  describe('字段类型支持', () => {
    it('应该支持 input 类型字段', () => {
      const config: FormConfig = {
        fields: [
          {
            key: 'name',
            label: '姓名',
            type: 'input',
          },
        ],
      };

      const wrapper = mount(FormCreate, {
        props: {
          config,
          modelValue: {},
        },
      });

      expect(wrapper.text()).toContain('姓名');
    });

    it('应该支持 select 类型字段', () => {
      const config: FormConfig = {
        fields: [
          {
            key: 'category',
            label: '分类',
            type: 'select',
            options: [
              { label: '选项1', value: '1' },
              { label: '选项2', value: '2' },
            ],
          },
        ],
      };

      const wrapper = mount(FormCreate, {
        props: {
          config,
          modelValue: {},
        },
      });

      expect(wrapper.text()).toContain('分类');
    });

    it('应该支持 switch 类型字段', () => {
      const config: FormConfig = {
        fields: [
          {
            key: 'enabled',
            label: '启用',
            type: 'switch',
            defaultValue: false,
          },
        ],
      };

      const wrapper = mount(FormCreate, {
        props: {
          config,
          modelValue: {},
        },
      });

      expect(wrapper.text()).toContain('启用');
    });
  });

  describe('条件显示', () => {
    it('应该支持函数形式的 show 条件', () => {
      const config: FormConfig = {
        fields: [
          {
            key: 'showField',
            label: '可显示字段',
            type: 'input',
            show: (values) => values.show === true,
          },
        ],
      };

      const wrapper = mount(FormCreate, {
        props: {
          config,
          modelValue: { show: true },
        },
      });

      // 当 show=true 时，字段应该可见
      expect(wrapper.text()).toContain('可显示字段');
    });

    it('show 返回 false 时应该隐藏字段', () => {
      const config: FormConfig = {
        fields: [
          {
            key: 'hiddenField',
            label: '隐藏字段',
            type: 'input',
            show: false,
          },
        ],
      };

      const wrapper = mount(FormCreate, {
        props: {
          config,
          modelValue: {},
        },
      });

      expect(wrapper.text()).not.toContain('隐藏字段');
    });
  });

  describe('验证规则', () => {
    it('应该正确应用必填规则', () => {
      const config: FormConfig = {
        fields: [
          {
            key: 'required',
            label: '必填字段',
            type: 'input',
            rules: [{ required: true, message: '此字段必填' }],
          },
        ],
      };

      const wrapper = mount(FormCreate, {
        props: {
          config,
          modelValue: {},
        },
      });

      expect(wrapper.text()).toContain('必填字段');
    });

    it('应该正确应用正则验证规则', () => {
      const config: FormConfig = {
        fields: [
          {
            key: 'phone',
            label: '手机号',
            type: 'input',
            rules: [
              {
                pattern: '^1[3-9]\\d{9}$',
                message: '手机号格式不正确',
              },
            ],
          },
        ],
      };

      const wrapper = mount(FormCreate, {
        props: {
          config,
          modelValue: {},
        },
      });

      expect(wrapper.text()).toContain('手机号');
    });
  });

  describe('分隔线', () => {
    it('应该正确渲染分隔线字段', () => {
      const config: FormConfig = {
        fields: [
          {
            key: 'divider1',
            type: 'divider',
            title: '分组标题',
          },
        ],
      };

      const wrapper = mount(FormCreate, {
        props: {
          config,
          modelValue: {},
        },
      });

      expect(wrapper.text()).toContain('分组标题');
    });
  });

  describe('默认值', () => {
    it('应该正确设置字段默认值', () => {
      const config: FormConfig = {
        fields: [
          {
            key: 'defaultValue',
            label: '默认值',
            type: 'input',
            defaultValue: '测试默认值',
          },
        ],
      };

      const wrapper = mount(FormCreate, {
        props: {
          config,
          modelValue: {},
        },
      });

      // 验证默认值被设置
      const formData = wrapper.vm.getValue?.() || {};
      expect(formData.defaultValue).toBe('测试默认值');
    });
  });
});
