package io.mango.job.core.service;

import io.mango.common.vo.PageResult;
import io.mango.job.api.command.SaveMangoJobAlarmRuleCommand;
import io.mango.job.api.command.UpdateMangoJobAlarmRuleStatusCommand;
import io.mango.job.api.query.MangoJobAlarmRulePageQuery;
import io.mango.job.api.vo.MangoJobAlarmRuleVO;

/**
 * Job 告警规则内部服务。
 */
public interface IMangoJobAlarmRuleService {

    /**
     * 分页查询告警规则。
     *
     * @param query 查询条件
     * @return 告警规则分页结果
     */
    PageResult<MangoJobAlarmRuleVO> pageAlarmRules(MangoJobAlarmRulePageQuery query);

    /**
     * 查询告警规则详情。
     *
     * @param id 告警规则 ID
     * @return 告警规则详情
     */
    MangoJobAlarmRuleVO detailAlarmRule(Long id);

    /**
     * 创建告警规则。
     *
     * @param command 保存命令
     * @return 告警规则 ID
     */
    Long createAlarmRule(SaveMangoJobAlarmRuleCommand command);

    /**
     * 更新告警规则。
     *
     * @param command 保存命令
     * @return true 表示更新成功
     */
    Boolean updateAlarmRule(SaveMangoJobAlarmRuleCommand command);

    /**
     * 更新告警规则启停状态。
     *
     * @param command 状态命令
     * @return true 表示更新成功
     */
    Boolean updateAlarmRuleStatus(UpdateMangoJobAlarmRuleStatusCommand command);

    /**
     * 删除告警规则。
     *
     * @param id 告警规则 ID
     * @return true 表示删除成功
     */
    Boolean deleteAlarmRule(Long id);
}
