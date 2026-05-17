package io.mango.authorization.api;

import io.mango.authorization.api.command.TenantAppBindingCommand;
import io.mango.authorization.api.vo.TenantAppBindingVO;
import io.mango.common.result.R;

import java.util.List;

/**
 * 租户应用开通 API。
 */
public interface TenantAppBindingApi {

    R<List<TenantAppBindingVO>> list(Long tenantId, String appCode, Integer status);

    R<Long> enable(TenantAppBindingCommand command);

    R<Boolean> disable(Long tenantId, String appCode);
}
