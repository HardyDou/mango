package io.mango.information.starter.controller;

import io.mango.information.api.command.SubmitInformationCommand;
import io.mango.information.api.vo.InformationSubmissionResultVO;
import io.mango.information.core.service.IInformationSubmissionService;
import io.mango.information.starter.security.InformationSubmitProtection;
import io.mango.information.starter.security.InformationSubmitProtectionException;
import io.mango.information.starter.support.InformationPublicSubmissionExecutor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 公开表单提交的风险保护与业务提交编排。 */
@Component
@RequiredArgsConstructor
public class InformationPublicSubmissionExecutorImpl implements InformationPublicSubmissionExecutor {

    private final IInformationSubmissionService submissionService;
    private final InformationSubmitProtection submitProtection;

    @Override
    public InformationSubmissionResultVO submit(SubmitInformationCommand command) {
        InformationSubmitProtection.Decision decision = submitProtection.check(currentRequest(), command.getFormCode());
        if (!decision.allowed()) {
            throw new InformationSubmitProtectionException(decision);
        }
        return submissionService.submit(command, StringUtils.hasText(command.getServerSecret()));
    }

    private HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
