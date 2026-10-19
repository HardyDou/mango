/**
 * 表单定义在存储层使用 form-create 规则 JSON（设计器直接产出），
 * 本模块负责规则与「采集项」视图模型之间的互转，供提交列、脱敏、托管渲染和旧数据兼容使用。
 */

export interface InformationFieldOption {
  label: string;
  value: string;
}

export interface InformationFieldDefinition {
  fieldCode: string;
  fieldName: string;
  fieldType: string;
  required?: boolean;
  placeholder?: string;
  options?: InformationFieldOption[];
  content?: string;
  sort?: number;
  /** 子表单/表格表单的行内字段，用于把值里的字段编码展示成标题。 */
  children?: InformationFieldDefinition[];
}

export interface FormCreateRule {
  type?: string;
  field?: string;
  title?: string;
  value?: unknown;
  options?: InformationFieldOption[];
  validate?: Array<Record<string, unknown>>;
  props?: Record<string, unknown>;
  children?: FormCreateRule[];
  [key: string]: unknown;
}

/** 只有带 field 的规则才产生提交值。 */
export function isInputRule(rule?: FormCreateRule | null): boolean {
  return Boolean(rule && typeof rule.field === 'string' && rule.field.trim());
}

/** 子表单/表格表单本身是一个字段，children 是行模板，不再当作顶层字段。 */
const SUBFORM_TYPES = new Set(['group', 'subform', 'tableform']);

/**
 * 把存储的规则转换成设计器可以安全加载的形状。
 *
 * 设计器的子表单/表格表单在加载时会直接读 `props`（`subForm.loadRule` 取 `props.rule`、
 * `tableForm.loadRule` 取 `props.columns[].style`），缺少这些字段会抛错导致画布空白，
 * 因此这里统一补齐 `props`，并把 `children` 还原成设计器期望的结构。
 */
export function toDesignerRules(rules: FormCreateRule[] | undefined): FormCreateRule[] {
  return (rules || []).map(normalizeRuleForDesigner);
}

function normalizeRuleForDesigner(rule: FormCreateRule): FormCreateRule {
  const type = String(rule.type || '').toLowerCase();
  const props: Record<string, unknown> = { ...(rule.props || {}) };
  const children = Array.isArray(rule.children) ? rule.children.map(normalizeRuleForDesigner) : [];

  if (type === 'tableform') {
    props.columns = toDesignerColumns(rule, props);
    // 表格表单的列由 props.columns 描述，children 由设计器自行生成。
    return { ...rule, props, children: [] };
  }

  if (SUBFORM_TYPES.has(type) && children.length) {
    // 子表单/分组的行内字段在设计器里存于 props.rule。
    props.rule = children;
  }

  return { ...rule, props, children };
}

function toDesignerColumns(rule: FormCreateRule, props: Record<string, unknown>): unknown[] {
  const raw = Array.isArray(props.columns) ? props.columns : [];
  const source = raw.length ? raw : Array.isArray(rule.children) ? rule.children : [];
  return source.map((column) => {
    const item = (column || {}) as Record<string, unknown>;
    const style = (item.style || {}) as Record<string, unknown>;
    return {
      label: String(item.label ?? item.title ?? ''),
      required: Boolean(item.required),
      style: { width: String(style.width ?? ''), color: String(style.color ?? '') },
      rule: Array.isArray(item.rule) ? item.rule : [],
    };
  });
}

export function walkRules(rules: FormCreateRule[] | undefined, visit: (rule: FormCreateRule) => void): void {
  for (const rule of rules || []) {
    if (!rule || typeof rule !== 'object') {
      continue;
    }
    visit(rule);
    const type = String(rule.type || '').toLowerCase();
    if (SUBFORM_TYPES.has(type) && isInputRule(rule)) {
      continue;
    }
    if (Array.isArray(rule.children) && rule.children.length) {
      walkRules(rule.children, visit);
    }
  }
}

const DISPLAY_RULE_TYPES = new Set(['elAlert', 'text', 'html', 'elTag', 'elImage', 'elButton', 'elDivider', 'divider']);

function isDisplayRule(rule: FormCreateRule): boolean {
  return DISPLAY_RULE_TYPES.has(String(rule.type || ''));
}

function propsOf(rule: FormCreateRule): Record<string, unknown> {
  return (rule.props || {}) as Record<string, unknown>;
}

function textProp(rule: FormCreateRule, key: string): string {
  const value = propsOf(rule)[key];
  return value === undefined || value === null ? '' : String(value);
}

/** 把 form-create 控件类型归一为采集项类型，用于脱敏、列标题和展示。 */
export function normalizeFieldType(rule: FormCreateRule): string {
  const type = String(rule.type || 'input');
  const nativeType = textProp(rule, 'type').toLowerCase();
  if (type === 'input') {
    if (nativeType === 'tel' || nativeType === 'phone' || nativeType === 'mobile') {
      return 'phone';
    }
    if (nativeType === 'email') {
      return 'email';
    }
    if (nativeType === 'number') {
      return 'number';
    }
    return 'text';
  }
  if (type === 'inputNumber') {
    return 'number';
  }
  if (type === 'datePicker' || type === 'elDatePicker') {
    return 'date';
  }
  if (isDisplayRule(rule)) {
    return type === 'elDivider' || type === 'divider' ? 'divider' : 'static';
  }
  return type;
}

export function ruleRequired(rule: FormCreateRule): boolean {
  if (rule.$required === true) {
    return true;
  }
  return (rule.validate || []).some((item) => Boolean((item as { required?: boolean })?.required));
}

export function rulePlaceholder(rule: FormCreateRule): string {
  return textProp(rule, 'placeholder');
}

export function ruleOptions(rule: FormCreateRule): InformationFieldOption[] {
  const raw: unknown = rule.options;
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
    .filter((item) => Boolean(item) && typeof item === 'object')
    .map((item) => {
      const record = item as unknown as Record<string, unknown>;
      return { label: String(record['label'] ?? record['value'] ?? ''), value: String(record['value'] ?? '') };
    });
}

/** 兼容旧版 `{fieldCode, fieldName, fieldType}` 定义，统一升级为 form-create 规则。 */
export function legacyFieldToRule(field: Record<string, unknown>): FormCreateRule {
  const fieldType = String(field.fieldType || 'text');
  const typeMap: Record<string, string> = {
    text: 'input',
    textarea: 'input',
    number: 'inputNumber',
    phone: 'input',
    email: 'input',
    radio: 'radio',
    checkbox: 'checkbox',
    select: 'select',
    date: 'datePicker',
    divider: 'elDivider',
    static: 'elAlert',
  };
  const props: Record<string, unknown> = {};
  if (field.placeholder) {
    props.placeholder = String(field.placeholder);
  }
  if (fieldType === 'textarea') {
    props.type = 'textarea';
  }
  if (fieldType === 'phone') {
    props.type = 'tel';
  }
  if (fieldType === 'email') {
    props.type = 'email';
  }
  const rule: FormCreateRule = {
    type: typeMap[fieldType] || 'input',
    field: String(field.fieldCode || ''),
    title: String(field.fieldName || ''),
    props,
  };
  if (field.required) {
    rule.validate = [{ required: true, message: `请填写${String(field.fieldName || '')}` }];
  }
  const options = field.options;
  if (Array.isArray(options) && options.length) {
    rule.options = (options as unknown[])
      .filter((item) => Boolean(item) && typeof item === 'object')
      .map((item) => {
        const record = item as unknown as Record<string, unknown>;
        return { label: String(record['label'] ?? ''), value: String(record['value'] ?? '') };
      });
  }
  if (fieldType === 'static' && field.content) {
    rule.props = { ...props, title: String(field.content) };
  }
  return rule;
}

/** 解析表单定义 JSON：既接受 form-create 规则，也接受历史的采集项数组。 */
export function parseRules(definitionJson?: string): FormCreateRule[] {
  if (!definitionJson) {
    return [];
  }
  try {
    const parsed = JSON.parse(definitionJson) as unknown;
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed
      .filter((item): item is Record<string, unknown> => Boolean(item) && typeof item === 'object')
      .map((item) =>
        item.field || item.fieldCode
          ? item.field
            ? (item as FormCreateRule)
            : legacyFieldToRule(item)
          : (item as FormCreateRule),
      );
  } catch {
    return [];
  }
}

/** 子表单/表格表单的行内字段，取值来自列定义或子规则。 */
function rowFieldsOf(rule: FormCreateRule): InformationFieldDefinition[] {
  const columns = (rule.props as Record<string, unknown> | undefined)?.columns;
  if (Array.isArray(columns)) {
    return columns
      .filter((column): column is Record<string, unknown> => Boolean(column) && typeof column === 'object')
      .map((column, index) => {
        const ruleList = Array.isArray(column.rule) ? (column.rule as FormCreateRule[]) : [];
        const inner = ruleList[0];
        return {
          fieldCode: String(inner?.field || `column${index + 1}`),
          fieldName: String(column.label || inner?.title || inner?.field || `第${index + 1}列`),
          fieldType: inner ? normalizeFieldType(inner) : 'input',
          sort: index + 1,
        };
      });
  }
  const children: InformationFieldDefinition[] = [];
  walkRules(rule.children, (child) => {
    if (isInputRule(child)) {
      children.push({
        fieldCode: String(child.field),
        fieldName: String(child.title || child.field),
        fieldType: normalizeFieldType(child),
        sort: children.length + 1,
      });
    }
  });
  return children;
}

const SUBFORM_FIELD_TYPES = new Set(['group', 'subform', 'tableform']);

/** 从规则中提取可提交的采集项，用于提交记录动态列和值展示。 */
export function rulesToFields(rules: FormCreateRule[]): InformationFieldDefinition[] {
  const fields: InformationFieldDefinition[] = [];
  walkRules(rules, (rule) => {
    if (!isInputRule(rule)) {
      return;
    }
    const fieldType = normalizeFieldType(rule);
    fields.push({
      fieldCode: String(rule.field),
      fieldName: String(rule.title || rule.field),
      fieldType,
      required: ruleRequired(rule),
      placeholder: rulePlaceholder(rule),
      options: ruleOptions(rule),
      sort: fields.length + 1,
      ...(SUBFORM_FIELD_TYPES.has(fieldType.toLowerCase()) ? { children: rowFieldsOf(rule) } : {}),
    });
  });
  return fields;
}

/** 设计器预览/托管页面统一使用平台 FormCreate 组件渲染。 */
export interface MangoFormFieldConfig {
  key: string;
  label: string;
  type: string;
  required: boolean;
  placeholder: string;
  options?: InformationFieldOption[];
  title?: string;
  props?: Record<string, unknown>;
}

const MANGO_FIELD_TYPES: Record<string, string> = {
  text: 'input',
  textarea: 'textarea',
  number: 'number',
  phone: 'input',
  email: 'input',
  select: 'select',
  radio: 'radio',
  checkbox: 'checkbox',
  date: 'date',
  divider: 'divider',
  static: 'divider',
};

function fallbackPlaceholder(fieldType: string, label: string): string {
  if (!label) {
    return '';
  }
  if (fieldType === 'select' || fieldType === 'radio' || fieldType === 'checkbox' || fieldType === 'date') {
    return `请选择${label}`;
  }
  return `请输入${label}`;
}

export function rulesToMangoFields(rules: FormCreateRule[]): MangoFormFieldConfig[] {
  const configs: MangoFormFieldConfig[] = [];
  walkRules(rules, (rule) => {
    const fieldType = normalizeFieldType(rule);
    const options = ruleOptions(rule);
    if (!isInputRule(rule)) {
      configs.push({
        key: `display_${configs.length + 1}`,
        label: String(rule.title || ''),
        type: MANGO_FIELD_TYPES[fieldType] || 'divider',
        required: false,
        placeholder: '',
        title: String(rule.title || textProp(rule, 'title') || ''),
      });
      return;
    }
    configs.push({
      key: String(rule.field),
      label: String(rule.title || rule.field),
      type: MANGO_FIELD_TYPES[fieldType] || 'input',
      required: ruleRequired(rule),
      placeholder: rulePlaceholder(rule) || fallbackPlaceholder(fieldType, String(rule.title || rule.field)),
      ...(options.length ? { options } : {}),
      ...(fieldType === 'phone' ? { props: { type: 'tel' } } : {}),
      ...(fieldType === 'email' ? { props: { type: 'email' } } : {}),
    });
  });
  return configs;
}

/**
 * 平台里上传与富文本有现成的 Mango 组件，文件统一落到 mango-file。
 * 渲染前把设计器规则替换为对应 Mango 组件，其余控件保持原样。
 */
export function toRuntimeRules(rules: FormCreateRule[]): FormCreateRule[] {
  const transform = (items: FormCreateRule[]): FormCreateRule[] =>
    (items || []).map((rule) => {
      const next: FormCreateRule = { ...rule };
      if (Array.isArray(rule.children) && rule.children.length) {
        next.children = transform(rule.children);
      }
      const type = String(rule.type || '');
      if (type === 'upload') {
        const limit = Number((rule.props as Record<string, unknown> | undefined)?.limit ?? 1) || 1;
        next.type = 'InformationUploadField';
        next.props = {
          ...(rule.props || {}),
          limit,
          listType: String((rule.props as Record<string, unknown> | undefined)?.listType || 'text'),
          multiple: Boolean((rule.props as Record<string, unknown> | undefined)?.multiple),
        };
        delete (next.props as Record<string, unknown>).action;
        delete (next.props as Record<string, unknown>).onSuccess;
      } else if (type === 'editor') {
        next.type = 'InformationEditorField';
        next.props = { ...(rule.props || {}) };
      }
      return next;
    });
  return transform(rules);
}

/** 让设计器在字段标题/编码缺失时给出可读兜底，避免出现空白字段块。 */
export function normalizeDesignerRules(rules: FormCreateRule[]): FormCreateRule[] {
  const used = new Set<string>();
  walkRules(rules, (rule) => {
    if (!isInputRule(rule)) {
      return;
    }
    if (used.has(String(rule.field))) {
      let index = 2;
      while (used.has(`${rule.field}_${index}`)) {
        index += 1;
      }
      rule.field = `${rule.field}_${index}`;
    }
    used.add(String(rule.field));
    if (!rule.title) {
      rule.title = String(rule.field);
    }
  });
  return rules;
}

/** 列表和详情默认脱敏：手机号保留前三后二，邮箱保留首字符与域名。 */
export function maskSensitiveValue(fieldType: string | undefined, value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  const text = Array.isArray(value) ? value.join('、') : String(value);
  const type = (fieldType || '').toLowerCase();
  if (type.includes('phone') || type.includes('mobile') || type.includes('tel')) {
    return text.length <= 4 ? '***' : `${text.slice(0, 3)}****${text.slice(-2)}`;
  }
  if (type.includes('email')) {
    const at = text.indexOf('@');
    return at <= 1 ? '***' : `${text.slice(0, 1)}***${text.slice(at)}`;
  }
  return text;
}

/** 选择型控件按选项标签展示，其余按脱敏规则展示。 */
/**
 * 采集值展示规则：
 * - 选择型控件优先显示选项标签；
 * - 多选、子表单、表格表单等数组/对象值序列化为可读文本，避免出现 [object Object]；
 * - 其余按脱敏规则展示。
 */
export function displayFieldValue(field: InformationFieldDefinition, value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  const options = field.options || [];
  const selectLike = ['select', 'radio', 'checkbox'].includes(field.fieldType);
  if (options.length && selectLike) {
    const values = Array.isArray(value) ? value : [value];
    return values
      .map((item) => options.find((option) => option.value === String(item))?.label ?? String(item))
      .join('、');
  }
  const labels = new Map((field.children || []).map((child) => [child.fieldCode, child.fieldName]));
  if (Array.isArray(value)) {
    if (!value.length) {
      return '';
    }
    return value.map((item) => formatComplexValue(item, labels)).join('；');
  }
  if (typeof value === 'object') {
    return formatComplexValue(value, labels);
  }
  return maskSensitiveValue(field.fieldType, value);
}

/** 子表单/表格表单等对象值按 `字段: 值` 拼接，保证列表、抽屉和导出都可读。 */
function formatComplexValue(value: unknown, labels?: Map<string, string>): string {
  if (value === null || value === undefined) {
    return '';
  }
  if (typeof value !== 'object') {
    return String(value);
  }
  if (Array.isArray(value)) {
    return value
      .map((item) => formatComplexValue(item, labels))
      .filter(Boolean)
      .join('、');
  }
  const record = value as Record<string, unknown>;
  return Object.entries(record)
    .filter(([, item]) => item !== undefined && item !== null && item !== '')
    .map(([key, item]) => `${labels?.get(key) || key}: ${formatComplexValue(item, labels)}`)
    .join('，');
}

export function stringifyRules(rules: FormCreateRule[]): string {
  return JSON.stringify(rules);
}
