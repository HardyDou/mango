package io.mango.auth.starter.web;

import io.mango.auth.api.AuthCode;
import io.mango.common.result.R;
import io.mango.common.result.Require;

/**
 * Adapts the captcha transport response to data used by the auth HTTP boundary.
 */
public final class CaptchaResponseAdapter {

    private CaptchaResponseAdapter() {}

    /**
     * Returns successful captcha data and preserves a downstream business failure.
     *
     * @param response captcha transport response
     * @param <T> response data type
     * @return successful response data
     */
    public static <T> T requireData(R<T> response) {
        Require.notNull(response, AuthCode.CAPTCHA_SERVICE_UNAVAILABLE);
        Require.isTrue(response.isSuccess(), response.getCode(), response.getMsg());
        return response.getData();
    }
}
