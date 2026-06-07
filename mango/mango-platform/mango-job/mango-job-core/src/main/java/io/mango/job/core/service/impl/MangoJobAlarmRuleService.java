package io.mango.job.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.job.api.command.SaveMangoJobAlarmRuleCommand;
import io.mango.job.api.command.UpdateMangoJobAlarmRuleStatusCommand;
import io.mango.job.api.query.MangoJobAlarmRulePageQuery;
import io.mango.job.api.vo.MangoJobAlarmRuleVO;
import io.mango.job.core.entity.MangoJobAlarmRuleEntity;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.mapper.MangoJobAlarmRuleMapper;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.job.core.service.IMangoJobAlarmRuleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Job 告警规则内部服务实现。
 */
@Service
public class MangoJobAlarmRuleService implements IMangoJobAlarmRuleService {

    private static final String ALARM_TYPE_INSTANCE_FAILED = "INSTANCE_FAILED";

    private static final String DEFAULT_TRIGGER_CONDITION = "{\"status\":\"FAILED\"}";

    private final MangoJobAlarmRuleMapper alarmRuleMapper;

    private final MangoJobDefinitionMapper definitionMapper;

    private final MangoJobDataSourceRouter dataSourceRouter;

    private final ObjectMapper objectMapper;

    public MangoJobAlarmRuleService(MangoJobAlarmRuleMapper alarmRuleMapper,
                                    MangoJobDefinitionMapper definitionMapper,
                                    MangoJobDataSourceRouter dataSourceRouter,
                                    ObjectMapper objectMapper) {
        this.alarmRuleMapper = alarmRuleMapper;
        this.definitionMapper = definitionMapper;
        this.dataSourceRouter = dataSourceRouter;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResult<MangoJobAlarmRuleVO> pageAlarmRules(MangoJobAlarmRulePageQuery query) {
        return dataSourceRouter.route(() -> {
            MangoJobAlarmRulePageQuery resolved = query == null ? new MangoJobAlarmRulePageQuery() : query;
            IPage<MangoJobAlarmRuleEntity> page = alarmRuleMapper.selectPage(
                    new Page<>(resolved.getPage(), resolved.getSize()),
                    alarmRuleWrapper(resolved));
            Map<Long, MangoJobDefinitionEntity> definitions = selectDefinitions(page.getRecords());
            List<MangoJobAlarmRuleVO> records = page.getRecords().stream()
                    .map(rule -> toAlarmRuleVO(rule, definitions.get(rule.getJobId())))
                    .toList();
            return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
        });
    }

    @Override
    public MangoJobAlarmRuleVO detailAlarmRule(Long id) {
        return dataSourceRouter.route(() -> {
            MangoJobAlarmRuleEntity rule = selectAlarmRuleRequired(id);
            return toAlarmRuleVO(rule, selectDefinition(rule.getJobId(), rule.getTenantId()));
        });
    }

    @Override
    public Long createAlarmRule(SaveMangoJobAlarmRuleCommand command) {
        return dataSourceRouter.route(() -> {
            Require.notNull(command, "告警规则不能为空");
            validateAlarmRule(command, false);
            String tenantId = MangoJobSupport.currentTenantId();
            MangoJobDefinitionEntity definition = selectDefinition(command.getJobId(), tenantId);
            validateJobScope(command, definition);

            MangoJobAlarmRuleEntity entity = new MangoJobAlarmRuleEntity();
            copyAlarmRule(command, entity, definition);
            entity.setTenantId(tenantId);
            alarmRuleMapper.insert(entity);
            return entity.getId();
        });
    }

    @Override
    public Boolean updateAlarmRule(SaveMangoJobAlarmRuleCommand command) {
        return dataSourceRouter.route(() -> {
            Require.notNull(command, "告警规则不能为空");
            Require.notNull(command.getId(), "告警规则 ID 不能为空");
            validateAlarmRule(command, true);
            MangoJobAlarmRuleEntity entity = selectAlarmRuleRequired(command.getId());
            MangoJobDefinitionEntity definition = selectDefinition(command.getJobId(), entity.getTenantId());
            validateJobScope(command, definition);
            copyAlarmRule(command, entity, definition);
            return alarmRuleMapper.updateById(entity) > 0;
        });
    }

    @Override
    public Boolean updateAlarmRuleStatus(UpdateMangoJobAlarmRuleStatusCommand command) {
        return dataSourceRouter.route(() -> {
            Require.notNull(command, "告警规则状态不能为空");
            Require.notNull(command.getId(), "告警规则 ID 不能为空");
            Require.notNull(command.getEnabled(), "启用状态不能为空");
            MangoJobAlarmRuleEntity entity = selectAlarmRuleRequired(command.getId());
            entity.setEnabled(Boolean.TRUE.equals(command.getEnabled()) ? 1 : 0);
            return alarmRuleMapper.updateById(entity) > 0;
        });
    }

    @Override
    public Boolean deleteAlarmRule(Long id) {
        return dataSourceRouter.route(() -> {
            selectAlarmRuleRequired(id);
            return alarmRuleMapper.deleteById(id) > 0;
        });
    }

    private LambdaQueryWrapper<MangoJobAlarmRuleEntity> alarmRuleWrapper(MangoJobAlarmRulePageQuery query) {
        String tenantId = MangoJobSupport.currentTenantId();
        String keyword = MangoJobSupport.trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<MangoJobAlarmRuleEntity>()
                .eq(MangoJobAlarmRuleEntity::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.getAppCode()), MangoJobAlarmRuleEntity::getAppCode, query.getAppCode())
                .eq(query.getJobId() != null, MangoJobAlarmRuleEntity::getJobId, query.getJobId())
                .eq(StringUtils.hasText(query.getAlarmType()), MangoJobAlarmRuleEntity::getAlarmType, query.getAlarmType())
                .eq(query.getEnabled() != null, MangoJobAlarmRuleEntity::getEnabled,
                        Boolean.TRUE.equals(query.getEnabled()) ? 1 : 0)
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(MangoJobAlarmRuleEntity::getRuleName, keyword)
                        .or()
                        .like(MangoJobAlarmRuleEntity::getNoticeSceneCode, keyword)
                        .or()
                        .like(MangoJobAlarmRuleEntity::getNoticeTemplateCode, keyword))
                .orderByDesc(MangoJobAlarmRuleEntity::getUpdatedAt);
    }

    private void validateAlarmRule(SaveMangoJobAlarmRuleCommand command, boolean update) {
        if (update) {
            Require.notNull(command.getId(), "告警规则 ID 不能为空");
        }
        Require.notBlank(command.getAppCode(), "所属应用不能为空");
        Require.notBlank(command.getRuleName(), "规则名称不能为空");
        Require.notBlank(command.getAlarmType(), "告警类型不能为空");
        Require.isTrue(ALARM_TYPE_INSTANCE_FAILED.equals(command.getAlarmType().trim()),
                "当前版本仅支持 INSTANCE_FAILED 告警");
        Require.notBlank(command.getNoticeSceneCode(), "通知场景编码不能为空");
        Require.notBlank(command.getNoticeTemplateCode(), "通知模板编码不能为空");
        validateJson(command.getTriggerCondition(), "触发条件 JSON 不合法");
        validateJson(command.getNoticeParams(), "通知参数 JSON 不合法");
    }

    private void validateJson(String json, String message) {
        if (!StringUtils.hasText(json)) {
            return;
        }
        try {
            objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            Require.fail(400, message);
        }
    }

    private void validateJobScope(SaveMangoJobAlarmRuleCommand command, MangoJobDefinitionEntity definition) {
        if (definition == null) {
            return;
        }
        Require.isTrue(definition.getAppCode().equals(command.getAppCode().trim()),
                "告警规则所属应用必须与任务所属应用一致");
    }

    private void copyAlarmRule(SaveMangoJobAlarmRuleCommand command,
                               MangoJobAlarmRuleEntity entity,
                               MangoJobDefinitionEntity definition) {
        entity.setJobId(definition == null ? null : definition.getId());
        entity.setAppCode(MangoJobSupport.normalizeRequired(command.getAppCode(), "所属应用不能为空"));
        entity.setRuleName(MangoJobSupport.normalizeRequired(command.getRuleName(), "规则名称不能为空"));
        entity.setAlarmType(ALARM_TYPE_INSTANCE_FAILED);
        entity.setTriggerCondition(StringUtils.hasText(command.getTriggerCondition())
                ? command.getTriggerCondition().trim()
                : DEFAULT_TRIGGER_CONDITION);
        entity.setNoticeSceneCode(MangoJobSupport.normalizeRequired(command.getNoticeSceneCode(), "通知场景编码不能为空"));
        entity.setNoticeTemplateCode(MangoJobSupport.normalizeRequired(command.getNoticeTemplateCode(), "通知模板编码不能为空"));
        entity.setNoticeParams(MangoJobSupport.trimToNull(command.getNoticeParams()));
        entity.setEnabled(Boolean.FALSE.equals(command.getEnabled()) ? 0 : 1);
    }

    private MangoJobAlarmRuleEntity selectAlarmRuleRequired(Long id) {
        Require.notNull(id, "告警规则 ID 不能为空");
        MangoJobAlarmRuleEntity entity = alarmRuleMapper.selectById(id);
        Require.notNull(entity, 404, "告警规则不存在");
        Require.isTrue(MangoJobSupport.currentTenantId().equals(entity.getTenantId()), 404, "告警规则不存在");
        return entity;
    }

    private MangoJobDefinitionEntity selectDefinition(Long jobId, String tenantId) {
        if (jobId == null) {
            return null;
        }
        MangoJobDefinitionEntity definition = definitionMapper.selectById(jobId);
        Require.notNull(definition, 404, "任务不存在");
        Require.isTrue(tenantId.equals(definition.getTenantId()), 404, "任务不存在");
        return definition;
    }

    private Map<Long, MangoJobDefinitionEntity> selectDefinitions(List<MangoJobAlarmRuleEntity> rules) {
        List<Long> jobIds = rules.stream()
                .map(MangoJobAlarmRuleEntity::getJobId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (jobIds.isEmpty()) {
            return Map.of();
        }
        String tenantId = MangoJobSupport.currentTenantId();
        return definitionMapper.selectList(new LambdaQueryWrapper<MangoJobDefinitionEntity>()
                        .eq(MangoJobDefinitionEntity::getTenantId, tenantId)
                        .in(MangoJobDefinitionEntity::getId, jobIds))
                .stream()
                .collect(Collectors.toMap(MangoJobDefinitionEntity::getId, Function.identity()));
    }

    private MangoJobAlarmRuleVO toAlarmRuleVO(MangoJobAlarmRuleEntity entity, MangoJobDefinitionEntity definition) {
        MangoJobAlarmRuleVO vo = new MangoJobAlarmRuleVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setJobId(entity.getJobId());
        vo.setAppCode(entity.getAppCode());
        vo.setRuleName(entity.getRuleName());
        vo.setAlarmType(entity.getAlarmType());
        vo.setTriggerCondition(entity.getTriggerCondition());
        vo.setNoticeSceneCode(entity.getNoticeSceneCode());
        vo.setNoticeTemplateCode(entity.getNoticeTemplateCode());
        vo.setNoticeParams(entity.getNoticeParams());
        vo.setEnabled(Integer.valueOf(1).equals(entity.getEnabled()));
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedBy(entity.getUpdatedBy());
        vo.setUpdatedAt(entity.getUpdatedAt());
        if (definition != null) {
            vo.setJobCode(definition.getJobCode());
            vo.setJobName(definition.getJobName());
        }
        return vo;
    }
}
