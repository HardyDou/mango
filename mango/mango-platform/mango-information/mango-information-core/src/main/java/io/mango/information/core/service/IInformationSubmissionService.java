package io.mango.information.core.service;

import io.mango.common.vo.PageResult;
import io.mango.information.api.command.ProcessInformationSubmissionCommand;
import io.mango.information.api.command.SubmitInformationCommand;
import io.mango.information.api.query.InformationSubmissionPageQuery;
import io.mango.information.api.vo.InformationSubmissionResultVO;
import io.mango.information.api.vo.InformationSubmissionVO;

public interface IInformationSubmissionService {
    PageResult<InformationSubmissionVO> page(InformationSubmissionPageQuery query);
    InformationSubmissionVO detail(Long id);
    boolean process(ProcessInformationSubmissionCommand command);
    String exportCsv(InformationSubmissionPageQuery query);
    InformationSubmissionResultVO submit(SubmitInformationCommand command, boolean serverRequest);
}
