package io.mango.information.api;

import io.mango.common.result.R;
import io.mango.information.api.command.SubmitInformationCommand;
import io.mango.information.api.vo.InformationPublicFormVO;
import io.mango.information.api.vo.InformationSubmissionResultVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** 信息采集公开及服务端接入 API。 */
public interface InformationPublicApi {
    R<InformationPublicFormVO> detail(@NotBlank String formCode);
    R<InformationSubmissionResultVO> submit(@Valid SubmitInformationCommand command);
}
