/**
 * FormCreate Component Types - Dynamic Form Generator
 */

/** 表单字段类型 */
export type FormFieldType =
  | 'input'      // 文本输入
  | 'textarea'   // 多行文本
  | 'number'     // 数字输入
  | 'password'   // 密码输入
  | 'select'     // 下拉选择
  | 'radio'      // 单选按钮
  | 'checkbox'   // 多选框
  | 'switch'     // 开关
  | 'date'       // 日期选择
  | 'datetime'   // 日期时间选择
  | 'daterange'  // 日期范围
  | 'upload'     // 文件上传
  | 'cascader'   // 级联选择
  | 'tree-select' // 树形选择
  | 'divider';    // 分隔线

/** 表单字段验证规则 */
export interface FormValidateRule {
  /** 是否必填 */
  required?: boolean;
  /** 错误提示信息 */
  message?: string;
  /** 正则表达式 */
  pattern?: string | RegExp;
  /** 最小值/最小长度 */
  min?: number;
  /** 最大值/最大长度 */
  max?: number;
  /** 自定义验证函数 */
  validator?: (value: any, callback: (error?: Error) => void) => void;
}

/** 表单字段配置 */
export interface FormField {
  /** 字段唯一标识 */
  key: string;
  /** 字段标题/标签 */
  label?: string;
  /** 字段类型 */
  type: FormFieldType;
  /** 默认值 */
  defaultValue?: any;
  /** 占位提示 */
  placeholder?: string;
  /** 是否禁用 */
  disabled?: boolean;
  /** 是否只读 */
  readonly?: boolean;
  /** 是否显示（条件显示） */
  visible?: boolean;
  /** 是否显示（条件显示，支持函数） */
  show?: boolean | ((values: Record<string, any>) => boolean);
  /** 验证规则 */
  rules?: FormValidateRule[];
  /** 下拉选项（select/radio/checkbox使用） */
  options?: SelectOption[];
  /** 级联选项（cascader使用） */
  cascaderOptions?: CascaderOption[];
  /** 树形数据（tree-select使用） */
  treeData?: TreeSelectOption[];
  /** 上传配置 */
  uploadConfig?: UploadConfig;
  /** 日期格式化 */
  format?: string;
  /** 日期范围分隔符 */
  separator?: string;
  /** 步进值（number使用） */
  step?: number;
  /** 前缀 */
  prefix?: string;
  /** 后缀 */
  suffix?: string;
  /** 分隔线标题（divider使用） */
  title?: string;
  /** 额外属性 */
  props?: Record<string, any>;
}

/** 下拉选项 */
export interface SelectOption {
  label: string;
  value: any;
  disabled?: boolean;
}

/** 级联选项 */
export interface CascaderOption {
  label: string;
  value: any;
  children?: CascaderOption[];
  disabled?: boolean;
}

/** 树形选择选项 */
export interface TreeSelectOption {
  label: string;
  value: any;
  children?: TreeSelectOption[];
  disabled?: boolean;
}

/** 上传配置 */
export interface UploadConfig {
  /** 上传地址 */
  action?: string;
  /** 上传类型 */
  accept?: string;
  /** 文件大小限制（MB） */
  maxSize?: number;
  /** 最大文件数 */
  limit?: number;
  /** 上传额外参数 */
  data?: Record<string, any>;
  /** 是否多选 */
  multiple?: boolean;
}

/** 表单配置 */
export interface FormConfig {
  /** 表单唯一标识 */
  name?: string;
  /** 表单字段列表 */
  fields: FormField[];
  /** 表单宽度 */
  labelWidth?: string | number;
  /** 是否行内布局 */
  inline?: boolean;
  /** 标签位置 */
  labelPosition?: 'left' | 'right' | 'top';
  /** 表单尺寸 */
  size?: 'large' | 'medium' | 'small';
  /** 是否显示必填星号 */
  showRequiredMark?: boolean;
}

/** 表单数据 */
export type FormData = Record<string, any>;

/** FormCreate Props */
export interface FormCreateProps {
  /** 表单配置 */
  config: FormConfig;
  /** 表单数据 */
  modelValue?: FormData;
  /** 是否禁用 */
  disabled?: boolean;
  /** 是否只读 */
  readonly?: boolean;
}

/** FormCreate Emits */
export interface FormCreateEmits {
  (e: 'update:modelValue', value: FormData): void;
  (e: 'submit', value: FormData): void;
  (e: 'reset'): void;
  (e: 'validate', valid: boolean): void;
}

/** FormCreate Expose */
export interface FormCreateExpose {
  /** 获取表单数据 */
  getValue(): FormData;
  /** 设置表单数据 */
  setValue(value: FormData): void;
  /** 重置表单 */
  reset(): void;
  /** 验证表单 */
  validate(): Promise<boolean>;
  /** 获取指定字段值 */
  getFieldValue(key: string): any;
  /** 设置指定字段值 */
  setFieldValue(key: string, value: any): void;
  /** 显示指定字段 */
  showField(key: string): void;
  /** 隐藏指定字段 */
  hideField(key: string): void;
  /** 启用指定字段 */
  enableField(key: string): void;
  /** 禁用指定字段 */
  disableField(key: string): void;
}
