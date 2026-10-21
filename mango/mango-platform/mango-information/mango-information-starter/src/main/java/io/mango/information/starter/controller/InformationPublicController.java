package io.mango.information.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.information.api.InformationPublicApi;
import io.mango.information.api.command.SubmitInformationCommand;
import io.mango.information.api.vo.InformationPublicFormVO;
import io.mango.information.api.vo.InformationSubmissionResultVO;
import io.mango.information.api.vo.InformationFormDetailVO;
import io.mango.information.core.service.IInformationFormService;
import io.mango.information.starter.support.InformationPublicSubmissionExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 信息采集公开接入接口。 */
@RestController
@RequestMapping("/information/open")
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring 容器管理的单例依赖，构造后不对外暴露"))
@Validated
@Tag(name = "信息采集公开接入")
public class InformationPublicController implements InformationPublicApi {
    private final IInformationFormService formService;
    private final InformationPublicSubmissionExecutor submissionExecutor;

    @Override
    @GetMapping("/forms/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "查询已发布信息采集表单")
    @Operation(summary = "查询托管表单定义")
    public R<InformationPublicFormVO> detail(@RequestParam String formCode) {
        InformationFormDetailVO source = formService.publicDetail(formCode);
        InformationPublicFormVO vo = new InformationPublicFormVO();
        vo.setFormCode(source.getFormCode());
        vo.setFormName(source.getFormName());
        vo.setDescription(source.getDescription());
        vo.setDefinitionJson(source.getDefinitionJson());
        vo.setSuccessMessage(source.getSuccessMessage());
        return R.ok(vo);
    }

    @Override
    @PostMapping("/forms/submit")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "提交信息采集数据")
    @Operation(summary = "提交表单数据")
    public R<InformationSubmissionResultVO> submit(@RequestBody SubmitInformationCommand command) {
        return R.ok(submissionExecutor.submit(command));
    }
}
