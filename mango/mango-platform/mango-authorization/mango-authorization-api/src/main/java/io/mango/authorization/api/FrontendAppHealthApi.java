package io.mango.authorization.api;

import io.mango.authorization.api.command.FrontendAppHealthCheckCommand;
import io.mango.authorization.api.vo.FrontendAppHealthCheckVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;

/**
 * 前端运行单元健康检查 API。
 *
 * <p>只探测注册表中已登记的运行单元地址，不接收任意目标 URL，也不返回凭证或响应正文。</p>
 */
public interface FrontendAppHealthApi {

    R<FrontendAppHealthCheckVO> check(@Valid FrontendAppHealthCheckCommand command);
}
