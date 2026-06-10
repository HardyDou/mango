package io.mango.payment.starter.controller;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentEnterpriseSubjectApi;
import io.mango.payment.api.command.SavePaymentEnterpriseSubjectCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentEnterpriseSubjectVO;
import io.mango.payment.core.service.IPaymentEnterpriseSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/payment/enterprise-subjects")
@RequiredArgsConstructor
@Tag(name = "支付企业主体管理", description = "收款主体后台管理接口")
public class PaymentEnterpriseSubjectController implements PaymentEnterpriseSubjectApi {

    private final IPaymentEnterpriseSubjectService enterpriseSubjectService;

    @Override
    @GetMapping("/page")
    @Operation(summary = "分页查询收款主体", description = "按当前租户查询收款主体")
    public R<PageResult<PaymentEnterpriseSubjectVO>> pageEnterpriseSubjects(@ParameterObject PaymentConfigPageQuery query) {
        return enterpriseSubjectService.pageEnterpriseSubjects(query);
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "查询收款主体详情", description = "按主体 ID 查询收款主体详情")
    public R<PaymentEnterpriseSubjectVO> detailEnterpriseSubject(@Parameter(description = "主体 ID", required = true) @RequestParam Long id) {
        return enterpriseSubjectService.detailEnterpriseSubject(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "新增收款主体", description = "创建收款主体")
    public R<Long> createEnterpriseSubject(@RequestBody SavePaymentEnterpriseSubjectCommand command) {
        return enterpriseSubjectService.createEnterpriseSubject(command);
    }

    @Override
    @PutMapping
    @Operation(summary = "修改收款主体", description = "更新收款主体")
    public R<Boolean> updateEnterpriseSubject(@RequestBody SavePaymentEnterpriseSubjectCommand command) {
        return enterpriseSubjectService.updateEnterpriseSubject(command);
    }

    @Override
    @DeleteMapping
    @Operation(summary = "删除收款主体", description = "按 ID 删除收款主体")
    public R<Boolean> deleteEnterpriseSubject(@Parameter(description = "主体 ID", required = true) @RequestParam Long id) {
        return enterpriseSubjectService.deleteEnterpriseSubject(id);
    }
}
