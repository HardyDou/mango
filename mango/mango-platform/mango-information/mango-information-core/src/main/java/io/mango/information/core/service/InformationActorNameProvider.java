package io.mango.information.core.service;

import java.util.Collection;
import java.util.Map;

/**
 * 平台人员显示名查询端口。
 * <p>
 * 信息采集模块只依赖该端口展示创建人和处理人名称；具体实现由 starter 适配平台身份能力，
 * 未装配时展示回退值，不阻断列表和详情。
 */
public interface InformationActorNameProvider {

    /**
     * 批量查询人员显示名。
     *
     * @param userIds 人员标识集合
     * @return 人员标识到显示名的映射，缺失的人员不在结果中
     */
    Map<Long, String> resolveNames(Collection<Long> userIds);
}
