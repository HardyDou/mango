package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentChannelApi;
import io.mango.payment.api.command.SavePaymentChannelCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelCapabilityVO;
import io.mango.payment.api.vo.PaymentChannelVO;
import io.mango.payment.core.service.IPaymentChannelService;
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
@RequestMapping("/payment/channels")
@RequiredArgsConstructor
@Tag(name = "支付通道管理", description = "支付通道后台管理接口")
public class PaymentChannelController implements PaymentChannelApi {

    private final IPaymentChannelService channelService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel:list")
    @Operation(summary = "分页查询支付通道", description = "按当前租户查询支付通道")
    public R<PageResult<PaymentChannelVO>> pageChannels(@ParameterObject PaymentConfigPageQuery query) {
        return channelService.pageChannels(query);
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel:query")
    @Operation(summary = "查询支付通道详情", description = "按通道 ID 查询支付通道详情")
    public R<PaymentChannelVO> detailChannel(@Parameter(description = "通道 ID", required = true) @RequestParam Long id) {
        return channelService.detailChannel(id);
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel:add")
    @Operation(summary = "新增支付通道", description = "创建支付通道配置")
    public R<Long> createChannel(@RequestBody SavePaymentChannelCommand command) {
        return channelService.createChannel(command);
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel:edit")
    @Operation(summary = "修改支付通道", description = "更新支付通道配置")
    public R<Boolean> updateChannel(@RequestBody SavePaymentChannelCommand command) {
        return channelService.updateChannel(command);
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel:delete")
    @Operation(summary = "删除支付通道", description = "按 ID 删除支付通道配置")
    public R<Boolean> deleteChannel(@Parameter(description = "通道 ID", required = true) @RequestParam Long id) {
        return channelService.deleteChannel(id);
    }

    @Override
    @GetMapping("/capabilities/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel:list")
    @Operation(summary = "分页查询通道能力")
    public R<PageResult<PaymentChannelCapabilityVO>> pageChannelCapabilities(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(channelService.pageChannelCapabilities(query));
    }
}
