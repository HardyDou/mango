package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentMethodApi;
import io.mango.payment.api.command.SavePaymentMethodCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentMethodCategoryVO;
import io.mango.payment.api.vo.PaymentMethodVO;
import io.mango.payment.core.service.IPaymentMethodService;
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
@RequestMapping("/payment/methods")
@RequiredArgsConstructor
@Tag(name = "支付方式管理", description = "支付方式后台管理接口")
public class PaymentMethodController implements PaymentMethodApi {

    private final IPaymentMethodService methodService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method:list")
    @Operation(summary = "分页查询支付方式", description = "按当前租户查询支付方式")
    public R<PageResult<PaymentMethodVO>> pageMethods(@ParameterObject PaymentConfigPageQuery query) {
        return methodService.pageMethods(query);
    }

    @Override
    @GetMapping("/categories")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method:list")
    @Operation(summary = "查询支付方式分类字典", description = "按当前租户查询启用的三级支付方式分类")
    public R<java.util.List<PaymentMethodCategoryVO>> listMethodCategories() {
        return methodService.listMethodCategories();
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method:query")
    @Operation(summary = "查询支付方式详情", description = "按支付方式 ID 查询详情")
    public R<PaymentMethodVO> detailMethod(@Parameter(description = "支付方式 ID", required = true) @RequestParam Long id) {
        return methodService.detailMethod(id);
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method:add")
    @Operation(summary = "新增支付方式", description = "创建支付方式")
    public R<Long> createMethod(@RequestBody SavePaymentMethodCommand command) {
        return methodService.createMethod(command);
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method:edit")
    @Operation(summary = "修改支付方式", description = "更新支付方式")
    public R<Boolean> updateMethod(@RequestBody SavePaymentMethodCommand command) {
        return methodService.updateMethod(command);
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:method:delete")
    @Operation(summary = "删除支付方式", description = "按 ID 删除支付方式")
    public R<Boolean> deleteMethod(@Parameter(description = "支付方式 ID", required = true) @RequestParam Long id) {
        return methodService.deleteMethod(id);
    }
}
