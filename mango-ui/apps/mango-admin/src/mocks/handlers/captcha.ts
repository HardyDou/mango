/**
 * Captcha Mock Handlers - 验证码
 */

import { http, HttpResponse } from 'msw';

const blockPuzzleX = 168;
const blockPuzzleY = 64;
const clickWordPoints = [
  { x: 76, y: 62 },
  { x: 166, y: 112 },
  { x: 252, y: 72 },
];

function svgDataUrl(svg: string) {
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

function createMockBlockBackground() {
  return svgDataUrl(`
    <svg xmlns="http://www.w3.org/2000/svg" width="280" height="160" viewBox="0 0 280 160">
      <defs>
        <linearGradient id="sky" x1="0" x2="1" y1="0" y2="1">
          <stop offset="0" stop-color="#8ec5ff"/>
          <stop offset="1" stop-color="#eef6d6"/>
        </linearGradient>
      </defs>
      <rect width="280" height="160" fill="url(#sky)"/>
      <path d="M0 116 C42 88 74 102 104 74 C143 38 180 57 212 30 C238 9 259 21 280 5 L280 160 L0 160 Z" fill="#78b66f"/>
      <path d="M0 135 C42 120 84 124 124 105 C166 85 217 102 280 72 L280 160 L0 160 Z" fill="#407b59"/>
      <rect x="42" y="58" width="62" height="48" rx="4" fill="#f2d08b"/>
      <rect x="52" y="68" width="16" height="18" fill="#4d7ea8"/>
      <rect x="78" y="68" width="16" height="18" fill="#4d7ea8"/>
      <path d="M37 58 L73 33 L110 58 Z" fill="#ad5f45"/>
      <circle cx="226" cy="48" r="18" fill="#ffd166"/>
      <path d="M168 64 h50 v50 h-50 z" fill="#000" opacity=".42"/>
      <path d="M170 66 h46 v46 h-46 z" fill="none" stroke="#fff" stroke-width="2" stroke-dasharray="5 4" opacity=".92"/>
    </svg>
  `);
}

function createMockBlockSlider() {
  return svgDataUrl(`
    <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50" viewBox="0 0 50 50">
      <defs>
        <linearGradient id="piece" x1="0" x2="1" y1="0" y2="1">
          <stop offset="0" stop-color="#77b46e"/>
          <stop offset=".55" stop-color="#43815d"/>
          <stop offset="1" stop-color="#3b744f"/>
        </linearGradient>
        <clipPath id="puzzle">
          <path d="M3 8 Q3 3 8 3 H21 C22 -1 28 -1 29 3 H42 Q47 3 47 8 V42 Q47 47 42 47 H8 Q3 47 3 42 V30 C-1 29 -1 21 3 20 Z"/>
        </clipPath>
      </defs>
      <g clip-path="url(#puzzle)">
        <rect width="50" height="50" fill="url(#piece)"/>
        <path d="M0 31 C11 22 22 29 31 18 C37 11 43 9 50 8 L50 50 L0 50 Z" fill="#407b59"/>
        <rect x="0" y="0" width="50" height="23" fill="#8ec5ff" opacity=".24"/>
      </g>
      <path d="M3 8 Q3 3 8 3 H21 C22 -1 28 -1 29 3 H42 Q47 3 47 8 V42 Q47 47 42 47 H8 Q3 47 3 42 V30 C-1 29 -1 21 3 20 Z" fill="none" stroke="#fff" stroke-width="2" opacity=".82"/>
    </svg>
  `);
}

function createMockClickWordImage() {
  return svgDataUrl(`
    <svg xmlns="http://www.w3.org/2000/svg" width="320" height="180" viewBox="0 0 320 180">
      <rect width="320" height="180" fill="#f2f7fa"/>
      <path d="M0 128 C48 96 86 118 126 82 C174 39 230 67 320 26 L320 180 L0 180 Z" fill="#d6e5d5"/>
      <circle cx="262" cy="42" r="32" fill="#ead7bd"/>
      <rect x="34" y="34" width="92" height="48" rx="18" fill="#c6ddea"/>
      <rect x="190" y="112" width="96" height="38" rx="15" fill="#e1dcec"/>
      <g fill="none" stroke="#9fb3c4" stroke-opacity=".28">
        <path d="M0 20 L180 180"/>
        <path d="M44 0 L224 180"/>
        <path d="M92 0 L272 180"/>
        <path d="M146 0 L320 174"/>
      </g>
      <text x="60" y="76" font-family="Arial, sans-serif" font-size="34" font-weight="700" fill="#274f8e" transform="rotate(-9 76 62)">云</text>
      <text x="150" y="126" font-family="Arial, sans-serif" font-size="34" font-weight="700" fill="#267a57" transform="rotate(8 166 112)">山</text>
      <text x="236" y="86" font-family="Arial, sans-serif" font-size="34" font-weight="700" fill="#98432f" transform="rotate(-5 252 72)">月</text>
      <text x="210" y="45" font-family="Arial, sans-serif" font-size="30" font-weight="700" fill="#6f499b" transform="rotate(12 225 34)">竹</text>
    </svg>
  `);
}

export const captchaHandlers = [
  // 获取验证码类型
  http.get('/api/captcha/types', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: {
        types: ['ARITHMETIC', 'BLOCK_PUZZLE', 'CLICK_WORD', 'BEHAVIOR', 'SMS', 'EMAIL'],
        currentStorage: 'memory',
      },
    });
  }),

  // 生成算术验证码
  http.get('/api/captcha/arithmetic', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: {
        key: `arith-${Date.now()}`,
        type: 'ARITHMETIC',
        image: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iNDAiPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudD0iYmFzZWxpbmUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIyNCIgZmlsbD0iIzY2NiI+MTwv dGV4dD48L3N2Zz4=',
        expireTime: 120,
        extra: '10', // 算术题答案
      },
    });
  }),

  // 生成滑块验证码
  http.get('/api/captcha/block-puzzle', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: {
        key: `block-${Date.now()}`,
        type: 'BLOCK_PUZZLE',
        backgroundImage: createMockBlockBackground(),
        sliderImage: createMockBlockSlider(),
        x: blockPuzzleX,
        y: blockPuzzleY,
        expireTime: 120,
      },
    });
  }),

  // 生成点选文字验证码
  http.get('/api/captcha/click-word', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: {
        key: `click-word-${Date.now()}`,
        type: 'CLICK_WORD',
        image: createMockClickWordImage(),
        target: '云,山,月',
        expireTime: 120,
        extra: JSON.stringify({ width: 320, height: 180, pointCount: 3 }),
      },
    });
  }),

  // 生成无感行为验证
  http.get('/api/captcha/behavior', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: {
        key: `behavior-${Date.now()}`,
        type: 'BEHAVIOR',
        expireTime: 120,
        extra: JSON.stringify({ mode: 'silent', passScore: 0.7, secondaryScore: 0.4 }),
      },
    });
  }),

  // 校验无感行为验证
  http.post('/api/captcha/behavior/verify', async ({ request }) => {
    const body = await request.json() as { key: string; pointJson?: string };
    const payload = body.pointJson ? JSON.parse(body.pointJson) : {};
    const trackSize = payload.behavior?.mouseTrack?.length ?? 0;
    const score = trackSize > 8 ? 0.86 : 0.38;
    const passed = score >= 0.7;
    return HttpResponse.json({
      code: 200,
      success: true,
      message: passed ? '验证成功' : '验证失败',
      data: {
        key: body.key,
        score,
        passed,
        riskLevel: passed ? 'LOW' : 'HIGH',
        suggestAction: passed ? 'ALLOW' : 'DENY',
        reason: passed ? 'OK' : 'MOUSE_TRACK_TOO_SHORT',
      },
    });
  }),

  // 发送短信验证码（Mock 直接成功）
  http.post('/api/auth/captcha/sms/send', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: '验证码已发送',
      data: null,
    });
  }),

  // 发送邮件验证码（Mock 直接成功）
  http.post('/api/auth/captcha/email/send', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: '验证码已发送',
      data: null,
    });
  }),

  // 校验验证码
  http.post('/api/captcha/verify', async ({ request }) => {
    const body = await request.json() as { key: string; type: string; code?: string; pointJson?: string };
    // Mock: 算术验证码答案 "10" 正确
    if (body.type === 'ARITHMETIC' && body.code === '10') {
      return HttpResponse.json({
        code: 200,
        success: true,
        message: '验证成功',
        data: true,
      });
    }
    if (body.type === 'CLICK_WORD') {
      const points = body.pointJson ? JSON.parse(body.pointJson).points as Array<{ x: number; y: number }> : [];
      const passed = points.length === clickWordPoints.length && points.every((point, index) => (
        Math.hypot(point.x - clickWordPoints[index].x, point.y - clickWordPoints[index].y) <= 28
      ));
      return HttpResponse.json({
        code: passed ? 200 : 400,
        success: passed,
        message: passed ? '验证成功' : '验证失败',
        data: passed,
      });
    }
    // 其他情况也返回成功（Mock 模式）
    return HttpResponse.json({
      code: 200,
      success: true,
      message: '验证成功',
      data: true,
    });
  }),
];
