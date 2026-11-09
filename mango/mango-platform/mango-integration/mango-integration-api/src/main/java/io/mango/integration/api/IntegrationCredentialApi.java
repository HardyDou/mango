package io.mango.integration.api;

import io.mango.integration.api.command.IntegrationCredentialCommand;
import io.mango.integration.api.command.IntegrationCredentialRotateCommand;
import io.mango.integration.api.vo.IntegrationCredentialRotationVO;
import io.mango.integration.api.vo.IntegrationCredentialVO;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 集成凭证 API。
 *
 * <p>敏感字段只写入不读取，列表和详情只返回掩码与版本。</p>
 */
public interface IntegrationCredentialApi {

    R<List<IntegrationCredentialVO>> list(@Size(max = 64) String connectorCode);

    R<Boolean> save(@Valid IntegrationCredentialCommand command);

    R<Boolean> rotate(@Valid IntegrationCredentialRotateCommand command);

    R<List<IntegrationCredentialRotationVO>> rotations(@Size(max = 64) String credentialCode);
}
