package io.mango.auth.core.support;

import io.mango.auth.api.enums.AuthCode;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.exception.BizException;

/**
 * 将跨模块统一响应转换为认证域数据，隔离传输层响应模型。
 */
public final class AuthApiResponseAdapter {

    private AuthApiResponseAdapter() {
    }

    public static <T> T requireCaptchaData(R<T> response) {
        Require.notNull(response, AuthCode.CAPTCHA_SERVICE_UNAVAILABLE);
        Require.isTrue(response.isSuccess(), response.getCode(), response.getMsg());
        return response.getData();
    }

    public static <T> T nullableData(R<T> response) {
        if (response == null || !response.isSuccess()) {
            return null;
        }
        return response.getData();
    }

    public static <T> T requireWecomConfig(R<T> response) {
        if (response == null) {
            return Require.fail(AuthCode.WECOM_CONFIG_UNAVAILABLE);
        }
        Require.isTrue(response.isSuccess(), response.getCode(), response.getMsg());
        T data = response.getData();
        Require.notNull(data, AuthCode.WECOM_CONFIG_UNAVAILABLE);
        return data;
    }

    public static <T> T requireIdentityData(R<T> response) {
        Require.notNull(response, AuthCode.CURRENT_USER_NOT_FOUND);
        Require.isTrue(response.isSuccess(), response.getCode(), response.getMsg());
        T data = response.getData();
        Require.notNull(data, AuthCode.CURRENT_USER_NOT_FOUND);
        return data;
    }

    public static <T> T rethrow(BizException exception) {
        return Require.fail(exception.getCode(), exception.getMessage());
    }
}
