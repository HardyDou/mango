package io.mango.authorization.api;

import io.mango.authorization.api.command.TenantAppBindingCommand;
import io.mango.authorization.api.query.TenantAppBindingQuery;
import io.mango.authorization.api.vo.TenantAppBindingVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 租户应用开通 API。
 */
public interface TenantAppBindingApi {

    R<List<TenantAppBindingVO>> list(@Valid TenantAppBindingQuery query);

    R<Long> enable(@Valid TenantAppBindingCommand command);

    R<Boolean> disable(@Positive Long tenantId, @NotBlank String appCode);
}
