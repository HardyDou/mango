/**
 * Captcha Mock Handlers - 验证码
 */

import { http, HttpResponse } from 'msw';

export const captchaHandlers = [
  // 获取验证码类型
  http.get('/api/captcha/types', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: {
        types: ['ARITHMETIC', 'BLOCK_PUZZLE', 'SMS', 'EMAIL'],
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
        image: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iNDAiPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudD0iYmFzZWxpbmUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIyNCIgZmlsbD0iIzY2NiI+MTwv dGV4dD48L3N2Zz4=',
        backgroundImage: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iNDAiPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudD0iYmFzZWxpbmUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIyNCIgZmlsbD0iI2Y0ZjRmNCI+MTwv dGV4dD48L3N2Zz4=',
        sliderImage: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MCIgaGVpZ2hodD0iNDAiPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudD0iYmFzZWxpbmUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIyNCIgZmlsbD0iIzQwNDFmZiI+MTwv dGV4dD48L3N2Zz4=',
        expireTime: 120,
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
    const body = await request.json() as { key: string; type: string; code?: string };
    // Mock: 算术验证码答案 "10" 正确
    if (body.type === 'ARITHMETIC' && body.code === '10') {
      return HttpResponse.json({
        code: 200,
        success: true,
        message: '验证成功',
        data: true,
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
