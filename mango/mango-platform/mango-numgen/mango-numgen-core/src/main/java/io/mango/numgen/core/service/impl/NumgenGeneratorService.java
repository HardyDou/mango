package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.vo.DomainVO;
import io.mango.numgen.api.command.SaveNumgenGeneratorCommand;
import io.mango.numgen.api.command.UpdateNumgenGeneratorStatusCommand;
import io.mango.numgen.api.enums.NumgenCode;
import io.mango.numgen.api.query.NumgenGeneratorPageQuery;
import io.mango.numgen.api.vo.NumgenGeneratorVO;
import io.mango.numgen.core.entity.NumgenGeneratorEntity;
import io.mango.numgen.core.mapper.NumgenGeneratorMapper;
import io.mango.numgen.core.mapper.NumgenRuleMapper;
import io.mango.numgen.core.service.INumgenGeneratorService;
import io.mango.numgen.core.service.support.NumgenDomainSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NumgenGeneratorService implements INumgenGeneratorService {

    private final NumgenGeneratorMapper generatorMapper;
    private final NumgenRuleMapper ruleMapper;
    private final NumgenDomainSupport domainSupport;

    @Override
    public PageResult<NumgenGeneratorVO> pageGenerators(NumgenGeneratorPageQuery query) {
        NumgenGeneratorPageQuery resolved = query == null ? new NumgenGeneratorPageQuery() : query;
        IPage<NumgenGeneratorEntity> page = generatorMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper(resolved));
        List<NumgenGeneratorVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public NumgenGeneratorVO detailGenerator(Long id) {
        return toVO(selectRequired(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGenerator(SaveNumgenGeneratorCommand command) {
        Require.notNull(command, NumgenCode.NUMGEN_INVALID, "编号生成器不能为空");
        validate(command, false);
        String tenantId = NumgenContextSupport.currentTenantId();
        Require.isTrue(selectByKey(command.getGenKey().trim(), tenantId) == null,
                NumgenCode.NUMGEN_GENERATOR_KEY_DUPLICATED);
        NumgenGeneratorEntity entity = new NumgenGeneratorEntity();
        entity.setTenantId(tenantId);
        copy(command, entity);
        entity.setCurrentPublishStatus(0);
        entity.setCurrentRuleVersion(null);
        generatorMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateGenerator(SaveNumgenGeneratorCommand command) {
        Require.notNull(command, NumgenCode.NUMGEN_INVALID, "编号生成器不能为空");
        Require.notNull(command.getId(), NumgenCode.NUMGEN_INVALID, "编号生成器 ID 不能为空");
        validate(command, true);
        NumgenGeneratorEntity entity = selectRequired(command.getId());
        Require.isTrue(entity.getGenKey().equals(command.getGenKey().trim()),
                NumgenCode.NUMGEN_GENERATOR_KEY_IMMUTABLE);
        copy(command, entity);
        return generatorMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateGeneratorStatus(UpdateNumgenGeneratorStatusCommand command) {
        Require.notNull(command, NumgenCode.NUMGEN_INVALID, "状态命令不能为空");
        Require.notNull(command.getId(), NumgenCode.NUMGEN_INVALID, "编号生成器 ID 不能为空");
        Require.notNull(command.getStatus(), NumgenCode.NUMGEN_INVALID, "状态不能为空");
        NumgenGeneratorEntity entity = selectRequired(command.getId());
        entity.setStatus(command.getStatus());
        return generatorMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteGenerator(Long id) {
        Require.notNull(id, NumgenCode.NUMGEN_INVALID, "编号生成器 ID 不能为空");
        selectRequired(id);
        return generatorMapper.deleteById(id) > 0;
    }

    private LambdaQueryWrapper<NumgenGeneratorEntity> wrapper(NumgenGeneratorPageQuery query) {
        String keyword = NumgenContextSupport.trimToNull(query.getKeyword());
        String domainCode = NumgenContextSupport.trimToNull(query.getDomainCode());
        return new LambdaQueryWrapper<NumgenGeneratorEntity>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(NumgenGeneratorEntity::getGenKey, keyword)
                        .or()
                        .like(NumgenGeneratorEntity::getGenName, keyword))
                .eq(StringUtils.hasText(domainCode), NumgenGeneratorEntity::getDomainCode, domainCode)
                .eq(query.getStatus() != null, NumgenGeneratorEntity::getStatus, query.getStatus())
                .eq(NumgenGeneratorEntity::getTenantId, NumgenContextSupport.currentTenantId())
                .orderByDesc(NumgenGeneratorEntity::getUpdatedAt);
    }

    private NumgenGeneratorEntity selectRequired(Long id) {
        Require.notNull(id, NumgenCode.NUMGEN_INVALID, "编号生成器 ID 不能为空");
        NumgenGeneratorEntity entity = generatorMapper.selectById(id);
        Require.notNull(entity, NumgenCode.NUMGEN_GENERATOR_NOT_FOUND);
        Require.isTrue(NumgenContextSupport.currentTenantId().equals(entity.getTenantId()),
                NumgenCode.NUMGEN_GENERATOR_NOT_FOUND);
        return entity;
    }

    private void validate(SaveNumgenGeneratorCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), NumgenCode.NUMGEN_INVALID, "编号生成器 ID 不能为空");
        }
        Require.notBlank(command.getGenKey(), NumgenCode.NUMGEN_INVALID, "业务 Key 不能为空");
        Require.notBlank(command.getGenName(), NumgenCode.NUMGEN_INVALID, "名称不能为空");
        validateDomain(command.getDomainCode());
        Require.notNull(command.getStatus(), NumgenCode.NUMGEN_INVALID, "状态不能为空");
    }

    private void copy(SaveNumgenGeneratorCommand command, NumgenGeneratorEntity entity) {
        entity.setGenKey(command.getGenKey().trim());
        entity.setGenName(command.getGenName().trim());
        entity.setDomainCode(validateDomain(command.getDomainCode()).getDomainCode());
        entity.setStatus(command.getStatus());
    }

    private NumgenGeneratorVO toVO(NumgenGeneratorEntity entity) {
        DomainVO domain = getDomain(entity.getDomainCode());
        NumgenGeneratorVO vo = new NumgenGeneratorVO();
        vo.setId(entity.getId());
        vo.setGenKey(entity.getGenKey());
        vo.setGenName(entity.getGenName());
        vo.setDomainCode(entity.getDomainCode());
        vo.setDomainName(domain == null ? null : domain.getDomainName());
        vo.setStatus(entity.getStatus());
        vo.setCurrentRuleVersion(entity.getCurrentRuleVersion());
        vo.setCurrentPublishStatus(entity.getCurrentPublishStatus());
        vo.setHasUnpublishedChanges(ruleMapper.selectLatestDraftByGenKey(entity.getGenKey(), entity.getTenantId()) != null);
        vo.setCreateTime(entity.getCreatedAt());
        vo.setUpdateTime(entity.getUpdatedAt());
        return vo;
    }

    private NumgenGeneratorEntity selectByKey(String genKey, String tenantId) {
        return generatorMapper.selectOne(new LambdaQueryWrapper<NumgenGeneratorEntity>()
                .eq(NumgenGeneratorEntity::getGenKey, genKey)
                .eq(NumgenGeneratorEntity::getTenantId, tenantId));
    }

    private DomainVO validateDomain(String domainCode) {
        Require.notBlank(domainCode, NumgenCode.NUMGEN_DOMAIN_INVALID, "业务域不能为空");
        DomainVO domain = domainSupport.getDomain(domainCode);
        Require.notNull(domain, NumgenCode.NUMGEN_DOMAIN_INVALID, "业务域不存在");
        Require.isTrue(Integer.valueOf(1).equals(domain.getStatus()),
                NumgenCode.NUMGEN_DOMAIN_INVALID, "业务域已停用");
        return domain;
    }

    private DomainVO getDomain(String domainCode) {
        return domainSupport.getDomain(domainCode);
    }
}
