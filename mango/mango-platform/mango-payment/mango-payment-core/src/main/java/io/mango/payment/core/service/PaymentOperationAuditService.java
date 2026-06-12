package io.mango.payment.core.service;

import io.mango.payment.core.entity.PaymentOperationAudit;
import io.mango.payment.core.mapper.PaymentOperationAuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentOperationAuditService {

    public static final String RESOURCE_PAYMENT_APPLICATION = "PAYMENT_APPLICATION";
    public static final String RESOURCE_PAYMENT_ENTERPRISE_SUBJECT = "PAYMENT_ENTERPRISE_SUBJECT";
    public static final String RESOURCE_PAYMENT_CHANNEL = "PAYMENT_CHANNEL";
    public static final String RESOURCE_PAYMENT_CHANNEL_CONTRACT = "PAYMENT_CHANNEL_CONTRACT";
    public static final String RESOURCE_PAYMENT_METHOD = "PAYMENT_METHOD";
    public static final String RESOURCE_PAYMENT_METHOD_ROUTE = "PAYMENT_METHOD_ROUTE";
    public static final String RESOURCE_PAYMENT_CASHIER_CONFIG = "PAYMENT_CASHIER_CONFIG";
    public static final String RESOURCE_PAYMENT_ORDER = "PAYMENT_ORDER";
    public static final String RESOURCE_PAYMENT_OFFLINE_COLLECTION = "PAYMENT_OFFLINE_COLLECTION";
    public static final String RESOURCE_PAYMENT_OFFLINE_REFUND = "PAYMENT_OFFLINE_REFUND";
    public static final String RESOURCE_PAYMENT_REFUND_APPROVAL = "PAYMENT_REFUND_APPROVAL";
    public static final String RESOURCE_PAYMENT_EXCEPTION_ORDER = "PAYMENT_EXCEPTION_ORDER";
    public static final String RESOURCE_PAYMENT_NOTIFICATION_RECORD = "PAYMENT_NOTIFICATION_RECORD";
    public static final String RESOURCE_PAYMENT_RECONCILIATION = "PAYMENT_RECONCILIATION";
    public static final String RESOURCE_PAYMENT_CHANNEL_BILL_SOURCE = "PAYMENT_CHANNEL_BILL_SOURCE";
    public static final String RESOURCE_PAYMENT_CHANNEL_BILL_FETCH_BATCH = "PAYMENT_CHANNEL_BILL_FETCH_BATCH";
    public static final String RESOURCE_PAYMENT_DIFFERENCE = "PAYMENT_DIFFERENCE";
    public static final String RESOURCE_PAYMENT_SETTLEMENT_SUMMARY = "PAYMENT_SETTLEMENT_SUMMARY";
    public static final String RESOURCE_PAYMENT_MANGO_PAY_CHANNEL_SCENARIO = "PAYMENT_MANGO_PAY_CHANNEL_SCENARIO";
    public static final String RESOURCE_PAYMENT_SENSITIVE_FIELDS = "PAYMENT_SENSITIVE_FIELDS";
    public static final String ACTION_CREATE_APPLICATION = "CREATE_APPLICATION";
    public static final String ACTION_UPDATE_APPLICATION = "UPDATE_APPLICATION";
    public static final String ACTION_DELETE_APPLICATION = "DELETE_APPLICATION";
    public static final String ACTION_CREATE_ENTERPRISE_SUBJECT = "CREATE_ENTERPRISE_SUBJECT";
    public static final String ACTION_UPDATE_ENTERPRISE_SUBJECT = "UPDATE_ENTERPRISE_SUBJECT";
    public static final String ACTION_DELETE_ENTERPRISE_SUBJECT = "DELETE_ENTERPRISE_SUBJECT";
    public static final String ACTION_CREATE_CHANNEL = "CREATE_CHANNEL";
    public static final String ACTION_UPDATE_CHANNEL = "UPDATE_CHANNEL";
    public static final String ACTION_DELETE_CHANNEL = "DELETE_CHANNEL";
    public static final String ACTION_CREATE_CHANNEL_CONTRACT = "CREATE_CHANNEL_CONTRACT";
    public static final String ACTION_UPDATE_CHANNEL_CONTRACT = "UPDATE_CHANNEL_CONTRACT";
    public static final String ACTION_DELETE_CHANNEL_CONTRACT = "DELETE_CHANNEL_CONTRACT";
    public static final String ACTION_ROTATE_CHANNEL_CERTIFICATE = "ROTATE_CHANNEL_CERTIFICATE";
    public static final String ACTION_CREATE_METHOD = "CREATE_METHOD";
    public static final String ACTION_UPDATE_METHOD = "UPDATE_METHOD";
    public static final String ACTION_DELETE_METHOD = "DELETE_METHOD";
    public static final String ACTION_CREATE_METHOD_ROUTE = "CREATE_METHOD_ROUTE";
    public static final String ACTION_UPDATE_METHOD_ROUTE = "UPDATE_METHOD_ROUTE";
    public static final String ACTION_DELETE_METHOD_ROUTE = "DELETE_METHOD_ROUTE";
    public static final String ACTION_TRIAL_METHOD_ROUTE = "TRIAL_METHOD_ROUTE";
    public static final String ACTION_CREATE_CASHIER_CONFIG = "CREATE_CASHIER_CONFIG";
    public static final String ACTION_UPDATE_CASHIER_CONFIG = "UPDATE_CASHIER_CONFIG";
    public static final String ACTION_DELETE_CASHIER_CONFIG = "DELETE_CASHIER_CONFIG";
    public static final String ACTION_CLOSE_PAYMENT_ORDER = "CLOSE_PAYMENT_ORDER";
    public static final String ACTION_SYNC_PAYMENT_ORDER_STATUS = "SYNC_PAYMENT_ORDER_STATUS";
    public static final String ACTION_EXPIRE_OPEN_PAYMENT_ORDERS = "EXPIRE_OPEN_PAYMENT_ORDERS";
    public static final String ACTION_QUERY_PROCESSING_PAYMENT_ORDERS = "QUERY_PROCESSING_PAYMENT_ORDERS";
    public static final String ACTION_SUBMIT_OFFLINE_TRANSFER_VOUCHER = "SUBMIT_OFFLINE_TRANSFER_VOUCHER";
    public static final String ACTION_CONFIRM_OFFLINE_COLLECTION = "CONFIRM_OFFLINE_COLLECTION";
    public static final String ACTION_IMPORT_OFFLINE_BANK_STATEMENT = "IMPORT_OFFLINE_BANK_STATEMENT";
    public static final String ACTION_CONFIRM_OFFLINE_BANK_STATEMENT_MATCH = "CONFIRM_OFFLINE_BANK_STATEMENT_MATCH";
    public static final String ACTION_CREATE_OFFLINE_REFUND = "CREATE_OFFLINE_REFUND";
    public static final String ACTION_CREATE_REFUND_APPROVAL = "CREATE_REFUND_APPROVAL";
    public static final String ACTION_APPROVE_REFUND_APPROVAL = "APPROVE_REFUND_APPROVAL";
    public static final String ACTION_REJECT_REFUND_APPROVAL = "REJECT_REFUND_APPROVAL";
    public static final String ACTION_HANDLE_EXCEPTION_ORDER = "HANDLE_EXCEPTION_ORDER";
    public static final String ACTION_RETRY_NOTIFICATION_RECORD = "RETRY_NOTIFICATION_RECORD";
    public static final String ACTION_DELIVER_DUE_NOTIFICATION_RECORDS = "DELIVER_DUE_NOTIFICATION_RECORDS";
    public static final String ACTION_IMPORT_RECONCILIATION = "IMPORT_RECONCILIATION";
    public static final String ACTION_GENERATE_MANGO_PAY_CHANNEL_BILL = "GENERATE_MANGO_PAY_CHANNEL_BILL";
    public static final String ACTION_SAVE_CHANNEL_BILL_SOURCE = "SAVE_CHANNEL_BILL_SOURCE";
    public static final String ACTION_FETCH_CHANNEL_BILL = "FETCH_CHANNEL_BILL";
    public static final String ACTION_CREATE_MANGO_PAY_CHANNEL_SCENARIO = "CREATE_MANGO_PAY_CHANNEL_SCENARIO";
    public static final String ACTION_REENCRYPT_SENSITIVE_FIELDS = "REENCRYPT_SENSITIVE_FIELDS";
    public static final String ACTION_HANDLE_DIFFERENCE = "HANDLE_DIFFERENCE";
    public static final String ACTION_GENERATE_SETTLEMENT_SUMMARY = "GENERATE_SETTLEMENT_SUMMARY";
    public static final String ACTION_REBUILD_SETTLEMENT_SUMMARY = "REBUILD_SETTLEMENT_SUMMARY";
    public static final String ACTION_CONFIRM_SETTLEMENT_SUMMARY = "CONFIRM_SETTLEMENT_SUMMARY";
    public static final String ACTION_VOID_SETTLEMENT_SUMMARY = "VOID_SETTLEMENT_SUMMARY";
    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_REJECTED = "REJECTED";

    private final PaymentOperationAuditMapper auditMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void record(String operationAction, String resourceType, String resourceId, String operationResult) {
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        PaymentOperationAudit audit = new PaymentOperationAudit();
        audit.setOperatorId(operatorId);
        audit.setOperatorName(PaymentContextSupport.currentPrincipalName());
        audit.setOperationAction(operationAction);
        audit.setResourceType(resourceType);
        audit.setResourceId(resourceId);
        audit.setOperationResult(operationResult);
        audit.setOperationTime(now);
        audit.setTenantId(PaymentContextSupport.currentTenantId());
        audit.setCreatedBy(operatorId);
        audit.setCreatedAt(now);
        audit.setUpdatedBy(operatorId);
        audit.setUpdatedAt(now);
        auditMapper.insert(audit);
    }
}
