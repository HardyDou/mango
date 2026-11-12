package io.mango.workflow.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.mango.workflow.core.entity.WorkflowTaskHistoryEntity;
import io.mango.workflow.core.model.WorkflowTaskHistoryCriteria;
import io.mango.workflow.core.model.WorkflowTaskHistoryRow;
import io.mango.workflow.core.model.WorkflowTaskIdentityLinkRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务历史只读查询 Mapper。
 *
 * <p>直接查询 Flowable 历史任务表并关联业务申请单，用于按业务主键批量读取全部任务。
 * 只读、不改引擎表结构；Flowable 升级时必须回归该查询与引擎 API 的结果一致性。
 */
@Mapper
public interface WorkflowTaskHistoryMapper extends BaseMapper<WorkflowTaskHistoryEntity> {

    /**
     * 统计符合条件的任务数量。
     *
     * @param criteria 查询条件
     * @return 任务数量
     */
    long countTasks(@Param("c") WorkflowTaskHistoryCriteria criteria);

    /**
     * 分页查询符合条件的任务。
     *
     * @param criteria 查询条件
     * @param limit    返回行数
     * @param offset   起始行
     * @return 任务行
     */
    List<WorkflowTaskHistoryRow> selectTasks(@Param("c") WorkflowTaskHistoryCriteria criteria,
                                             @Param("limit") int limit,
                                             @Param("offset") long offset);

    /**
     * 批量查询任务的候选身份关联。
     *
     * @param taskIds 任务 ID 集合，调用方必须保证非空
     * @return 候选身份关联行
     */
    List<WorkflowTaskIdentityLinkRow> selectCandidateLinks(@Param("taskIds") List<String> taskIds);
}
