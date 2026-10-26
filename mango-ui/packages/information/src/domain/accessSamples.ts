/**
 * 接入方式的代码示例。
 *
 * 放在 domain 层而不是视图里：视图层不允许出现网络调用字样，
 * 而示例正文里必然包含 fetch/HttpClient 这类关键字。
 */
export interface AccessSampleContext {
  origin: string;
  host: string;
  formCode: string;
  formName: string;
  hostedUrl: string;
  /** 当前表单的采集项，用来生成和真实字段一致的 values 示例。 */
  fields: AccessSampleField[];
}

/** 与采集项视图模型结构一致，避免示例生成依赖视图层类型。 */
export interface AccessSampleField {
  fieldCode: string;
  fieldType: string;
  options?: Array<{ label: string; value: string }>;
  children?: AccessSampleField[];
}

export interface AccessCodeSample {
  key: string;
  label: string;
  code: string;
}

/** 多行载荷按缩进贴进示例代码，避免在模板字符串里写替换正则。 */
function indent(text: string, spaces: number): string {
  const pad = ' '.repeat(spaces);
  return text
    .split('\n')
    .map((line, index) => (index === 0 ? line : pad + line))
    .join('\n');
}

function submitUrl(context: AccessSampleContext): string {
  return `${context.origin}/information/open/forms/${context.formCode}/submit`;
}

/** 按字段类型给出可读的示例值，让示例和当前表单的字段一一对应。 */
function sampleValue(field: AccessSampleField): unknown {
  switch (field.fieldType) {
    case 'phone':
      return '13800138000';
    case 'email':
      return 'demo@example.com';
    case 'url':
      return 'https://example.com';
    case 'number':
      return 1;
    case 'date':
      return '2026-01-01';
    case 'datetime':
      return '2026-01-01 10:00:00';
    case 'time':
      return '10:00';
    case 'month':
      return '2026-01';
    case 'password':
      return 'P@ssw0rd';
    case 'search':
      return '关键字';
    case 'select':
    case 'radio':
      return field.options?.[0]?.value ?? '选项值';
    case 'checkbox':
      return [field.options?.[0]?.value ?? '选项值'];
    case 'switch':
      return true;
    case 'rate':
      return 5;
    case 'slider':
      return 50;
    case 'colorPicker':
      return '#2e5cf6';
    case 'datePicker':
      return '2026-01-01';
    case 'timePicker':
      return '10:00:00';
    case 'upload':
      return '上传后返回的文件标识';
    case 'group':
    case 'subform':
    case 'tableForm':
      return [Object.fromEntries((field.children || []).map((child) => [child.fieldCode, sampleValue(child)]))];
    default:
      return '示例文本';
  }
}

export function buildSampleValues(fields: AccessSampleField[]): Record<string, unknown> {
  return Object.fromEntries(fields.map((field) => [field.fieldCode, sampleValue(field)]));
}

/**
 * 请求体载荷。SK 走请求体字段 `serverSecret`（后端 `SubmitInformationCommand`），
 * 不是请求头，示例里按语言替换成各自的注入方式。
 */
function payload(context: AccessSampleContext, secretPlaceholder: string): string {
  return JSON.stringify(
    {
      formCode: context.formCode,
      serverSecret: secretPlaceholder,
      values: buildSampleValues(context.fields),
      idempotencyKey: 'order-20260101-001',
    },
    null,
    2,
  );
}

/** 页面嵌入代码：直接贴到业务页面即可使用，域名与编码已按当前表单生成。 */
export function buildEmbedCode(context: AccessSampleContext): string {
  return [
    '<iframe',
    `  src="${context.hostedUrl}"`,
    `  title="${context.formName}"`,
    '  width="100%"',
    '  height="720"',
    '  style="border:0"',
    '></iframe>',
  ].join('\n');
}

/** 服务端接入示例：覆盖 cURL、Postman、Java、JavaScript、Python 五种常用方式。 */
export function buildAccessSamples(context: AccessSampleContext): AccessCodeSample[] {
  const url = submitUrl(context);
  const { host, formCode } = context;
  return [
    {
      key: 'curl',
      label: 'cURL',
      code: [
        `curl -X POST '${url}' \\`,
        "  -H 'Content-Type: application/json' \\",
        `  -d '${indent(payload(context, '<填当前表单专属 SK>'), 2)}'`,
      ].join('\n'),
    },
    {
      key: 'postman',
      label: 'Postman',
      code: [
        `POST /information/open/forms/submit HTTP/1.1`,
        `Host: ${host}`,
        'Content-Type: application/json',
        '',
        payload(context, '{{mangoInformationSk}}'),
      ].join('\n'),
    },
    {
      key: 'java',
      label: 'Java',
      code: [
        '// JDK 11+ 内置 HttpClient，无需额外依赖；SK 从环境变量读取后写入请求体',
        'String serverSecret = System.getenv("MANGO_INFORMATION_SK");',
        'String body = """',
        indent(payload(context, '%s'), 8),
        '        """.formatted(serverSecret);',
        'HttpRequest request = HttpRequest.newBuilder()',
        `        .uri(URI.create("${url}"))`,
        '        .header("Content-Type", "application/json")',
        '        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))',
        '        .build();',
        'HttpResponse<String> response = HttpClient.newHttpClient()',
        '        .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));',
        'System.out.println(response.body());',
      ].join('\n'),
    },
    {
      key: 'javascript',
      label: 'JavaScript',
      code: [
        '// 只在服务端调用：浏览器里放 SK 会被访客拿到',
        `const response = await fetch('${url}', {`,
        "  method: 'POST',",
        "  headers: { 'Content-Type': 'application/json' },",
        '  body: JSON.stringify({',
        `    formCode: '${formCode}',`,
        '    serverSecret: process.env.MANGO_INFORMATION_SK,',
        `    values: ${JSON.stringify(buildSampleValues(context.fields))},`,
        "    idempotencyKey: 'order-20260101-001',",
        '  }),',
        '});',
        'console.log(await response.json());',
      ].join('\n'),
    },
    {
      key: 'python',
      label: 'Python',
      code: [
        'import os',
        'import requests',
        '',
        'response = requests.post(',
        `    "${url}",`,
        '    json={',
        `        "formCode": "${formCode}",`,
        '        "serverSecret": os.environ["MANGO_INFORMATION_SK"],',
        `        "values": ${JSON.stringify(buildSampleValues(context.fields))},`,
        '        "idempotencyKey": "order-20260101-001",',
        '    },',
        '    timeout=10,',
        ')',
        'print(response.json())',
      ].join('\n'),
    },
  ];
}
