package io.mango.payment.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.command.SavePaymentMethodCommand;
import io.mango.payment.api.query.PaymentConfigPageQuery;
import io.mango.payment.api.vo.PaymentMethodCategoryVO;
import io.mango.payment.api.vo.PaymentMethodVO;
import io.mango.payment.core.entity.PaymentMethodCategory;
import io.mango.payment.core.entity.PaymentMethod;
import io.mango.payment.core.mapper.PaymentMethodCategoryMapper;
import io.mango.payment.core.mapper.PaymentMethodMapper;
import io.mango.payment.core.service.IPaymentMethodService;
import io.mango.payment.core.service.PaymentContextSupport;
import io.mango.payment.core.service.PaymentOperationAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements IPaymentMethodService {

    private static final Set<String> TERMINAL_TYPES = Set.of("WEB", "H5");
    private static final Set<String> PAYMENT_MATERIAL_TYPES = Set.of("QR", "REDIRECT_URL", "HTML_FORM", "TRANSFER_ACCOUNT", "H5_PARAM");
    private static final Set<Integer> STATUS_VALUES = Set.of(0, 1);
    private static final Long ROOT_CATEGORY_PARENT_ID = 0L;

    private final PaymentMethodMapper methodMapper;
    private final PaymentMethodCategoryMapper categoryMapper;
    private final PaymentOperationAuditService auditService;

    @Override
    public R<PageResult<PaymentMethodVO>> pageMethods(PaymentConfigPageQuery query) {
        PaymentConfigPageQuery resolved = query == null ? new PaymentConfigPageQuery() : query;
        IPage<PaymentMethod> page = methodMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper(resolved));
        List<PaymentMethodVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<List<PaymentMethodCategoryVO>> listMethodCategories() {
        List<PaymentMethodCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<PaymentMethodCategory>()
                .eq(PaymentMethodCategory::getTenantId, PaymentContextSupport.currentTenantId())
                .eq(PaymentMethodCategory::getStatus, 1)
                .orderByAsc(PaymentMethodCategory::getLevel)
                .orderByAsc(PaymentMethodCategory::getSort)
                .orderByAsc(PaymentMethodCategory::getId));
        Map<Long, PaymentMethodCategoryVO> byId = new LinkedHashMap<>();
        for (PaymentMethodCategory category : categories) {
            byId.put(category.getId(), toCategoryVO(category));
        }
        List<PaymentMethodCategoryVO> roots = new ArrayList<>();
        for (PaymentMethodCategory category : categories) {
            PaymentMethodCategoryVO vo = byId.get(category.getId());
            if (ROOT_CATEGORY_PARENT_ID.equals(category.getParentId())) {
                roots.add(vo);
            } else {
                PaymentMethodCategoryVO parent = byId.get(category.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                }
            }
        }
        return R.ok(roots);
    }

    @Override
    public R<PaymentMethodVO> detailMethod(Long id) {
        return R.ok(toVO(selectRequired(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createMethod(SavePaymentMethodCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_METHOD_INVALID);
        validate(command, false);
        PaymentMethod entity = new PaymentMethod();
        copy(command, entity);
        entity.setTenantId(PaymentContextSupport.currentTenantId());
        methodMapper.insert(entity);
        auditService.record(
                PaymentOperationAuditService.ACTION_CREATE_METHOD,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD,
                entity.getMethodCode(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateMethod(SavePaymentMethodCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_METHOD_INVALID);
        validate(command, true);
        PaymentMethod entity = selectRequired(command.getId());
        copy(command, entity);
        boolean updated = methodMapper.updateById(entity) > 0;
        auditService.record(
                PaymentOperationAuditService.ACTION_UPDATE_METHOD,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD,
                entity.getMethodCode(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteMethod(Long id) {
        PaymentMethod entity = selectRequired(id);
        long relationCount = countDeleteRelations(entity);
        if (relationCount > 0) {
            auditService.record(
                    PaymentOperationAuditService.ACTION_DELETE_METHOD,
                    PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD,
                    entity.getMethodCode(),
                    PaymentOperationAuditService.RESULT_REJECTED);
        }
        Require.isTrue(relationCount == 0, PaymentCode.PAYMENT_METHOD_DELETE_HAS_RELATIONS);
        boolean deleted = methodMapper.deleteById(id) > 0;
        Require.isTrue(deleted, PaymentCode.PAYMENT_METHOD_DELETE_FAILED);
        auditService.record(
                PaymentOperationAuditService.ACTION_DELETE_METHOD,
                PaymentOperationAuditService.RESOURCE_PAYMENT_METHOD,
                entity.getMethodCode(),
                PaymentOperationAuditService.RESULT_SUCCESS);
        return R.ok(true);
    }

    private LambdaQueryWrapper<PaymentMethod> wrapper(PaymentConfigPageQuery query) {
        String keyword = PaymentContextSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<PaymentMethod>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(PaymentMethod::getMethodCode, keyword)
                        .or()
                        .like(PaymentMethod::getMethodName, keyword))
                .eq(query.getStatus() != null, PaymentMethod::getStatus, query.getStatus())
                .eq(PaymentMethod::getTenantId, PaymentContextSupport.currentTenantId())
                .orderByAsc(PaymentMethod::getSort)
                .orderByDesc(PaymentMethod::getUpdatedAt);
    }

    private PaymentMethod selectRequired(Long id) {
        Require.notNull(id, PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "支付方式 ID 不能为空");
        PaymentMethod entity = methodMapper.selectById(id);
        Require.notNull(entity, PaymentCode.PAYMENT_METHOD_NOT_FOUND);
        Require.isTrue(PaymentContextSupport.currentTenantId().equals(entity.getTenantId()), PaymentCode.PAYMENT_METHOD_NOT_FOUND);
        return entity;
    }

    private void validate(SavePaymentMethodCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "支付方式 ID 不能为空");
        }
        Require.notBlank(command.getMethodCode(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "支付方式编码不能为空");
        Require.notBlank(command.getMethodName(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "支付方式名称不能为空");
        Require.notBlank(command.getAccountNature(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "一级分类不能为空");
        Require.notBlank(command.getInstrumentType(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "二级分类不能为空");
        Require.notBlank(command.getInteractionType(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "三级分类不能为空");
        Require.notBlank(command.getTerminalScope(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "终端范围不能为空");
        Require.notBlank(command.getPaymentMaterialType(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "支付物料类型不能为空");
        Require.notBlank(command.getCashierGroupCode(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "收银台展示分组编码不能为空");
        Require.notBlank(command.getCashierGroupName(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "收银台展示分组名称不能为空");
        Require.notNull(command.getStatus(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "状态不能为空");
        Require.isTrue(STATUS_VALUES.contains(command.getStatus()), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "状态不正确");
        Require.isTrue(command.getRequiresBankSelection() == null || STATUS_VALUES.contains(command.getRequiresBankSelection()), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "是否需要银行列表不正确");
        Require.isTrue(command.getRequiresQrRefresh() == null || STATUS_VALUES.contains(command.getRequiresQrRefresh()), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "二维码刷新开关不正确");
        Require.isTrue(PAYMENT_MATERIAL_TYPES.contains(command.getPaymentMaterialType().trim()), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "支付物料类型不正确");
        validateCategoryPath(command);
        validateTerminalScope(command.getTerminalScope());
        validateUniqueMethodCode(command, update);
    }

    private void validateCategoryPath(SavePaymentMethodCommand command) {
        PaymentMethodCategory accountNature = selectCategory(command.getAccountNature().trim(), 1, ROOT_CATEGORY_PARENT_ID);
        PaymentMethodCategory instrumentType = selectCategory(command.getInstrumentType().trim(), 2, accountNature.getId());
        selectCategory(command.getInteractionType().trim(), 3, instrumentType.getId());
    }

    private PaymentMethodCategory selectCategory(String categoryCode, int level, Long parentId) {
        PaymentMethodCategory category = categoryMapper.selectOne(new LambdaQueryWrapper<PaymentMethodCategory>()
                .eq(PaymentMethodCategory::getTenantId, PaymentContextSupport.currentTenantId())
                .eq(PaymentMethodCategory::getCategoryCode, categoryCode)
                .eq(PaymentMethodCategory::getLevel, level)
                .eq(PaymentMethodCategory::getParentId, parentId)
                .eq(PaymentMethodCategory::getStatus, 1)
                .last("limit 1"));
        Require.notNull(category, PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "支付方式三级分类不正确");
        return category;
    }

    private void validateTerminalScope(String terminalScope) {
        List<String> terminals = splitCsv(terminalScope);
        Require.isTrue(!terminals.isEmpty(), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "终端范围不能为空");
        for (String terminal : terminals) {
            Require.isTrue(TERMINAL_TYPES.contains(terminal), PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "终端范围仅支持 WEB、H5");
        }
    }

    private void validateUniqueMethodCode(SavePaymentMethodCommand command, boolean update) {
        LambdaQueryWrapper<PaymentMethod> wrapper = new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getTenantId, PaymentContextSupport.currentTenantId())
                .eq(PaymentMethod::getMethodCode, command.getMethodCode().trim());
        if (update) {
            wrapper.ne(PaymentMethod::getId, command.getId());
        }
        Long count = methodMapper.selectCount(wrapper);
        Require.isTrue(count == null || count == 0L, PaymentCode.PAYMENT_METHOD_INVALID.getCode(), "支付方式编码不能重复");
    }

    private List<String> splitCsv(String value) {
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private long countDeleteRelations(PaymentMethod entity) {
        return methodMapper.countDeleteRelations(entity.getTenantId(), entity.getId(), entity.getMethodCode());
    }

    private void copy(SavePaymentMethodCommand command, PaymentMethod entity) {
        entity.setMethodCode(command.getMethodCode().trim());
        entity.setMethodName(command.getMethodName().trim());
        entity.setChannelId(null);
        entity.setAccountNature(command.getAccountNature().trim());
        entity.setInstrumentType(command.getInstrumentType().trim());
        entity.setInteractionType(command.getInteractionType().trim());
        entity.setTerminalScope(command.getTerminalScope().trim());
        entity.setPaymentMaterialType(command.getPaymentMaterialType().trim());
        entity.setCashierGroupCode(command.getCashierGroupCode().trim());
        entity.setCashierGroupName(command.getCashierGroupName().trim());
        entity.setCashierGroupSort(command.getCashierGroupSort() == null ? 0 : command.getCashierGroupSort());
        entity.setIconFileId(command.getIconFileId());
        entity.setRequiresBankSelection(command.getRequiresBankSelection() == null ? 0 : command.getRequiresBankSelection());
        entity.setRequiresQrRefresh(command.getRequiresQrRefresh() == null ? 0 : command.getRequiresQrRefresh());
        entity.setDescription(PaymentContextSupport.trimToNull(command.getDescription()));
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setStatus(command.getStatus());
    }

    private PaymentMethodVO toVO(PaymentMethod entity) {
        PaymentMethodVO vo = new PaymentMethodVO();
        vo.setId(entity.getId());
        vo.setMethodCode(entity.getMethodCode());
        vo.setMethodName(entity.getMethodName());
        vo.setAccountNature(entity.getAccountNature());
        vo.setInstrumentType(entity.getInstrumentType());
        vo.setInteractionType(entity.getInteractionType());
        vo.setTerminalScope(entity.getTerminalScope());
        vo.setPaymentMaterialType(entity.getPaymentMaterialType());
        vo.setCashierGroupCode(entity.getCashierGroupCode());
        vo.setCashierGroupName(entity.getCashierGroupName());
        vo.setCashierGroupSort(entity.getCashierGroupSort());
        vo.setIconFileId(entity.getIconFileId());
        vo.setRequiresBankSelection(entity.getRequiresBankSelection());
        vo.setRequiresQrRefresh(entity.getRequiresQrRefresh());
        vo.setDescription(entity.getDescription());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private PaymentMethodCategoryVO toCategoryVO(PaymentMethodCategory entity) {
        PaymentMethodCategoryVO vo = new PaymentMethodCategoryVO();
        vo.setId(entity.getId());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setLevel(entity.getLevel());
        vo.setParentId(entity.getParentId());
        vo.setSort(entity.getSort());
        return vo;
    }
}
