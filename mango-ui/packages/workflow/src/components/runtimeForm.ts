export type RuntimeFormFieldType = 'input' | 'textarea' | 'password' | 'number' | 'select' | 'radio' | 'checkbox' | 'switch' | 'date' | 'daterange';

export interface RuntimeFormOption {
  label: string;
  value: any;
}

export interface RuntimeFormField {
  key: string;
  label: string;
  type: RuntimeFormFieldType;
  placeholder?: string;
  readonly?: boolean;
  min?: number;
  max?: number;
  step?: number;
  options?: RuntimeFormOption[];
  rules?: any[];
  defaultValue?: any;
}

export function parseRuntimeForm(formJson?: string): { fields: RuntimeFormField[]; unsupported: Array<{ label: string; type: string }> } {
  if (!formJson) {
    return { fields: [], unsupported: [] };
  }
  try {
    const parsed = JSON.parse(formJson);
    const rules = Array.isArray(parsed)
      ? parsed
      : Array.isArray(parsed?.rules)
        ? parsed.rules
        : Array.isArray(parsed?.fields)
          ? customFieldsToRules(parsed.fields)
          : [];
    const fields: RuntimeFormField[] = [];
    const unsupported: Array<{ label: string; type: string }> = [];
    for (const rule of flattenRules(rules)) {
      const field = ruleToRuntimeField(rule);
      if (field) {
        fields.push(field);
      } else if (rule?.field) {
        unsupported.push({ label: String(rule.title || rule.label || rule.field), type: String(rule.type || 'unknown') });
      }
    }
    return { fields, unsupported };
  } catch {
    return { fields: [], unsupported: [{ label: '表单配置', type: 'invalid-json' }] };
  }
}

export function createDefaultVariables(fields: RuntimeFormField[]) {
  return fields.reduce<Record<string, any>>((values, field) => {
    if (field.defaultValue !== undefined) {
      values[field.key] = field.defaultValue;
    } else if (field.type === 'checkbox' || field.type === 'daterange') {
      values[field.key] = [];
    } else if (field.type === 'switch') {
      values[field.key] = false;
    } else {
      values[field.key] = undefined;
    }
    return values;
  }, {});
}

function flattenRules(rules: any[]): any[] {
  return (rules || []).flatMap((rule) => [
    rule,
    ...flattenRules(Array.isArray(rule?.children) ? rule.children : []),
  ]);
}

function customFieldsToRules(fields: any[]) {
  return (fields || []).map((field) => ({
    type: field.type,
    field: field.key || field.field,
    title: field.label || field.title,
    props: {
      placeholder: field.type === 'select' ? `请选择${field.label || field.title || ''}` : `请输入${field.label || field.title || ''}`,
    },
    options: Array.isArray(field.options)
      ? field.options
      : optionsTextToOptions(field.optionsText),
    validate: field.required
      ? [{ required: true, message: `${field.label || field.title || field.key}不能为空`, trigger: 'change' }]
      : [],
  }));
}

function ruleToRuntimeField(rule: any): RuntimeFormField | null {
  if (!rule?.field) {
    return null;
  }
  const mappedType = mapFieldType(rule.type);
  if (!mappedType) {
    return null;
  }
  const label = String(rule.title || rule.label || rule.field);
  const props = rule.props || {};
  return {
    key: String(rule.field),
    label,
    type: mappedType,
    placeholder: props.placeholder || (mappedType === 'select' ? `请选择${label}` : `请输入${label}`),
    readonly: Boolean(props.readonly || props.readOnly),
    min: props.min,
    max: props.max,
    step: props.step,
    options: normalizeOptions(rule.options || props.options),
    rules: normalizeRules(rule.validate || rule.rules, label, mappedType),
    defaultValue: rule.value ?? rule.defaultValue ?? props.defaultValue,
  };
}

function mapFieldType(type?: string): RuntimeFormFieldType | null {
  const normalized = String(type || '');
  if (['input', 'password', 'textarea', 'select', 'radio', 'checkbox', 'switch'].includes(normalized)) {
    return normalized as RuntimeFormFieldType;
  }
  if (['inputNumber', 'number'].includes(normalized)) return 'number';
  if (['datePicker', 'date'].includes(normalized)) return 'date';
  if (['dateRange', 'daterange'].includes(normalized)) return 'daterange';
  return null;
}

function normalizeOptions(options: any): RuntimeFormOption[] {
  if (!Array.isArray(options)) {
    return [];
  }
  return options.map((option) => ({
    label: String(option.label ?? option.name ?? option.value ?? ''),
    value: option.value ?? option.label ?? option.name,
  }));
}

function optionsTextToOptions(optionsText?: string) {
  return String(optionsText || '')
    .split('\n')
    .map(item => item.trim())
    .filter(Boolean)
    .map((line) => {
      const [label, value] = line.split(':');
      return { label: label.trim(), value: (value || label).trim() };
    });
}

function normalizeRules(rules: any[] | undefined, label: string, type: RuntimeFormFieldType) {
  if (!Array.isArray(rules)) {
    return [];
  }
  return rules.map((rule) => ({
    ...rule,
    trigger: rule.trigger || (['input', 'textarea', 'password'].includes(type) ? 'blur' : 'change'),
    message: rule.message || `${label}不能为空`,
  }));
}
