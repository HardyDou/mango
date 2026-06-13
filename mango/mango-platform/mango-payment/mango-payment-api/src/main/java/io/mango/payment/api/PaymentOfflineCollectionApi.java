package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.command.ConfirmOfflineBankStatementMatchCommand;
import io.mango.payment.api.command.ConfirmOfflineCollectionCommand;
import io.mango.payment.api.command.CreateOfflineRefundCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentOfflineBankStatementBatchStatusVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementBatchVO;
import io.mango.payment.api.vo.PaymentOfflineBankStatementMatchStatusVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionStatusVO;
import io.mango.payment.api.vo.PaymentOfflineCollectionVO;
import io.mango.payment.api.vo.PaymentOfflineRefundVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.util.List;

@Validated
public interface PaymentOfflineCollectionApi {

    R<PageResult<PaymentOfflineCollectionVO>> pageOfflineCollections(@Valid PaymentConfigPageQuery query);

    R<PaymentOfflineCollectionVO> detailOfflineCollection(@NotNull(message = "线下收款 ID 不能为空") Long id);

    R<List<PaymentOfflineCollectionStatusVO>> listOfflineCollectionStatuses();

    R<PaymentOfflineCollectionVO> confirmOfflineCollection(@Valid ConfirmOfflineCollectionCommand command);

    R<PageResult<PaymentOfflineBankStatementBatchVO>> pageOfflineBankStatements(@Valid PaymentConfigPageQuery query);

    R<PaymentOfflineBankStatementBatchVO> detailOfflineBankStatement(@NotNull(message = "银行流水批次 ID 不能为空") Long id);

    R<List<PaymentOfflineBankStatementBatchStatusVO>> listOfflineBankStatementStatuses();

    R<List<PaymentOfflineBankStatementMatchStatusVO>> listOfflineBankStatementMatchStatuses();

    R<PaymentOfflineBankStatementBatchVO> importOfflineBankStatement(byte[] fileContent, String originalFilename, Long statementFileId) throws IOException;

    R<PaymentOfflineBankStatementBatchVO> confirmOfflineBankStatementMatch(@Valid ConfirmOfflineBankStatementMatchCommand command);

    R<PaymentOfflineRefundVO> createOfflineRefund(@Valid CreateOfflineRefundCommand command);
}
