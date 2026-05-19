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
  children?: RuntimeFormField[];
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
    const fields = flattenRules(rules)
      .map((rule, index) => ruleToRuntimeField(rule, index, `${index}`))
      .filter(Boolean) as RuntimeFormField[];
    const unsupported = collectUnsupportedRules(rules);
    return { fields, unsupported };
  } catch {
    return { fields: [], unsupported: [{ label: '表单配置', type: 'invalid-json' }] };
  }
}

export function createDefaultVariables(fields: RuntimeFormField[]) {
  const values: Record<string, any> = {};
  fields.forEach(field => fillDefaultVariable(values, field));
  return values;
}

function fillDefaultVariable(values: Record<string, any>, field: RuntimeFormField) {
  if (field.children?.length) {
    field.children.forEach(child => fillDefaultVariable(values, child));
  }
  if (!isValueField(field)) {
    return;
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
}

function collectUnsupportedRules(rules: any[]): Array<{ label: string; type: string }> {
  return (rules || []).flatMap((rule) => {
    const children = Array.isArray(rule?.children) ? collectUnsupportedRules(rule.children) : [];
    if (rule?.field && !resolveRuntimeType(rule)) {
      return [
        { label: String(rule.title || rule.label || rule.field), type: String(rule.type || 'unknown') },
        ...children,
      ];
    }
    return children;
  });
}

function flattenRules(rules: any[]): any[] {
  return (rules || []).flatMap((rule) => {
    if (isContainerType(rule?.type)) {
      const field = ruleToRuntimeField(rule, 0, '0');
      return field ? [rule] : flattenRules(Array.isArray(rule?.children) ? rule.children : []);
    }
    return [rule];
  });
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

function ruleToRuntimeField(rule: any, index: number, path = `${index}`): RuntimeFormField | null {
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
  const children = Array.isArray(rule.children)
    ? rule.children
      .map((child: any, childIndex: number) => ruleToRuntimeField(child, childIndex, `${path}_${childIndex}`))
      .filter(Boolean) as RuntimeFormField[]
    : [];
  return {
    key: String(rule.field || `__runtime_${mappedType}_${path}`),
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
      originalType: rule.type,
      dictType: props.dictType || props.dictCode || props.typeCode,
      multiple: Boolean(props.multiple),
      clearable: props.clearable !== false,
      filterable: props.filterable !== false,
      accept: isImageUpload ? 'image/*' : props.accept,
      limit: Number(props.limit || (isImageUpload ? 6 : mappedType === 'upload' ? 5 : 0)) || undefined,
    },
    rules: normalizeRules(rule.validate || rule.rules, label, mappedType),
    defaultValue: rule.value ?? rule.defaultValue ?? props.defaultValue,
    content: normalizeContent(rule, props),
    children,
  };
}

function mapFieldType(type?: string): RuntimeFormFieldType | null {
  const normalized = String(type || '');
  if (['input', 'elInput', 'ElInput'].includes(normalized)) return 'input';
  if (['password'].includes(normalized)) return 'password';
  if (['textarea'].includes(normalized)) return 'textarea';
  if (['select', 'elSelect', 'ElSelect'].includes(normalized)) return 'select';
  if (['radio', 'elRadio', 'elRadioGroup', 'ElRadio', 'ElRadioGroup'].includes(normalized)) return 'radio';
  if (['checkbox', 'elCheckbox', 'elCheckboxGroup', 'ElCheckbox', 'ElCheckboxGroup'].includes(normalized)) return 'checkbox';
  if (['switch', 'elSwitch', 'ElSwitch'].includes(normalized)) return 'switch';
  if (['rate', 'elRate', 'ElRate'].includes(normalized)) return 'rate';
  if (['slider', 'elSlider', 'ElSlider'].includes(normalized)) return 'slider';
  if (['input', 'password', 'textarea', 'select', 'radio', 'checkbox', 'switch', 'rate', 'slider'].includes(normalized)) {
    return normalized as RuntimeFormFieldType;
  }
  if (['inputNumber', 'elInputNumber', 'ElInputNumber', 'number'].includes(normalized)) return 'number';
  if (['datePicker', 'elDatePicker', 'ElDatePicker', 'date'].includes(normalized)) return 'date';
  if (['dateRange', 'daterange'].includes(normalized)) return 'daterange';
  if (['datetimePicker', 'datetime'].includes(normalized)) return 'datetime';
  if (['datetimeRange', 'datetimerange'].includes(normalized)) return 'datetimerange';
  if (['timePicker', 'elTimePicker', 'ElTimePicker', 'time'].includes(normalized)) return 'time';
  if (['timeRange', 'timerange'].includes(normalized)) return 'timerange';
  if (['colorPicker', 'elColorPicker', 'ElColorPicker', 'color'].includes(normalized)) return 'color';
  if (['cascader', 'elCascader', 'ElCascader'].includes(normalized)) return 'cascader';
  if (['elTreeSelect', 'ElTreeSelect', 'treeSelect', 'tree', 'elTree', 'ElTree'].includes(normalized)) return 'treeSelect';
  if (['elTransfer', 'ElTransfer', 'transfer'].includes(normalized)) return 'transfer';
  if (['fcEditor', 'editor'].includes(normalized)) return 'editor';
  if (['upload', 'elUpload', 'ElUpload', 'fcUpload'].includes(normalized)) return 'upload';
  if (['elAlert', 'ElAlert', 'alert'].includes(normalized)) return 'alert';
  if (['text'].includes(normalized)) return 'text';
  if (['html'].includes(normalized)) return 'html';
  if (['elDivider', 'ElDivider', 'divider'].includes(normalized)) return 'divider';
  if (['elTag', 'ElTag', 'tag'].includes(normalized)) return 'tag';
  if (['elImage', 'ElImage', 'image'].includes(normalized)) return 'image';
  if (['elButton', 'ElButton', 'button'].includes(normalized)) return 'button';
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
    label: String(option.label ?? option.name ?? option.title ?? option.key ?? option.value ?? ''),
    value: option.value ?? option.id ?? option.key ?? option.label ?? option.name,
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
  return ['systemUser', 'systemOrg', 'systemDept', 'systemPost', 'systemRole', 'systemDict', 'businessType', 'signature'].includes(type);
}

function isDisplayType(type?: string) {
  return ['elAlert', 'ElAlert', 'alert', 'text', 'html', 'elDivider', 'ElDivider', 'divider', 'elTag', 'ElTag', 'tag', 'elImage', 'ElImage', 'image', 'elButton', 'ElButton', 'button', 'div'].includes(String(type || ''));
}

function isContainerType(type?: string) {
  return ['group', 'subForm', 'tableForm', 'tableFormColumn', 'fcRow', 'FcRow', 'col', 'elCol', 'ElCol', 'elCard', 'ElCard', 'elTabs', 'ElTabs', 'elTabPane', 'ElTabPane', 'elCollapse', 'ElCollapse', 'elCollapseItem', 'ElCollapseItem', 'fcTable', 'space'].includes(String(type || ''));
}
