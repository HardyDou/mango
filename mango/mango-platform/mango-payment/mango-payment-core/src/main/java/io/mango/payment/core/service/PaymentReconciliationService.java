package io.mango.payment.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.FetchPaymentChannelBillCommand;
import io.mango.payment.api.command.GenerateMangoPayVirtualBillCommand;
import io.mango.payment.api.command.GeneratePaymentLocalOrderCheckCommand;
import io.mango.payment.api.command.ImportPaymentReconciliationCommand;
import io.mango.payment.api.command.SavePaymentChannelBillSourceCommand;
import io.mango.payment.api.enums.PaymentBusinessOrderStatusEnum;
import io.mango.payment.api.enums.PaymentChannelBillFetchModeEnum;
import io.mango.payment.api.enums.PaymentChannelBillFetchStatusEnum;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentChannelBillFetchBatchVO;
import io.mango.payment.api.vo.PaymentChannelBillFetchModeVO;
import io.mango.payment.api.vo.PaymentChannelBillSourceVO;
import io.mango.payment.api.vo.PaymentChannelBillDetailVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.api.vo.PaymentReconciliationStatusVO;
import io.mango.payment.api.vo.PaymentReconciliationVO;
import io.mango.payment.core.entity.PaymentBusinessOrderEntity;
import io.mango.payment.core.entity.PaymentChannelBillBatchEntity;
import io.mango.payment.core.entity.PaymentChannelBillFetchBatchEntity;
import io.mango.payment.core.entity.PaymentChannelBillSourceEntity;
import io.mango.payment.core.entity.PaymentChannelBillDetailEntity;
import io.mango.payment.core.entity.PaymentChannel;
import io.mango.payment.core.entity.PaymentChannelContract;
import io.mango.payment.core.entity.PaymentDifferenceEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.entity.PaymentRefundOrderEntity;
import io.mango.payment.core.entity.PaymentReconciliationEntity;
import io.mango.payment.core.entity.PaymentTransactionFlowEntity;
import io.mango.payment.core.mapper.PaymentChannelBillBatchMapper;
import io.mango.payment.core.mapper.PaymentChannelBillFetchBatchMapper;
import io.mango.payment.core.mapper.PaymentChannelBillSourceMapper;
import io.mango.payment.core.mapper.PaymentChannelBillDetailMapper;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelMapper;
import io.mango.payment.core.mapper.PaymentDifferenceMapper;
import io.mango.payment.core.mapper.PaymentBusinessOrderMapper;
import io.mango.payment.core.mapper.PaymentOrderMapper;
import io.mango.payment.core.mapper.PaymentRefundOrderMapper;
import io.mango.payment.core.mapper.PaymentReconciliationMapper;
import io.mango.payment.core.mapper.PaymentTransactionFlowMapper;
import io.mango.payment.core.model.Money;
import io.mango.payment.core.model.PaymentChannelBillItemRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private static final String STATUS_MATCHED = "MATCHED";
    private static final String STATUS_DIFFERENCE = "DIFFERENCE";
    private static final String STATUS_IMPORTED = "IMPORTED";
    private static final String MANGO_PAY_CHANNEL_CODE = "MANGO_PAY";
    private static final String FLOW_TYPE_CHANNEL_FEE = "CHANNEL_FEE";
    private static final String FETCH_STATUS_SUCCESS = "SUCCESS";
    private static final String FETCH_STATUS_FAILED = "FAILED";
    private static final String FETCH_STATUS_PROCESSING = "PROCESSING";

    private final PaymentReconciliationMapper reconciliationMapper;
    private final PaymentChannelBillBatchMapper billBatchMapper;
    private final PaymentChannelBillSourceMapper billSourceMapper;
    private final PaymentChannelBillFetchBatchMapper billFetchBatchMapper;
    private final PaymentChannelBillDetailMapper billDetailMapper;
    private final PaymentChannelContractMapper channelContractMapper;
    private final PaymentChannelMapper channelMapper;
    private final PaymentDifferenceMapper differenceMapper;
    private final PaymentBusinessOrderMapper businessOrderMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentTransactionFlowMapper transactionFlowMapper;
    private final PaymentOperationAuditService auditService;
    private final PaymentChannelAdapterRegistry channelAdapterRegistry;
    private final PaymentOrderStateService orderStateService;
    private final PaymentOrderStatusFlowService statusFlowService;
    private final PaymentDuplicateRefundCompletionService duplicateRefundCompletionService;
    private final PaymentObservabilityService observabilityService;
    private final PaymentNumberService numberService;
    private final PaymentChannelBillFileClient billFileClient;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    public PageResult<PaymentReconciliationVO> pageReconciliations(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        String statusCode = PaymentContextSupport.trimToNull(resolved.getStatusCode());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = reconciliationMapper.countReconciliations(tenantId, keyword, statusCode);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentReconciliationVO> rows = reconciliationMapper.selectReconciliationPage(tenantId, keyword, statusCode, size, (page - 1) * size);
        rows.forEach(this::fillReconciliationSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentReconciliationVO detailReconciliation(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "对账批次 ID 不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentReconciliationVO vo = reconciliationMapper.selectReconciliationDetail(tenantId, id);
        Require.notNull(vo, PaymentCode.PAYMENT_RECONCILIATION_NOT_FOUND);
        fillReconciliationSummary(vo);
        List<PaymentChannelBillDetailVO> details = billDetailMapper.selectBillDetails(tenantId, id);
        details.forEach(this::fillBillDetailSummary);
        vo.setDetails(details);
        return vo;
    }

    public List<PaymentReconciliationStatusVO> listReconciliationStatuses() {
        return List.of(
                reconciliationStatus(STATUS_IMPORTED),
                reconciliationStatus(STATUS_MATCHED),
                reconciliationStatus(STATUS_DIFFERENCE));
    }

    public List<PaymentChannelBillFetchModeVO> listBillFetchModes() {
        return PaymentChannelBillFetchModeEnum.options().stream().map(mode -> {
            PaymentChannelBillFetchModeVO vo = new PaymentChannelBillFetchModeVO();
            vo.setFetchMode(mode.getCode());
            vo.setFetchModeName(mode.getLabel());
            return vo;
        }).toList();
    }

    public PageResult<PaymentChannelBillSourceVO> pageBillSources(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = billSourceMapper.countBillSources(tenantId, keyword, resolved.getContractId());
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentChannelBillSourceVO> rows = billSourceMapper.selectBillSourcePage(
                tenantId, keyword, resolved.getContractId(), size, (page - 1) * size);
        rows.forEach(this::fillBillSourceSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentChannelBillSourceVO detailBillSource(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单获取源 ID 不能为空");
        PaymentChannelBillSourceVO vo = billSourceMapper.selectBillSourceDetail(PaymentContextSupport.currentTenantId(), id);
        Require.notNull(vo, PaymentCode.PAYMENT_RECONCILIATION_NOT_FOUND.getCode(), "账单获取源不存在");
        fillBillSourceSummary(vo);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentChannelBillSourceVO saveBillSource(SavePaymentChannelBillSourceCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_RECONCILIATION_INVALID);
        Require.notNull(command.getContractId(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "签约通道不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentChannelContract contract = selectBillSourceContract(command.getContractId(), tenantId);
        PaymentChannel channel = selectBillSourceChannel(contract, tenantId);
        String channelCode = normalizeCode(channel.getChannelCode());
        String fetchMode = normalizeCode(command.getFetchMode());
        String endpoint = PaymentContextSupport.trimToNull(command.getEndpoint());
        String remotePath = PaymentContextSupport.trimToNull(command.getRemotePath());
        String credentialRef = PaymentContextSupport.trimToNull(command.getCredentialRef());
        String pageMode = normalizeCode(command.getPageMode());
        Require.notBlank(fetchMode, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "获取方式不能为空");
        Require.isTrue(PaymentChannelBillFetchModeEnum.contains(fetchMode), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "获取方式仅支持 MANUAL、FTP、FTPS、HTTP");
        Require.isTrue(channelBillFetchModes(channel).contains(fetchMode),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "支付通道未声明支持该账单获取方式");
        Require.isTrue(command.getEnabled() != null && (command.getEnabled() == 0 || command.getEnabled() == 1),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "启用状态只能是 0 或 1");
        if ("HTTP".equals(fetchMode)) {
            Require.notBlank(endpoint, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "HTTP 获取地址不能为空");
            Require.isTrue(pageMode == null || "PAGE".equals(pageMode) || "CURSOR".equals(pageMode),
                    PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "HTTP 分页模式仅支持 PAGE、CURSOR");
        }
        if ("FTP".equals(fetchMode) || "FTPS".equals(fetchMode)) {
            Require.notBlank(endpoint, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "FTP/FTPS 服务器地址不能为空");
            Require.notBlank(remotePath, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "FTP/FTPS 远端路径不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        PaymentChannelBillSourceEntity entity = command.getId() == null ? new PaymentChannelBillSourceEntity() : billSourceMapper.selectById(command.getId());
        if (command.getId() != null) {
            Require.notNull(entity, PaymentCode.PAYMENT_RECONCILIATION_NOT_FOUND.getCode(), "账单获取源不存在");
            Require.isTrue(tenantId.equals(entity.getTenantId()) && Integer.valueOf(0).equals(entity.getDelFlag()),
                PaymentCode.PAYMENT_RECONCILIATION_NOT_FOUND.getCode(), "账单获取源不存在");
        } else {
            entity.setId(IdWorker.getId());
            entity.setTenantId(tenantId);
            entity.setCreatedBy(operatorId);
            entity.setCreatedAt(now);
        }
        entity.setContractId(contract.getId());
        entity.setChannelCode(channelCode);
        entity.setFetchMode(fetchMode);
        entity.setEndpoint(endpoint);
        entity.setRemotePath(remotePath);
        entity.setCredentialRef(credentialRef);
        entity.setPageMode(pageMode);
        entity.setEnabled(command.getEnabled());
        entity.setUpdatedBy(operatorId);
        entity.setUpdatedAt(now);
        if (command.getId() == null) {
            billSourceMapper.insert(entity);
        } else {
            billSourceMapper.updateById(entity);
        }
        auditService.record(
                PaymentOperationAuditService.ACTION_SAVE_CHANNEL_BILL_SOURCE,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL_BILL_SOURCE,
                String.valueOf(entity.getId()),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return detailBillSource(entity.getId());
    }

    public PageResult<PaymentChannelBillFetchBatchVO> pageBillFetchBatches(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        String keyword = PaymentContextSupport.trimToNull(resolved.getKeyword());
        Long tenantId = PaymentContextSupport.currentTenantId();
        long total = billFetchBatchMapper.countFetchBatches(tenantId, keyword);
        long page = resolved.getPage();
        long size = resolved.getSize();
        List<PaymentChannelBillFetchBatchVO> rows = billFetchBatchMapper.selectFetchBatchPage(tenantId, keyword, size, (page - 1) * size);
        rows.forEach(this::fillFetchBatchSummary);
        return PageResult.of(rows, total, page, size);
    }

    public PaymentReconciliationVO fetchChannelBill(FetchPaymentChannelBillCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_RECONCILIATION_INVALID);
        Require.notNull(command.getSourceId(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单获取源 ID 不能为空");
        Require.notNull(command.getBillDate(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单日期不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        PaymentChannelBillSourceEntity source = billSourceMapper.selectById(command.getSourceId());
        Require.notNull(source, PaymentCode.PAYMENT_RECONCILIATION_NOT_FOUND.getCode(), "账单获取源不存在");
        Require.isTrue(tenantId.equals(source.getTenantId()) && Integer.valueOf(0).equals(source.getDelFlag()),
                PaymentCode.PAYMENT_RECONCILIATION_NOT_FOUND.getCode(), "账单获取源不存在");
        Require.isTrue(Integer.valueOf(1).equals(source.getEnabled()),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单获取源未启用");
        if ("MANUAL".equals(source.getFetchMode())) {
            throw new IllegalArgumentException("手动获取方式请使用导入账单入口");
        }
        Require.isTrue("HTTP".equals(source.getFetchMode()) || "FTP".equals(source.getFetchMode()) || "FTPS".equals(source.getFetchMode()),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "当前仅支持 HTTP、FTP、FTPS 自动获取执行");

        LocalDateTime started = LocalDateTime.now();
        PaymentChannelBillFetchBatchEntity fetchBatch = createFetchBatch(source, command, tenantId, started);
        inTransaction(() -> {
            billFetchBatchMapper.insert(fetchBatch);
            return null;
        });
        try {
            ImportPaymentReconciliationCommand importCommand = fetchBill(source, command);
            return inTransaction(() -> completeFetchedBill(
                    source,
                    fetchBatch,
                    importCommand,
                    tenantId,
                    importCommand.getBillFileName(),
                    importCommand.getFileDigest()));
        } catch (RuntimeException ex) {
            inTransaction(() -> {
                fetchBatch.setFetchStatus(FETCH_STATUS_FAILED);
                fetchBatch.setFetchResult(truncate(ex.getMessage(), 512));
                fetchBatch.setFetchEndTime(LocalDateTime.now());
                fetchBatch.setUpdatedAt(fetchBatch.getFetchEndTime());
                billFetchBatchMapper.updateById(fetchBatch);
                return null;
            });
            throw ex;
        }
    }

    private PaymentReconciliationVO completeFetchedBill(
            PaymentChannelBillSourceEntity source,
            PaymentChannelBillFetchBatchEntity fetchBatch,
            ImportPaymentReconciliationCommand importCommand,
            Long tenantId,
            String billFileName,
            String fileDigest) {
        PaymentReconciliationVO reconciliation = createReconciliation(
                importCommand,
                tenantId,
                normalizeCode(source.getChannelCode()),
                billFileName,
                fileDigest,
                PaymentOperationAuditService.ACTION_FETCH_CHANNEL_BILL);
        fetchBatch.setReconciliationId(reconciliation.getId());
        fetchBatch.setResponseDigest(fileDigest);
        fetchBatch.setTotalCount(importCommand.getItems().size());
        fetchBatch.setFetchStatus(FETCH_STATUS_SUCCESS);
        fetchBatch.setFetchResult(fetchModeName(source.getFetchMode()) + "通道账单获取并对账完成");
        fetchBatch.setFetchEndTime(LocalDateTime.now());
        fetchBatch.setUpdatedAt(fetchBatch.getFetchEndTime());
        billFetchBatchMapper.updateById(fetchBatch);
        auditService.record(
                PaymentOperationAuditService.ACTION_FETCH_CHANNEL_BILL,
                PaymentOperationAuditService.RESOURCE_PAYMENT_CHANNEL_BILL_FETCH_BATCH,
                fetchBatch.getBatchNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return reconciliation;
    }

    private <T> T inTransaction(Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> action.get());
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentReconciliationVO importReconciliation(ImportPaymentReconciliationCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_RECONCILIATION_INVALID);
        String channelCode = normalizeCode(command.getChannelCode());
        String billFileName = PaymentContextSupport.trimToNull(command.getBillFileName());
        String fileDigest = PaymentContextSupport.trimToNull(command.getFileDigest());
        Require.notBlank(channelCode, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "通道编码不能为空");
        Require.notNull(command.getBillDate(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单日期不能为空");
        Require.notBlank(billFileName, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单文件名不能为空");
        Require.notBlank(fileDigest, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单文件摘要不能为空");
        Require.notEmpty(command.getItems(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单明细不能为空");
        Require.isTrue(channelCode.length() <= 32, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "通道编码不能超过 32 个字符");
        Require.isTrue(billFileName.length() <= 255, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单文件名不能超过 255 个字符");
        Require.isTrue(fileDigest.length() <= 128, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单文件摘要不能超过 128 个字符");

        Long tenantId = PaymentContextSupport.currentTenantId();
        return createReconciliation(command, tenantId, channelCode, billFileName, fileDigest,
                PaymentOperationAuditService.ACTION_IMPORT_RECONCILIATION);
    }

    private PaymentChannelContract selectBillSourceContract(Long contractId, Long tenantId) {
        PaymentChannelContract contract = channelContractMapper.selectById(contractId);
        Require.notNull(contract, PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND.getCode(), "签约通道不存在");
        Require.isTrue(tenantId.equals(contract.getTenantId()) && Integer.valueOf(0).equals(contract.getDelFlag()),
                PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND.getCode(), "签约通道不存在");
        Require.isTrue(Integer.valueOf(1).equals(contract.getStatus()),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "签约通道未启用");
        return contract;
    }

    private PaymentChannel selectBillSourceChannel(PaymentChannelContract contract, Long tenantId) {
        PaymentChannel channel = channelMapper.selectById(contract.getChannelId());
        Require.notNull(channel, PaymentCode.PAYMENT_CHANNEL_NOT_FOUND.getCode(), "支付通道不存在");
        Require.isTrue(tenantId.equals(channel.getTenantId()) && Integer.valueOf(0).equals(channel.getDelFlag()),
                PaymentCode.PAYMENT_CHANNEL_NOT_FOUND.getCode(), "支付通道不存在");
        Require.isTrue(Integer.valueOf(1).equals(channel.getStatus()),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "支付通道未启用");
        Require.notBlank(channel.getChannelCode(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "支付通道编码不能为空");
        return channel;
    }

    private Set<String> channelBillFetchModes(PaymentChannel channel) {
        String modes = PaymentContextSupport.trimToNull(channel.getBillFetchModes());
        Require.notBlank(modes, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "支付通道未配置账单获取方式");
        Set<String> result = new LinkedHashSet<>();
        Arrays.stream(modes.split(","))
                .map(this::normalizeCode)
                .filter(item -> item != null && PaymentChannelBillFetchModeEnum.contains(item))
                .forEach(result::add);
        Require.isTrue(!result.isEmpty(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "支付通道账单获取方式配置无效");
        return result;
    }

    public PaymentReconciliationVO generateMangoPayVirtualBill(GenerateMangoPayVirtualBillCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_RECONCILIATION_INVALID);
        String channelCode = normalizeCode(command.getChannelCode());
        Require.notBlank(channelCode, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "通道编码不能为空");
        Require.notNull(command.getBillDate(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单日期不能为空");
        Require.isTrue(MANGO_PAY_CHANNEL_CODE.equals(channelCode),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "芒果支付账单生成仅支持 MANGO_PAY 通道");
        Long tenantId = PaymentContextSupport.currentTenantId();
        List<PaymentChannelBillItemRow> rows = channelAdapterRegistry.requireAdapter(channelCode)
                .generateBill(new IPaymentChannelAdapter.ChannelBillCommand(
                        tenantId,
                        channelCode,
                        command.getContractId(),
                        command.getBillDate()))
                .rows();
        ImportPaymentReconciliationCommand importCommand = channelBillImportCommand(channelCode, command.getBillDate(), rows);
        return inTransaction(() -> createReconciliation(
                importCommand,
                tenantId,
                channelCode,
                importCommand.getBillFileName(),
                importCommand.getFileDigest(),
                PaymentOperationAuditService.ACTION_GENERATE_MANGO_PAY_CHANNEL_BILL));
    }

    public PaymentReconciliationVO generateLocalOrderCheck(GeneratePaymentLocalOrderCheckCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_RECONCILIATION_INVALID);
        String channelCode = normalizeCode(command.getChannelCode());
        Require.notBlank(channelCode, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "通道编码不能为空");
        Require.notNull(command.getContractId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "签约通道 ID 不能为空");
        Require.notNull(command.getBillDate(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "核验日期不能为空");
        Long tenantId = PaymentContextSupport.currentTenantId();
        List<PaymentChannelBillItemRow> rows = channelAdapterRegistry.requireAdapter(channelCode)
                .generateBill(new IPaymentChannelAdapter.ChannelBillCommand(
                        tenantId,
                        channelCode,
                        command.getContractId(),
                        command.getBillDate()))
                .rows();
        ImportPaymentReconciliationCommand importCommand = channelBillImportCommand(channelCode, command.getBillDate(), rows);
        return inTransaction(() -> createReconciliation(
                importCommand,
                tenantId,
                channelCode,
                importCommand.getBillFileName(),
                importCommand.getFileDigest(),
                PaymentOperationAuditService.ACTION_GENERATE_LOCAL_ORDER_CHECK));
    }

    private PaymentReconciliationVO createReconciliation(
            ImportPaymentReconciliationCommand command,
            Long tenantId,
            String channelCode,
            String billFileName,
            String fileDigest,
            String auditAction) {
        long startedAt = System.nanoTime();
        long imported = reconciliationMapper.countImportedFile(tenantId, channelCode, command.getBillDate(), fileDigest);
        Require.isTrue(imported == 0, PaymentCode.PAYMENT_RECONCILIATION_FILE_DUPLICATED);

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        String batchNo = numberService.next(PaymentNumberService.PAY_RECON_BATCH_NO);
        PaymentReconciliationEntity reconciliation = new PaymentReconciliationEntity();
        reconciliation.setId(IdWorker.getId());
        reconciliation.setReconciliationNo(batchNo);
        reconciliation.setChannelCode(channelCode);
        reconciliation.setBillDate(command.getBillDate());
        reconciliation.setTotalCount(command.getItems().size());
        reconciliation.setTotalAmount(0L);
        reconciliation.setTotalFee(0L);
        reconciliation.setMatchStatus(STATUS_IMPORTED);
        reconciliation.setBillFileId(command.getBillFileId());
        reconciliation.setBillFileName(billFileName);
        reconciliation.setFileDigest(fileDigest);
        reconciliation.setImporterId(operatorId);
        reconciliation.setImporterName(PaymentContextSupport.currentPrincipalName());
        reconciliation.setImportTime(now);
        reconciliation.setTenantId(tenantId);
        reconciliation.setCreatedBy(operatorId);
        reconciliation.setCreatedAt(now);
        reconciliation.setUpdatedBy(operatorId);
        reconciliation.setUpdatedAt(now);

        ReconciliationSummary summary = persistBillDetailsAndDifferences(command, reconciliation, now, operatorId, tenantId, channelCode, batchNo);
        reconciliation.setTotalAmount(summary.totalAmount());
        reconciliation.setTotalFee(summary.totalFee());
        reconciliation.setMatchStatus(summary.differenceCount() == 0 ? STATUS_MATCHED : STATUS_DIFFERENCE);
        reconciliation.setReconcileResult(summary.differenceCount() == 0
                ? "通道账单与本地支付成功订单金额一致"
                : "发现 " + summary.differenceCount() + " 条对账差异，需进入差异处理");
        reconciliationMapper.insert(reconciliation);
        insertBillBatch(command, reconciliation, summary, now, operatorId, tenantId, channelCode, batchNo);

        auditService.record(
                auditAction,
                PaymentOperationAuditService.RESOURCE_PAYMENT_RECONCILIATION,
                reconciliation.getReconciliationNo(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        observabilityService.logSummary("RECONCILIATION_BATCH", reconciliation.getReconciliationNo(),
                reconciliation.getMatchStatus(), reconciliation.getTotalAmount(), channelCode,
                elapsedMillis(startedAt), PaymentOperationAuditService.RESULT_SUCCESS);
        return detailReconciliation(reconciliation.getId());
    }

    private void insertBillBatch(
            ImportPaymentReconciliationCommand command,
            PaymentReconciliationEntity reconciliation,
            ReconciliationSummary summary,
            LocalDateTime now,
            Long operatorId,
            Long tenantId,
            String channelCode,
            String batchNo) {
        PaymentChannelBillBatchEntity batch = new PaymentChannelBillBatchEntity();
        batch.setId(IdWorker.getId());
        batch.setBatchNo(batchNo);
        batch.setReconciliationId(reconciliation.getId());
        batch.setChannelCode(channelCode);
        batch.setBillDate(command.getBillDate());
        batch.setFileDigest(reconciliation.getFileDigest());
        batch.setBillFileId(command.getBillFileId());
        batch.setBillFileName(reconciliation.getBillFileName());
        batch.setTotalCount(command.getItems().size());
        batch.setTotalAmount(summary.totalAmount());
        batch.setTotalFee(summary.totalFee());
        batch.setImportStatus(reconciliation.getMatchStatus());
        batch.setImporterId(operatorId);
        batch.setImporterName(PaymentContextSupport.currentPrincipalName());
        batch.setImportTime(now);
        batch.setTenantId(tenantId);
        batch.setCreatedBy(operatorId);
        batch.setCreatedAt(now);
        batch.setUpdatedBy(operatorId);
        batch.setUpdatedAt(now);
        billBatchMapper.insert(batch);
    }

    private ReconciliationSummary persistBillDetailsAndDifferences(
            ImportPaymentReconciliationCommand command,
            PaymentReconciliationEntity reconciliation,
            LocalDateTime now,
            Long operatorId,
            Long tenantId,
            String channelCode,
            String batchNo) {
        long totalAmount = 0L;
        long totalFee = 0L;
        int differenceCount = 0;
        List<PaymentDifferenceEntity> differences = new ArrayList<>();
        Set<String> paymentBillTradeNos = new HashSet<>();
        Set<String> refundBillTradeNos = new HashSet<>();
        for (ImportPaymentReconciliationCommand.BillItem item : command.getItems()) {
            BillItemNormalized normalized = normalizeBillItem(item);
            collectBillTradeNo(normalized, paymentBillTradeNos, refundBillTradeNos);
            totalAmount = Money.cents(totalAmount).add(Money.cents(normalized.amount())).toNonNegativeCents();
            totalFee = Money.cents(totalFee).add(Money.cents(normalized.fee())).toNonNegativeCents();
            MatchResult matchResult = matchBillItem(tenantId, channelCode, normalized, now);
            PaymentChannelBillDetailEntity detail = new PaymentChannelBillDetailEntity();
            detail.setId(IdWorker.getId());
            detail.setReconciliationId(reconciliation.getId());
            detail.setBatchNo(batchNo);
            detail.setChannelCode(channelCode);
            detail.setBillDate(command.getBillDate());
            detail.setChannelTradeNo(normalized.channelTradeNo());
            detail.setTradeType(normalized.tradeType());
            detail.setAmount(normalized.amount());
            detail.setFee(normalized.fee());
            detail.setTradeTime(normalized.tradeTime());
            detail.setMatchStatus(matchResult.matchStatus());
            detail.setMatchedOrderNo(matchResult.matchedOrderNo());
            detail.setMatchMessage(matchResult.matchMessage());
            detail.setTenantId(tenantId);
            detail.setCreatedBy(operatorId);
            detail.setCreatedAt(now);
            detail.setUpdatedBy(operatorId);
            detail.setUpdatedAt(now);
            billDetailMapper.insert(detail);

            FeeFlowResult feeFlowResult = ensureChannelFeeFlow(tenantId, normalized, matchResult, now, operatorId);
            if (feeFlowResult != null) {
                matchResult = matchResult.withFeeDifference(feeFlowResult);
            }
            if (matchResult.differenceType() != null) {
                differenceCount++;
                differences.add(createDifference(
                        reconciliation.getId(),
                        tenantId,
                        matchResult.relatedOrderNo(),
                        matchResult.differenceType(),
                        matchResult.differenceAmount(),
                        matchResult.matchMessage(),
                        now,
                        operatorId));
            }
        }
        differenceCount += appendLocalMissingDifferences(
                reconciliation.getId(),
                tenantId,
                channelCode,
                command.getBillDate(),
                paymentBillTradeNos,
                refundBillTradeNos,
                now,
                operatorId,
                differences);
        differences.forEach(differenceMapper::insert);
        return new ReconciliationSummary(totalAmount, totalFee, differenceCount);
    }

    private PaymentChannelBillFetchBatchEntity createFetchBatch(
            PaymentChannelBillSourceEntity source,
            FetchPaymentChannelBillCommand command,
            Long tenantId,
            LocalDateTime started) {
        LocalDateTime requestStart = command.getStartTime() == null
                ? command.getBillDate().atStartOfDay()
                : command.getStartTime();
        LocalDateTime requestEnd = command.getEndTime() == null
                ? command.getBillDate().plusDays(1).atStartOfDay()
                : command.getEndTime();
        Require.isTrue(requestEnd.isAfter(requestStart),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "请求结束时间必须晚于开始时间");
        Long operatorId = PaymentContextSupport.currentUserId();
        PaymentChannelBillFetchBatchEntity batch = new PaymentChannelBillFetchBatchEntity();
        batch.setId(IdWorker.getId());
        batch.setSourceId(source.getId());
        batch.setBatchNo(numberService.next(PaymentNumberService.PAY_RECON_BATCH_NO));
        batch.setChannelCode(normalizeCode(source.getChannelCode()));
        batch.setFetchMode(source.getFetchMode());
        batch.setBillDate(command.getBillDate());
        batch.setRequestStartTime(requestStart);
        batch.setRequestEndTime(requestEnd);
        batch.setRequestPage("PAGE".equals(source.getPageMode()) ? 1 : null);
        batch.setPageSize(200);
        batch.setTotalCount(0);
        batch.setFetchStatus(FETCH_STATUS_PROCESSING);
        batch.setOperatorId(operatorId);
        batch.setOperatorName(PaymentContextSupport.currentPrincipalName());
        batch.setFetchStartTime(started);
        batch.setTenantId(tenantId);
        batch.setCreatedBy(operatorId);
        batch.setCreatedAt(started);
        batch.setUpdatedBy(operatorId);
        batch.setUpdatedAt(started);
        return batch;
    }

    private ImportPaymentReconciliationCommand fetchHttpBill(
            PaymentChannelBillSourceEntity source,
            FetchPaymentChannelBillCommand command) {
        String endpoint = PaymentContextSupport.trimToNull(source.getEndpoint());
        Require.notBlank(endpoint, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "HTTP 获取地址不能为空");
        LocalDateTime startTime = command.getStartTime() == null ? command.getBillDate().atStartOfDay() : command.getStartTime();
        LocalDateTime endTime = command.getEndTime() == null ? command.getBillDate().plusDays(1).atStartOfDay() : command.getEndTime();
        try {
            String url = appendQuery(endpoint, startTime, endTime, command.getBillDate());
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Require.isTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                    PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "HTTP 账单获取失败，状态码：" + response.statusCode());
            return parseBillResponse(source, command, httpBillFileName(source.getChannelCode(), command.getBillDate()), response.body());
        } catch (IOException ex) {
            throw new IllegalArgumentException("HTTP 账单获取失败：" + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("HTTP 账单获取被中断", ex);
        }
    }

    private ImportPaymentReconciliationCommand fetchBill(
            PaymentChannelBillSourceEntity source,
            FetchPaymentChannelBillCommand command) {
        if ("HTTP".equals(source.getFetchMode())) {
            return fetchHttpBill(source, command);
        }
        PaymentChannelBillFileClient.RemoteBillFile file = billFileClient.fetch(source);
        try {
            return parseBillResponse(source, command, file.fileName(), file.body());
        } catch (IOException ex) {
            throw new IllegalArgumentException(source.getFetchMode() + " 账单解析失败：" + ex.getMessage(), ex);
        }
    }

    private ImportPaymentReconciliationCommand parseBillResponse(
            PaymentChannelBillSourceEntity source,
            FetchPaymentChannelBillCommand command,
            String billFileName,
            String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode itemsNode = root.isArray() ? root : root.get("items");
        Require.isTrue(itemsNode != null && itemsNode.isArray() && !itemsNode.isEmpty(),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "通道账单响应必须包含非空 items 数组");
        List<ImportPaymentReconciliationCommand.BillItem> items = new ArrayList<>();
        for (JsonNode node : itemsNode) {
            ImportPaymentReconciliationCommand.BillItem item = new ImportPaymentReconciliationCommand.BillItem();
            item.setChannelTradeNo(requiredText(node, "channelTradeNo"));
            item.setTradeType(requiredText(node, "tradeType"));
            item.setAmount(requiredLong(node, "amount"));
            item.setFee(requiredLong(node, "fee"));
            item.setTradeTime(LocalDateTime.parse(requiredText(node, "tradeTime").replace(' ', 'T')));
            items.add(item);
        }
        String digest = sha256(body);
        ImportPaymentReconciliationCommand importCommand = new ImportPaymentReconciliationCommand();
        importCommand.setChannelCode(source.getChannelCode());
        importCommand.setBillDate(command.getBillDate());
        importCommand.setBillFileName(PaymentContextSupport.trimToNull(billFileName));
        importCommand.setFileDigest(digest);
        importCommand.setItems(items);
        return importCommand;
    }

    private String appendQuery(String endpoint, LocalDateTime startTime, LocalDateTime endTime, LocalDate billDate) {
        String separator = endpoint.contains("?") ? "&" : "?";
        return endpoint + separator
                + "billDate=" + encode(billDate.toString())
                + "&startTime=" + encode(startTime.toString())
                + "&endTime=" + encode(endTime.toString())
                + "&page=1&pageSize=200";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        Require.isTrue(value != null && !value.asText().isBlank(),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "通道账单明细缺少字段：" + field);
        return value.asText();
    }

    private Long requiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        Require.isTrue(value != null && value.canConvertToLong(),
                PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "通道账单明细金额字段无效：" + field);
        return value.asLong();
    }

    private String httpBillFileName(String channelCode, LocalDate billDate) {
        return normalizeCode(channelCode) + "-" + billDate + "-http.bill";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void collectBillTradeNo(
            BillItemNormalized item,
            Set<String> paymentBillTradeNos,
            Set<String> refundBillTradeNos) {
        if ("PAYMENT".equals(item.tradeType())) {
            paymentBillTradeNos.add(item.channelTradeNo());
        } else if ("REFUND".equals(item.tradeType())) {
            refundBillTradeNos.add(item.channelTradeNo());
        }
    }

    private int appendLocalMissingDifferences(
            Long reconciliationId,
            Long tenantId,
            String channelCode,
            LocalDate billDate,
            Set<String> paymentBillTradeNos,
            Set<String> refundBillTradeNos,
            LocalDateTime now,
            Long operatorId,
            List<PaymentDifferenceEntity> differences) {
        int count = 0;
        List<PaymentOrderEntity> missingPaymentOrders = paymentOrderMapper.selectSuccessfulChannelOrdersMissingInBill(
                tenantId,
                channelCode,
                billDate,
                billDate.plusDays(1),
                List.copyOf(paymentBillTradeNos));
        for (PaymentOrderEntity order : missingPaymentOrders) {
            differences.add(createDifference(
                    reconciliationId,
                    tenantId,
                    order.getPayOrderNo(),
                    "LOCAL_SUCCESS_CHANNEL_MISSING",
                    order.getAmount(),
                    "本地支付订单成功但通道账单未出现该支付记录",
                    now,
                    operatorId));
            count++;
        }
        List<PaymentRefundOrderEntity> missingRefundOrders = refundOrderMapper.selectSuccessfulChannelRefundsMissingInBill(
                tenantId,
                channelCode,
                billDate,
                billDate.plusDays(1),
                List.copyOf(refundBillTradeNos));
        for (PaymentRefundOrderEntity refundOrder : missingRefundOrders) {
            differences.add(createDifference(
                    reconciliationId,
                    tenantId,
                    refundOrder.getRefundOrderNo(),
                    "LOCAL_REFUND_CHANNEL_MISSING",
                    refundOrder.getRefundAmount(),
                    "本地退款订单成功但通道账单未出现该退款记录",
                    now,
                    operatorId));
            count++;
        }
        return count;
    }

    private PaymentDifferenceEntity createDifference(
            Long reconciliationId,
            Long tenantId,
            String relatedOrderNo,
            String differenceType,
            Long differenceAmount,
            String processResult,
            LocalDateTime now,
            Long operatorId) {
        PaymentDifferenceEntity difference = new PaymentDifferenceEntity();
        difference.setId(IdWorker.getId());
        difference.setDifferenceNo(numberService.next(PaymentNumberService.PAY_DIFF_NO));
        difference.setReconciliationId(reconciliationId);
        difference.setRelatedOrderNo(relatedOrderNo);
        difference.setDifferenceType(differenceType);
        difference.setDifferenceAmount(differenceAmount);
        difference.setProcessStatus("PENDING");
        difference.setProcessResult(processResult);
        difference.setTenantId(tenantId);
        difference.setCreatedBy(operatorId);
        difference.setCreatedAt(now);
        difference.setUpdatedBy(operatorId);
        difference.setUpdatedAt(now);
        return difference;
    }

    private BillItemNormalized normalizeBillItem(ImportPaymentReconciliationCommand.BillItem item) {
        Require.notNull(item, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单明细不能为空");
        String channelTradeNo = PaymentContextSupport.trimToNull(item.getChannelTradeNo());
        String tradeType = normalizeCode(item.getTradeType());
        Require.notBlank(channelTradeNo, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "通道交易号不能为空");
        Require.notBlank(tradeType, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "交易类型不能为空");
        Require.notNull(item.getAmount(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "金额不能为空");
        Require.notNull(item.getFee(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "手续费不能为空");
        Require.notNull(item.getTradeTime(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "通道交易时间不能为空");
        Require.isTrue(channelTradeNo.length() <= 128, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "通道交易号不能超过 128 个字符");
        Require.isTrue(tradeType.length() <= 32, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "交易类型不能超过 32 个字符");
        long amount = Money.cents(item.getAmount()).toNonNegativeCents();
        long fee = Money.cents(item.getFee()).toNonNegativeCents();
        Require.isTrue(isSupportedTradeType(tradeType), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "交易类型仅支持 PAYMENT、REFUND、FEE");
        return new BillItemNormalized(channelTradeNo, tradeType, amount, fee, item.getTradeTime());
    }

    private MatchResult matchBillItem(Long tenantId, String channelCode, BillItemNormalized item, LocalDateTime compensateTime) {
        if ("PAYMENT".equals(item.tradeType())) {
            return matchPaymentBillItem(tenantId, channelCode, item, compensateTime);
        }
        if ("REFUND".equals(item.tradeType())) {
            return matchRefundBillItem(tenantId, item, compensateTime);
        }
        return new MatchResult(STATUS_IMPORTED, null, "手续费账单无独立本地手续费订单，本轮仅入库供结算汇总使用", null, null, 0L, null, null, null);
    }

    private MatchResult matchPaymentBillItem(Long tenantId, String channelCode, BillItemNormalized item, LocalDateTime compensateTime) {
        PaymentOrderEntity order = paymentOrderMapper.selectByTenantAndChannelTradeNo(tenantId, channelCode, item.channelTradeNo());
        if (order == null) {
            return new MatchResult(
                    STATUS_DIFFERENCE,
                    null,
                    "通道成功但本地未找到支付订单",
                    item.channelTradeNo(),
                    "CHANNEL_SUCCESS_LOCAL_MISSING",
                    item.amount(),
                    null,
                    null,
                    null);
        }
        if (PaymentOrderStatusEnum.PAYING.getCode().equals(order.getStatus()) && item.amount().equals(order.getAmount())) {
            compensatePaymentSuccess(tenantId, order, item, compensateTime);
            return new MatchResult(
                    STATUS_MATCHED,
                    order.getPayOrderNo(),
                    "通道账单确认支付成功，已对账补偿推进本地支付订单",
                    null,
                    null,
                    0L,
                    order.getBusinessOrderId(),
                    order.getId(),
                    null);
        }
        if (!"SUCCESS".equals(order.getStatus())) {
            return new MatchResult(
                    STATUS_DIFFERENCE,
                    order.getPayOrderNo(),
                    "本地支付订单状态不是成功",
                    order.getPayOrderNo(),
                    "STATUS_MISMATCH",
                    item.amount(),
                    null,
                    null,
                    null);
        }
        if (!item.amount().equals(order.getAmount())) {
            return new MatchResult(
                    STATUS_DIFFERENCE,
                    order.getPayOrderNo(),
                    "通道账单金额与本地支付订单金额不一致",
                    order.getPayOrderNo(),
                    "AMOUNT_MISMATCH",
                    differenceAmount(item.amount(), order.getAmount()),
                    null,
                    null,
                    null);
        }
        return new MatchResult(STATUS_MATCHED, order.getPayOrderNo(), "支付成功金额一致", null, null, 0L, order.getBusinessOrderId(), order.getId(), null);
    }

    private MatchResult matchRefundBillItem(Long tenantId, BillItemNormalized item, LocalDateTime compensateTime) {
        PaymentRefundOrderEntity refundOrder = refundOrderMapper.selectEntityByTenantAndChannelRefundNo(tenantId, item.channelTradeNo());
        if (refundOrder == null) {
            return new MatchResult(
                    STATUS_DIFFERENCE,
                    null,
                    "通道退款成功但本地未找到退款订单",
                    item.channelTradeNo(),
                    "CHANNEL_REFUND_LOCAL_MISSING",
                    item.amount(),
                    null,
                    null,
                    null);
        }
        if (isRefundProcessing(refundOrder.getStatus()) && item.amount().equals(refundOrder.getRefundAmount())) {
            PaymentOrderEntity paymentOrder = paymentOrderMapper.selectEntityByTenantAndId(tenantId, refundOrder.getPaymentOrderId());
            compensateRefundSuccess(tenantId, refundOrder, paymentOrder, item, compensateTime);
            return new MatchResult(
                    STATUS_MATCHED,
                    refundOrder.getRefundOrderNo(),
                    "通道账单确认退款成功，已对账补偿推进本地退款订单",
                    null,
                    null,
                    0L,
                    paymentOrder == null ? null : paymentOrder.getBusinessOrderId(),
                    refundOrder.getPaymentOrderId(),
                    refundOrder.getId());
        }
        if (!"SUCCESS".equals(refundOrder.getStatus())) {
            return new MatchResult(
                    STATUS_DIFFERENCE,
                    refundOrder.getRefundOrderNo(),
                    "本地退款订单状态不是成功",
                    refundOrder.getRefundOrderNo(),
                    "REFUND_STATUS_MISMATCH",
                    item.amount(),
                    null,
                    null,
                    null);
        }
        if (!item.amount().equals(refundOrder.getRefundAmount())) {
            return new MatchResult(
                    STATUS_DIFFERENCE,
                    refundOrder.getRefundOrderNo(),
                    "通道退款账单金额与本地退款订单金额不一致",
                    refundOrder.getRefundOrderNo(),
                    "REFUND_AMOUNT_MISMATCH",
                    differenceAmount(item.amount(), refundOrder.getRefundAmount()),
                    null,
                    null,
                    null);
        }
        PaymentOrderEntity paymentOrder = paymentOrderMapper.selectEntityByTenantAndId(tenantId, refundOrder.getPaymentOrderId());
        return new MatchResult(
                STATUS_MATCHED,
                refundOrder.getRefundOrderNo(),
                "退款成功金额一致",
                null,
                null,
                0L,
                paymentOrder == null ? null : paymentOrder.getBusinessOrderId(),
                refundOrder.getPaymentOrderId(),
                refundOrder.getId());
    }

    private void compensatePaymentSuccess(
            Long tenantId,
            PaymentOrderEntity order,
            BillItemNormalized item,
            LocalDateTime compensateTime) {
        orderStateService.requirePaymentTransition(order.getStatus(), PaymentOrderStatusEnum.SUCCESS.getCode());
        int updated = paymentOrderMapper.updatePayingQueryResult(
                tenantId,
                order.getId(),
                PaymentOrderStatusEnum.SUCCESS.getCode(),
                1,
                item.tradeTime());
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "支付订单状态已变化，对账补偿未执行");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_PAYMENT,
                order.getId(),
                order.getPayOrderNo(),
                order.getStatus(),
                PaymentOrderStatusEnum.SUCCESS.getCode(),
                PaymentOrderStatusFlowService.SOURCE_RECONCILIATION_COMPENSATE,
                item.channelTradeNo(),
                compensateTime,
                "对账账单确认支付成功并补偿推进支付订单状态");
        int businessUpdated = businessOrderMapper.markCashierPaySuccess(tenantId, order.getBusinessOrderId(), order.getAmount());
        Require.isTrue(businessUpdated == 1, PaymentCode.PAYMENT_BUSINESS_ORDER_STATE_CHANGED);
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS,
                order.getBusinessOrderId(),
                selectBusinessOrderNo(tenantId, order.getBusinessOrderId()),
                PaymentBusinessOrderStatusEnum.PAYING.getCode(),
                PaymentBusinessOrderStatusEnum.PAID.getCode(),
                PaymentOrderStatusFlowService.SOURCE_RECONCILIATION_COMPENSATE,
                item.channelTradeNo(),
                compensateTime,
                "对账账单确认支付成功并补偿推进业务订单状态");
        PaymentTransactionFlowEntity flow = new PaymentTransactionFlowEntity();
        flow.setFlowNo(numberService.next(PaymentNumberService.PAY_FLOW_NO));
        flow.setBusinessOrderId(order.getBusinessOrderId());
        flow.setPaymentOrderId(order.getId());
        flow.setRefundOrderId(null);
        flow.setFlowType("PAY_SUCCESS");
        flow.setAmount(order.getAmount());
        flow.setTenantId(tenantId);
        flow.setCreatedBy(PaymentContextSupport.currentUserId());
        flow.setCreatedAt(compensateTime);
        flow.setUpdatedBy(PaymentContextSupport.currentUserId());
        flow.setUpdatedAt(compensateTime);
        transactionFlowMapper.insert(flow);
    }

    private void compensateRefundSuccess(
            Long tenantId,
            PaymentRefundOrderEntity refundOrder,
            PaymentOrderEntity paymentOrder,
            BillItemNormalized item,
            LocalDateTime compensateTime) {
        Require.notNull(paymentOrder, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        String currentStatus = normalizeRefundStatus(refundOrder.getStatus());
        orderStateService.requireRefundTransition(currentStatus, PaymentRefundOrderStatusEnum.SUCCESS.getCode());
        int updated = refundOrderMapper.updateRefundingQueryResult(
                tenantId,
                refundOrder.getId(),
                PaymentRefundOrderStatusEnum.SUCCESS.getCode(),
                item.tradeTime());
        Require.isTrue(updated == 1, PaymentCode.PAYMENT_REFUND_ORDER_STATE_INVALID.getCode(), "退款订单状态已变化，对账补偿未执行");
        statusFlowService.record(
                tenantId,
                PaymentOrderStatusFlowService.ORDER_TYPE_REFUND,
                refundOrder.getId(),
                refundOrder.getRefundOrderNo(),
                currentStatus,
                PaymentRefundOrderStatusEnum.SUCCESS.getCode(),
                PaymentOrderStatusFlowService.SOURCE_RECONCILIATION_COMPENSATE,
                item.channelTradeNo(),
                compensateTime,
                "对账账单确认退款成功并补偿推进退款订单状态");
        boolean duplicateRefund = duplicateRefundCompletionService.completeIfDuplicateRefund(
                tenantId,
                toRefundOrderVO(refundOrder, paymentOrder),
                PaymentOrderStatusFlowService.SOURCE_RECONCILIATION_COMPENSATE,
                item.channelTradeNo(),
                compensateTime);
        if (!duplicateRefund) {
            PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, paymentOrder.getBusinessOrderId());
            Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
            int businessUpdated = businessOrderMapper.updateRefundProgress(tenantId, paymentOrder.getBusinessOrderId(), refundOrder.getRefundAmount());
            Require.isTrue(businessUpdated == 1, PaymentCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
            statusFlowService.record(
                    tenantId,
                    PaymentOrderStatusFlowService.ORDER_TYPE_BUSINESS,
                    paymentOrder.getBusinessOrderId(),
                    businessOrder.getBizOrderNo(),
                    businessOrder.getStatus(),
                    nextBusinessRefundStatus(businessOrder, refundOrder.getRefundAmount()),
                    PaymentOrderStatusFlowService.SOURCE_RECONCILIATION_COMPENSATE,
                    item.channelTradeNo(),
                    compensateTime,
                    "对账账单确认退款成功并补偿推进业务订单状态");
        }
        PaymentTransactionFlowEntity flow = new PaymentTransactionFlowEntity();
        flow.setFlowNo(numberService.next(PaymentNumberService.PAY_REFUND_FLOW_NO));
        flow.setBusinessOrderId(paymentOrder.getBusinessOrderId());
        flow.setPaymentOrderId(paymentOrder.getId());
        flow.setRefundOrderId(refundOrder.getId());
        flow.setFlowType("REFUND_SUCCESS");
        flow.setAmount(refundOrder.getRefundAmount());
        flow.setTenantId(tenantId);
        flow.setCreatedBy(PaymentContextSupport.currentUserId());
        flow.setCreatedAt(compensateTime);
        flow.setUpdatedBy(PaymentContextSupport.currentUserId());
        flow.setUpdatedAt(compensateTime);
        transactionFlowMapper.insert(flow);
    }

    private String selectBusinessOrderNo(Long tenantId, Long businessOrderId) {
        PaymentBusinessOrderEntity businessOrder = businessOrderMapper.selectCashierBusinessOrder(tenantId, businessOrderId);
        Require.notNull(businessOrder, PaymentCode.PAYMENT_BUSINESS_ORDER_NOT_FOUND);
        return businessOrder.getBizOrderNo();
    }

    private String nextBusinessRefundStatus(PaymentBusinessOrderEntity businessOrder, Long refundAmount) {
        long paidAmount = businessOrder.getPaidAmount() == null ? 0L : businessOrder.getPaidAmount();
        long refundedAmount = businessOrder.getRefundedAmount() == null ? 0L : businessOrder.getRefundedAmount();
        long nextRefundedAmount = Money.cents(refundedAmount)
                .add(Money.cents(refundAmount == null ? 0L : refundAmount))
                .toNonNegativeCents();
        if (nextRefundedAmount >= Money.cents(paidAmount).toPositiveCents("原支付金额")) {
            return PaymentBusinessOrderStatusEnum.REFUNDED.getCode();
        }
        return PaymentBusinessOrderStatusEnum.PARTIAL_REFUNDED.getCode();
    }

    private boolean isRefundProcessing(String status) {
        return PaymentRefundOrderStatusEnum.REFUNDING.getCode().equals(normalizeRefundStatus(status));
    }

    private String normalizeRefundStatus(String status) {
        if ("PROCESSING".equals(status)) {
            return PaymentRefundOrderStatusEnum.REFUNDING.getCode();
        }
        return status;
    }

    private FeeFlowResult ensureChannelFeeFlow(
            Long tenantId,
            BillItemNormalized item,
            MatchResult matchResult,
            LocalDateTime now,
            Long operatorId) {
        if (!STATUS_MATCHED.equals(matchResult.matchStatus()) || item.fee() <= 0) {
            return null;
        }
        if ("PAYMENT".equals(item.tradeType())) {
            PaymentTransactionFlowEntity existing = transactionFlowMapper.selectChannelFeeFlowByPaymentOrder(tenantId, matchResult.paymentOrderId());
            return ensureChannelFeeFlow(tenantId, matchResult.businessOrderId(), matchResult.paymentOrderId(), null, item.fee(), existing, now, operatorId);
        }
        if ("REFUND".equals(item.tradeType())) {
            PaymentTransactionFlowEntity existing = transactionFlowMapper.selectChannelFeeFlowByRefundOrder(tenantId, matchResult.refundOrderId());
            return ensureChannelFeeFlow(tenantId, matchResult.businessOrderId(), matchResult.paymentOrderId(), matchResult.refundOrderId(), item.fee(), existing, now, operatorId);
        }
        return null;
    }

    private FeeFlowResult ensureChannelFeeFlow(
            Long tenantId,
            Long businessOrderId,
            Long paymentOrderId,
            Long refundOrderId,
            Long fee,
            PaymentTransactionFlowEntity existing,
            LocalDateTime now,
            Long operatorId) {
        if (existing != null) {
            if (!fee.equals(existing.getAmount())) {
                return new FeeFlowResult("CHANNEL_FEE_MISMATCH", differenceAmount(fee, existing.getAmount()), "通道手续费账单金额与已生成手续费流水不一致");
            }
            return null;
        }
        PaymentTransactionFlowEntity flow = new PaymentTransactionFlowEntity();
        flow.setId(IdWorker.getId());
        flow.setFlowNo(numberService.next(PaymentNumberService.PAY_FEE_FLOW_NO));
        flow.setBusinessOrderId(businessOrderId);
        flow.setPaymentOrderId(paymentOrderId);
        flow.setRefundOrderId(refundOrderId);
        flow.setFlowType(FLOW_TYPE_CHANNEL_FEE);
        flow.setAmount(fee);
        flow.setTenantId(tenantId);
        flow.setCreatedBy(operatorId);
        flow.setCreatedAt(now);
        flow.setUpdatedBy(operatorId);
        flow.setUpdatedAt(now);
        transactionFlowMapper.insert(flow);
        return null;
    }

    private void fillReconciliationSummary(PaymentReconciliationVO vo) {
        vo.setMatchStatusName(reconciliationStatusName(vo.getMatchStatus()));
    }

    private void fillBillSourceSummary(PaymentChannelBillSourceVO vo) {
        vo.setFetchModeName(fetchModeName(vo.getFetchMode()));
        vo.setEnabledName(Integer.valueOf(1).equals(vo.getEnabled()) ? "启用" : "停用");
    }

    private void fillFetchBatchSummary(PaymentChannelBillFetchBatchVO vo) {
        vo.setFetchModeName(fetchModeName(vo.getFetchMode()));
        vo.setFetchStatusName(fetchStatusName(vo.getFetchStatus()));
    }

    private void fillBillDetailSummary(PaymentChannelBillDetailVO vo) {
        vo.setTradeTypeName(tradeTypeName(vo.getTradeType()));
        vo.setMatchStatusName(reconciliationStatusName(vo.getMatchStatus()));
    }

    private PaymentReconciliationStatusVO reconciliationStatus(String statusCode) {
        PaymentReconciliationStatusVO vo = new PaymentReconciliationStatusVO();
        vo.setStatusCode(statusCode);
        vo.setStatusName(reconciliationStatusName(statusCode));
        return vo;
    }

    private String reconciliationStatusName(String status) {
        if (STATUS_IMPORTED.equals(status)) {
            return "已导入";
        }
        if (STATUS_MATCHED.equals(status)) {
            return "已平账";
        }
        if (STATUS_DIFFERENCE.equals(status)) {
            return "存在差异";
        }
        return status;
    }

    private String tradeTypeName(String tradeType) {
        if ("PAYMENT".equals(tradeType)) {
            return "支付";
        }
        if ("REFUND".equals(tradeType)) {
            return "退款";
        }
        if ("FEE".equals(tradeType)) {
            return "手续费";
        }
        return tradeType;
    }

    private String fetchModeName(String fetchMode) {
        return PaymentChannelBillFetchModeEnum.labelOf(fetchMode);
    }

    private String fetchStatusName(String status) {
        return PaymentChannelBillFetchStatusEnum.labelOf(status);
    }

    private String normalizeCode(String value) {
        String normalized = PaymentContextSupport.trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private boolean isSupportedTradeType(String tradeType) {
        return "PAYMENT".equals(tradeType) || "REFUND".equals(tradeType) || "FEE".equals(tradeType);
    }

    private ImportPaymentReconciliationCommand.BillItem toBillItem(PaymentChannelBillItemRow row) {
        ImportPaymentReconciliationCommand.BillItem item = new ImportPaymentReconciliationCommand.BillItem();
        item.setChannelTradeNo(row.getChannelTradeNo());
        item.setTradeType(row.getTradeType());
        item.setAmount(row.getAmount());
        item.setFee(row.getFee());
        item.setTradeTime(row.getTradeTime());
        return item;
    }

    private ImportPaymentReconciliationCommand channelBillImportCommand(
            String channelCode,
            LocalDate billDate,
            List<PaymentChannelBillItemRow> rows) {
        Require.notEmpty(rows, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "通道账单明细不能为空");
        ImportPaymentReconciliationCommand importCommand = new ImportPaymentReconciliationCommand();
        importCommand.setChannelCode(channelCode);
        importCommand.setBillDate(billDate);
        importCommand.setBillFileName(mangoPayBillFileName(channelCode, billDate));
        importCommand.setFileDigest(mangoPayBillDigest(channelCode, billDate, rows));
        importCommand.setItems(rows.stream().map(this::toBillItem).toList());
        return importCommand;
    }

    private long differenceAmount(Long channelAmount, Long localAmount) {
        return Money.cents(channelAmount)
                .subtract(Money.cents(localAmount))
                .abs()
                .toNonNegativeCents();
    }

    private String mangoPayBillFileName(String channelCode, LocalDate billDate) {
        return channelCode + "-" + billDate + "-generated.bill";
    }

    private String mangoPayBillDigest(String channelCode, LocalDate billDate, List<PaymentChannelBillItemRow> rows) {
        StringBuilder builder = new StringBuilder(channelCode).append('|').append(billDate);
        rows.forEach(row -> builder.append('|')
                .append(row.getChannelTradeNo()).append(',')
                .append(row.getTradeType()).append(',')
                .append(row.getAmount()).append(',')
                .append(row.getFee()).append(',')
                .append(row.getTradeTime()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private PaymentRefundOrderVO toRefundOrderVO(PaymentRefundOrderEntity refundOrder, PaymentOrderEntity paymentOrder) {
        PaymentRefundOrderVO vo = new PaymentRefundOrderVO();
        vo.setId(refundOrder.getId());
        vo.setRefundOrderNo(refundOrder.getRefundOrderNo());
        vo.setBizRefundNo(refundOrder.getBizRefundNo());
        vo.setPaymentOrderId(refundOrder.getPaymentOrderId());
        vo.setPayOrderNo(paymentOrder.getPayOrderNo());
        vo.setBusinessOrderId(paymentOrder.getBusinessOrderId());
        vo.setRefundAmount(refundOrder.getRefundAmount());
        vo.setChannelRefundNo(refundOrder.getChannelRefundNo());
        vo.setStatus(refundOrder.getStatus());
        return vo;
    }

    private record BillItemNormalized(
            String channelTradeNo,
            String tradeType,
            Long amount,
            Long fee,
            LocalDateTime tradeTime
    ) {
    }

    private record MatchResult(
            String matchStatus,
            String matchedOrderNo,
            String matchMessage,
            String relatedOrderNo,
            String differenceType,
            Long differenceAmount,
            Long businessOrderId,
            Long paymentOrderId,
            Long refundOrderId
    ) {
        private MatchResult withFeeDifference(FeeFlowResult feeFlowResult) {
            return new MatchResult(
                    STATUS_DIFFERENCE,
                    matchedOrderNo,
                    feeFlowResult.matchMessage(),
                    matchedOrderNo,
                    feeFlowResult.differenceType(),
                    feeFlowResult.differenceAmount(),
                    businessOrderId,
                    paymentOrderId,
                    refundOrderId);
        }
    }

    private record ReconciliationSummary(
            long totalAmount,
            long totalFee,
            int differenceCount
    ) {
    }

    private record FeeFlowResult(
            String differenceType,
            Long differenceAmount,
            String matchMessage
    ) {
    }
}
