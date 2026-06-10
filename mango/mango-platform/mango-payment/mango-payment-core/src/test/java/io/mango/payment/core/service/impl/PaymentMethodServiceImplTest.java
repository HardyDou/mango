package io.mango.payment.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.SavePaymentMethodCommand;
import io.mango.payment.api.vo.PaymentMethodCategoryVO;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.entity.PaymentMethodCategory;
import io.mango.payment.core.mapper.PaymentMethodCategoryMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.service.PaymentOperationAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentMethodServiceImplTest {

    private PaymentMethodMapper methodMapper;
    private PaymentMethodCategoryMapper categoryMapper;
    private PaymentOperationAuditService auditService;
    private PaymentMethodServiceImpl service;

    @BeforeEach
    void setUp() {
        methodMapper = mock(PaymentMethodMapper.class);
        categoryMapper = mock(PaymentMethodCategoryMapper.class);
        auditService = mock(PaymentOperationAuditService.class);
        service = new PaymentMethodServiceImpl(methodMapper, categoryMapper, auditService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("createMethod should validate category path, force channel empty, and audit")
    void createMethod_validCategory_insertsAndAudits() {
        mockCategoryPath();
        when(methodMapper.selectCount(any())).thenReturn(0L);
        ArgumentCaptor<PaymentMethod> captor = ArgumentCaptor.forClass(PaymentMethod.class);

        service.createMethod(command());

        verify(methodMapper).insert(captor.capture());
        PaymentMethod entity = captor.getValue();
        assertThat(entity.getTenantId()).isEqualTo(1L);
        assertThat(entity.getChannelId()).isNull();
        assertThat(entity.getAccountNature()).isEqualTo("PERSONAL");
        assertThat(entity.getInstrumentType()).isEqualTo("WECHAT");
        assertThat(entity.getInteractionType()).isEqualTo("QR_CODE");
        assertThat(entity.getCashierGroupCode()).isEqualTo("WECHAT_PAY");
        assertThat(entity.getCashierGroupName()).isEqualTo("微信支付");
        assertThat(entity.getCashierGroupSort()).isEqualTo(10);
        assertThat(entity.getRequiresQrRefresh()).isEqualTo(1);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_CREATE_METHOD,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD,
                "PERSONAL_WECHAT_QR_TEST",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("createMethod should reject duplicate method code")
    void createMethod_duplicateCode_rejects() {
        mockCategoryPath();
        when(methodMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createMethod(command()))
                .isInstanceOf(BizException.class)
                .hasMessage("支付方式编码不能重复");

        verify(methodMapper, never()).insert(any(PaymentMethod.class));
    }

    @Test
    @DisplayName("createMethod should reject invalid category path")
    void createMethod_invalidCategory_rejects() {
        when(categoryMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.createMethod(command()))
                .isInstanceOf(BizException.class)
                .hasMessage("支付方式三级分类不正确");

        verify(methodMapper, never()).insert(any(PaymentMethod.class));
    }

    @Test
    @DisplayName("createMethod should accept bank card debit quick standard hierarchy")
    void createMethod_bankCardDebitQuick_acceptsStandardHierarchy() {
        PaymentMethodCategory personal = category(360001L, "PERSONAL", "对私", 1, 0L, 10);
        PaymentMethodCategory bankCard = category(360104L, "BANK_CARD", "银行卡", 2, 360001L, 40);
        PaymentMethodCategory debitQuick = category(360331L, "DEBIT_QUICK", "储蓄卡快捷", 3, 360104L, 10);
        when(categoryMapper.selectOne(any())).thenReturn(personal, bankCard, debitQuick);
        when(methodMapper.selectCount(any())).thenReturn(0L);
        ArgumentCaptor<PaymentMethod> captor = ArgumentCaptor.forClass(PaymentMethod.class);

        SavePaymentMethodCommand command = command();
        command.setMethodCode("PERSONAL_DEBIT_QUICK_TEST");
        command.setMethodName("储蓄卡快捷测试");
        command.setInstrumentType("BANK_CARD");
        command.setInteractionType("DEBIT_QUICK");
        command.setPaymentMaterialType("HTML_FORM");
        command.setRequiresBankSelection(1);
        service.createMethod(command);

        verify(methodMapper).insert(captor.capture());
        PaymentMethod entity = captor.getValue();
        assertThat(entity.getAccountNature()).isEqualTo("PERSONAL");
        assertThat(entity.getInstrumentType()).isEqualTo("BANK_CARD");
        assertThat(entity.getInteractionType()).isEqualTo("DEBIT_QUICK");
        assertThat(entity.getCashierGroupCode()).isEqualTo("WECHAT_PAY");
    }

    @Test
    @DisplayName("deleteMethod should reject and audit when method has related data")
    void deleteMethod_withRelations_rejectsAndAudits() {
        PaymentMethod method = method();
        when(methodMapper.selectById(340001L)).thenReturn(method);
        when(methodMapper.countDeleteRelations(1L, 340001L, "PERSONAL_WECHAT_QR")).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteMethod(340001L))
                .isInstanceOf(BizException.class)
                .hasMessage(PaymentCode.PAYMENT_METHOD_DELETE_HAS_RELATIONS.getMessage());

        verify(methodMapper, never()).deleteById(340001L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_METHOD,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD,
                "PERSONAL_WECHAT_QR",
                PaymentOperationAuditService.RESULT_REJECTED);
    }

    @Test
    @DisplayName("deleteMethod should logical delete and audit when method has no related data")
    void deleteMethod_withoutRelations_deletesAndAudits() {
        PaymentMethod method = method();
        when(methodMapper.selectById(340001L)).thenReturn(method);
        when(methodMapper.countDeleteRelations(1L, 340001L, "PERSONAL_WECHAT_QR")).thenReturn(0L);
        when(methodMapper.deleteById(340001L)).thenReturn(1);

        service.deleteMethod(340001L);

        verify(methodMapper).deleteById(340001L);
        verify(auditService).record(
                PaymentOperationAuditService.ACTION_DELETE_METHOD,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD,
                "PERSONAL_WECHAT_QR",
                PaymentOperationAuditService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("listMethodCategories should return ordered tree")
    void listMethodCategories_returnsTree() {
        PaymentMethodCategory personal = category(360001L, "PERSONAL", "对私", 1, 0L, 10);
        PaymentMethodCategory wechat = category(360101L, "WECHAT", "微信", 2, 360001L, 10);
        PaymentMethodCategory qr = category(360301L, "QR_CODE", "扫码", 3, 360101L, 10);
        when(categoryMapper.selectList(any())).thenReturn(List.of(personal, wechat, qr));

        List<PaymentMethodCategoryVO> tree = service.listMethodCategories().getData();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getCategoryCode()).isEqualTo("PERSONAL");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren().get(0).getCategoryCode()).isEqualTo("QR_CODE");
    }

    private void mockCategoryPath() {
        PaymentMethodCategory personal = category(360001L, "PERSONAL", "对私", 1, 0L, 10);
        PaymentMethodCategory wechat = category(360101L, "WECHAT", "微信", 2, 360001L, 10);
        PaymentMethodCategory qr = category(360301L, "QR_CODE", "扫码", 3, 360101L, 10);
        when(categoryMapper.selectOne(any())).thenReturn(personal, wechat, qr);
    }

    private SavePaymentMethodCommand command() {
        SavePaymentMethodCommand command = new SavePaymentMethodCommand();
        command.setMethodCode("PERSONAL_WECHAT_QR_TEST");
        command.setMethodName("微信扫码测试");
        command.setAccountNature("PERSONAL");
        command.setInstrumentType("WECHAT");
        command.setInteractionType("QR_CODE");
        command.setTerminalScope("WEB,H5");
        command.setPaymentMaterialType("QR");
        command.setCashierGroupCode("WECHAT_PAY");
        command.setCashierGroupName("微信支付");
        command.setCashierGroupSort(10);
        command.setRequiresQrRefresh(1);
        command.setMinAmount(1L);
        command.setMaxAmount(5000000L);
        command.setSort(100);
        command.setStatus(1);
        return command;
    }

    private PaymentMethod method() {
        PaymentMethod method = new PaymentMethod();
        method.setId(340001L);
        method.setTenantId(1L);
        method.setMethodCode("PERSONAL_WECHAT_QR");
        method.setMethodName("微信扫码");
        return method;
    }

    private PaymentMethodCategory category(Long id, String code, String name, Integer level, Long parentId, Integer sort) {
        PaymentMethodCategory category = new PaymentMethodCategory();
        category.setId(id);
        category.setCategoryCode(code);
        category.setCategoryName(name);
        category.setLevel(level);
        category.setParentId(parentId);
        category.setSort(sort);
        category.setStatus(1);
        category.setTenantId(1L);
        return category;
    }
}
