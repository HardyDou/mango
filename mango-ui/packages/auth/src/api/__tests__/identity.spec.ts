import { beforeEach, describe, expect, it, vi } from 'vitest';

const request = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

vi.mock('@mango/common/utils/request', () => request);

import {
  getCurrentUserProfile,
  listCurrentExternalIdentities,
  sendCurrentContactCaptcha,
  unbindCurrentExternalIdentity,
  updateCurrentUserContact,
  updateCurrentUserProfile,
} from '../identity';

describe('current identity API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('uses current-user endpoints without accepting another user id', async () => {
    await getCurrentUserProfile();
    await updateCurrentUserProfile({ realName: '测试用户', documentType: 'ID_CARD' });
    await listCurrentExternalIdentities();

    expect(request.get).toHaveBeenNthCalledWith(1, '/identity/me/profile');
    expect(request.put).toHaveBeenNthCalledWith(1, '/identity/me/profile', {
      realName: '测试用户',
      documentType: 'ID_CARD',
    });
    expect(request.get).toHaveBeenNthCalledWith(2, '/identity/me/external-identities');
  });

  it('sends password and captcha only on the contact update request', async () => {
    await sendCurrentContactCaptcha({ contactType: 'EMAIL', target: 'new@example.com' });
    await updateCurrentUserContact({
      contactType: 'EMAIL',
      target: 'new@example.com',
      currentPassword: 'password',
      captchaKey: 'CHANGE_EMAIL:new@example.com',
      captchaCode: '123456',
    });

    expect(request.post).toHaveBeenCalledWith('/identity/me/contact-captcha', {
      contactType: 'EMAIL',
      target: 'new@example.com',
    });
    expect(request.put).toHaveBeenCalledWith(
      '/identity/me/contact',
      expect.objectContaining({
        currentPassword: 'password',
        captchaCode: '123456',
      }),
    );
  });

  it('places password-protected unbind data in the DELETE body', async () => {
    await unbindCurrentExternalIdentity({ bindingId: 9, currentPassword: 'password' });

    expect(request.del).toHaveBeenCalledWith('/identity/me/external-identities', {
      data: { bindingId: 9, currentPassword: 'password' },
    });
  });
});
