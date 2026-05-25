<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>文件暂时无法预览</title>
    <style type="text/css">
        :root {
            color-scheme: light;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
            color: #1f2937;
            background: #f6f8fb;
        }

        body {
            min-height: 100vh;
            margin: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #f6f8fb;
        }

        .container {
            width: min(640px, calc(100vw - 48px));
            padding: 40px 44px;
            border: 1px solid #e5e7eb;
            border-radius: 8px;
            background: #ffffff;
            box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
        }

        .eyebrow {
            margin: 0 0 12px;
            color: #2563eb;
            font-size: 13px;
            font-weight: 600;
        }

        h1 {
            margin: 0 0 12px;
            font-size: 24px;
            line-height: 1.35;
            font-weight: 650;
        }

        p {
            margin: 0;
            font-size: 15px;
            line-height: 1.8;
            color: #4b5563;
        }

        .reason {
            margin-top: 20px;
            padding: 14px 16px;
            border: 1px solid #fecaca;
            border-radius: 6px;
            background: #fff7f7;
            color: #b91c1c;
            word-break: break-word;
        }

        .suggestion {
            margin-top: 20px;
            color: #374151;
        }
    </style>
</head>

<body>
<div class="container">
    <p class="eyebrow">Mango 文件预览</p>
    <h1>文件暂时无法在线预览</h1>
    <p>Mango 当前无法直接预览该 ${fileType} 文件。</p>
    <div class="reason">${msg}</div>
    <p class="suggestion">你可以先下载文件查看；如果该类型应支持在线预览，请联系管理员检查预览服务配置。</p>
</div>
</body>
</html>
