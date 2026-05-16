export type RuntimeFormFieldType =
  | 'input'
  | 'textarea'
  | 'password'
  | 'number'
  | 'select'
  | 'radio'
  | 'checkbox'
  | 'switch'
  | 'date'
  | 'daterange'
  | 'time'
  | 'timerange'
  | 'datetime'
  | 'datetimerange'
  | 'rate'
  | 'slider'
  | 'color'
  | 'cascader'
  | 'treeSelect'
  | 'transfer'
  | 'upload'
  | 'imageUpload'
  | 'editor'
  | 'systemUser'
  | 'systemOrg'
  | 'systemDept'
  | 'systemPost'
  | 'systemRole'
  | 'systemDict'
  | 'businessType'
  | 'signature'
  | 'serialNo'
  | 'alert'
  | 'text'
  | 'html'
  | 'divider'
  | 'tag'
  | 'image'
  | 'button'
  | 'container';

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
  treeOptions?: any[];
  props?: Record<string, any>;
  rules?: any[];
  defaultValue?: any;
  content?: string;
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
    for (const [index, rule] of flattenRules(rules).entries()) {
      const field = ruleToRuntimeField(rule, index);
      if (field) {
        fields.push(field);
      } else if (rule?.field && !isContainerType(rule.type)) {
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
    if (!isValueField(field)) {
      return values;
    }
    if (field.defaultValue !== undefined) {
      values[field.key] = field.defaultValue;
    } else if (isArrayValueField(field)) {
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
  return (fields || []).map((field) => {
    const type = String(field.type || 'input');
    const systemType = isSystemDataType(type) ? type : '';
    const label = field.label || field.title || '';
    return {
      type: systemType === 'systemOrg' ? 'elTreeSelect' : systemType ? 'select' : type,
      field: field.key || field.field,
      title: label,
      props: {
        ...(field.props || {}),
        placeholder: isSelectLikeType(type) ? `请选择${label}` : `请输入${label}`,
        workflowDataType: systemType || field.workflowDataType || field.props?.workflowDataType,
      },
      options: Array.isArray(field.options)
        ? field.options
        : optionsTextToOptions(field.optionsText),
      validate: field.required
        ? [{ required: true, message: `${label || field.key}不能为空`, trigger: 'change' }]
        : [],
    };
  });
}

function ruleToRuntimeField(rule: any, index: number): RuntimeFormField | null {
  if (!rule?.field && !isDisplayType(rule?.type) && !isContainerType(rule?.type)) {
    return null;
  }
  const mappedType = resolveRuntimeType(rule);
  if (!mappedType) {
    return null;
  }
  const label = String(rule.title || rule.label || rule.field);
  const props = rule.props || {};
  const workflowDataType = String(props.workflowDataType || rule.workflowDataType || '');
  const isImageUpload = mappedType === 'imageUpload';
  return {
    key: String(rule.field || `__runtime_${mappedType}_${index}`),
    label,
    type: mappedType,
    placeholder: props.placeholder || (isSelectLikeField(mappedType) ? `请选择${label}` : `请输入${label}`),
    readonly: Boolean(props.readonly || props.readOnly),
    min: props.min,
    max: props.max,
    step: props.step,
    options: normalizeOptions(rule.options || props.options || props.data),
    treeOptions: Array.isArray(props.data) ? props.data : Array.isArray(props.options) ? props.options : [],
    props: {
      ...props,
      workflowDataType,
      multiple: Boolean(props.multiple),
      clearable: props.clearable !== false,
      filterable: props.filterable !== false,
      accept: isImageUpload ? 'image/*' : props.accept,
      limit: Number(props.limit || (isImageUpload ? 6 : mappedType === 'upload' ? 5 : 0)) || undefined,
    },
    rules: normalizeRules(rule.validate || rule.rules, label, mappedType),
    defaultValue: rule.value ?? rule.defaultValue ?? props.defaultValue,
    content: normalizeContent(rule, props),
  };
}

function mapFieldType(type?: string): RuntimeFormFieldType | null {
  const normalized = String(type || '');
  if (['input', 'password', 'textarea', 'select', 'radio', 'checkbox', 'switch', 'rate', 'slider'].includes(normalized)) {
    return normalized as RuntimeFormFieldType;
  }
  if (['inputNumber', 'number'].includes(normalized)) return 'number';
  if (['datePicker', 'date'].includes(normalized)) return 'date';
  if (['dateRange', 'daterange'].includes(normalized)) return 'daterange';
  if (['datetimePicker', 'datetime'].includes(normalized)) return 'datetime';
  if (['datetimeRange', 'datetimerange'].includes(normalized)) return 'datetimerange';
  if (['timePicker', 'time'].includes(normalized)) return 'time';
  if (['timeRange', 'timerange'].includes(normalized)) return 'timerange';
  if (['colorPicker', 'color'].includes(normalized)) return 'color';
  if (['cascader'].includes(normalized)) return 'cascader';
  if (['elTreeSelect', 'treeSelect', 'tree'].includes(normalized)) return 'treeSelect';
  if (['elTransfer', 'transfer'].includes(normalized)) return 'transfer';
  if (['fcEditor', 'editor'].includes(normalized)) return 'editor';
  if (['upload'].includes(normalized)) return 'upload';
  if (['elAlert', 'alert'].includes(normalized)) return 'alert';
  if (['text'].includes(normalized)) return 'text';
  if (['html'].includes(normalized)) return 'html';
  if (['elDivider', 'divider'].includes(normalized)) return 'divider';
  if (['elTag', 'tag'].includes(normalized)) return 'tag';
  if (['elImage', 'image'].includes(normalized)) return 'image';
  if (['elButton', 'button'].includes(normalized)) return 'button';
  if (isContainerType(normalized)) return 'container';
  return null;
}

function resolveRuntimeType(rule: any): RuntimeFormFieldType | null {
  const props = rule?.props || {};
  const workflowDataType = String(props.workflowDataType || rule?.workflowDataType || '');
  if (workflowDataType === 'systemUser') return 'systemUser';
  if (workflowDataType === 'systemOrg') return 'systemOrg';
  if (workflowDataType === 'systemDept') return 'systemDept';
  if (workflowDataType === 'systemPost') return 'systemPost';
  if (workflowDataType === 'systemRole') return 'systemRole';
  if (workflowDataType === 'systemDict') return 'systemDict';
  if (workflowDataType === 'businessType') return 'businessType';
  if (workflowDataType === 'signature') return 'signature';
  if (workflowDataType === 'serialNo') return 'serialNo';
  const mappedType = mapFieldType(rule?.type);
  if (mappedType === 'upload' && (props.listType === 'picture-card' || String(props.accept || '').includes('image'))) {
    return 'imageUpload';
  }
  return mappedType;
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

function normalizeContent(rule: any, props: Record<string, any>) {
  const children = rule?.children;
  if (typeof children === 'string') return children;
  if (Array.isArray(children) && children.every(item => typeof item === 'string')) {
    return children.join('');
  }
  return String(props.content ?? props.html ?? props.title ?? props.text ?? rule?.title ?? '');
}

function isArrayValueField(field: RuntimeFormField) {
  return ['checkbox', 'daterange', 'timerange', 'datetimerange', 'upload', 'imageUpload'].includes(field.type)
    || Boolean(field.props?.multiple);
}

function isValueField(field: RuntimeFormField) {
  return !['alert', 'text', 'html', 'divider', 'tag', 'image', 'button', 'container'].includes(field.type);
}

function isSelectLikeField(type: RuntimeFormFieldType) {
  return ['select', 'radio', 'checkbox', 'cascader', 'treeSelect', 'systemUser', 'systemOrg', 'systemDept', 'systemPost', 'systemRole', 'systemDict', 'businessType', 'upload', 'imageUpload'].includes(type);
}

function isSelectLikeType(type: string) {
  return ['select', 'radio', 'checkbox', 'cascader', 'elTreeSelect', 'treeSelect', 'systemUser', 'systemOrg', 'systemDept', 'systemPost', 'systemRole', 'systemDict', 'businessType', 'upload'].includes(type);
}

function isSystemDataType(type: string) {
  return ['systemUser', 'systemOrg', 'systemDept', 'systemPost', 'systemRole', 'systemDict', 'businessType'].includes(type);
}

function isDisplayType(type?: string) {
  return ['elAlert', 'alert', 'text', 'html', 'elDivider', 'divider', 'elTag', 'tag', 'elImage', 'image', 'elButton', 'button', 'div'].includes(String(type || ''));
}

function isContainerType(type?: string) {
  return ['group', 'subForm', 'tableForm', 'tableFormColumn', 'fcRow', 'FcRow', 'col', 'elCol', 'elCard', 'elTabs', 'elTabPane', 'elCollapse', 'elCollapseItem', 'fcTable', 'space'].includes(String(type || ''));
}
