/**
 * 采集项类型展示元数据。
 * 表单定义本身以 form-create 规则存储（见 `./rules`），这里只保留详情页和列表展示需要的类型标签。
 */

const FIELD_TYPE_LABELS: Record<string, string> = {
  input: '单行文本',
  text: '单行文本',
  textarea: '多行文本',
  inputNumber: '数字',
  number: '数字',
  phone: '手机号',
  email: '邮箱',
  url: '网址',
  password: '密码',
  search: '搜索',
  time: '时间',
  month: '月份',
  datetime: '日期时间',
  radio: '单选',
  checkbox: '多选',
  select: '下拉选择',
  datePicker: '日期',
  date: '日期',
  divider: '分割线',
  elDivider: '分割线',
  static: '说明文字',
  elAlert: '说明文字',
};

export function fieldTypeLabel(fieldType?: string): string {
  return FIELD_TYPE_LABELS[String(fieldType || '')] || fieldType || '-';
}

/** 展示型与布局型控件不产生提交值。 */
export function isDisplayOnly(fieldType?: string): boolean {
  return ['divider', 'elDivider', 'static', 'elAlert', 'text', 'html'].includes(String(fieldType || ''));
}
