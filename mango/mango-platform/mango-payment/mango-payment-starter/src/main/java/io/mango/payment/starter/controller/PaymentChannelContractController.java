package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentChannelContractApi;
import io.mango.payment.api.command.RotatePaymentChannelContractCertificateCommand;
import io.mango.payment.api.command.SavePaymentChannelBillSourceCommand;
import io.mango.payment.api.command.SavePaymentChannelContractCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelCertificateExpiryVO;
import io.mango.payment.api.vo.PaymentChannelBillSourceVO;
import io.mango.payment.api.vo.PaymentChannelCertificateRotationRecordVO;
import io.mango.payment.api.vo.PaymentChannelContractVO;
import io.mango.payment.core.service.IPaymentChannelContractService;
import io.mango.payment.core.service.PaymentReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/payment/channel-contracts")
@RequiredArgsConstructor
@Tag(name = "通道签约配置", description = "企业主体与支付通道签约配置后台管理接口")
public class PaymentChannelContractController implements PaymentChannelContractApi {

    private final IPaymentChannelContractService channelContractService;
    private final PaymentReconciliationService reconciliationService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel-contract:list")
    @Operation(summary = "分页查询通道签约配置", description = "按当前租户查询企业主体与支付通道签约配置")
    public R<PageResult<PaymentChannelContractVO>> pageChannelContracts(@ParameterObject PaymentConfigPageQuery query) {
        return channelContractService.pageChannelContracts(query);
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel-contract:query")
    @Operation(summary = "查询通道签约配置详情", description = "按签约配置 ID 查询详情")
    public R<PaymentChannelContractVO> detailChannelContract(@Parameter(description = "签约配置 ID", required = true) @RequestParam Long id) {
        return channelContractService.detailChannelContract(id);
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel-contract:add")
    @Operation(summary = "新增通道签约配置", description = "创建企业主体在支付通道下的签约配置")
    public R<Long> createChannelContract(@Valid @RequestBody SavePaymentChannelContractCommand command) {
        return channelContractService.createChannelContract(command);
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel-contract:edit")
    @Operation(summary = "修改通道签约配置", description = "更新企业主体在支付通道下的签约配置")
    public R<Boolean> updateChannelContract(@Valid @RequestBody SavePaymentChannelContractCommand command) {
        return channelContractService.updateChannelContract(command);
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel-contract:delete")
    @Operation(summary = "删除通道签约配置", description = "按 ID 删除通道签约配置")
    public R<Boolean> deleteChannelContract(@Parameter(description = "签约配置 ID", required = true) @RequestParam Long id) {
        return channelContractService.deleteChannelContract(id);
    }

    @Override
    @GetMapping("/certificates/expiring")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel-contract:certificate-expiry")
    @Operation(summary = "查询通道证书到期提醒", description = "查询当前租户即将到期或已到期的通道签约证书")
    public R<List<PaymentChannelCertificateExpiryVO>> listExpiringCertificates(
            @Parameter(description = "提醒天数，默认 30 天") @RequestParam(required = false) Integer warningDays) {
        return channelContractService.listExpiringCertificates(warningDays);
    }

    @Override
    @PostMapping("/certificates/rotate")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel-contract:certificate-rotate")
    @Operation(summary = "登记通道证书轮换", description = "登记通道签约证书轮换记录，并同步证书文件 ID 和证书有效期")
    public R<PaymentChannelCertificateRotationRecordVO> rotateCertificate(
            @Valid @RequestBody RotatePaymentChannelContractCertificateCommand command) {
        return channelContractService.rotateCertificate(command);
    }

    @GetMapping("/bill-sources/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel-contract:list")
    @Operation(summary = "分页查询签约通道账单获取配置", description = "按签约通道查询手动、FTP、FTPS、HTTP 等账单获取配置")
    public R<PageResult<PaymentChannelBillSourceVO>> pageBillSources(@ParameterObject PaymentConfigPageQuery query) {
        return R.ok(reconciliationService.pageBillSources(query));
    }

    @PostMapping("/bill-sources")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "payment:channel-contract:edit")
    @Operation(summary = "保存签约通道账单获取配置", description = "在签约通道下配置手动、FTP、FTPS、HTTP 账单获取方式")
    public R<PaymentChannelBillSourceVO> saveBillSource(@Valid @RequestBody SavePaymentChannelBillSourceCommand command) {
        return R.ok(reconciliationService.saveBillSource(command));
    }
}
