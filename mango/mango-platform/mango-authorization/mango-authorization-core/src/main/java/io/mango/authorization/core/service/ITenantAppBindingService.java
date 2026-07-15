package io.mango.authorization.core.service;

import io.mango.authorization.api.command.TenantAppBindingCommand;
import io.mango.authorization.api.query.TenantAppBindingQuery;
import io.mango.authorization.api.vo.TenantAppBindingVO;

import java.util.List;

/**
 * 租户应用开通服务。
 */
public interface ITenantAppBindingService {

    List<TenantAppBindingVO> list(TenantAppBindingQuery query);

    Long enable(TenantAppBindingCommand command);

    Boolean disable(Long tenantId, String appCode);

    Boolean disableRequired(Long tenantId, String appCode);

    void ensureEnabled(Long tenantId, String appCode);

    boolean isEnabled(Long tenantId, String appCode);
}
