package io.mango.cms.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CMS 模块业务码。
 *
 * <p>历史 CMS 业务校验统一返回 400；具体失败消息继续由各领域前置条件提供，
 * 因而对外 code 与 message 均保持不变。</p>
 */
@Getter
@AllArgsConstructor
public enum CmsCode implements BizCode {

    /** CMS 业务前置条件不满足。 */
    CMS_BUSINESS_ERROR(400, "CMS 业务校验失败");

    private final int code;
    private final String message;
}
