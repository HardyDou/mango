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

function payload(context: AccessSampleContext): string {
  return [
    '{',
    `  "formCode": "${context.formCode}",`,
    '  "values": { "name": "张三", "phone": "13800138000" },',
    '  "idempotencyKey": "order-20260101-001"',
    '}',
  ].join('\n');
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
  const body = payload(context);
  const { host, formCode } = context;
  return [
    {
      key: 'curl',
      label: 'cURL',
      code: [
        `curl -X POST '${url}' \\`,
        "  -H 'Content-Type: application/json' \\",
        "  -H 'X-Mango-SK: $MANGO_INFORMATION_SK' \\",
        `  -d '${indent(body, 2)}'`,
      ].join('\n'),
    },
    {
      key: 'postman',
      label: 'Postman',
      code: [
        `POST /information/open/forms/${formCode}/submit HTTP/1.1`,
        `Host: ${host}`,
        'Content-Type: application/json',
        'X-Mango-SK: {{mangoInformationSk}}',
        '',
        body,
      ].join('\n'),
    },
    {
      key: 'java',
      label: 'Java',
      code: [
        '// JDK 11+ 内置 HttpClient，无需额外依赖',
        'String body = """',
        indent(body, 8),
        '        """;',
        'HttpRequest request = HttpRequest.newBuilder()',
        `        .uri(URI.create("${url}"))`,
        '        .header("Content-Type", "application/json")',
        '        .header("X-Mango-SK", System.getenv("MANGO_INFORMATION_SK"))',
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
        '  headers: {',
        "    'Content-Type': 'application/json',",
        "    'X-Mango-SK': process.env.MANGO_INFORMATION_SK,",
        '  },',
        '  body: JSON.stringify({',
        `    formCode: '${formCode}',`,
        "    values: { name: '张三', phone: '13800138000' },",
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
        '    headers={"X-Mango-SK": os.environ["MANGO_INFORMATION_SK"]},',
        '    json={',
        `        "formCode": "${formCode}",`,
        '        "values": {"name": "张三", "phone": "13800138000"},',
        '        "idempotencyKey": "order-20260101-001",',
        '    },',
        '    timeout=10,',
        ')',
        'print(response.json())',
      ].join('\n'),
    },
  ];
}
