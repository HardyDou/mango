package io.mango.numgen.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.numgen.api.command.SaveNumgenGeneratorCommand;
import io.mango.numgen.api.command.UpdateNumgenGeneratorStatusCommand;
import io.mango.numgen.api.query.NumgenGeneratorPageQuery;
import io.mango.numgen.api.vo.NumgenGeneratorVO;
import io.mango.numgen.core.entity.NumgenGenerator;
import io.mango.numgen.core.mapper.NumgenGeneratorMapper;
import io.mango.numgen.core.mapper.NumgenRuleMapper;
import io.mango.numgen.core.service.INumgenGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NumgenGeneratorServiceImpl implements INumgenGeneratorService {

    private final NumgenGeneratorMapper generatorMapper;
    private final NumgenRuleMapper ruleMapper;

    @Override
    public R<PageResult<NumgenGeneratorVO>> pageGenerators(NumgenGeneratorPageQuery query) {
        NumgenGeneratorPageQuery resolved = query == null ? new NumgenGeneratorPageQuery() : query;
        IPage<NumgenGenerator> page = generatorMapper.selectPage(new Page<>(resolved.getPage(), resolved.getSize()), wrapper(resolved));
        List<NumgenGeneratorVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<NumgenGeneratorVO> detailGenerator(Long id) {
        return R.ok(toVO(selectRequired(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createGenerator(SaveNumgenGeneratorCommand command) {
        Require.notNull(command, "编号生成器不能为空");
        validate(command, false);
        Long tenantId = NumgenContextSupport.currentTenantId();
        Require.isTrue(generatorMapper.selectByKey(command.getGenKey().trim(), tenantId) == null, "业务 Key 已存在");
        NumgenGenerator entity = new NumgenGenerator();
        copy(command, entity);
        entity.setCurrentPublishStatus(0);
        entity.setCurrentRuleVersion(null);
        entity.setTenantId(tenantId);
        generatorMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateGenerator(SaveNumgenGeneratorCommand command) {
        Require.notNull(command, "编号生成器不能为空");
        Require.notNull(command.getId(), "编号生成器 ID 不能为空");
        validate(command, true);
        NumgenGenerator entity = selectRequired(command.getId());
        if (!entity.getGenKey().equals(command.getGenKey().trim())) {
            Require.isTrue(generatorMapper.selectByKey(command.getGenKey().trim(), entity.getTenantId()) == null, "业务 Key 已存在");
        }
        copy(command, entity);
        return R.ok(generatorMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateGeneratorStatus(UpdateNumgenGeneratorStatusCommand command) {
        Require.notNull(command, "状态命令不能为空");
        Require.notNull(command.getId(), "编号生成器 ID 不能为空");
        Require.notNull(command.getStatus(), "状态不能为空");
        NumgenGenerator entity = selectRequired(command.getId());
        entity.setStatus(command.getStatus());
        return R.ok(generatorMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> deleteGenerator(Long id) {
        selectRequired(id);
        return R.ok(generatorMapper.deleteById(id) > 0);
    }

    private LambdaQueryWrapper<NumgenGenerator> wrapper(NumgenGeneratorPageQuery query) {
        String keyword = NumgenContextSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<NumgenGenerator>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(NumgenGenerator::getGenKey, keyword)
                        .or()
                        .like(NumgenGenerator::getGenName, keyword))
                .eq(query.getStatus() != null, NumgenGenerator::getStatus, query.getStatus())
                .eq(NumgenGenerator::getTenantId, NumgenContextSupport.currentTenantId())
                .orderByDesc(NumgenGenerator::getUpdateTime);
    }

    private NumgenGenerator selectRequired(Long id) {
        Require.notNull(id, "编号生成器 ID 不能为空");
        NumgenGenerator entity = generatorMapper.selectById(id);
        Require.notNull(entity, "编号生成器不存在");
        Require.isTrue(NumgenContextSupport.currentTenantId().equals(entity.getTenantId()), "编号生成器不存在");
        return entity;
    }

    private void validate(SaveNumgenGeneratorCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), "编号生成器 ID 不能为空");
        }
        Require.notBlank(command.getGenKey(), "业务 Key 不能为空");
        Require.notBlank(command.getGenName(), "名称不能为空");
        Require.notNull(command.getStatus(), "状态不能为空");
    }

    private void copy(SaveNumgenGeneratorCommand command, NumgenGenerator entity) {
        entity.setGenKey(command.getGenKey().trim());
        entity.setGenName(command.getGenName().trim());
        entity.setStatus(command.getStatus());
    }

    private NumgenGeneratorVO toVO(NumgenGenerator entity) {
        NumgenGeneratorVO vo = new NumgenGeneratorVO();
        vo.setId(entity.getId());
        vo.setGenKey(entity.getGenKey());
        vo.setGenName(entity.getGenName());
        vo.setStatus(entity.getStatus());
        vo.setCurrentRuleVersion(entity.getCurrentRuleVersion());
        vo.setCurrentPublishStatus(entity.getCurrentPublishStatus());
        vo.setHasUnpublishedChanges(ruleMapper.selectLatestDraftByGenKey(entity.getGenKey(), entity.getTenantId()) != null);
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
