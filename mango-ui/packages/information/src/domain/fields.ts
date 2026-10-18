export type InformationFieldType =
  | 'text'
  | 'textarea'
  | 'number'
  | 'phone'
  | 'email'
  | 'radio'
  | 'checkbox'
  | 'select'
  | 'date'
  | 'file'
  | 'static'
  | 'divider';

export interface InformationFieldOption {
  label: string;
  value: string;
}

export interface InformationFieldDefinition {
  fieldCode: string;
  fieldName: string;
  fieldType: InformationFieldType;
  required?: boolean;
  placeholder?: string;
  options?: InformationFieldOption[];
  content?: string;
  sort?: number;
}

export interface InformationFieldToolboxItem {
  fieldType: InformationFieldType;
  label: string;
  icon: string;
}

export interface InformationFieldToolboxGroup {
  title: string;
  items: InformationFieldToolboxItem[];
}

/** 展示型控件不参与提交值与提交记录动态列。 */
const DISPLAY_ONLY_TYPES: InformationFieldType[] = ['static', 'divider'];

export const FIELD_TOOLBOX_GROUPS: InformationFieldToolboxGroup[] = [
  {
    title: '基础字段',
    items: [
      { fieldType: 'text', label: '单行文本', icon: 'T' },
      { fieldType: 'textarea', label: '多行文本', icon: '▤' },
      { fieldType: 'number', label: '数字', icon: '#' },
      { fieldType: 'phone', label: '手机号', icon: '☎' },
      { fieldType: 'email', label: '邮箱', icon: '@' },
    ],
  },
  {
    title: '选择字段',
    items: [
      { fieldType: 'radio', label: '单选', icon: '◉' },
      { fieldType: 'checkbox', label: '多选', icon: '☑' },
      { fieldType: 'select', label: '下拉选择', icon: '⌄' },
    ],
  },
  {
    title: '日期与附件',
    items: [
      { fieldType: 'date', label: '日期', icon: '▣' },
      { fieldType: 'file', label: '文件上传（P1）', icon: '↑' },
    ],
  },
  {
    title: '展示组件',
    items: [
      { fieldType: 'static', label: '说明文字', icon: '≡' },
      { fieldType: 'divider', label: '分割线', icon: '—' },
    ],
  },
];

/** 首版未实现文件上传，工具箱保留入口但不可添加。 */
export const UNSUPPORTED_FIELD_TYPES: InformationFieldType[] = ['file'];

const FIELD_TYPE_LABELS: Record<InformationFieldType, string> = {
  text: '单行文本',
  textarea: '多行文本',
  number: '数字',
  phone: '手机号',
  email: '邮箱',
  radio: '单选',
  checkbox: '多选',
  select: '下拉选择',
  date: '日期',
  file: '文件上传',
  static: '说明文字',
  divider: '分割线',
};

const NEEDS_OPTIONS: InformationFieldType[] = ['radio', 'checkbox', 'select'];

export function fieldTypeLabel(fieldType?: string): string {
  return FIELD_TYPE_LABELS[(fieldType || '') as InformationFieldType] || fieldType || '-';
}

export function isDisplayOnly(fieldType?: string): boolean {
  return DISPLAY_ONLY_TYPES.includes((fieldType || '') as InformationFieldType);
}

export function needsOptions(fieldType?: string): boolean {
  return NEEDS_OPTIONS.includes((fieldType || '') as InformationFieldType);
}

export function isUnsupported(fieldType?: string): boolean {
  return UNSUPPORTED_FIELD_TYPES.includes((fieldType || '') as InformationFieldType);
}

/** 解析字段定义 JSON；非法内容返回空数组，由调用方决定提示方式。 */
export function parseFields(definitionJson?: string): InformationFieldDefinition[] {
  if (!definitionJson) {
    return [];
  }
  try {
    const parsed = JSON.parse(definitionJson) as unknown;
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed
      .filter((item): item is InformationFieldDefinition => Boolean(item) && typeof item === 'object')
      .map((item, index) => ({
        fieldCode: String(item.fieldCode ?? ''),
        fieldName: String(item.fieldName ?? ''),
        fieldType: (item.fieldType ?? 'text') as InformationFieldType,
        required: Boolean(item.required),
        placeholder: item.placeholder ? String(item.placeholder) : '',
        options: Array.isArray(item.options)
          ? item.options.map((option) => ({ label: String(option?.label ?? ''), value: String(option?.value ?? '') }))
          : undefined,
        content: item.content ? String(item.content) : '',
        sort: typeof item.sort === 'number' ? item.sort : index + 1,
      }))
      .sort((left, right) => (left.sort ?? 0) - (right.sort ?? 0));
  } catch {
    return [];
  }
}

export function stringifyFields(fields: InformationFieldDefinition[]): string {
  return JSON.stringify(
    fields.map((field, index) => ({
      fieldCode: field.fieldCode,
      fieldName: field.fieldName,
      fieldType: field.fieldType,
      required: Boolean(field.required),
      placeholder: field.placeholder || '',
      ...(needsOptions(field.fieldType) ? { options: field.options || [] } : {}),
      ...(field.fieldType === 'static' ? { content: field.content || '' } : {}),
      sort: index + 1,
    })),
  );
}

/** 生成未占用的字段编码，避免同表单内重复。 */
export function nextFieldCode(fields: InformationFieldDefinition[]): string {
  const base = `field${fields.length + 1}`;
  const used = new Set(fields.map((field) => field.fieldCode));
  let candidate = base;
  let index = 1;
  while (used.has(candidate)) {
    index += 1;
    candidate = `${base}_${index}`;
  }
  return candidate;
}

/**
 * 列表和详情默认脱敏：手机号保留前三后二，邮箱保留首字符与域名。
 * 与导出脱敏规则保持一致，避免页面成为个人信息泄露入口。
 */
export function maskSensitiveValue(fieldType: string | undefined, value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  const text = Array.isArray(value) ? value.join('、') : String(value);
  const type = (fieldType || '').toLowerCase();
  if (type.includes('phone') || type.includes('mobile') || type.includes('tel')) {
    if (text.length <= 4) {
      return '***';
    }
    return `${text.slice(0, 3)}****${text.slice(-2)}`;
  }
  if (type.includes('email')) {
    const at = text.indexOf('@');
    return at <= 1 ? '***' : `${text.slice(0, 1)}***${text.slice(at)}`;
  }
  return text;
}

/** 选择型控件按选项标签展示，其余控件按脱敏规则展示。 */
export function displayFieldValue(field: InformationFieldDefinition, value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  if (needsOptions(field.fieldType)) {
    const options = field.options || [];
    const values = Array.isArray(value) ? value : [value];
    const labels = values.map((item) => options.find((option) => option.value === String(item))?.label ?? String(item));
    return labels.join('、');
  }
  return maskSensitiveValue(field.fieldType, value);
}

export function defaultValueFor(field: InformationFieldDefinition): unknown {
  if (field.fieldType === 'checkbox') {
    return [];
  }
  return '';
}
