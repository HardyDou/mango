package io.mango.information.starter.support;

import io.mango.information.api.command.SubmitInformationCommand;
import io.mango.information.api.vo.InformationSubmissionResultVO;

/** 公开表单提交的入口编排端口。 */
public interface InformationPublicSubmissionExecutor {

    InformationSubmissionResultVO submit(SubmitInformationCommand command);
}
