package io.mango.payment.starter.remote;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentOfflineCollectionApi;
import io.mango.payment.api.command.ConfirmOfflineBankStatementMatchCommand;
import io.mango.payment.api.command.ConfirmOfflineCollectionCommand;
import io.mango.payment.api.command.CreateOfflineRefundCommand;
import io.mango.payment.api.command.ImportOfflineBankStatementCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOfflineBankStatementBatchStatusVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementBatchVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementMatchStatusVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionStatusVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionVO;
import io.mango.payment.api.vo.PaymentOfflineRefundVO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "mango-payment", contextId = "paymentOfflineCollectionFeignClient", path = "/payment/offline-collections")
public interface PaymentOfflineCollectionFeignClient extends PaymentOfflineCollectionApi {

    @Override
    @GetMapping("/page")
    R<PageResult<PaymentOfflineCollectionVO>> pageOfflineCollections(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/detail")
    R<PaymentOfflineCollectionVO> detailOfflineCollection(@RequestParam("id") Long id);

    @Override
    @GetMapping("/statuses")
    R<List<PaymentOfflineCollectionStatusVO>> listOfflineCollectionStatuses();

    @Override
    @PostMapping("/confirm")
    R<PaymentOfflineCollectionVO> confirmOfflineCollection(@RequestBody ConfirmOfflineCollectionCommand command);

    @Override
    @GetMapping("/bank-statements/page")
    R<PageResult<PaymentOfflineBankStatementBatchVO>> pageOfflineBankStatements(@SpringQueryMap PaymentConfigPageQuery query);

    @Override
    @GetMapping("/bank-statements/detail")
    R<PaymentOfflineBankStatementBatchVO> detailOfflineBankStatement(@RequestParam("id") Long id);

    @Override
    @GetMapping("/bank-statements/statuses")
    R<List<PaymentOfflineBankStatementBatchStatusVO>> listOfflineBankStatementStatuses();

    @Override
    @GetMapping("/bank-statements/match-statuses")
    R<List<PaymentOfflineBankStatementMatchStatusVO>> listOfflineBankStatementMatchStatuses();

    @Override
    @PostMapping("/bank-statements/import")
    R<PaymentOfflineBankStatementBatchVO> importOfflineBankStatement(
            @RequestBody ImportOfflineBankStatementCommand command);

    @Override
    @PostMapping("/bank-statements/confirm")
    R<PaymentOfflineBankStatementBatchVO> confirmOfflineBankStatementMatch(@RequestBody ConfirmOfflineBankStatementMatchCommand command);

    @Override
    @PostMapping("/refund")
    R<PaymentOfflineRefundVO> createOfflineRefund(@RequestBody CreateOfflineRefundCommand command);
}
