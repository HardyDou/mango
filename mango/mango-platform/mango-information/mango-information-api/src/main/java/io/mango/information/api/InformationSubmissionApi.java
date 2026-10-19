package io.mango.information.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.information.api.command.ProcessInformationSubmissionCommand;
import io.mango.information.api.query.InformationSubmissionPageQuery;
import io.mango.information.api.vo.InformationSubmissionVO;
import jakarta.validation.Valid;

/** 信息采集采集记录管理 API。 */
public interface InformationSubmissionApi {
    R<PageResult<InformationSubmissionVO>> page(@Valid InformationSubmissionPageQuery query);
    R<InformationSubmissionVO> detail(Long id);
    R<String> export(@Valid InformationSubmissionPageQuery query);
    R<Boolean> process(@Valid ProcessInformationSubmissionCommand command);
}
