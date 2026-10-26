package io.mango.authorization.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API 资源登记来源。
 */
@Schema(description = "API 资源登记来源")
public enum ApiResourceSource {

    /** 固定资源声明或显式资源登记。 */
    FIXED_DECLARATION,

    /** Spring MVC Controller 自动扫描。 */
    CONTROLLER_SCAN,

    /** 后台或其它运行时管理入口。 */
    MANUAL
}
